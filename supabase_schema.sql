-- ==============================================================================
-- Project: Aegis Focus — Reclaim Your Focus
-- Database Schema & Server-Authoritative Security for Supabase PostgreSQL
-- ==============================================================================

-- 1. Profiles Table (Linked to Supabase Auth + RBAC)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
    email TEXT NOT NULL,
    display_name TEXT,
    role TEXT DEFAULT 'user' NOT NULL, -- 'user', 'admin', 'super_admin'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Ensure role column exists if profiles already created
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'user' NOT NULL;

-- 2. Subscriptions Table (Pro Entitlements)
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL UNIQUE,
    plan_type TEXT DEFAULT 'FREE' NOT NULL, -- FREE, PRO_MONTHLY, PRO_YEARLY, LIFETIME, STUDENT_YEARLY
    status TEXT DEFAULT 'active' NOT NULL, -- active, expired, cancelled, revoked
    license_key TEXT,
    expiry_epoch_ms BIGINT DEFAULT 0 NOT NULL, -- 0 = lifetime or free
    started_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    activated_by UUID REFERENCES public.profiles(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Ensure activated_by column exists
ALTER TABLE public.subscriptions ADD COLUMN IF NOT EXISTS activated_by UUID REFERENCES public.profiles(id);

-- 3. Payment Requests Table (Manual Admin Verification)
CREATE TABLE IF NOT EXISTS public.payment_requests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    user_email TEXT NOT NULL,
    payment_method TEXT NOT NULL, -- 'bKash', 'Nagad', 'Rocket'
    sender_number TEXT NOT NULL,
    transaction_id TEXT NOT NULL,
    amount TEXT NOT NULL,
    currency TEXT DEFAULT 'BDT' NOT NULL,
    plan_requested TEXT NOT NULL, -- 'PRO_MONTHLY', 'PRO_YEARLY', 'LIFETIME', 'STUDENT_YEARLY'
    status TEXT DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'VERIFIED', 'REJECTED', 'CANCELLED'
    rejection_reason TEXT,
    reviewed_by UUID REFERENCES public.profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_method_trx_id UNIQUE (payment_method, transaction_id)
);

-- 4. User Discipline & Stats Cloud Sync
CREATE TABLE IF NOT EXISTS public.user_stats_sync (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL UNIQUE,
    total_focus_minutes INT DEFAULT 0 NOT NULL,
    discipline_score INT DEFAULT 80 NOT NULL,
    current_streak INT DEFAULT 0 NOT NULL,
    longest_streak INT DEFAULT 0 NOT NULL,
    total_xp INT DEFAULT 0 NOT NULL,
    current_level TEXT DEFAULT 'NEW_STARTER' NOT NULL,
    last_synced_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. Central License Keys Pool (Protected Server-Side)
CREATE TABLE IF NOT EXISTS public.license_keys (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    key_code TEXT NOT NULL UNIQUE,
    tier TEXT NOT NULL, -- LIFE, YEAR, MNTH, STUD, VIP
    duration_days INT DEFAULT 0 NOT NULL, -- 0 = lifetime
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    redeemed_by UUID REFERENCES public.profiles(id),
    redeemed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ─── Row Level Security (RLS) ────────────────────────────────────────────────
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_stats_sync ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

-- Helper functions for RBAC
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role IN ('admin', 'super_admin')
    );
$$ LANGUAGE sql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.is_super_admin()
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'super_admin'
    );
$$ LANGUAGE sql SECURITY DEFINER;

-- Profiles Policies
DROP POLICY IF EXISTS "Users can view their own profile" ON public.profiles;
CREATE POLICY "Users can view their own profile" 
    ON public.profiles FOR SELECT 
    USING (auth.uid() = id OR public.is_admin());

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile" 
    ON public.profiles FOR UPDATE 
    USING (auth.uid() = id);

-- Subscriptions Policies (READ-ONLY for clients; modifications only via secure RPC)
DROP POLICY IF EXISTS "Users can view their own subscription" ON public.subscriptions;
CREATE POLICY "Users can view their own subscription" 
    ON public.subscriptions FOR SELECT 
    USING (auth.uid() = user_id OR public.is_admin());

-- Payment Requests Policies
DROP POLICY IF EXISTS "Users can insert their own payment requests" ON public.payment_requests;
CREATE POLICY "Users can insert their own payment requests"
    ON public.payment_requests FOR INSERT
    WITH CHECK (auth.uid() = user_id AND status = 'PENDING');

DROP POLICY IF EXISTS "Users can view their own payment requests" ON public.payment_requests;
CREATE POLICY "Users can view their own payment requests"
    ON public.payment_requests FOR SELECT
    USING (auth.uid() = user_id OR public.is_admin());

-- Stats Sync Policies
DROP POLICY IF EXISTS "Users can view their own stats" ON public.user_stats_sync;
CREATE POLICY "Users can view their own stats" 
    ON public.user_stats_sync FOR SELECT 
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert or update their own stats" ON public.user_stats_sync;
CREATE POLICY "Users can insert or update their own stats" 
    ON public.user_stats_sync FOR ALL 
    USING (auth.uid() = user_id);

-- License Keys
DROP POLICY IF EXISTS "Users can only view their already redeemed keys" ON public.license_keys;
CREATE POLICY "Users can only view their already redeemed keys" 
    ON public.license_keys FOR SELECT 
    USING (redeemed_by = auth.uid() OR public.is_admin());

-- ─── 1. Secure RPC: Submit Payment Request ────────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_payment_request(
    p_payment_method TEXT,
    p_sender_number TEXT,
    p_transaction_id TEXT,
    p_amount TEXT,
    p_plan_requested TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_user_email TEXT;
    v_clean_trx TEXT;
    v_clean_method TEXT;
    v_clean_plan TEXT;
    v_request_id UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Authentication required.');
    END IF;

    SELECT email INTO v_user_email FROM public.profiles WHERE id = v_user_id;
    IF v_user_email IS NULL THEN
        SELECT email INTO v_user_email FROM auth.users WHERE id = v_user_id;
    END IF;

    v_clean_trx := UPPER(TRIM(p_transaction_id));
    v_clean_method := TRIM(p_payment_method);
    v_clean_plan := UPPER(TRIM(p_plan_requested));

    IF length(v_clean_trx) < 4 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Please enter a valid Transaction ID.');
    END IF;

    -- Check duplicate active/verified request for this method + trx_id
    IF EXISTS (
        SELECT 1 FROM public.payment_requests
        WHERE payment_method = v_clean_method 
          AND transaction_id = v_clean_trx 
          AND status IN ('PENDING', 'VERIFIED')
    ) THEN
        RETURN jsonb_build_object('success', false, 'error', 'This Transaction ID has already been submitted.');
    END IF;

    INSERT INTO public.payment_requests (
        user_id,
        user_email,
        payment_method,
        sender_number,
        transaction_id,
        amount,
        plan_requested,
        status,
        submitted_at
    ) VALUES (
        v_user_id,
        v_user_email,
        v_clean_method,
        TRIM(p_sender_number),
        v_clean_trx,
        TRIM(p_amount),
        v_clean_plan,
        'PENDING',
        timezone('utc'::text, now())
    ) RETURNING id INTO v_request_id;

    RETURN jsonb_build_object(
        'success', true,
        'request_id', v_request_id,
        'status', 'PENDING',
        'message', 'Payment claim submitted. Awaiting Admin verification.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── 2. Secure RPC: Admin Approve Payment Request ────────────────────────────
CREATE OR REPLACE FUNCTION public.approve_payment_request(
    p_request_id UUID,
    p_plan_override TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_admin_id UUID;
    v_req RECORD;
    v_plan_to_activate TEXT;
    v_expiry_ms BIGINT := 0;
BEGIN
    v_admin_id := auth.uid();
    IF v_admin_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Authentication required.');
    END IF;

    -- Verify caller is admin or super_admin
    IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = v_admin_id AND role IN ('admin', 'super_admin')) THEN
        RETURN jsonb_build_object('success', false, 'error', 'Unauthorized. Admin privileges required.');
    END IF;

    -- Lock and fetch payment request
    SELECT * INTO v_req
    FROM public.payment_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Payment request not found.');
    END IF;

    IF v_req.status != 'PENDING' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Payment request is already ' || v_req.status);
    END IF;

    v_plan_to_activate := COALESCE(p_plan_override, v_req.plan_requested);

    -- If Lifetime, ensure caller is super_admin
    IF v_plan_to_activate = 'LIFETIME' THEN
        IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = v_admin_id AND role = 'super_admin') THEN
            RETURN jsonb_build_object('success', false, 'error', 'Only Super Admin can approve Lifetime plans.');
        END IF;
        v_expiry_ms := 0;
    ELSIF v_plan_to_activate = 'PRO_MONTHLY' THEN
        v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (30::BIGINT * 86400000);
    ELSE
        -- Default yearly / student yearly
        v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (365::BIGINT * 86400000);
    END IF;

    -- 1. Update Payment Request status
    UPDATE public.payment_requests
    SET status = 'VERIFIED',
        reviewed_by = v_admin_id,
        reviewed_at = timezone('utc'::text, now()),
        updated_at = timezone('utc'::text, now())
    WHERE id = v_req.id;

    -- 2. Atomically Upsert Subscription
    INSERT INTO public.subscriptions (
        user_id,
        plan_type,
        status,
        expiry_epoch_ms,
        activated_by,
        started_at,
        updated_at
    ) VALUES (
        v_req.user_id,
        v_plan_to_activate,
        'active',
        v_expiry_ms,
        v_admin_id,
        timezone('utc'::text, now()),
        timezone('utc'::text, now())
    )
    ON CONFLICT (user_id) DO UPDATE
    SET plan_type = EXCLUDED.plan_type,
        status = 'active',
        expiry_epoch_ms = EXCLUDED.expiry_epoch_ms,
        activated_by = EXCLUDED.activated_by,
        updated_at = timezone('utc'::text, now());

    RETURN jsonb_build_object(
        'success', true,
        'user_id', v_req.user_id,
        'plan', v_plan_to_activate,
        'expiry_epoch_ms', v_expiry_ms,
        'message', 'Payment verified and Pro activated successfully.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── 3. Secure RPC: Admin Reject Payment Request ─────────────────────────────
CREATE OR REPLACE FUNCTION public.reject_payment_request(
    p_request_id UUID,
    p_reason TEXT DEFAULT 'Transaction verification failed'
)
RETURNS JSONB AS $$
DECLARE
    v_admin_id UUID;
    v_req RECORD;
BEGIN
    v_admin_id := auth.uid();
    IF v_admin_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Authentication required.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = v_admin_id AND role IN ('admin', 'super_admin')) THEN
        RETURN jsonb_build_object('success', false, 'error', 'Unauthorized. Admin privileges required.');
    END IF;

    SELECT * INTO v_req
    FROM public.payment_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Payment request not found.');
    END IF;

    IF v_req.status != 'PENDING' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Payment request is already ' || v_req.status);
    END IF;

    UPDATE public.payment_requests
    SET status = 'REJECTED',
        rejection_reason = TRIM(p_reason),
        reviewed_by = v_admin_id,
        reviewed_at = timezone('utc'::text, now()),
        updated_at = timezone('utc'::text, now())
    WHERE id = v_req.id;

    RETURN jsonb_build_object(
        'success', true,
        'status', 'REJECTED',
        'message', 'Payment request has been rejected.'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── 4. Secure RPC: Redeem License Key ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.redeem_license_key(key_input TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_key_record RECORD;
    v_target_plan TEXT;
    v_expiry_ms BIGINT := 0;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Authentication required.');
    END IF;

    -- Look up active license key with row lock
    SELECT * INTO v_key_record
    FROM public.license_keys
    WHERE key_code = UPPER(TRIM(key_input))
      AND is_active = TRUE
      AND redeemed_by IS NULL
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Invalid or already redeemed license key.');
    END IF;

    -- Map tier to Subscription Plan
    CASE v_key_record.tier
        WHEN 'LIFE' THEN v_target_plan := 'LIFETIME'; v_expiry_ms := 0;
        WHEN 'YEAR' THEN v_target_plan := 'PRO_YEARLY'; v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (365::BIGINT * 86400000);
        WHEN 'STUD' THEN v_target_plan := 'STUDENT_YEARLY'; v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (365::BIGINT * 86400000);
        WHEN 'MNTH' THEN v_target_plan := 'PRO_MONTHLY'; v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (30::BIGINT * 86400000);
        WHEN 'VIP'  THEN v_target_plan := 'LIFETIME'; v_expiry_ms := 0;
        ELSE v_target_plan := 'PRO_YEARLY'; v_expiry_ms := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT + (365::BIGINT * 86400000);
    END CASE;

    -- Mark license as redeemed
    UPDATE public.license_keys
    SET is_active = FALSE,
        redeemed_by = v_user_id,
        redeemed_at = timezone('utc'::text, now())
    WHERE id = v_key_record.id;

    -- Update subscription record
    INSERT INTO public.subscriptions (user_id, plan_type, status, license_key, expiry_epoch_ms, updated_at)
    VALUES (v_user_id, v_target_plan, 'active', v_key_record.key_code, v_expiry_ms, timezone('utc'::text, now()))
    ON CONFLICT (user_id) DO UPDATE
    SET plan_type = EXCLUDED.plan_type,
        status = 'active',
        license_key = EXCLUDED.license_key,
        expiry_epoch_ms = EXCLUDED.expiry_epoch_ms,
        updated_at = timezone('utc'::text, now());

    RETURN jsonb_build_object(
        'success', true,
        'plan', v_target_plan,
        'expiry_epoch_ms', v_expiry_ms,
        'message', 'License successfully activated!'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── Automatic User Registration Trigger ─────────────────────────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_display_name TEXT;
BEGIN
    -- Extract display name from metadata (works for Email signup, Google OAuth, etc.)
    v_display_name := COALESCE(
        NULLIF(TRIM(NEW.raw_user_meta_data->>'display_name'), ''),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'full_name'), ''),
        NULLIF(TRIM(NEW.raw_user_meta_data->>'name'), ''),
        split_part(NEW.email, '@', 1)
    );

    -- 1. Create Profile
    INSERT INTO public.profiles (id, email, display_name, role)
    VALUES (
        NEW.id,
        NEW.email,
        v_display_name,
        'user'
    ) ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = COALESCE(public.profiles.display_name, EXCLUDED.display_name),
        updated_at = timezone('utc'::text, now());

    -- 2. Create Initial Free Subscription
    INSERT INTO public.subscriptions (user_id, plan_type, status, expiry_epoch_ms)
    VALUES (NEW.id, 'FREE', 'active', 0)
    ON CONFLICT (user_id) DO NOTHING;

    -- 3. Create Initial Discipline Stats Sync Row
    INSERT INTO public.user_stats_sync (user_id, total_focus_minutes, discipline_score, current_streak, longest_streak, total_xp, current_level)
    VALUES (NEW.id, 0, 80, 0, 0, 0, 'NEW_STARTER')
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on auth.users insert
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
