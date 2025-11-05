package com.csci310.anchornotes.util;

import com.csci310.anchornotes.dto.attachment.AttachmentResponse;
import com.csci310.anchornotes.dto.geofence.GeofenceResponse;
import com.csci310.anchornotes.dto.note.NoteResponse;
import com.csci310.anchornotes.dto.tag.TagResponse;
import com.csci310.anchornotes.dto.template.TemplateResponse;
import com.csci310.anchornotes.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping entities to DTOs
 */
@Component
public class EntityMapper {

    public NoteResponse toNoteResponse(Note note) {
        if (note == null) {
            return null;
        }

        return NoteResponse.builder()
                .id(note.getId().toString())
                .title(note.getTitle())
                .text(note.getText())
                .pinned(note.getPinned())
                .lastEdited(note.getLastEdited())
                .createdAt(note.getCreatedAt())
                .tags(toTagResponseList(note.getTags()))
                .geofence(note.getGeofence() != null ? toGeofenceResponse(note.getGeofence(), note.getId()) : null)
                .reminderTimeUtc(note.getReminderTime())
                .image(note.getImage() != null ? toAttachmentResponse(note.getImage()) : null)
                .audio(note.getAudio() != null ? toAttachmentResponse(note.getAudio()) : null)
                .hasPhoto(note.hasPhoto())
                .hasAudio(note.hasAudio())
                .build();
    }

    public TagResponse toTagResponse(Tag tag) {
        if (tag == null) {
            return null;
        }

        return TagResponse.builder()
                .id(tag.getId().toString())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }

    public List<TagResponse> toTagResponseList(Iterable<Tag> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }

        List<TagResponse> responses = new ArrayList<>();
        for (Tag tag : tags) {
            responses.add(toTagResponse(tag));
        }
        return responses;
    }

    public GeofenceResponse toGeofenceResponse(Geofence geofence, Long noteId) {
        if (geofence == null) {
            return null;
        }

        return GeofenceResponse.builder()
                .id(noteId != null ? geofence.getGeofenceId(noteId) : geofence.getId().toString())
                .latitude(geofence.getLatitude())
                .longitude(geofence.getLongitude())
                .radius(geofence.getRadius())
                .addressName(geofence.getAddressName())
                .build();
    }

    public AttachmentResponse toAttachmentResponse(PhotoAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return AttachmentResponse.builder()
                .id(attachment.getId().toString())
                .url(attachment.getMediaUrl())
                .durationSec(attachment.getDurationSec())
                .build();
    }

    public AttachmentResponse toAttachmentResponse(AudioAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return AttachmentResponse.builder()
                .id(attachment.getId().toString())
                .url(attachment.getMediaUrl())
                .durationSec(attachment.getDurationSec())
                .build();
    }

    public TemplateResponse toTemplateResponse(Template template) {
        if (template == null) {
            return null;
        }

        return TemplateResponse.builder()
                .id(template.getId().toString())
                .name(template.getName())
                .text(template.getText())
                .pinned(template.getPinned())
                .tags(toTagResponseList(template.getTags()))
                .geofence(template.getGeofence() != null ? toGeofenceResponse(template.getGeofence(), null) : null)
                .image(template.getImage() != null ? toAttachmentResponse(template.getImage()) : null)
                .audio(template.getAudio() != null ? toAttachmentResponse(template.getAudio()) : null)
                .build();
    }
}
