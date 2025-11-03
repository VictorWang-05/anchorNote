package com.example.anchornotes_team3.dto;

import java.util.List;

/**
 * Request DTO for setting tags on a note
 * Backend endpoint: PUT /api/notes/{id}/tags
 */
public class SetTagsRequest {
    private List<String> tagIds;

    public SetTagsRequest() {
    }

    public SetTagsRequest(List<String> tagIds) {
        this.tagIds = tagIds;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds = tagIds;
    }
}

