-- ==============================================================================
-- Project: ASAR — Turn Time Into Progress
-- Database Schema & Server-Authoritative Security for Supabase PostgreSQL
-- ==============================================================================

-- 1. Profiles Table (Linked to Supabase Auth)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
    email TEXT NOT NULL,
    display_name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Subscriptions Table (Pro Entitlements)
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL UNIQUE,
    plan_type TEXT DEFAULT 'FREE' NOT NULL, -- FREE, PRO_MONTHLY, PRO_YEARLY, LIFETIME, STUDENT_YEARLY
    status TEXT DEFAULT 'active' NOT NULL,
    license_key TEXT,
    expiry_epoch_ms BIGINT DEFAULT 0 NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. User Discipline & Stats Cloud Sync
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

-- 4. Central License Keys Pool (Protected Server-Side)
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
ALTER TABLE public.user_stats_sync ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Users can view their own profile" 
    ON public.profiles FOR SELECT 
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile" 
    ON public.profiles FOR UPDATE 
    USING (auth.uid() = id);

-- Subscriptions Policies (READ-ONLY for clients; modifications only via secure RPC)
CREATE POLICY "Users can view their own subscription" 
    ON public.subscriptions FOR SELECT 
    USING (auth.uid() = user_id);

-- Stats Sync Policies
CREATE POLICY "Users can view their own stats" 
    ON public.user_stats_sync FOR SELECT 
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert or update their own stats" 
    ON public.user_stats_sync FOR ALL 
    USING (auth.uid() = user_id);

-- License Keys: Client has NO direct SELECT/UPDATE access (handled securely by RPC)
CREATE POLICY "Users can only view their already redeemed keys" 
    ON public.license_keys FOR SELECT 
    USING (redeemed_by = auth.uid());

-- ─── Server-Authoritative License Redemption Function ────────────────────────
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
    UPDATE public.subscriptions
    SET plan_type = v_target_plan,
        status = 'active',
        license_key = v_key_record.key_code,
        expiry_epoch_ms = v_expiry_ms,
        updated_at = timezone('utc'::text, now())
    WHERE user_id = v_user_id;

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
BEGIN
    -- 1. Create Profile
    INSERT INTO public.profiles (id, email, display_name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1))
    );

    -- 2. Create Initial Free Subscription
    INSERT INTO public.subscriptions (user_id, plan_type, status, expiry_epoch_ms)
    VALUES (NEW.id, 'FREE', 'active', 0);

    -- 3. Create Initial Discipline Stats Sync Row
    INSERT INTO public.user_stats_sync (user_id, total_focus_minutes, discipline_score, current_streak, longest_streak, total_xp, current_level)
    VALUES (NEW.id, 0, 80, 0, 0, 0, 'NEW_STARTER');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on auth.users insert
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
