package com.csci310.anchornotes.dto.note;

import com.csci310.anchornotes.dto.tag.TagResponse;
import com.csci310.anchornotes.dto.geofence.GeofenceResponse;
import com.csci310.anchornotes.dto.attachment.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {
    private String id;
    private String title;
    private String text;
    private Boolean pinned;
    private Instant lastEdited;
    private Instant createdAt;
    private List<TagResponse> tags;
    private GeofenceResponse geofence;
    private Instant reminderTimeUtc;
    private AttachmentResponse image;
    private AttachmentResponse audio;
    private Boolean hasPhoto;
    private Boolean hasAudio;
}
