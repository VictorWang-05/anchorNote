package com.example.anchornotes_team3.api;

import com.example.anchornotes_team3.dto.*;
import com.example.anchornotes_team3.model.Tag;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API service interface
 * Defines all API endpoints aligned with backend
 */
public interface ApiService {
    
    // ==================== Authentication ====================
    
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
    
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);
    
    // ==================== Notes ====================
    
    @GET("api/notes")
    Call<List<NoteResponse>> getAllNotes();
    
    @GET("api/notes/{id}")
    Call<NoteResponse> getNoteById(@Path("id") String id);
    
    @POST("api/notes")
    Call<NoteResponse> createNote(@Body CreateNoteRequest request);
    
    @PUT("api/notes/{id}")
    Call<NoteResponse> updateNote(@Path("id") String id, @Body UpdateNoteRequest request);
    
    @DELETE("api/notes/{id}")
    Call<Void> deleteNote(@Path("id") String id);
    
    @POST("api/notes/{id}/pin")
    Call<NoteResponse> pinNote(@Path("id") String id, @Body PinRequest request);
    
    // ==================== Tags ====================
    
    @GET("api/tags")
    Call<List<Tag>> getAllTags();
    
    @POST("api/tags")
    Call<Tag> createTag(@Body CreateTagRequest request);
    
    @DELETE("api/tags/{id}")
    Call<Void> deleteTag(@Path("id") String id);
    
    // Set all tags for a note (replaces existing tags)
    @PUT("api/notes/{id}/tags")
    Call<NoteResponse> setNoteTags(@Path("id") String noteId, @Body SetTagsRequest request);
    
    // ==================== Reminders ====================
    
    @PUT("api/notes/{id}/reminder/time")
    Call<NoteResponse> setTimeReminder(@Path("id") String id, @Body TimeReminderRequest request);
    
    @PUT("api/notes/{id}/reminder/geofence")
    Call<NoteResponse> setGeofence(@Path("id") String id, @Body GeofenceRequest request);
    
    // Clears ALL reminders (both time and geofence)
    @DELETE("api/notes/{id}/reminder")
    Call<Void> clearReminders(@Path("id") String id);
    
    // ==================== Attachments ====================
    
    @Multipart
    @POST("api/attachments/photo")
    Call<AttachmentUploadResponse> uploadPhoto(
            @Part("noteId") String noteId,
            @Part MultipartBody.Part file
    );
    
    @Multipart
    @POST("api/attachments/audio")
    Call<AttachmentUploadResponse> uploadAudio(
            @Part("noteId") String noteId,
            @Part MultipartBody.Part file,
            @Part("durationSec") Integer durationSec
    );
    
    @DELETE("api/attachments/photo/{id}")
    Call<Void> deletePhotoAttachment(@Path("id") String id);
    
    @DELETE("api/attachments/audio/{id}")
    Call<Void> deleteAudioAttachment(@Path("id") String id);
    
    // ==================== Search & Filter ====================
    
    @GET("api/notes/search")
    Call<List<NoteResponse>> searchNotes(@Query("q") String query);
    
    @GET("api/notes/filter")
    Call<List<NoteResponse>> filterNotes(
            @Query("tagIds") List<String> tagIds,
            @Query("pinned") Boolean pinned,
            @Query("hasPhoto") Boolean hasPhoto,
            @Query("hasAudio") Boolean hasAudio
    );
    
    // ==================== Response DTO for upload ====================
    
    /**
     * Response DTO for attachment upload
     */
    class AttachmentUploadResponse {
        private String attachmentId;
        private String mediaUrl;
        
        public String getAttachmentId() {
            return attachmentId;
        }
        
        public String getMediaUrl() {
            return mediaUrl;
        }
    }
}

