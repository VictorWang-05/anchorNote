-- ============================================================================
-- FIX COLUMN TYPE ISSUES IN NOTES TABLE
-- ============================================================================
-- This script fixes the column type issues that are causing runtime errors:
-- 1. Converts bytea columns to text for text and title
-- ============================================================================

-- Check current column types (for verification)
SELECT
    table_name,
    column_name,
    data_type,
    udt_name
FROM information_schema.columns
WHERE table_name = 'notes'
    AND column_name IN ('text', 'title', 'user_id', 'audio_file', 'image_file')
ORDER BY ordinal_position;

-- Fix text column if it's bytea
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'notes'
            AND column_name = 'text'
            AND (data_type = 'bytea' OR udt_name = 'bytea')
    ) THEN
        -- If the column contains data, try to convert it
        ALTER TABLE notes ALTER COLUMN text TYPE TEXT USING convert_from(text, 'UTF8');
        RAISE NOTICE 'Converted notes.text from bytea to text';
    ELSE
        RAISE NOTICE 'notes.text is already text type or does not exist';
    END IF;
END $$;

-- Fix title column if it's bytea
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'notes'
            AND column_name = 'title'
            AND (data_type = 'bytea' OR udt_name = 'bytea')
    ) THEN
        -- If the column contains data, try to convert it
        ALTER TABLE notes ALTER COLUMN title TYPE VARCHAR(500) USING convert_from(title, 'UTF8');
        RAISE NOTICE 'Converted notes.title from bytea to varchar(500)';
    ELSE
        RAISE NOTICE 'notes.title is already varchar/text type or does not exist';
    END IF;
END $$;

-- Verify the changes
SELECT
    table_name,
    column_name,
    data_type,
    udt_name,
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'notes'
    AND column_name IN ('text', 'title', 'user_id', 'audio_file', 'image_file')
ORDER BY ordinal_position;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these to verify everything is correct:
--
-- 1. Check that text and title are text/varchar:
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'notes' AND column_name IN ('text', 'title');
--
-- 2. Check that user_id is uuid:
-- SELECT column_name, udt_name FROM information_schema.columns
-- WHERE table_name = 'notes' AND column_name = 'user_id';
-- ============================================================================
