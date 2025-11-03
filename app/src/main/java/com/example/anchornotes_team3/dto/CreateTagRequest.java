package com.example.anchornotes_team3.dto;

/**
 * Request DTO for creating a tag
 */
public class CreateTagRequest {
    private String name;
    private String color;

    public CreateTagRequest(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }
}

