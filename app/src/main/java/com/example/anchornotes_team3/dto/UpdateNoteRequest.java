package com.example.anchornotes_team3.dto;

import java.util.List;

/**
 * Request DTO for updating a note
 */
public class UpdateNoteRequest {
    private String title;
    private String text;
    private Boolean pinned;
    private List<String> tagIds;  // Backend uses String IDs
    private String backgroundColor;

    public UpdateNoteRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds = tagIds;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}

