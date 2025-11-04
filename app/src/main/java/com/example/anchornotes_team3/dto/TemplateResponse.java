package com.example.anchornotes_team3.dto;

import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Geofence;
import com.example.anchornotes_team3.model.Tag;

import java.util.List;

/**
 * Response DTO for Template from backend
 */
public class TemplateResponse {
    private String id;
    private String name;
    private String text;
    private Boolean pinned;
    private List<Tag> tags;
    private Geofence geofence;
    private AttachmentResponse image;
    private AttachmentResponse audio;

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public AttachmentResponse getImage() {
        return image;
    }

    public AttachmentResponse getAudio() {
        return audio;
    }
    
    /**
     * Nested class for attachment response
     */
    public static class AttachmentResponse {
        private String id;
        private String url;
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

