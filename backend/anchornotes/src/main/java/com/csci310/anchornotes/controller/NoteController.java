package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.ApiResponse;
import com.csci310.anchornotes.dto.geofence.GeofenceRequest;
import com.csci310.anchornotes.dto.note.*;
import com.csci310.anchornotes.dto.reminder.TimeReminderRequest;
import com.csci310.anchornotes.service.NoteService;
import com.csci310.anchornotes.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {

    private final NoteService noteService;
    private final UserContextUtil userContextUtil;

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            Authentication auth,
            @Valid @RequestBody CreateNoteRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.createNote(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAllNotes(Authentication auth) {
        String userId = userContextUtil.getCurrentUserId(auth);
        List<NoteResponse> response = noteService.getAllNotes(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNote(
            Authentication auth,
            @PathVariable Long id) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.getNote(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.updateNote(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            Authentication auth,
            @PathVariable Long id) {
        String userId = userContextUtil.getCurrentUserId(auth);
        noteService.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<NoteResponse> pinNote(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody PinRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.pinNote(userId, id, request.getPinned());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<NoteResponse> setTags(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody SetTagsRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.setTags(userId, id, request.getTagIds());
        return ResponseEntity.ok(response);
    }

    // REMINDER ENDPOINTS - Both can be set independently

    @PutMapping("/{id}/reminder/time")
    public ResponseEntity<Map<String, Object>> setTimeReminder(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody TimeReminderRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.setTimeReminder(userId, id, request);

        return ResponseEntity.ok(Map.of(
            "noteId", response.getId(),
            "reminderTimeUtc", response.getReminderTimeUtc()
        ));
    }

    @PutMapping("/{id}/reminder/geofence")
    public ResponseEntity<NoteResponse> setGeofenceReminder(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody GeofenceRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = noteService.setGeofenceReminder(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/reminder")
    public ResponseEntity<Void> clearReminders(
            Authentication auth,
            @PathVariable Long id) {
        String userId = userContextUtil.getCurrentUserId(auth);
        noteService.clearReminders(userId, id);
        return ResponseEntity.noContent().build();
    }

    // SEARCH & FILTER

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchNotes(
            Authentication auth,
            @ModelAttribute SearchRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        SearchResponse response = noteService.searchNotes(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<SearchResponse> filterNotes(
            Authentication auth,
            @ModelAttribute com.csci310.anchornotes.dto.note.FilterRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        SearchResponse response = noteService.filterNotes(userId, request);
        return ResponseEntity.ok(response);
    }

    // RELEVANT NOTES - Critical endpoint

    @PostMapping("/relevant-notes")
    public ResponseEntity<List<NoteResponse>> getRelevantNotes(
            Authentication auth,
            @Valid @RequestBody RelevantNotesRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        List<NoteResponse> response = noteService.getRelevantNotes(userId, request);
        return ResponseEntity.ok(response);
    }
}
