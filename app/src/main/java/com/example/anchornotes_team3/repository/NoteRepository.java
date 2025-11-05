package com.example.anchornotes_team3.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.anchornotes_team3.api.ApiClient;
import com.example.anchornotes_team3.api.ApiService;
import com.example.anchornotes_team3.dto.*;
import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Geofence;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for Note data operations using backend API
 */
public class NoteRepository {
    
    private static final String TAG = "NoteRepository";
    private static NoteRepository instance;
    private final Context context;
    
    private NoteRepository(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized NoteRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NoteRepository(context);
        }
        return instance;
    }
    
    /**
     * Get the ApiService - always fetches fresh to ensure we have the latest token
     */
    /**
     * Get ApiService instance for making API calls
     * Exposed publicly for advanced use cases (like geofence sync)
     */
    public ApiService getApiService() {
        return ApiClient.getApiService(context);
    }
    
    // ==================== Callbacks ====================
    
    public interface NoteCallback {
        void onSuccess(Note note);
        void onError(String error);
    }
    
    public interface NotesCallback {
        void onSuccess(List<Note> notes);
        void onError(String error);
    }
    
    public interface TagsCallback {
        void onSuccess(List<Tag> tags);
        void onError(String error);
    }
    
    public interface AttachmentCallback {
        void onSuccess(String attachmentId, String mediaUrl);
        void onError(String error);
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface TemplateCallback {
        void onSuccess(com.example.anchornotes_team3.model.Template template);
        void onError(String error);
    }
    
    public interface TemplatesCallback {
        void onSuccess(List<com.example.anchornotes_team3.model.Template> templates);
        void onError(String error);
    }
    
    // ==================== User Operations ====================
    
    /**
     * Change user password
     */
    public void changePassword(String currentPassword, String newPassword, SimpleCallback callback) {
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
        
        getApiService().changePassword(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "Password changed successfully");
                        callback.onSuccess();
                    } else {
                        String errorMsg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Failed to change password";
                        Log.e(TAG, "Password change failed: " + errorMsg);
                        callback.onError(errorMsg);
                    }
                } else {
                    String errorMsg = "Failed to change password";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "Password change failed: " + response.code());
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "Password change network error", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Note Operations ====================
    
    /**
     * Load all notes
     */
    public void getAllNotes(NotesCallback callback) {
        getApiService().getAllNotes().enqueue(new Callback<List<NoteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<NoteResponse>> call, @NonNull Response<List<NoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Note> notes = response.body().stream()
                            .map(NoteRepository.this::mapToNote)
                            .collect(Collectors.toList());
                    
                    // Log notes with geofences for debugging location search
                    Log.d(TAG, "📥 Loaded " + notes.size() + " notes from backend");
                    for (Note note : notes) {
                        if (note.getGeofence() != null && note.getGeofence().getAddressName() != null) {
                            Log.d(TAG, "  📍 Note '" + note.getTitle() + "' has geofence at: '" + note.getGeofence().getAddressName() + "'");
                        }
                    }
                    
                    callback.onSuccess(notes);
                } else {
                    callback.onError("Failed to load notes: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<NoteResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading notes", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Load a note by ID
     */
    public void getNoteById(String noteId, NoteCallback callback) {
        getApiService().getNoteById(noteId).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    callback.onSuccess(note);
                } else {
                    callback.onError("Failed to load note: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading note", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Create a new note
     */
    public void createNote(Note note, NoteCallback callback) {
        Log.d(TAG, "📤 Creating note: " + note.getTitle());
        CreateNoteRequest request = new CreateNoteRequest(note.getTitle(), note.getText());
        request.setPinned(note.isPinned());
        
        // Extract tag IDs
        List<String> tagIds = note.getTags().stream()
                .map(Tag::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!tagIds.isEmpty()) {
            request.setTagIds(tagIds);
        }
        
        Log.d(TAG, "🌐 Making API call to create note...");
        getApiService().createNote(request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                Log.d(TAG, "📨 Response received: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Note created successfully!");
                    Note createdNote = mapToNote(response.body());
                    callback.onSuccess(createdNote);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "❌ Failed to create note: " + response.code() + " - " + errorBody);
                    callback.onError("Failed to create note: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "💥 Network error creating note", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Update an existing note
     */
    public void updateNote(Note note, NoteCallback callback) {
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitle(note.getTitle());
        request.setText(note.getText());
        request.setPinned(note.isPinned());
        
        // Extract tag IDs
        List<String> tagIds = note.getTags().stream()
                .map(Tag::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!tagIds.isEmpty()) {
            request.setTagIds(tagIds);
        }
        
        getApiService().updateNote(note.getId(), request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note updatedNote = mapToNote(response.body());
                    callback.onSuccess(updatedNote);
                } else {
                    callback.onError("Failed to update note: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error updating note", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Delete a note
     */
    public void deleteNote(String noteId, SimpleCallback callback) {
        getApiService().deleteNote(noteId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to delete note: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error deleting note", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Pin/unpin a note
     */
    public void pinNote(String noteId, boolean pinned, NoteCallback callback) {
        PinRequest request = new PinRequest(pinned);
        getApiService().pinNote(noteId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    callback.onSuccess(note);
                } else {
                    callback.onError("Failed to pin note: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error pinning note", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Filter notes by criteria
     */
    public void filterNotes(List<String> tagIds, Boolean pinned, Boolean hasPhoto, Boolean hasAudio, Boolean hasLocation, NotesCallback callback) {
        // Convert empty list to null for API call
        List<String> apiTagIds = (tagIds != null && !tagIds.isEmpty()) ? tagIds : null;

        getApiService().filterNotes(apiTagIds, pinned, hasPhoto, hasAudio, hasLocation).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    if (searchResponse.getItems() != null) {
                        List<Note> notes = searchResponse.getItems().stream()
                                .map(NoteRepository.this::mapToNote)
                                .collect(Collectors.toList());
                        callback.onSuccess(notes);
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                } else {
                    String errorMsg = "Failed to filter notes: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error filtering notes", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Search notes by query string (searches content, title, tags, and address)
     */
    public void searchNotes(String query, NotesCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            // If query is empty, return all notes instead
            getAllNotes(callback);
            return;
        }
        
        Log.d(TAG, "🔍 Searching notes with query: '" + query + "'");
        Log.d(TAG, "🔍 API call: GET /api/notes/search?q=" + query);
        
        // Only pass 'q' parameter for free-text search
        // Backend should search across title, body, tags, and address fields
        // Don't pass 'location' parameter - that would create an AND condition
        getApiService().searchNotes(query, null).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                Log.d(TAG, "🔍 Search response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    if (searchResponse.getItems() != null) {
                        List<Note> notes = searchResponse.getItems().stream()
                                .map(NoteRepository.this::mapToNote)
                                .collect(Collectors.toList());
                        Log.d(TAG, "✅ Found " + searchResponse.getTotal() + " notes matching query: '" + query + "'");
                        
                        // Log each note's title and location for debugging
                        for (Note note : notes) {
                            String location = note.getGeofence() != null && note.getGeofence().getAddressName() != null 
                                ? note.getGeofence().getAddressName() 
                                : "no location";
                            Log.d(TAG, "  - Note: '" + note.getTitle() + "' at " + location);
                        }
                        
                        callback.onSuccess(notes);
                    } else {
                        Log.e(TAG, "❌ Search response items is null");
                        callback.onError("No results found");
                    }
                } else {
                    String errorMsg = "Failed to search notes: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e(TAG, "❌ Error response body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error searching notes", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Tag Operations ====================
    
    /**
     * Get all tags
     */
    public void getAllTags(TagsCallback callback) {
        getApiService().getAllTags().enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(@NonNull Call<List<Tag>> call, @NonNull Response<List<Tag>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load tags: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<Tag>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading tags", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Create a new tag
     */
    public void createTag(String name, String color, Callback<Tag> callback) {
        CreateTagRequest request = new CreateTagRequest(name, color);
        getApiService().createTag(request).enqueue(callback);
    }
    
    /**
     * Set tags for a note (replaces existing tags)
     */
    public void setNoteTags(String noteId, List<String> tagIds, NoteCallback callback) {
        SetTagsRequest request = new SetTagsRequest(tagIds);
        getApiService().setNoteTags(noteId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    callback.onSuccess(note);
                } else {
                    callback.onError("Failed to set tags: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error setting tags", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Reminder Operations ====================
    
    /**
     * Set time reminder
     */
    public void setTimeReminder(String noteId, Instant reminderTime, NoteCallback callback) {
        // Convert Instant to LocalDateTime in system default timezone
        java.time.ZoneId systemZone = java.time.ZoneId.systemDefault();
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.ofInstant(reminderTime, systemZone);
        String localDateTimeStr = localDateTime.toString(); // ISO format: 2024-11-04T10:30:00
        String timeZoneStr = systemZone.getId(); // e.g., "America/Los_Angeles"
        
        TimeReminderRequest request = new TimeReminderRequest(localDateTimeStr, timeZoneStr);
        
        getApiService().setTimeReminder(noteId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    callback.onSuccess(note);
                } else {
                    callback.onError("Failed to set reminder: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error setting reminder", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Set geofence reminder
     */
    public void setGeofence(String noteId, Geofence geofence, NoteCallback callback) {
        GeofenceRequest request = new GeofenceRequest(
                geofence.getLatitude(),
                geofence.getLongitude(),
                geofence.getRadius(),
                geofence.getAddressName()
        );
        
        // Log the request object as JSON
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String requestJson = gson.toJson(request);
            Log.d(TAG, "📍 Saving geofence for note " + noteId);
            Log.d(TAG, "📤 REQUEST JSON: " + requestJson);
        } catch (Exception e) {
            Log.e(TAG, "Error serializing request to JSON", e);
        }
        
        getApiService().setGeofence(noteId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NoteResponse noteResponse = response.body();
                    
                    // Log the response geofence data
                    if (noteResponse.getGeofence() != null) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            String responseJson = gson.toJson(noteResponse.getGeofence());
                            Log.d(TAG, "📥 RESPONSE GEOFENCE JSON: " + responseJson);
                        } catch (Exception e) {
                            Log.e(TAG, "Error serializing response to JSON", e);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Backend response has NULL geofence!");
                    }
                    
                    Note note = mapToNote(noteResponse);
                    callback.onSuccess(note);
                } else {
                    Log.e(TAG, "❌ Failed to set geofence: " + response.code());
                    callback.onError("Failed to set geofence: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error setting geofence", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Clear all reminders (time and geofence)
     * Backend endpoint: DELETE /api/notes/{id}/reminder
     */
    public void clearReminders(String noteId, SimpleCallback callback) {
        Log.d(TAG, "🗑️ Clearing all reminders for note " + noteId);
        
        getApiService().clearReminders(noteId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Reminders cleared successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "❌ Failed to clear reminders: " + response.code());
                    callback.onError("Failed to clear reminders: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "💥 Error clearing reminders", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Attachment Operations ====================
    
    /**
     * Upload a photo attachment
     */
    public void uploadPhoto(String noteId, Uri photoUri, AttachmentCallback callback) {
        try {
            Log.d(TAG, "📸 Starting photo upload for note: " + noteId + ", URI: " + photoUri);
            File file = createFileFromUri(photoUri);
            Log.d(TAG, "📸 Created temp file: " + file.getAbsolutePath() + ", size: " + file.length() + " bytes");

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            Log.d(TAG, "📸 Sending upload request to backend...");
            getApiService().uploadPhoto(noteId, body).enqueue(new Callback<NoteResponse>() {
                @Override
                public void onResponse(@NonNull Call<NoteResponse> call,
                                     @NonNull Response<NoteResponse> response) {
                    Log.d(TAG, "📸 Upload response code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        NoteResponse noteResponse = response.body();
                        // Extract attachment info from the image field
                        String attachmentId = noteResponse.getImage() != null ? noteResponse.getImage().getId() : null;
                        String mediaUrl = noteResponse.getImage() != null ? noteResponse.getImage().getUrl() : null;
                        Log.d(TAG, "✅ Photo upload successful! Attachment ID: " + attachmentId + ", URL: " + mediaUrl);
                        callback.onSuccess(attachmentId, mediaUrl);
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, "❌ Photo upload failed: " + response.code() + ", error: " + errorBody);
                        callback.onError("Failed to upload photo: " + response.code() + " - " + errorBody);
                    }
                    // Clean up temp file
                    file.delete();
                    Log.d(TAG, "🗑️ Temp file deleted");
                }

                @Override
                public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "❌ Network error uploading photo", t);
                    callback.onError("Network error: " + t.getMessage());
                    file.delete();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "❌ Error creating file from URI", e);
            callback.onError("Failed to read photo file: " + e.getMessage());
        }
    }
    
    /**
     * Upload an audio attachment
     */
    public void uploadAudio(String noteId, Uri audioUri, int durationSec, AttachmentCallback callback) {
        try {
            Log.d(TAG, "🎵 Starting audio upload for note: " + noteId + ", URI: " + audioUri + ", duration: " + durationSec + "s");
            File file = createFileFromUri(audioUri);
            Log.d(TAG, "🎵 Created temp file: " + file.getAbsolutePath() + ", size: " + file.length() + " bytes");

            RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            Log.d(TAG, "🎵 Sending upload request to backend...");
            getApiService().uploadAudio(noteId, body, durationSec).enqueue(new Callback<NoteResponse>() {
                @Override
                public void onResponse(@NonNull Call<NoteResponse> call,
                                     @NonNull Response<NoteResponse> response) {
                    Log.d(TAG, "🎵 Upload response code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        NoteResponse noteResponse = response.body();
                        // Extract attachment info from the audio field
                        String attachmentId = noteResponse.getAudio() != null ? noteResponse.getAudio().getId() : null;
                        String mediaUrl = noteResponse.getAudio() != null ? noteResponse.getAudio().getUrl() : null;
                        Log.d(TAG, "✅ Audio upload successful! Attachment ID: " + attachmentId + ", URL: " + mediaUrl);
                        callback.onSuccess(attachmentId, mediaUrl);
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, "❌ Audio upload failed: " + response.code() + ", error: " + errorBody);
                        callback.onError("Failed to upload audio: " + response.code() + " - " + errorBody);
                    }
                    file.delete();
                    Log.d(TAG, "🗑️ Temp file deleted");
                }

                @Override
                public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "❌ Network error uploading audio", t);
                    callback.onError("Network error: " + t.getMessage());
                    file.delete();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "❌ Error creating file from URI", e);
            callback.onError("Failed to read audio file: " + e.getMessage());
        }
    }

    /**
     * Delete a photo attachment from backend
     */
    public void deletePhotoAttachment(String noteId, String attachmentId, SimpleCallback callback) {
        Log.d(TAG, "🗑️ Deleting photo attachment: " + attachmentId + " from note: " + noteId);

        getApiService().deletePhotoAttachment(noteId, attachmentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Photo attachment deleted successfully");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to delete photo: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error deleting photo", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Delete an audio attachment from backend
     */
    public void deleteAudioAttachment(String noteId, String attachmentId, SimpleCallback callback) {
        Log.d(TAG, "🗑️ Deleting audio attachment: " + attachmentId + " from note: " + noteId);

        getApiService().deleteAudioAttachment(noteId, attachmentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Audio attachment deleted successfully");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to delete audio: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error deleting audio", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // ==================== Helper Methods ====================
    
    /**
     * Map NoteResponse DTO to Note model
     */
    private Note mapToNote(NoteResponse response) {
        Note note = new Note();
        note.setId(response.getId());  // Already a String, no parsing needed
        note.setTitle(response.getTitle());
        note.setText(response.getText());
        note.setPinned(response.getPinned() != null && response.getPinned());
        note.setLastEdited(response.getLastEdited());
        note.setCreatedAt(response.getCreatedAt());
        
        // Map tags
        if (response.getTags() != null) {
            note.setTags(response.getTags());
        }
        
        // Map geofence
        if (response.getGeofence() != null) {
            note.setGeofence(response.getGeofence());
        }
        
        // Map time reminder
        if (response.getReminderTimeUtc() != null) {
            note.setReminderTime(response.getReminderTimeUtc());
        }
        
        // Map attachments
        List<Attachment> attachments = new ArrayList<>();
        if (response.getImage() != null) {
            NoteResponse.AttachmentResponse img = response.getImage();
            Attachment photoAttachment = new Attachment(
                    img.getId(),  // Already a String
                    Attachment.AttachmentType.PHOTO,
                    img.getUrl(),
                    null
            );
            attachments.add(photoAttachment);
        }
        if (response.getAudio() != null) {
            NoteResponse.AttachmentResponse aud = response.getAudio();
            Attachment audioAttachment = new Attachment(
                    aud.getId(),  // Already a String
                    Attachment.AttachmentType.AUDIO,
                    aud.getUrl(),
                    aud.getDurationSec()
            );
            attachments.add(audioAttachment);
        }
        note.setAttachments(attachments);
        
        return note;
    }
    
    /**
     * Create a temporary file from URI (for multipart upload)
     */
    private File createFileFromUri(Uri uri) throws IOException {
        File tempFile = File.createTempFile("upload", null, context.getCacheDir());
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Cannot open input stream from URI");
            }
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
    
    // ==================== Template Operations ====================
    
    /**
     * Get all templates for the current user
     */
    public void getAllTemplates(TemplatesCallback callback) {
        Log.d(TAG, "📋 Fetching all templates...");
        
        getApiService().getAllTemplates().enqueue(new Callback<List<com.example.anchornotes_team3.dto.TemplateResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.example.anchornotes_team3.dto.TemplateResponse>> call,
                                 @NonNull Response<List<com.example.anchornotes_team3.dto.TemplateResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.example.anchornotes_team3.model.Template> templates = response.body().stream()
                        .map(NoteRepository.this::mapToTemplate)
                        .collect(Collectors.toList());
                    Log.d(TAG, "✅ Loaded " + templates.size() + " templates");
                    callback.onSuccess(templates);
                } else {
                    String errorMsg = "Failed to load templates: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<com.example.anchornotes_team3.dto.TemplateResponse>> call,
                                @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error loading templates", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Create a new template
     */
    public void createTemplate(com.example.anchornotes_team3.model.Template template, TemplateCallback callback) {
        Log.d(TAG, "📋 Creating template: " + template.getName());
        
        // Convert template to request DTO
        com.example.anchornotes_team3.dto.CreateTemplateRequest request = new com.example.anchornotes_team3.dto.CreateTemplateRequest();
        request.setName(template.getName());
        request.setText(template.getText());
        request.setPinned(template.getPinned());
        request.setGeofence(template.getGeofence());
        
        // Convert tag IDs from String to Long
        if (template.getTags() != null && !template.getTags().isEmpty()) {
            List<Long> tagIds = template.getTags().stream()
                .map(tag -> {
                    try {
                        return Long.parseLong(tag.getId());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid tag ID format: " + tag.getId());
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());
            request.setTagIds(tagIds);
        }
        
        getApiService().createTemplate(request).enqueue(new Callback<com.example.anchornotes_team3.dto.TemplateResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.anchornotes_team3.dto.TemplateResponse> call,
                                 @NonNull Response<com.example.anchornotes_team3.dto.TemplateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.anchornotes_team3.model.Template createdTemplate = mapToTemplate(response.body());
                    Log.d(TAG, "✅ Template created: " + createdTemplate.getId());
                    callback.onSuccess(createdTemplate);
                } else {
                    String errorMsg = "Failed to create template: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.example.anchornotes_team3.dto.TemplateResponse> call,
                                @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error creating template", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Update an existing template
     */
    public void updateTemplate(String templateId, com.example.anchornotes_team3.model.Template template, TemplateCallback callback) {
        Log.d(TAG, "📋 Updating template: " + templateId);
        
        // Convert template to request DTO
        com.example.anchornotes_team3.dto.CreateTemplateRequest request = new com.example.anchornotes_team3.dto.CreateTemplateRequest();
        request.setName(template.getName());
        request.setText(template.getText());
        request.setPinned(template.getPinned());
        request.setGeofence(template.getGeofence());
        
        // Convert tag IDs from String to Long
        if (template.getTags() != null && !template.getTags().isEmpty()) {
            List<Long> tagIds = template.getTags().stream()
                .map(tag -> {
                    try {
                        return Long.parseLong(tag.getId());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid tag ID format: " + tag.getId());
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());
            request.setTagIds(tagIds);
        }
        
        getApiService().updateTemplate(templateId, request).enqueue(new Callback<com.example.anchornotes_team3.dto.TemplateResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.anchornotes_team3.dto.TemplateResponse> call,
                                 @NonNull Response<com.example.anchornotes_team3.dto.TemplateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.anchornotes_team3.model.Template updatedTemplate = mapToTemplate(response.body());
                    Log.d(TAG, "✅ Template updated: " + updatedTemplate.getId());
                    callback.onSuccess(updatedTemplate);
                } else {
                    String errorMsg = "Failed to update template: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.example.anchornotes_team3.dto.TemplateResponse> call,
                                @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error updating template", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Delete a template
     */
    public void deleteTemplate(String templateId, SimpleCallback callback) {
        Log.d(TAG, "🗑️ Deleting template: " + templateId);
        
        getApiService().deleteTemplate(templateId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Template deleted successfully");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to delete template: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error deleting template", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Instantiate a template into a new note
     */
    public void instantiateTemplate(String templateId, String noteTitle, NoteCallback callback) {
        Log.d(TAG, "📝 Instantiating template: " + templateId + " with title: " + noteTitle);
        
        com.example.anchornotes_team3.dto.InstantiateTemplateRequest request = 
            new com.example.anchornotes_team3.dto.InstantiateTemplateRequest(noteTitle);
        
        getApiService().instantiateTemplate(templateId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    Log.d(TAG, "✅ Template instantiated into note: " + note.getId());
                    callback.onSuccess(note);
                } else {
                    String errorMsg = "Failed to instantiate template: " + response.code();
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error instantiating template", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Map TemplateResponse DTO to Template model
     */
    private com.example.anchornotes_team3.model.Template mapToTemplate(com.example.anchornotes_team3.dto.TemplateResponse response) {
        com.example.anchornotes_team3.model.Template template = new com.example.anchornotes_team3.model.Template();
        template.setId(response.getId());
        template.setName(response.getName());
        template.setText(response.getText());
        template.setPinned(response.getPinned() != null && response.getPinned());
        
        // Map tags
        if (response.getTags() != null) {
            template.setTags(response.getTags());
        }
        
        // Map geofence
        if (response.getGeofence() != null) {
            template.setGeofence(response.getGeofence());
        }
        
        // Map attachments
        if (response.getImage() != null) {
            TemplateResponse.AttachmentResponse img = response.getImage();
            Attachment photoAttachment = new Attachment(
                    img.getId(),
                    Attachment.AttachmentType.PHOTO,
                    img.getUrl(),
                    null
            );
            template.setImage(photoAttachment);
        }
        if (response.getAudio() != null) {
            TemplateResponse.AttachmentResponse aud = response.getAudio();
            Attachment audioAttachment = new Attachment(
                    aud.getId(),
                    Attachment.AttachmentType.AUDIO,
                    aud.getUrl(),
                    aud.getDurationSec()
            );
            template.setAudio(audioAttachment);
        }
        
        return template;
    }
}
