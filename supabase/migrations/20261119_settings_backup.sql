-- ==============================================================================
-- Cloud backup of the user's setup (habits, gates, limits, YouTube topics,
-- routines, theme). Run once in the Supabase SQL editor.
-- Each user already can read and update only their own profile row, so no new
-- policy is needed. The app stores the backup as one JSON text.
-- ==============================================================================
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS settings_json TEXT;
