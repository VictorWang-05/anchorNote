-- Migration script for AnchorNotes database schema
-- This script updates the existing schema to support the API requirements

-- Step 1: Add missing columns to existing tables
-- Add user_id to all tables for multi-tenancy
ALTER TABLE notes ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE tags ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE geofence ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE templates ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE photo_attachment ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);

-- Add title column to notes (required by API)
ALTER TABLE notes ADD COLUMN IF NOT EXISTS title VARCHAR(500);

-- Add audio duration to photo_attachment
ALTER TABLE photo_attachment ADD COLUMN IF NOT EXISTS duration_sec INT;

-- Step 2: Create join table for Note-Tags many-to-many relationship
CREATE TABLE IF NOT EXISTS note_tags (
  note_id INT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
  tag_id INT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (note_id, tag_id)
);

-- Step 3: Create join table for Template-Tags many-to-many relationship
CREATE TABLE IF NOT EXISTS template_tags (
  template_id INT NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
  tag_id INT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (template_id, tag_id)
);

-- Step 4: Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_reminder_time ON notes(reminder_time);
CREATE INDEX IF NOT EXISTS idx_notes_geofence ON notes(geofence);
CREATE INDEX IF NOT EXISTS idx_notes_last_edited ON notes(last_edited);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON tags(user_id);
CREATE INDEX IF NOT EXISTS idx_geofence_user_id ON geofence(user_id);
CREATE INDEX IF NOT EXISTS idx_templates_user_id ON templates(user_id);
CREATE INDEX IF NOT EXISTS idx_photo_attachment_user_id ON photo_attachment(user_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_note_id ON note_tags(note_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_tag_id ON note_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_template_tags_template_id ON template_tags(template_id);
CREATE INDEX IF NOT EXISTS idx_template_tags_tag_id ON template_tags(tag_id);

-- Step 5: Migrate existing tag associations to note_tags table (if any exist)
-- This will handle the old single-tag relationship
INSERT INTO note_tags (note_id, tag_id)
SELECT id, tag
FROM notes
WHERE tag IS NOT NULL
ON CONFLICT DO NOTHING;

-- Step 6: Migrate existing tag associations from templates to template_tags table
INSERT INTO template_tags (template_id, tag_id)
SELECT id, tag
FROM templates
WHERE tag IS NOT NULL
ON CONFLICT DO NOTHING;

-- Step 7: Add comments for documentation
COMMENT ON TABLE note_tags IS 'Junction table for many-to-many relationship between notes and tags';
COMMENT ON TABLE template_tags IS 'Junction table for many-to-many relationship between templates and tags';
COMMENT ON COLUMN notes.reminder_time IS 'Time-based reminder in UTC. Can coexist with geofence reminder.';
COMMENT ON COLUMN notes.geofence IS 'Geofence-based reminder. Can coexist with time reminder.';
COMMENT ON COLUMN photo_attachment.duration_sec IS 'Duration in seconds for audio attachments';

-- Step 8: Create function to automatically set user_id from JWT context (optional, for RLS)
-- This is for future use if you want to enable Row Level Security
CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $$
BEGIN
  RETURN auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Note: We intentionally DO NOT add a mutual exclusivity constraint
-- because notes can have BOTH time reminder AND geofence reminder simultaneously
