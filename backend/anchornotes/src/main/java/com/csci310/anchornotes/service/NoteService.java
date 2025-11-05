package com.csci310.anchornotes.service;

import com.csci310.anchornotes.dto.geofence.GeofenceRequest;
import com.csci310.anchornotes.dto.note.*;
import com.csci310.anchornotes.dto.reminder.TimeReminderRequest;
import com.csci310.anchornotes.entity.Geofence;
import com.csci310.anchornotes.entity.Note;
import com.csci310.anchornotes.entity.Tag;
import com.csci310.anchornotes.exception.ResourceNotFoundException;
import com.csci310.anchornotes.repository.*;
import com.csci310.anchornotes.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final GeofenceRepository geofenceRepository;
    private final EntityMapper entityMapper;

    /**
     * Create a new note
     */
    @Transactional
    public NoteResponse createNote(String userId, CreateNoteRequest request) {
        log.info("Creating note for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);

        Note note = Note.builder()
                .userId(userUuid)
                .title(request.getTitle())
                .text(request.getText())
                .pinned(request.getPinned() != null ? request.getPinned() : false)
                .build();

        // Handle tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findByIdAndUserId(tagId, userUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
                    .collect(Collectors.toSet());
            note.setTags(tags);
        }

        // Handle geofence (if provided in creation)
        if (request.getGeofence() != null) {
            Geofence geofence = createOrFindGeofence(userId, request.getGeofence());
            note.setGeofence(geofence);
        }

        // Handle time reminder (if provided)
        if (request.getReminder() != null) {
            Instant reminderTime = convertToUtc(
                    request.getReminder().getLocalDateTime(),
                    request.getReminder().getTimeZone()
            );
            note.setReminderTime(reminderTime);
        }

        Note saved = noteRepository.save(note);
        log.info("Note created successfully with ID: {}", saved.getId());

        return entityMapper.toNoteResponse(saved);
    }

    /**
     * Get a single note by ID
     */
    @Transactional(readOnly = true)
    public NoteResponse getNote(String userId, Long noteId) {
        log.info("Fetching note {} for user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        return entityMapper.toNoteResponse(note);
    }

    /**
     * Get all notes for the current user
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getAllNotes(String userId) {
        log.info("Fetching all notes for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);
        List<Note> notes = noteRepository.findAllByUserIdOrderByLastEditedDesc(userUuid);

        log.info("Found {} notes for user: {}", notes.size(), userId);

        return notes.stream()
                .map(entityMapper::toNoteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing note
     */
    @Transactional
    public NoteResponse updateNote(String userId, Long noteId, UpdateNoteRequest request) {
        log.info("Updating note {} for user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getText() != null) {
            note.setText(request.getText());
        }
        if (request.getPinned() != null) {
            note.setPinned(request.getPinned());
        }

        // Update tags if provided
        if (request.getTagIds() != null) {
            Set<Tag> tags = request.getTagIds().stream()
                    .map(tagId -> tagRepository.findByIdAndUserId(tagId, userUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
                    .collect(Collectors.toSet());
            note.setTags(tags);
        }

        Note updated = noteRepository.save(note);
        log.info("Note {} updated successfully", noteId);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Delete a note
     */
    @Transactional
    public void deleteNote(String userId, Long noteId) {
        log.info("Deleting note {} for user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        if (!noteRepository.findByIdAndUserId(noteId, userUuid).isPresent()) {
            throw new ResourceNotFoundException("Note not found");
        }

        noteRepository.deleteByIdAndUserId(noteId, userUuid);
        log.info("Note {} deleted successfully", noteId);
    }

    /**
     * Pin or unpin a note
     */
    @Transactional
    public NoteResponse pinNote(String userId, Long noteId, Boolean pinned) {
        log.info("Setting pin status to {} for note {} and user: {}", pinned, noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        note.setPinned(pinned);
        Note updated = noteRepository.save(note);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Set tags on a note
     */
    @Transactional
    public NoteResponse setTags(String userId, Long noteId, List<Long> tagIds) {
        log.info("Setting tags for note {} and user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        Set<Tag> tags = new HashSet<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            tags = tagIds.stream()
                    .map(tagId -> tagRepository.findByIdAndUserId(tagId, userUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
                    .collect(Collectors.toSet());
        }

        note.setTags(tags);
        Note updated = noteRepository.save(note);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Set time reminder on a note
     * NOTE: Does NOT clear geofence - both can coexist
     */
    @Transactional
    public NoteResponse setTimeReminder(String userId, Long noteId, TimeReminderRequest request) {
        log.info("Setting time reminder for note {} and user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        Instant reminderTime = convertToUtc(
                request.getLocalDateTime(),
                request.getTimeZone()
        );

        note.setReminderTime(reminderTime);
        // NOTE: Does NOT clear geofence - both can coexist

        Note updated = noteRepository.save(note);
        log.info("Time reminder set for note {}: {}", noteId, reminderTime);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Set geofence reminder on a note
     * NOTE: Does NOT clear time reminder - both can coexist
     */
    @Transactional
    public NoteResponse setGeofenceReminder(String userId, Long noteId, GeofenceRequest request) {
        log.info("Setting geofence reminder for note {} and user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        Geofence geofence = createOrFindGeofence(userId, request);
        note.setGeofence(geofence);
        // NOTE: Does NOT clear reminderTime - both can coexist

        Note updated = noteRepository.save(note);
        log.info("Geofence reminder set for note {}", noteId);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Clear ALL reminders (both time and geofence)
     */
    @Transactional
    public void clearReminders(String userId, Long noteId) {
        log.info("Clearing all reminders for note {} and user: {}", noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        note.setReminderTime(null);
        note.setGeofence(null);
        noteRepository.save(note);

        log.info("All reminders cleared for note {}", noteId);
    }

    /**
     * Get relevant notes (within time window OR inside geofence)
     * CRITICAL: Supports BOTH time and geofence simultaneously
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getRelevantNotes(String userId, RelevantNotesRequest request) {
        log.info("Fetching relevant notes for user: {}", userId);

        Set<Note> relevantNotes = new HashSet<>();

        // 1. Time-based relevant notes (within ±1 hour)
        Instant now = request.getNowUtc();
        Instant oneHourBefore = now.minus(1, ChronoUnit.HOURS);
        Instant oneHourAfter = now.plus(1, ChronoUnit.HOURS);

        UUID userUuid = UUID.fromString(userId);
        List<Note> timeRelevant = noteRepository.findTimeRelevantNotes(
                userUuid, oneHourBefore, oneHourAfter
        );
        relevantNotes.addAll(timeRelevant);

        log.info("Found {} time-relevant notes", timeRelevant.size());

        // 2. Geofence-based relevant notes
        if (request.getInsideGeofenceIds() != null && !request.getInsideGeofenceIds().isEmpty()) {
            // Extract note IDs from geofence IDs (format: "note_123")
            List<Long> noteIds = request.getInsideGeofenceIds().stream()
                    .filter(geoId -> geoId.startsWith("note_"))
                    .map(geoId -> Long.parseLong(geoId.substring(5)))
                    .collect(Collectors.toList());

            if (!noteIds.isEmpty()) {
                List<Note> geoRelevant = noteRepository.findGeofenceRelevantNotes(userUuid, noteIds);
                relevantNotes.addAll(geoRelevant);
                log.info("Found {} geofence-relevant notes", geoRelevant.size());
            }
        }

        log.info("Total relevant notes: {}", relevantNotes.size());

        // Convert to responses and sort by lastEdited
        return relevantNotes.stream()
                .map(entityMapper::toNoteResponse)
                .sorted(Comparator.comparing(NoteResponse::getLastEdited).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Search notes by title and content only
     */
    @Transactional(readOnly = true)
    public SearchResponse searchNotes(String userId, SearchRequest request) {
        log.info("Searching notes for user: {} with query: {}", userId, request.getQ());

        Pageable pageable = PageRequest.of(
                request.getOffset() / request.getLimit(),
                request.getLimit()
        );

        UUID userUuid = UUID.fromString(userId);

        // Convert null query to empty string to avoid type ambiguity in PostgreSQL
        String query = request.getQ() != null ? request.getQ() : "";

        Page<Note> page = noteRepository.searchNotes(
                userUuid,
                query,
                pageable
        );

        List<NoteResponse> items = page.getContent().stream()
                .map(entityMapper::toNoteResponse)
                .collect(Collectors.toList());

        log.info("Search returned {} results out of {} total", items.size(), page.getTotalElements());

        return SearchResponse.builder()
                .total(page.getTotalElements())
                .items(items)
                .build();
    }

    /**
     * Filter notes with various criteria
     */
    @Transactional(readOnly = true)
    public SearchResponse filterNotes(String userId, com.csci310.anchornotes.dto.note.FilterRequest request) {
        log.info("Filtering notes for user: {}", userId);

        Pageable pageable = PageRequest.of(
                request.getOffset() / request.getLimit(),
                request.getLimit()
        );

        UUID userUuid = UUID.fromString(userId);

        // Convert tag IDs list to PostgreSQL array format
        boolean hasTagFilter = request.getTagIds() != null && !request.getTagIds().isEmpty();
        String tagIdsArray = hasTagFilter ?
            "{" + request.getTagIds().stream().map(String::valueOf).collect(Collectors.joining(",")) + "}" :
            "{}";

        Page<Note> page = noteRepository.filterNotes(
                userUuid,
                hasTagFilter,
                tagIdsArray,
                request.getHasPhoto(),
                request.getHasAudio(),
                request.getHasLocation(),
                request.getEditedStart(),
                request.getEditedEnd(),
                pageable
        );

        List<NoteResponse> items = page.getContent().stream()
                .map(entityMapper::toNoteResponse)
                .collect(Collectors.toList());

        log.info("Filter returned {} results out of {} total", items.size(), page.getTotalElements());

        return SearchResponse.builder()
                .total(page.getTotalElements())
                .items(items)
                .build();
    }

    // Helper methods

    /**
     * Create or find an existing geofence
     */
    private Geofence createOrFindGeofence(String userId, GeofenceRequest request) {
        // Create new geofence (could be optimized by finding existing identical ones)
        UUID userUuid = UUID.fromString(userId);
        return geofenceRepository.save(Geofence.builder()
                .userId(userUuid)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radius(request.getRadius())
                .addressName(request.getAddressName())
                .build());
    }

    /**
     * Convert local date time to UTC instant
     */
    private Instant convertToUtc(String localDateTime, String timeZone) {
        try {
            ZoneId zone = ZoneId.of(timeZone);
            LocalDateTime ldt = LocalDateTime.parse(localDateTime);
            return ldt.atZone(zone).toInstant();
        } catch (Exception e) {
            log.error("Error converting time to UTC: localDateTime={}, timeZone={}", localDateTime, timeZone, e);
            throw new IllegalArgumentException("Invalid date time or timezone");
        }
    }
}
