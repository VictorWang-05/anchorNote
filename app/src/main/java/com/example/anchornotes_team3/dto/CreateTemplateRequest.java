package com.example.anchornotes_team3.dto;

import com.example.anchornotes_team3.model.Geofence;

import java.util.List;

/**
 * Request DTO for creating a template
 */
public class CreateTemplateRequest {
    private String name;
    private String text;
    private Boolean pinned;
    private List<Long> tagIds;  // Backend expects Long IDs
    private Geofence geofence;
    private String backgroundColor;

    public CreateTemplateRequest() {
    }

    public CreateTemplateRequest(String name, String text, Boolean pinned, List<Long> tagIds, Geofence geofence) {
        this.name = name;
        this.text = text;
        this.pinned = pinned;
        this.tagIds = tagIds;
        this.geofence = geofence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public void setGeofence(Geofence geofence) {
        this.geofence = geofence;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}

