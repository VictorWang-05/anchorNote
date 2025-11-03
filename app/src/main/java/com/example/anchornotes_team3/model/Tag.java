package com.example.anchornotes_team3.model;

/**
 * Model class representing a Tag
 * Aligned with backend TagResponse structure
 */
public class Tag {
    private String id;  // Backend uses String IDs
    private String name;
    private String color;  // Hex color code (e.g., "#FF5733")

    public Tag() {
    }

    public Tag(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id != null && id.equals(tag.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return name;
    }
}

