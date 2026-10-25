-- ==============================================================================
-- Aegis Focus: security hardening (safe to run more than once)
--
-- HOW TO RUN: Supabase dashboard -> SQL Editor -> paste this whole file -> Run.
-- Run supabase_schema.sql and 20261022_one_time_license_keys.sql first.
--
-- What this fixes
--   1. Any signed-in user could set their own profiles.role to 'super_admin'.
--      Roles can now only be changed from the SQL editor / service key, never from the app.
--   2. user_stats_sync allowed any operation without a WITH CHECK.
--   3. SECURITY DEFINER functions now pin search_path, and the payment and admin
--      RPCs are no longer callable by signed-out (anon) clients.
--   4. Accounts created before the signup trigger existed get their missing
--      profile / subscription / stats rows.
-- ==============================================================================

-- ─── 1. profiles: edit your own profile, never your own role ─────────────────
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- auth.uid() is NULL in the SQL editor and for the service key, so those can still
-- grant roles. Any request made as a signed-in app user has a uid and is refused.
CREATE OR REPLACE FUNCTION public.protect_profile_role()
RETURNS TRIGGER
LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
    IF auth.uid() IS NOT NULL
       AND (NEW.role IS DISTINCT FROM OLD.role OR NEW.id IS DISTINCT FROM OLD.id) THEN
        RAISE EXCEPTION 'Changing role or id is not allowed from the app.'
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS protect_profile_role ON public.profiles;
CREATE TRIGGER protect_profile_role
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.protect_profile_role();

-- ─── 2. user_stats_sync: own rows only, on insert and on update ──────────────
-- The app upserts (POST with on_conflict), which needs INSERT and UPDATE. It never deletes.
DROP POLICY IF EXISTS "Users can insert or update their own stats" ON public.user_stats_sync;
DROP POLICY IF EXISTS "Users can insert their own stats" ON public.user_stats_sync;
DROP POLICY IF EXISTS "Users can update their own stats" ON public.user_stats_sync;

CREATE POLICY "Users can insert their own stats"
    ON public.user_stats_sync FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own stats"
    ON public.user_stats_sync FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ─── 3. SECURITY DEFINER functions: fixed search_path, no anon access ────────
ALTER FUNCTION public.is_admin()       SET search_path = public;
ALTER FUNCTION public.is_super_admin() SET search_path = public;
ALTER FUNCTION public.handle_new_user() SET search_path = public;
ALTER FUNCTION public.submit_payment_request(TEXT, TEXT, TEXT, TEXT, TEXT) SET search_path = public;
ALTER FUNCTION public.approve_payment_request(UUID, TEXT)                  SET search_path = public;
ALTER FUNCTION public.reject_payment_request(UUID, TEXT)                   SET search_path = public;

-- is_admin() / is_super_admin() stay callable because row-level-security policies use them.
REVOKE ALL ON FUNCTION public.submit_payment_request(TEXT, TEXT, TEXT, TEXT, TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.approve_payment_request(UUID, TEXT)                  FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.reject_payment_request(UUID, TEXT)                   FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_payment_request(TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_payment_request(UUID, TEXT)                  TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_payment_request(UUID, TEXT)                   TO authenticated;

-- The signup trigger is fired by Postgres, nobody needs to call it.
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;

-- ─── 4. Backfill accounts that signed up before the signup trigger existed ───
INSERT INTO public.profiles (id, email, display_name, role)
SELECT u.id, u.email, split_part(u.email, '@', 1), 'user'
FROM auth.users u
LEFT JOIN public.profiles p ON p.id = u.id
WHERE p.id IS NULL
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.subscriptions (user_id, plan_type, status, expiry_epoch_ms)
SELECT p.id, 'FREE', 'active', 0
FROM public.profiles p
LEFT JOIN public.subscriptions s ON s.user_id = p.id
WHERE s.user_id IS NULL
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO public.user_stats_sync (user_id)
SELECT p.id
FROM public.profiles p
LEFT JOIN public.user_stats_sync s ON s.user_id = p.id
WHERE s.user_id IS NULL
ON CONFLICT (user_id) DO NOTHING;
