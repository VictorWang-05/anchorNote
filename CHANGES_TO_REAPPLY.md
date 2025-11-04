# Changes Made - Note Loading Fix & Template Feature

## 1. Note Loading Fix (MainActivity.java)

### Problem Fixed:
- Notes weren't loading properly when app started
- Race condition: `loadNotes()` called in both `onCreate()` and `onResume()`
- No validation before loading notes

### Solution:
- Removed `loadNotes()` call from `onCreate()` (only in `onResume()`)
- Added `isLoadingNotes` flag to prevent duplicate concurrent calls
- Added validation: check login status and token existence before loading
- Improved error handling for authentication errors (401/403)
- Better logging for debugging

### Key Code Changes:
```java
// Added flags
private boolean isLoadingNotes = false;
private boolean notesLoaded = false;

// In onCreate() - REMOVED loadNotes() call
// Only call in onResume() to avoid race condition

// In loadNotes():
private void loadNotes() {
    // Prevent duplicate concurrent calls
    if (isLoadingNotes) {
        Log.d(TAG, "📥 Already loading notes, skipping duplicate call");
        return;
    }
    
    // Double-check login status
    if (!authManager.isLoggedIn()) {
        Log.w(TAG, "📥 Cannot load notes - user not logged in");
        clearAllNotes();
        return;
    }
    
    // Verify token exists
    String token = authManager.getToken();
    if (token == null || token.isEmpty()) {
        Log.e(TAG, "📥 Cannot load notes - no token found");
        Toast.makeText(MainActivity.this, "Please login again", Toast.LENGTH_SHORT).show();
        clearAllNotes();
        return;
    }
    
    isLoadingNotes = true;
    // ... rest of loading logic
    // Set isLoadingNotes = false in callbacks
}
```

## 2. Template Feature Implementation

### Files Created:

#### Models/DTOs:
- `app/src/main/java/com/example/anchornotes_team3/model/Template.java`
- `app/src/main/java/com/example/anchornotes_team3/dto/TemplateResponse.java`
- `app/src/main/java/com/example/anchornotes_team3/dto/CreateTemplateRequest.java`
- `app/src/main/java/com/example/anchornotes_team3/dto/InstantiateTemplateRequest.java`

#### Activities/Adapters:
- `app/src/main/java/com/example/anchornotes_team3/TemplateActivity.java`
- `app/src/main/java/com/example/anchornotes_team3/adapter/TemplateAdapter.java`

#### Layouts:
- `app/src/main/res/layout/activity_template.xml`
- `app/src/main/res/layout/item_template.xml`
- `app/src/main/res/layout/dialog_create_template.xml`

### Files Modified:

#### ApiService.java:
Added template endpoints:
```java
// ==================== Templates ====================
@GET("api/templates")
Call<List<TemplateResponse>> getAllTemplates();

@POST("api/templates")
Call<TemplateResponse> createTemplate(@Body CreateTemplateRequest request);

@PUT("api/templates/{id}")
Call<TemplateResponse> updateTemplate(@Path("id") String id, @Body CreateTemplateRequest request);

@DELETE("api/templates/{id}")
Call<Void> deleteTemplate(@Path("id") String id);

@POST("api/templates/{id}/instantiate")
Call<NoteResponse> instantiateTemplate(@Path("id") String id, @Body InstantiateTemplateRequest request);
```

#### NoteRepository.java:
Added template operations:
- `getAllTemplates(TemplatesCallback callback)`
- `createTemplate(Template template, TemplateCallback callback)`
- `deleteTemplate(String templateId, SimpleCallback callback)`
- `instantiateTemplate(String templateId, String noteTitle, NoteCallback callback)`
- `mapToTemplate(TemplateResponse response)` - mapping method

Added callbacks:
- `TemplateCallback` interface
- `TemplatesCallback` interface

#### MainActivity.java:
Added template FAB click listener:
```java
private com.google.android.material.floatingactionbutton.FloatingActionButton createTemplateFab;

// In onCreate():
createTemplateFab = findViewById(R.id.createTemplateFab);

// In setupClickListeners():
createTemplateFab.setOnClickListener(v -> {
    if (authManager.isLoggedIn()) {
        Intent intent = new Intent(MainActivity.this, TemplateActivity.class);
        startActivity(intent);
    } else {
        Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
});
```

#### AndroidManifest.xml:
Added TemplateActivity registration:
```xml
<activity
    android:name=".TemplateActivity"
    android:exported="false"
    android:theme="@style/Theme.Anchornotes_team3" />
```

#### strings.xml:
Added template-related strings:
- `no_templates`
- `delete_template`
- `delete_template_confirmation`
- `use_template`
- `create_note_from_template`

### TemplateActivity Features:
- Displays all templates in RecyclerView
- FAB button to create new templates
- "Use" button on each template to create note from template
- Delete button on each template with confirmation
- Empty state handling
- Auto-refresh on resume

### TemplateAdapter Features:
- Displays template name, preview text, and tags
- "Use" button to instantiate template
- Delete button (×) to remove template

