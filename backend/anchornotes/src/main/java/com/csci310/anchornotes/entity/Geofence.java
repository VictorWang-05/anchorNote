package com.csci310.anchornotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "geofence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"notes", "templates"})
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer radius;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "geofence", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<Note> notes = new HashSet<>();

    @OneToMany(mappedBy = "geofence", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<Template> templates = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Generate geofenceId for Android registration
    @Transient
    public String getGeofenceId(Long noteId) {
        return "note_" + noteId;
    }
}
