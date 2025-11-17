package com.csci310.anchornotes.service;

import com.csci310.anchornotes.dto.note.NoteResponse;
import com.csci310.anchornotes.dto.template.CreateTemplateRequest;
import com.csci310.anchornotes.dto.template.InstantiateTemplateRequest;
import com.csci310.anchornotes.dto.template.TemplateResponse;
import com.csci310.anchornotes.dto.template.UpdateTemplateRequest;
import com.csci310.anchornotes.entity.*;
import com.csci310.anchornotes.exception.ResourceNotFoundException;
import com.csci310.anchornotes.repository.GeofenceRepository;
import com.csci310.anchornotes.repository.NoteRepository;
import com.csci310.anchornotes.repository.TagRepository;
import com.csci310.anchornotes.repository.TemplateRepository;
import com.csci310.anchornotes.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TagRepository tagRepository;
    private final GeofenceRepository geofenceRepository;
    private final NoteRepository noteRepository;
    private final EntityMapper entityMapper;

    /**
     * Get all templates for a user
     */
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates(String userId) {
        log.info("Fetching all templates for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);
        List<Template> templates = templateRepository.findByUserId(userUuid);

        return templates.stream()
                .map(entityMapper::toTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new template
     */
    @Transactional
    public TemplateResponse createTemplate(String userId, CreateTemplateRequest request) {
        log.info("Creating template for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);

        Template template = Template.builder()
                .userId(userUuid)
                .name(request.getName())
                .text(request.getText())
                .pinned(request.getPinned() != null ? request.getPinned() : false)
                .backgroundColor(request.getBackgroundColor())
                .build();

        // Handle tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findByIdAndUserId(tagId, userUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
                    .collect(Collectors.toSet());
            template.setTags(tags);
        }

        // Handle geofence
        if (request.getGeofence() != null) {
            Geofence geofence = geofenceRepository.save(Geofence.builder()
                    .userId(userUuid)
                    .latitude(request.getGeofence().getLatitude())
                    .longitude(request.getGeofence().getLongitude())
                    .radius(request.getGeofence().getRadius())
                    .addressName(request.getGeofence().getAddressName())
                    .build());
            template.setGeofence(geofence);
        }

        Template saved = templateRepository.save(template);
        log.info("Template created successfully with ID: {}", saved.getId());

        return entityMapper.toTemplateResponse(saved);
    }

    /**
     * Update a template
     */
    @Transactional
    public TemplateResponse updateTemplate(String userId, Long templateId, UpdateTemplateRequest request) {
        log.info("Updating template {} for user: {}", templateId, userId);

        UUID userUuid = UUID.fromString(userId);

        Template template = templateRepository.findByIdAndUserId(templateId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getText() != null) {
            template.setText(request.getText());
        }
        if (request.getPinned() != null) {
            template.setPinned(request.getPinned());
        }
        if (request.getBackgroundColor() != null) {
            template.setBackgroundColor(request.getBackgroundColor());
        }

        // Update tags if provided
        if (request.getTagIds() != null) {
            Set<Tag> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findByIdAndUserId(tagId, userUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
                    .collect(Collectors.toSet());
            template.setTags(tags);
        }

        // Update geofence if provided
        if (request.getGeofence() != null) {
            Geofence geofence = geofenceRepository.save(Geofence.builder()
                    .userId(userUuid)
                    .latitude(request.getGeofence().getLatitude())
                    .longitude(request.getGeofence().getLongitude())
                    .radius(request.getGeofence().getRadius())
                    .addressName(request.getGeofence().getAddressName())
                    .build());
            template.setGeofence(geofence);
        }

        Template updated = templateRepository.save(template);
        log.info("Template {} updated successfully", templateId);

        return entityMapper.toTemplateResponse(updated);
    }

    /**
     * Delete a template
     */
    @Transactional
    public void deleteTemplate(String userId, Long templateId) {
        log.info("Deleting template {} for user: {}", templateId, userId);

        UUID userUuid = UUID.fromString(userId);

        if (!templateRepository.findByIdAndUserId(templateId, userUuid).isPresent()) {
            throw new ResourceNotFoundException("Template not found");
        }

        templateRepository.deleteByIdAndUserId(templateId, userUuid);
        log.info("Template {} deleted successfully", templateId);
    }

    /**
     * Instantiate a template to create a new note
     */
    @Transactional
    public NoteResponse instantiateTemplate(String userId, Long templateId, InstantiateTemplateRequest request) {
        log.info("Instantiating template {} for user: {}", templateId, userId);

        UUID userUuid = UUID.fromString(userId);

        Template template = templateRepository.findByIdAndUserId(templateId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        // Create note from template
        Note note = Note.builder()
                .userId(userUuid)
                .title(request.getTitle())
                .text(template.getText())
                .pinned(template.getPinned())
                .tags(new HashSet<>(template.getTags())) // Copy tags
                .geofence(template.getGeofence()) // Link to same geofence
                .backgroundColor(template.getBackgroundColor()) // Copy background color
                .build();

        Note saved = noteRepository.save(note);
        log.info("Template {} instantiated to note {}", templateId, saved.getId());

        return entityMapper.toNoteResponse(saved);
    }
}
