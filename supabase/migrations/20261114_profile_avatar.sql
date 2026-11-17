-- ==============================================================================
-- Profile photo backup
-- Run once in the Supabase SQL editor. Lets the app restore a user's profile
-- photo after a reinstall. The photo is a small (about 256 px) JPEG stored as
-- base64. Existing policies already let each user read and update only their
-- own profile row, so nothing else needs to change.
-- ==============================================================================
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_b64 TEXT;
