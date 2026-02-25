package com.csci310.anchornotes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "photo_attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private AttachmentStatus status = AttachmentStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
