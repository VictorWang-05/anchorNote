package com.example.anchornotes_team3.dto;

/**
 * Request DTO for instantiating a template into a note
 */
public class InstantiateTemplateRequest {
    private String title;

    public InstantiateTemplateRequest() {
    }

    public InstantiateTemplateRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

