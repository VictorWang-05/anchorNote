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
    private ApiService getApiService() {
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
        String isoTime = reminderTime.toString();
        TimeReminderRequest request = new TimeReminderRequest(isoTime);
        
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
                geofence.getRadius()
        );
        
        getApiService().setGeofence(noteId, request).enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<NoteResponse> call, @NonNull Response<NoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Note note = mapToNote(response.body());
                    callback.onSuccess(note);
                } else {
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
            File file = createFileFromUri(photoUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            
            getApiService().uploadPhoto(noteId, body).enqueue(new Callback<NoteResponse>() {
                @Override
                public void onResponse(@NonNull Call<NoteResponse> call, 
                                     @NonNull Response<NoteResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        NoteResponse noteResponse = response.body();
                        // Extract attachment info from the image field
                        String attachmentId = noteResponse.getImage() != null ? noteResponse.getImage().getId() : null;
                        String mediaUrl = noteResponse.getImage() != null ? noteResponse.getImage().getUrl() : null;
                        callback.onSuccess(attachmentId, mediaUrl);
                    } else {
                        callback.onError("Failed to upload photo: " + response.code());
                    }
                    // Clean up temp file
                    file.delete();
                }
                
                @Override
                public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading photo", t);
                    callback.onError("Network error: " + t.getMessage());
                    file.delete();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error creating file from URI", e);
            callback.onError("Failed to read photo file");
        }
    }
    
    /**
     * Upload an audio attachment
     */
    public void uploadAudio(String noteId, Uri audioUri, int durationSec, AttachmentCallback callback) {
        try {
            File file = createFileFromUri(audioUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            
            getApiService().uploadAudio(noteId, body, durationSec).enqueue(new Callback<NoteResponse>() {
                @Override
                public void onResponse(@NonNull Call<NoteResponse> call,
                                     @NonNull Response<NoteResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        NoteResponse noteResponse = response.body();
                        // Extract attachment info from the audio field
                        String attachmentId = noteResponse.getAudio() != null ? noteResponse.getAudio().getId() : null;
                        String mediaUrl = noteResponse.getAudio() != null ? noteResponse.getAudio().getUrl() : null;
                        callback.onSuccess(attachmentId, mediaUrl);
                    } else {
                        callback.onError("Failed to upload audio: " + response.code());
                    }
                    file.delete();
                }
                
                @Override
                public void onFailure(@NonNull Call<NoteResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading audio", t);
                    callback.onError("Network error: " + t.getMessage());
                    file.delete();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error creating file from URI", e);
            callback.onError("Failed to read audio file");
        }
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
}
