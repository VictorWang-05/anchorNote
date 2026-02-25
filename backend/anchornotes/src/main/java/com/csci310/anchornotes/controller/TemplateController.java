package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.note.NoteResponse;
import com.csci310.anchornotes.dto.template.CreateTemplateRequest;
import com.csci310.anchornotes.dto.template.InstantiateTemplateRequest;
import com.csci310.anchornotes.dto.template.TemplateResponse;
import com.csci310.anchornotes.dto.template.UpdateTemplateRequest;
import com.csci310.anchornotes.service.TemplateService;
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
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

    private final TemplateService templateService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates(Authentication auth) {
        String userId = userContextUtil.getCurrentUserId(auth);
        List<TemplateResponse> response = templateService.getAllTemplates(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
            Authentication auth,
            @Valid @RequestBody CreateTemplateRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        TemplateResponse response = templateService.createTemplate(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        TemplateResponse response = templateService.updateTemplate(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            Authentication auth,
            @PathVariable Long id) {
        String userId = userContextUtil.getCurrentUserId(auth);
        templateService.deleteTemplate(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/instantiate")
    public ResponseEntity<NoteResponse> instantiateTemplate(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody InstantiateTemplateRequest request) {
        String userId = userContextUtil.getCurrentUserId(auth);
        NoteResponse response = templateService.instantiateTemplate(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
