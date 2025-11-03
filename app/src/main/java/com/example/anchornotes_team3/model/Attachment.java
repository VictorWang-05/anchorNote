package com.example.anchornotes_team3.model;

import android.net.Uri;

/**
 * Model class representing an Attachment (photo or audio)
 * Stores both local URI and backend attachment ID
 */
public class Attachment {
    
    public enum AttachmentType {
        PHOTO, AUDIO
    }

    private String id;  // Backend attachment ID (returned after upload) - String to match backend
    private AttachmentType type;
    private Uri uri;  // Local URI (for display before/after upload)
    private String mediaUrl;  // Backend URL (from server after upload)
    private Integer durationSec; // For audio only
    private String displayName;
    private boolean isUploaded;  // Track upload status

    public Attachment(AttachmentType type, Uri uri) {
        this.type = type;
        this.uri = uri;
        this.displayName = "";
        this.isUploaded = false;
    }

    public Attachment(AttachmentType type, Uri uri, int durationSec) {
        this.type = type;
        this.uri = uri;
        this.durationSec = durationSec;
        this.displayName = "";
        this.isUploaded = false;
    }

    // Constructor for attachments loaded from backend
    public Attachment(String id, AttachmentType type, String mediaUrl, Integer durationSec) {
        this.id = id;
        this.type = type;
        this.mediaUrl = mediaUrl;
        this.durationSec = durationSec;
        this.isUploaded = true;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean uploaded) {
        isUploaded = uploaded;
    }

    public String getFormattedDuration() {
        if (durationSec == null || type != AttachmentType.AUDIO) {
            return "";
        }
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}

