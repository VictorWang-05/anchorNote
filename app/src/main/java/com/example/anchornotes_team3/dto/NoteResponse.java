package com.example.anchornotes_team3.dto;

import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Geofence;
import com.example.anchornotes_team3.model.Tag;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for Note from backend
 */
public class NoteResponse {
    private String id;  // Backend returns as String
    private String title;
    private String text;
    private Boolean pinned;
    private Instant lastEdited;
    private Instant createdAt;
    private List<Tag> tags;
    private Geofence geofence;
    private Instant reminderTimeUtc;
    private AttachmentResponse image;
    private AttachmentResponse audio;
    private Boolean hasPhoto;
    private Boolean hasAudio;
    private String backgroundColor;

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public Instant getLastEdited() {
        return lastEdited;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public Instant getReminderTimeUtc() {
        return reminderTimeUtc;
    }

    public AttachmentResponse getImage() {
        return image;
    }

    public AttachmentResponse getAudio() {
        return audio;
    }

    public Boolean getHasPhoto() {
        return hasPhoto;
    }

    public Boolean getHasAudio() {
        return hasAudio;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Nested class for attachment response
     */
    public static class AttachmentResponse {
        private String id;
        private String url;  // Backend returns "url", not "mediaUrl"
        private Integer durationSec;

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public Integer getDurationSec() {
            return durationSec;
        }
    }
}

