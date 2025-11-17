package com.example.anchornotes_team3.model;

import java.util.List;

/**
 * Model class representing a Template
 */
public class Template {
    private String id;
    private String name;
    private String text;
    private Boolean pinned;
    private List<Tag> tags;
    private Geofence geofence;
    private Attachment image;
    private Attachment audio;
    private String backgroundColor;

    public Template() {
    }

    public Template(String id, String name, String text, Boolean pinned, List<Tag> tags, Geofence geofence, Attachment image, Attachment audio, String backgroundColor) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.pinned = pinned;
        this.tags = tags;
        this.geofence = geofence;
        this.image = image;
        this.audio = audio;
        this.backgroundColor = backgroundColor;
    }

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getPinned() {
        return pinned != null ? pinned : false;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public void setGeofence(Geofence geofence) {
        this.geofence = geofence;
    }

    public Attachment getImage() {
        return image;
    }

    public void setImage(Attachment image) {
        this.image = image;
    }

    public Attachment getAudio() {
        return audio;
    }

    public void setAudio(Attachment audio) {
        this.audio = audio;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}

