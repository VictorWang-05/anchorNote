package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.attachment.CompleteUploadRequest;
import com.csci310.anchornotes.dto.attachment.UploadRequest;
import com.csci310.anchornotes.dto.attachment.UploadResponse;
import com.csci310.anchornotes.dto.note.NoteResponse;
import com.csci310.anchornotes.service.AttachmentService;
import com.csci310.anchornotes.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/notes/{noteId}")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final UserContextUtil userContextUtil;

    // Photo endpoints

    @PostMapping("/photo")
    public ResponseEntity<UploadResponse> initiatePhotoUpload(
            Authentication auth,
            @PathVariable Long noteId,
            @Valid @RequestBody UploadRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        UploadResponse response = attachmentService.initiatePhotoUpload(userId, noteId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/photo/{attachmentId}/complete")
    public ResponseEntity<NoteResponse> completePhotoUpload(
            Authentication auth,
            @PathVariable Long noteId,
            @PathVariable Long attachmentId) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = attachmentService.completePhotoUpload(userId, noteId, attachmentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/photo/{attachmentId}")
    public ResponseEntity<Void> deletePhoto(
            Authentication auth,
            @PathVariable Long noteId,
            @PathVariable Long attachmentId) {
        String userId = userContextUtil.getCurrentUserId(auth);
        attachmentService.deletePhoto(userId, noteId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    // Audio endpoints

    @PostMapping("/audio")
    public ResponseEntity<UploadResponse> initiateAudioUpload(
            Authentication auth,
            @PathVariable Long noteId,
            @Valid @RequestBody UploadRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        UploadResponse response = attachmentService.initiateAudioUpload(userId, noteId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/audio/{attachmentId}/complete")
    public ResponseEntity<NoteResponse> completeAudioUpload(
            Authentication auth,
            @PathVariable Long noteId,
            @PathVariable Long attachmentId,
            @RequestBody(required = false) CompleteUploadRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = attachmentService.completeAudioUpload(userId, noteId, attachmentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/audio/{attachmentId}")
    public ResponseEntity<Void> deleteAudio(
            Authentication auth,
            @PathVariable Long noteId,
            @PathVariable Long attachmentId) {
        String userId = userContextUtil.getCurrentUserId(auth);
        attachmentService.deleteAudio(userId, noteId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    // Direct upload endpoints (single-step through backend)

    @PostMapping(value = "/photo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResponse> uploadPhoto(
            Authentication auth,
            @PathVariable Long noteId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = attachmentService.uploadPhoto(userId, noteId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/audio/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResponse> uploadAudio(
            Authentication auth,
            @PathVariable Long noteId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "durationSec", required = false) Integer durationSec) throws IOException {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = attachmentService.uploadAudio(userId, noteId, file, durationSec);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
