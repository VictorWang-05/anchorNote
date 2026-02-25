package com.csci310.anchornotes.repository;

import com.csci310.anchornotes.entity.AudioAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudioAttachmentRepository extends JpaRepository<AudioAttachment, Long> {

    Optional<AudioAttachment> findByIdAndUserId(Long id, UUID userId);

    void deleteByIdAndUserId(Long id, UUID userId);
}
