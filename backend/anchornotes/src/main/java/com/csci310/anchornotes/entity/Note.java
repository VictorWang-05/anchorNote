package com.csci310.anchornotes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"tags", "geofence", "image", "audio"})
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_edited")
    private Instant lastEdited;

    // BOTH can be set - not mutually exclusive
    @Column(name = "reminder_time")
    private Instant reminderTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geofence")
    private Geofence geofence;

    // Many-to-Many with Tags via note_tags join table
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "note_tags",
        joinColumns = @JoinColumn(name = "note_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file")
    private PhotoAttachment image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_file")
    private AudioAttachment audio;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastEdited == null) {
            lastEdited = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastEdited = Instant.now();
    }

    // Helper methods
    @Transient
    public boolean hasPhoto() {
        return image != null;
    }

    @Transient
    public boolean hasAudio() {
        return audio != null;
    }
}
