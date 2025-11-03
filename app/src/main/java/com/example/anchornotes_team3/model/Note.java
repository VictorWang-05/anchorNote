package com.example.anchornotes_team3.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a Note
 * Aligned with backend NoteResponse structure
 */
public class Note {
    private String id;  // Backend uses String IDs
    private String title;
    private String text;  // Changed from 'body' to 'text' to match backend
    private boolean pinned;
    private List<Tag> tags;  // Changed from List<String> to List<Tag>
    private Instant lastEdited;
    private Instant createdAt;
    
    // Reminder and Geofence are separate (backend supports both simultaneously)
    private Instant reminderTime;  // For time-based reminders
    private Geofence geofence;     // For location-based reminders
    
    private List<Attachment> attachments;
    
    // Helper flags
    private Boolean hasPhoto;
    private Boolean hasAudio;

    public Note() {
        this.title = "";
        this.text = "";
        this.pinned = false;
        this.tags = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.hasPhoto = false;
        this.hasAudio = false;
    }

    public Note(String id) {
        this();
        this.id = id;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public Instant getLastEdited() {
        return lastEdited;
    }

    public void setLastEdited(Instant lastEdited) {
        this.lastEdited = lastEdited;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Instant reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean hasTimeReminder() {
        return reminderTime != null;
    }

    public void clearTimeReminder() {
        this.reminderTime = null;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public void setGeofence(Geofence geofence) {
        this.geofence = geofence;
    }

    public boolean hasGeofence() {
        return geofence != null;
    }

    public void clearGeofence() {
        this.geofence = null;
    }

    public void clearAllReminders() {
        this.reminderTime = null;
        this.geofence = null;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        updateAttachmentFlags();
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
        updateAttachmentFlags();
    }

    public void removeAttachment(Attachment attachment) {
        this.attachments.remove(attachment);
        updateAttachmentFlags();
    }

    private void updateAttachmentFlags() {
        this.hasPhoto = false;
        this.hasAudio = false;
        for (Attachment att : attachments) {
            if (att.getType() == Attachment.AttachmentType.PHOTO) {
                this.hasPhoto = true;
            } else if (att.getType() == Attachment.AttachmentType.AUDIO) {
                this.hasAudio = true;
            }
        }
    }

    public Boolean getHasPhoto() {
        return hasPhoto;
    }

    public Boolean getHasAudio() {
        return hasAudio;
    }
}

