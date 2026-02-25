package com.csci310.anchornotes.repository;

import com.csci310.anchornotes.entity.PhotoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoAttachmentRepository extends JpaRepository<PhotoAttachment, Long> {

    Optional<PhotoAttachment> findByIdAndUserId(Long id, UUID userId);
}
