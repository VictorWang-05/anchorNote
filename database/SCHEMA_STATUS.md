# Database Schema Status & Explanation

## Current Schema Structure

Your Supabase database has **8 tables** organized as follows:

### User Data Tables (with user_id)
1. **notes** - Core note storage
2. **tags** - Tag definitions
3. **geofence** - Location-based reminders
4. **photo_attachment** - Photo/image files
5. **templates** - Reusable note templates

### Junction Tables (many-to-many relationships)
6. **note_tags** - Links notes to multiple tags
7. **template_tags** - Links templates to multiple tags

### Missing User Isolation
8. **audio_attachment** - ⚠️ **MISSING user_id column**

---

## Schema Issues Found

### ❌ Critical Issues

**1. audio_attachment missing user_id**
```sql
-- Current: No user_id column
-- Fix: Add user_id UUID REFERENCES auth.users(id)
```
**Impact**: Cannot isolate audio attachments by user, security risk

**2. Redundant user columns**
```sql
-- notes table has BOTH:
notes.user     -- Old column (UUID)
notes.user_id  -- New column (UUID) ✅ This is correct

-- templates table has BOTH:
templates.user     -- Old column (UUID)
templates.user_id  -- New column (UUID) ✅ This is correct
```
**Impact**: Wasted storage, potential data inconsistency

**3. Old single-tag columns**
```sql
-- notes table has:
notes.tag  -- Old single tag (int) ⚠️ Should be removed

-- templates table has:
templates.tags  -- Old single tag (int) ⚠️ Should be removed
```
**Impact**: Confusing schema, junction tables (note_tags, template_tags) are the correct approach

---

## What Should Be Fixed

Run the cleanup script at: `database/CLEANUP_SCHEMA.sql`

This script will:

### ✅ Step 1: Add missing user_id
```sql
ALTER TABLE audio_attachment ADD COLUMN user_id UUID REFERENCES auth.users(id);
```

### ✅ Step 2: Remove redundant user columns
```sql
ALTER TABLE notes DROP COLUMN user;
ALTER TABLE templates DROP COLUMN user;
```

### ✅ Step 3: Migrate old tag data
```sql
-- Migrate notes.tag → note_tags junction table
-- Migrate templates.tags → template_tags junction table
```

### ✅ Step 4: Remove old tag columns
```sql
ALTER TABLE notes DROP COLUMN tag;
ALTER TABLE templates DROP COLUMN tags;
```

### ✅ Step 5: Add NOT NULL constraints
```sql
ALTER TABLE notes ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tags ALTER COLUMN user_id SET NOT NULL;
-- ... (for all user_id columns)
```

### ✅ Step 6: Create performance indexes
```sql
CREATE INDEX idx_notes_user_id ON notes(user_id);
CREATE INDEX idx_notes_reminder_time ON notes(reminder_time);
CREATE INDEX idx_notes_geofence ON notes(geofence);
-- ... (13 total indexes)
```

---

## Final Clean Schema

After running the cleanup script, your schema will be:

### notes
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL)
- `title` - Note title
- `text` - Note content
- `pinned` - Pin status
- `created_at` - Creation timestamp
- `last_edited` - Last edit timestamp
- `reminder_time` - Time-based reminder (UTC, can coexist with geofence)
- `geofence` - FK to geofence table (can coexist with time reminder)
- `image_file` - FK to photo_attachment
- `audio_file` - FK to audio_attachment

### tags
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL)
- `name` - Tag name
- `color` - Hex color

### note_tags (many-to-many)
- `note_id` - FK to notes
- `tag_id` - FK to tags
- `created_at` - Creation timestamp

### geofence
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL)
- `latitude` - Location latitude
- `longitude` - Location longitude
- `radius` - Geofence radius in meters
- `created_at` - Creation timestamp

### photo_attachment
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL)
- `media_url` - File URL in Supabase Storage
- `media_type` - MIME type
- `duration_sec` - Duration (for video, if needed)
- `created_at` - Creation timestamp

### audio_attachment
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL) **ADDED BY CLEANUP**
- `media_url` - File URL in Supabase Storage
- `media_type` - MIME type
- `created_at` - Creation timestamp

### templates
- `id` - Primary key
- `user_id` - ✅ User isolation (NOT NULL)
- `name` - Template name
- `text` - Template content
- `pinned` - Pin status
- `geofence` - FK to geofence table
- `image_file` - FK to photo_attachment
- `audio_file` - FK to audio_attachment
- `created_at` - Creation timestamp

### template_tags (many-to-many)
- `template_id` - FK to templates
- `tag_id` - FK to tags
- `created_at` - Creation timestamp

---

## Key Features of Clean Schema

### ✅ Multi-User Isolation
Every table has `user_id` referencing `auth.users(id)`

### ✅ Many-to-Many Relationships
- Notes can have multiple tags (via note_tags)
- Templates can have multiple tags (via template_tags)

### ✅ Dual Reminders (NOT Mutually Exclusive)
- Notes can have BOTH `reminder_time` AND `geofence`
- No mutual exclusivity constraint
- Backend logic handles both simultaneously

### ✅ File Attachments
- Separate tables for photos and audio
- Both linked via foreign keys
- Stored in Supabase Storage buckets

### ✅ Performance Optimized
- 13 indexes created for common queries
- Indexed on user_id, reminder_time, geofence, last_edited, etc.

---

## How to Apply the Fix

**1. Go to Supabase Dashboard**
```
https://supabase.com/dashboard
```

**2. Open SQL Editor**
```
Your Project > SQL Editor > New Query
```

**3. Copy and Run**
```
database/CLEANUP_SCHEMA.sql
```

**4. Verify**
```sql
-- Check audio_attachment has user_id
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'audio_attachment';

-- Check notes no longer has 'user' or 'tag' columns
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'notes';
```

---

## Summary

### Before Cleanup
- ❌ audio_attachment missing user_id
- ❌ Redundant 'user' columns in notes and templates
- ❌ Old single-tag columns (notes.tag, templates.tags)
- ❌ Missing indexes

### After Cleanup
- ✅ All tables have user_id for isolation
- ✅ Clean many-to-many tag relationships
- ✅ No redundant columns
- ✅ 13 performance indexes
- ✅ Ready for Spring Boot backend

**Status**: **Schema needs cleanup before backend will work correctly**

**Action Required**: Run `database/CLEANUP_SCHEMA.sql` in Supabase SQL Editor
