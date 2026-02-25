package com.csci310.anchornotes.service;

import com.csci310.anchornotes.dto.attachment.CompleteUploadRequest;
import com.csci310.anchornotes.dto.attachment.UploadRequest;
import com.csci310.anchornotes.dto.attachment.UploadResponse;
import com.csci310.anchornotes.dto.note.NoteResponse;
import com.csci310.anchornotes.entity.AttachmentStatus;
import com.csci310.anchornotes.entity.AudioAttachment;
import com.csci310.anchornotes.entity.Note;
import com.csci310.anchornotes.entity.PhotoAttachment;
import com.csci310.anchornotes.exception.ResourceNotFoundException;
import com.csci310.anchornotes.repository.AudioAttachmentRepository;
import com.csci310.anchornotes.repository.NoteRepository;
import com.csci310.anchornotes.repository.PhotoAttachmentRepository;
import com.csci310.anchornotes.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final PhotoAttachmentRepository attachmentRepository;
    private final AudioAttachmentRepository audioAttachmentRepository;
    private final NoteRepository noteRepository;
    private final SupabaseStorageService storageService;
    private final EntityMapper entityMapper;

    /**
     * Initiate photo upload
     */
    @Transactional
    public UploadResponse initiatePhotoUpload(String userId, Long noteId, UploadRequest request) {
        log.info("Initiating photo upload for note {} and user: {}", noteId, userId);

        // Verify note exists
        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Create attachment record
        PhotoAttachment attachment = PhotoAttachment.builder()
                .userId(userUuid)
                .mediaType(request.getMime())
                .status(AttachmentStatus.PENDING)
                .build();

        PhotoAttachment saved = attachmentRepository.save(attachment);

        // Generate pre-signed URL
        SupabaseStorageService.UploadUrlResponse uploadUrl = storageService.generateUploadUrl(
                "attachment",
                request.getFileName(),
                request.getMime()
        );

        // Store the file path in the attachment
        saved.setMediaUrl(uploadUrl.getFilePath());
        attachmentRepository.save(saved);

        log.info("Photo upload initiated with attachment ID: {}", saved.getId());

        return UploadResponse.builder()
                .uploadUrl(uploadUrl.getUploadUrl())
                .attachmentId(saved.getId().toString())
                .build();
    }

    /**
     * Complete photo upload
     */
    @Transactional
    public NoteResponse completePhotoUpload(String userId, Long noteId, Long attachmentId) {
        log.info("Completing photo upload for note {}, attachment {}, user: {}", noteId, attachmentId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        PhotoAttachment attachment = attachmentRepository.findByIdAndUserId(attachmentId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Update attachment status
        attachment.setStatus(AttachmentStatus.COMPLETED);
        String publicUrl = storageService.getPublicUrl("attachment", attachment.getMediaUrl());
        attachment.setMediaUrl(publicUrl);

        // Link to note
        note.setImage(attachment);

        Note updated = noteRepository.save(note);
        log.info("Photo upload completed for note {}", noteId);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Initiate audio upload
     */
    @Transactional
    public UploadResponse initiateAudioUpload(String userId, Long noteId, UploadRequest request) {
        log.info("Initiating audio upload for note {} and user: {}", noteId, userId);

        // Verify note exists
        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Create attachment record
        AudioAttachment attachment = AudioAttachment.builder()
                .userId(userUuid)
                .mediaType(request.getMime())
                .status(AttachmentStatus.PENDING)
                .build();

        AudioAttachment saved = audioAttachmentRepository.save(attachment);

        // Generate pre-signed URL
        SupabaseStorageService.UploadUrlResponse uploadUrl = storageService.generateUploadUrl(
                "attachment",
                request.getFileName(),
                request.getMime()
        );

        // Store the file path in the attachment
        saved.setMediaUrl(uploadUrl.getFilePath());
        audioAttachmentRepository.save(saved);

        log.info("Audio upload initiated with attachment ID: {}", saved.getId());

        return UploadResponse.builder()
                .uploadUrl(uploadUrl.getUploadUrl())
                .attachmentId(saved.getId().toString())
                .build();
    }

    /**
     * Complete audio upload
     */
    @Transactional
    public NoteResponse completeAudioUpload(String userId, Long noteId, Long attachmentId, CompleteUploadRequest request) {
        log.info("Completing audio upload for note {}, attachment {}, user: {}", noteId, attachmentId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        AudioAttachment attachment = audioAttachmentRepository.findByIdAndUserId(attachmentId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Update attachment status
        attachment.setStatus(AttachmentStatus.COMPLETED);
        String publicUrl = storageService.getPublicUrl("attachment", attachment.getMediaUrl());
        attachment.setMediaUrl(publicUrl);

        // Set duration if provided
        if (request != null && request.getDurationSec() != null) {
            attachment.setDurationSec(request.getDurationSec());
        }

        // Link to note
        note.setAudio(attachment);

        Note updated = noteRepository.save(note);
        log.info("Audio upload completed for note {}", noteId);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Delete photo attachment
     */
    @Transactional
    public void deletePhoto(String userId, Long noteId, Long attachmentId) {
        log.info("Deleting photo {} from note {} for user: {}", attachmentId, noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        PhotoAttachment attachment = attachmentRepository.findByIdAndUserId(attachmentId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Remove from note
        if (note.getImage() != null && note.getImage().getId().equals(attachmentId)) {
            note.setImage(null);
            noteRepository.save(note);
        }

        // Delete from storage
        storageService.deleteFile("Photo", attachment.getMediaUrl());

        // Delete record
        attachmentRepository.delete(attachment);

        log.info("Photo attachment {} deleted successfully", attachmentId);
    }

    /**
     * Delete audio attachment
     */
    @Transactional
    public void deleteAudio(String userId, Long noteId, Long attachmentId) {
        log.info("Deleting audio {} from note {} for user: {}", attachmentId, noteId, userId);

        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        AudioAttachment attachment = audioAttachmentRepository.findByIdAndUserId(attachmentId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Remove from note
        if (note.getAudio() != null && note.getAudio().getId().equals(attachmentId)) {
            note.setAudio(null);
            noteRepository.save(note);
        }

        // Delete from storage
        storageService.deleteFile("attachment", attachment.getMediaUrl());

        // Delete record
        audioAttachmentRepository.delete(attachment);

        log.info("Audio attachment {} deleted successfully", attachmentId);
    }

    /**
     * Upload photo directly (single-step upload through backend)
     */
    @Transactional
    public NoteResponse uploadPhoto(String userId, Long noteId, MultipartFile file) throws IOException {
        log.info("Uploading photo for note {} and user: {}", noteId, userId);

        // Verify note exists
        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Upload file to Supabase
        SupabaseStorageService.FileUploadResponse uploadResponse = storageService.uploadFile("attachment", file);

        // Create attachment record
        PhotoAttachment attachment = PhotoAttachment.builder()
                .userId(userUuid)
                .mediaUrl(uploadResponse.getPublicUrl())
                .mediaType(file.getContentType())
                .status(AttachmentStatus.COMPLETED)
                .build();

        PhotoAttachment saved = attachmentRepository.save(attachment);

        // Link to note
        note.setImage(saved);
        Note updated = noteRepository.save(note);

        log.info("Photo uploaded and attached to note {}", noteId);

        return entityMapper.toNoteResponse(updated);
    }

    /**
     * Upload audio directly (single-step upload through backend)
     */
    @Transactional
    public NoteResponse uploadAudio(String userId, Long noteId, MultipartFile file, Integer durationSec) throws IOException {
        log.info("Uploading audio for note {} and user: {}", noteId, userId);

        // Verify note exists
        UUID userUuid = UUID.fromString(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Upload file to Supabase
        SupabaseStorageService.FileUploadResponse uploadResponse = storageService.uploadFile("attachment", file);

        // Create attachment record
        AudioAttachment attachment = AudioAttachment.builder()
                .userId(userUuid)
                .mediaUrl(uploadResponse.getPublicUrl())
                .mediaType(file.getContentType())
                .durationSec(durationSec)
                .status(AttachmentStatus.COMPLETED)
                .build();

        AudioAttachment saved = audioAttachmentRepository.save(attachment);

        // Link to note
        note.setAudio(saved);
        Note updated = noteRepository.save(note);

        log.info("Audio uploaded and attached to note {}", noteId);

        return entityMapper.toNoteResponse(updated);
    }
}
