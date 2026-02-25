package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.tag.CreateTagRequest;
import com.csci310.anchornotes.dto.tag.TagResponse;
import com.csci310.anchornotes.service.TagService;
import com.csci310.anchornotes.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

    private final TagService tagService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags(Authentication auth) {
        String userId = userContextUtil.getCurrentUserId(auth);
        List<TagResponse> response = tagService.getAllTags(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            Authentication auth,
            @Valid @RequestBody CreateTagRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        TagResponse response = tagService.createTag(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(
            Authentication auth,
            @PathVariable Long id) {
        String userId = userContextUtil.getCurrentUserId(auth);
        tagService.deleteTag(userId, id);
        return ResponseEntity.noContent().build();
    }
}
