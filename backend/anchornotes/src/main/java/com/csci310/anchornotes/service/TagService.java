package com.csci310.anchornotes.service;

import com.csci310.anchornotes.dto.tag.CreateTagRequest;
import com.csci310.anchornotes.dto.tag.TagResponse;
import com.csci310.anchornotes.entity.Tag;
import com.csci310.anchornotes.exception.BadRequestException;
import com.csci310.anchornotes.exception.ResourceNotFoundException;
import com.csci310.anchornotes.repository.TagRepository;
import com.csci310.anchornotes.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private final TagRepository tagRepository;
    private final EntityMapper entityMapper;

    /**
     * Get all tags for a user
     */
    public List<TagResponse> getAllTags(String userId) {
        log.info("Fetching all tags for user: {}", userId);

        UUID userUuid = UUID.fromString(userId);
        List<Tag> tags = tagRepository.findByUserId(userUuid);

        return tags.stream()
                .map(entityMapper::toTagResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new tag
     */
    @Transactional
    public TagResponse createTag(String userId, CreateTagRequest request) {
        log.info("Creating tag for user: {} with name: {}", userId, request.getName());

        UUID userUuid = UUID.fromString(userId);

        // Check if tag with same name already exists for this user
        if (tagRepository.existsByUserIdAndName(userUuid, request.getName())) {
            throw new BadRequestException("Tag with name '" + request.getName() + "' already exists");
        }

        Tag tag = Tag.builder()
                .userId(userUuid)
                .name(request.getName())
                .color(request.getColor())
                .build();

        Tag saved = tagRepository.save(tag);
        log.info("Tag created successfully with ID: {}", saved.getId());

        return entityMapper.toTagResponse(saved);
    }

    /**
     * Delete a tag
     */
    @Transactional
    public void deleteTag(String userId, Long tagId) {
        log.info("Deleting tag {} for user: {}", tagId, userId);

        UUID userUuid = UUID.fromString(userId);

        if (!tagRepository.findByIdAndUserId(tagId, userUuid).isPresent()) {
            throw new ResourceNotFoundException("Tag not found");
        }

        tagRepository.deleteByIdAndUserId(tagId, userUuid);
        log.info("Tag {} deleted successfully", tagId);
    }
}
