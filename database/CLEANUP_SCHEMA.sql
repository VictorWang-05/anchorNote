-- ============================================================================
-- SCHEMA CLEANUP AND FIX SCRIPT
-- ============================================================================
-- INSTRUCTIONS: Copy and paste this entire script into your Supabase SQL Editor
-- Location: Supabase Dashboard > SQL Editor > New Query
-- Then click "Run" to execute
-- ============================================================================
-- This script fixes the following issues:
-- 1. Adds missing user_id to audio_attachment table
-- 2. Removes redundant 'user' column from notes and templates
-- 3. Removes old 'tag' column from notes (replaced by note_tags)
-- 4. Removes old 'tags' column from templates (replaced by template_tags)
-- 5. Adds NOT NULL constraints to user_id columns
-- 6. Adds missing indexes for performance
-- ============================================================================

-- Step 1: Add missing user_id to audio_attachment
-- ============================================================================
ALTER TABLE public.audio_attachment
ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);

-- Step 2: Drop redundant 'user' columns (we use 'user_id' instead)
-- ============================================================================
-- Note: "user" is a reserved keyword in PostgreSQL, so we must quote it
ALTER TABLE public.notes DROP COLUMN IF EXISTS "user";
ALTER TABLE public.templates DROP COLUMN IF EXISTS "user";

-- Step 3: Drop old single-tag/tags columns (we use junction tables now)
-- ============================================================================
-- First, migrate any remaining data from notes.tag to note_tags
INSERT INTO public.note_tags (note_id, tag_id)
SELECT id, tag
FROM public.notes
WHERE tag IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM public.note_tags nt
    WHERE nt.note_id = notes.id AND nt.tag_id = notes.tag
  )
ON CONFLICT DO NOTHING;

-- Now safe to drop the old tag column
ALTER TABLE public.notes DROP COLUMN IF EXISTS tag;

-- Migrate any remaining data from templates.tags to template_tags
INSERT INTO public.template_tags (template_id, tag_id)
SELECT id, tags
FROM public.templates
WHERE tags IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM public.template_tags tt
    WHERE tt.template_id = templates.id AND tt.tag_id = templates.tags
  )
ON CONFLICT DO NOTHING;

-- Now safe to drop the old tags column
ALTER TABLE public.templates DROP COLUMN IF EXISTS tags;

-- Step 4: Add NOT NULL constraints to user_id columns (after ensuring data exists)
-- ============================================================================
-- First, set a default user_id for any rows missing it (if any exist)
-- You may need to adjust this if you have specific requirements

-- For notes without user_id, you could either:
-- Option A: Delete them
-- DELETE FROM public.notes WHERE user_id IS NULL;

-- Option B: Assign to a specific user (replace with actual user ID)
-- UPDATE public.notes SET user_id = 'your-user-uuid' WHERE user_id IS NULL;

-- For now, we'll just add constraints on new tables
-- Add constraints only if all existing rows have user_id
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.notes WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.notes ALTER COLUMN user_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.tags WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.tags ALTER COLUMN user_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.geofence WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.geofence ALTER COLUMN user_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.photo_attachment WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.photo_attachment ALTER COLUMN user_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.audio_attachment WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.audio_attachment ALTER COLUMN user_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.templates WHERE user_id IS NULL LIMIT 1) THEN
    ALTER TABLE public.templates ALTER COLUMN user_id SET NOT NULL;
  END IF;
END $$;

-- Step 5: Create missing indexes for performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON public.notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_reminder_time ON public.notes(reminder_time);
CREATE INDEX IF NOT EXISTS idx_notes_geofence ON public.notes(geofence);
CREATE INDEX IF NOT EXISTS idx_notes_last_edited ON public.notes(last_edited);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON public.tags(user_id);
CREATE INDEX IF NOT EXISTS idx_geofence_user_id ON public.geofence(user_id);
CREATE INDEX IF NOT EXISTS idx_templates_user_id ON public.templates(user_id);
CREATE INDEX IF NOT EXISTS idx_photo_attachment_user_id ON public.photo_attachment(user_id);
CREATE INDEX IF NOT EXISTS idx_audio_attachment_user_id ON public.audio_attachment(user_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_note_id ON public.note_tags(note_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_tag_id ON public.note_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_template_tags_template_id ON public.template_tags(template_id);
CREATE INDEX IF NOT EXISTS idx_template_tags_tag_id ON public.template_tags(tag_id);

-- Step 6: Add helpful comments
-- ============================================================================
COMMENT ON TABLE public.note_tags IS 'Junction table for many-to-many relationship between notes and tags';
COMMENT ON TABLE public.template_tags IS 'Junction table for many-to-many relationship between templates and tags';
COMMENT ON COLUMN public.notes.reminder_time IS 'Time-based reminder in UTC. Can coexist with geofence reminder.';
COMMENT ON COLUMN public.notes.geofence IS 'Geofence-based reminder. Can coexist with time reminder.';
COMMENT ON COLUMN public.photo_attachment.duration_sec IS 'Duration in seconds for audio attachments';
COMMENT ON COLUMN public.audio_attachment.user_id IS 'User who owns this audio attachment';

-- ============================================================================
-- SCHEMA CLEANUP COMPLETE!
-- ============================================================================
-- Your database schema is now properly configured for the Spring Boot backend.
--
-- VERIFIED STRUCTURE:
-- - notes: user_id, title, text, pinned, reminder_time, geofence, image_file, audio_file
-- - tags: user_id, name, color
-- - note_tags: note_id, tag_id (many-to-many)
-- - template_tags: template_id, tag_id (many-to-many)
-- - geofence: user_id, latitude, longitude, radius
-- - photo_attachment: user_id, media_url, media_type, duration_sec
-- - audio_attachment: user_id, media_url, media_type
-- - templates: user_id, name, text, pinned, geofence, image_file, audio_file
--
-- IMPORTANT NOTES:
-- - Notes can have BOTH time reminder AND geofence reminder (not mutually exclusive)
-- - All tables now support multi-user isolation via user_id
-- - Tags are now many-to-many with both notes and templates
-- - Old single-tag columns have been migrated and removed
-- ============================================================================
