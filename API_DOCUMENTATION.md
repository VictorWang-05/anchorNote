# Notes Search & Filter API Documentation

## Overview

The search and filter functionality has been updated to provide a cleaner separation of concerns:

1. **Search** - Simple text search across note titles and content
2. **Filter** - Apply filters (tags, dates, attachments) to all user's notes

## API Endpoints

### 1. Search Notes (Updated)

**Endpoint:** `GET /api/notes/search`

**Description:** Searches notes by title and content only. Returns notes that match the search query.

**Query Parameters:**
- `q` (String, optional): Search query to match against note title and content
- `limit` (Integer, optional, default: 50): Maximum number of results to return
- `offset` (Integer, optional, default: 0): Pagination offset

**Example Request:**
```
GET /api/notes/search?q=meeting&limit=20&offset=0
```

**Response:**
```json
{
  "total": 5,
  "items": [
    {
      "id": "123",
      "title": "Team Meeting Notes",
      "text": "Discussion about project timeline...",
      "pinned": false,
      "lastEdited": "2025-11-01T20:00:00Z",
      "createdAt": "2025-10-30T10:00:00Z",
      "tags": [...],
      "geofence": null,
      "reminderTimeUtc": null,
      "image": null,
      "audio": null,
      "hasPhoto": false,
      "hasAudio": false
    },
    ...
  ]
}
```

---

### 2. Filter Notes (New)

**Endpoint:** `GET /api/notes/filter`

**Description:** Filters all user's notes based on various criteria (tags, date range, attachments).

**Query Parameters:**
- `tagIds` (List<Long>, optional): Filter by tag IDs (comma-separated)
- `editedStart` (ISO-8601 DateTime, optional): Filter notes edited after this date
- `editedEnd` (ISO-8601 DateTime, optional): Filter notes edited before this date
- `hasPhoto` (Boolean, optional): Filter notes with/without photos
  - `true`: Only notes with photos
  - `false`: Only notes without photos
  - `null`: Include all notes regardless of photos
- `hasAudio` (Boolean, optional): Filter notes with/without audio
  - `true`: Only notes with audio
  - `false`: Only notes without audio
  - `null`: Include all notes regardless of audio
- `hasLocation` (Boolean, optional): Filter notes with/without geofence
  - `true`: Only notes with geofence
  - `false`: Only notes without geofence
  - `null`: Include all notes regardless of geofence
- `limit` (Integer, optional, default: 50): Maximum number of results
- `offset` (Integer, optional, default: 0): Pagination offset

**Example Request:**
```
GET /api/notes/filter?tagIds=1,2,3&hasPhoto=true&editedStart=2025-10-01T00:00:00Z&limit=20
```

**Response:**
```json
{
  "total": 12,
  "items": [
    {
      "id": "456",
      "title": "Photo Note",
      "text": "Note with image",
      "tags": [
        {
          "id": "1",
          "name": "work",
          "color": "#FF5733"
        }
      ],
      "hasPhoto": true,
      ...
    },
    ...
  ]
}
```

---

## Typical Usage Workflow

### Scenario 1: Simple Search
User wants to find notes containing "project":

1. Call `GET /api/notes/search?q=project`
2. Display search results

### Scenario 2: Search + Filter
User searches for "meeting" then filters results by tags:

1. Call `GET /api/notes/search?q=meeting` - Get all notes with "meeting"
2. User applies tag filter in UI
3. Call `GET /api/notes/filter?tagIds=1,2` - Get filtered notes (Note: This filters ALL notes, not just search results)

**Important:** The filter endpoint filters ALL user notes, not just search results. If you want to filter search results in the UI, you should:
- Call search endpoint
- Store results locally
- Apply filters client-side on the search results

OR

- Implement client-side filtering of search results after receiving them from the search endpoint

### Scenario 3: Direct Filtering
User wants to see all notes with photos from last week:

1. Call `GET /api/notes/filter?hasPhoto=true&editedStart=2025-10-25T00:00:00Z`
2. Display filtered results

---

## Migration Guide

### Old Search Endpoint (Deprecated)
The old search endpoint accepted filter parameters directly:
```
GET /api/notes/search?q=meeting&tagIds=1,2&hasPhoto=true
```

### New Approach
Now search and filter are separated:

**Search only:**
```
GET /api/notes/search?q=meeting
```

**Filter only:**
```
GET /api/notes/filter?tagIds=1,2&hasPhoto=true
```

### Backward Compatibility
The old filter parameters in the search endpoint are deprecated but still present in the `SearchRequest` class for backward compatibility. However, they will be ignored by the new implementation. Please migrate to the new separate endpoints.

---

## Implementation Notes for Android

1. **Update API Service Interface:**
   ```java
   // Search by text only
   @GET("api/notes/search")
   Call<SearchResponse> searchNotes(
       @Query("q") String query,
       @Query("limit") Integer limit,
       @Query("offset") Integer offset
   );

   // Filter notes
   @GET("api/notes/filter")
   Call<SearchResponse> filterNotes(
       @Query("tagIds") List<Long> tagIds,
       @Query("editedStart") String editedStart,
       @Query("editedEnd") String editedEnd,
       @Query("hasPhoto") Boolean hasPhoto,
       @Query("hasAudio") Boolean hasAudio,
       @Query("hasLocation") Boolean hasLocation,
       @Query("limit") Integer limit,
       @Query("offset") Integer offset
   );
   ```

2. **Handle Search + Filter in UI:**
   - For search-only: Call `searchNotes()`
   - For filter-only: Call `filterNotes()`
   - For search with client-side filters: Call `searchNotes()`, then filter results in the app

3. **Date Format:**
   - Use ISO-8601 format: `2025-11-01T20:00:00Z`
   - Can use `SimpleDateFormat` or `Instant.toString()` in Java

---

## Testing

### Test Search
```bash
curl -X GET "http://localhost:8080/api/notes/search?q=test&limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Filter
```bash
curl -X GET "http://localhost:8080/api/notes/filter?hasPhoto=true&tagIds=1,2" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Questions?

If you have any questions about the new API, please reach out to the backend team.
