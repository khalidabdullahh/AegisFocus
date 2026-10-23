-- ==============================================================================
-- Aegis Focus: one-time license keys (server-authoritative)
--
-- HOW TO RUN (Supabase dashboard -> SQL Editor):
--   1. Run supabase_schema.sql first (it is idempotent) if you have never run it.
--   2. Run this whole file.
--   3. Make your own account an admin (replace the email, run once):
--        update public.profiles set role = 'super_admin'
--        where id = (select id from auth.users where email = 'YOUR_EMAIL_HERE');
--
-- Rules enforced here, not in the app:
--   * A key can be redeemed exactly once. The row is locked while redeeming, so two
--     people pasting the same key at the same moment cannot both win.
--   * Only admins can create, list or revoke keys.
--   * 5 wrong attempts in 15 minutes lock redeeming for that account for 15 minutes.
-- ==============================================================================

-- Key pool (already exists in supabase_schema.sql, kept here so this file is self-contained)
CREATE TABLE IF NOT EXISTS public.license_keys (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    key_code TEXT NOT NULL UNIQUE,
    tier TEXT NOT NULL,
    duration_days INT DEFAULT 0 NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    redeemed_by UUID REFERENCES public.profiles(id),
    redeemed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);
ALTER TABLE public.license_keys ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES public.profiles(id);
ALTER TABLE public.license_keys ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

-- Failed-attempt log used for brute-force protection
CREATE TABLE IF NOT EXISTS public.license_redeem_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    success BOOLEAN NOT NULL,
    attempted_at TIMESTAMPTZ DEFAULT now() NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_redeem_attempts_user_time
    ON public.license_redeem_attempts (user_id, attempted_at DESC);
ALTER TABLE public.license_redeem_attempts ENABLE ROW LEVEL SECURITY;
-- No policies on purpose: nobody can read or write this table directly, only the functions below.

-- Normal users must never see the key pool. Admins and the redeemer of a key may.
DROP POLICY IF EXISTS "Users can only view their already redeemed keys" ON public.license_keys;
CREATE POLICY "Users can only view their already redeemed keys"
    ON public.license_keys FOR SELECT
    USING (redeemed_by = auth.uid() OR public.is_admin());

-- ─── Tier table: one place that maps a key tier to the app plan and duration ──
CREATE OR REPLACE FUNCTION public.license_tier_info(p_tier TEXT)
RETURNS TABLE (plan_type TEXT, duration_days INT)
LANGUAGE sql IMMUTABLE AS $$
    SELECT t.plan_type, t.duration_days FROM (VALUES
        ('MNTH', 'PRO_1_MONTH',    30),
        ('3M',   'PRO_3_MONTHS',   90),
        ('6M',   'PRO_6_MONTHS',  180),
        ('YEAR', 'PRO_12_MONTHS', 365),
        ('STUD', 'STUDENT_YEARLY',365),
        ('LIFE', 'LIFETIME',        0)
    ) AS t(tier, plan_type, duration_days)
    WHERE t.tier = upper(p_tier);
$$;

-- ─── Admin: generate a key ────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_generate_license_key(p_tier TEXT, p_note TEXT DEFAULT NULL)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_tier TEXT := upper(trim(p_tier));
    v_info RECORD;
    v_raw TEXT;
    v_key TEXT;
BEGIN
    IF NOT public.is_admin() THEN
        RETURN jsonb_build_object('success', false, 'error', 'Admin access required.');
    END IF;

    SELECT * INTO v_info FROM public.license_tier_info(v_tier);
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Unknown tier: ' || v_tier);
    END IF;

    LOOP
        -- 16 hex characters = 64 bits of randomness, shown as AEGIS-XXXX-XXXX-XXXX-XXXX
        v_raw := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 16));
        v_key := 'AEGIS-' || substr(v_raw, 1, 4) || '-' || substr(v_raw, 5, 4) || '-' ||
                 substr(v_raw, 9, 4) || '-' || substr(v_raw, 13, 4);
        EXIT WHEN NOT EXISTS (SELECT 1 FROM public.license_keys WHERE key_code = v_key);
    END LOOP;

    INSERT INTO public.license_keys (key_code, tier, duration_days, created_by, note)
    VALUES (v_key, v_tier, v_info.duration_days, auth.uid(), NULLIF(trim(p_note), ''));

    RETURN jsonb_build_object('success', true, 'key', v_key, 'tier', v_tier,
                              'plan', v_info.plan_type, 'duration_days', v_info.duration_days);
END;
$$;

-- ─── Admin: list recent keys with status ──────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_license_keys(p_limit INT DEFAULT 100)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NOT public.is_admin() THEN
        RETURN jsonb_build_object('success', false, 'error', 'Admin access required.');
    END IF;

    RETURN jsonb_build_object('success', true, 'keys', COALESCE((
        SELECT jsonb_agg(row_to_json(k)::jsonb ORDER BY k.created_at DESC)
        FROM (
            SELECT lk.key_code,
                   lk.tier,
                   CASE
                       WHEN lk.redeemed_by IS NOT NULL THEN 'USED'
                       WHEN NOT lk.is_active THEN 'REVOKED'
                       ELSE 'UNUSED'
                   END AS status,
                   p.email AS redeemed_by_email,
                   lk.redeemed_at,
                   lk.created_at,
                   lk.note
            FROM public.license_keys lk
            LEFT JOIN public.profiles p ON p.id = lk.redeemed_by
            ORDER BY lk.created_at DESC
            LIMIT GREATEST(1, LEAST(p_limit, 500))
        ) k
    ), '[]'::jsonb));
END;
$$;

-- ─── Admin: revoke an unused key ──────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_revoke_license_key(p_key TEXT)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_count INT;
BEGIN
    IF NOT public.is_admin() THEN
        RETURN jsonb_build_object('success', false, 'error', 'Admin access required.');
    END IF;

    UPDATE public.license_keys
    SET is_active = FALSE
    WHERE key_code = upper(trim(p_key)) AND redeemed_by IS NULL AND is_active = TRUE;
    GET DIAGNOSTICS v_count = ROW_COUNT;

    IF v_count = 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Key not found, already used or already revoked.');
    END IF;
    RETURN jsonb_build_object('success', true);
END;
$$;

-- ─── User: redeem a key (one time only) ───────────────────────────────────────
-- The parameter is named p_key because that is what the app sends.
DROP FUNCTION IF EXISTS public.redeem_license_key(TEXT);
CREATE OR REPLACE FUNCTION public.redeem_license_key(p_key TEXT)
RETURNS JSONB
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_key RECORD;
    v_info RECORD;
    v_now_ms BIGINT := (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT;
    v_base_ms BIGINT;
    v_existing_expiry BIGINT;
    v_expiry_ms BIGINT := 0;
    v_recent_failures INT;
BEGIN
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Please sign in first.');
    END IF;

    SELECT count(*) INTO v_recent_failures
    FROM public.license_redeem_attempts
    WHERE user_id = v_user_id AND success = FALSE
      AND attempted_at > now() - interval '15 minutes';

    IF v_recent_failures >= 5 THEN
        RETURN jsonb_build_object('success', false,
            'error', 'Too many wrong attempts. Try again in 15 minutes.');
    END IF;

    -- Lock the row so only one redeemer can ever win
    SELECT * INTO v_key
    FROM public.license_keys
    WHERE key_code = upper(trim(p_key))
      AND is_active = TRUE
      AND redeemed_by IS NULL
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO public.license_redeem_attempts (user_id, success) VALUES (v_user_id, FALSE);
        RETURN jsonb_build_object('success', false, 'error', 'Invalid or already used key.');
    END IF;

    SELECT * INTO v_info FROM public.license_tier_info(v_key.tier);
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'This key has an unknown tier. Contact support.');
    END IF;

    IF v_info.duration_days > 0 THEN
        -- Stack on top of remaining time if the user is already subscribed
        SELECT expiry_epoch_ms INTO v_existing_expiry
        FROM public.subscriptions
        WHERE user_id = v_user_id AND status = 'active';
        v_base_ms := GREATEST(v_now_ms, COALESCE(v_existing_expiry, 0));
        v_expiry_ms := v_base_ms + (v_info.duration_days::BIGINT * 86400000);
    END IF;

    UPDATE public.license_keys
    SET is_active = FALSE,
        redeemed_by = v_user_id,
        redeemed_at = timezone('utc'::text, now())
    WHERE id = v_key.id;

    INSERT INTO public.subscriptions (user_id, plan_type, status, license_key, expiry_epoch_ms, updated_at)
    VALUES (v_user_id, v_info.plan_type, 'active', v_key.key_code, v_expiry_ms, timezone('utc'::text, now()))
    ON CONFLICT (user_id) DO UPDATE
    SET plan_type = EXCLUDED.plan_type,
        status = 'active',
        license_key = EXCLUDED.license_key,
        expiry_epoch_ms = EXCLUDED.expiry_epoch_ms,
        updated_at = timezone('utc'::text, now());

    INSERT INTO public.license_redeem_attempts (user_id, success) VALUES (v_user_id, TRUE);

    RETURN jsonb_build_object('success', true,
                              'plan', v_info.plan_type,
                              'expiry_epoch_ms', v_expiry_ms,
                              'message', 'Pro activated.');
END;
$$;

-- ─── Permissions ──────────────────────────────────────────────────────────────
REVOKE ALL ON FUNCTION public.admin_generate_license_key(TEXT, TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_list_license_keys(INT)           FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_revoke_license_key(TEXT)         FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.redeem_license_key(TEXT)               FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_generate_license_key(TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_license_keys(INT)           TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_revoke_license_key(TEXT)         TO authenticated;
GRANT EXECUTE ON FUNCTION public.redeem_license_key(TEXT)               TO authenticated;
