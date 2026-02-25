package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.geofence.GeofenceRegistrationResponse;
import com.csci310.anchornotes.service.GeofenceService;
import com.csci310.anchornotes.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
@Slf4j
public class GeofenceController {

    private final GeofenceService geofenceService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<List<GeofenceRegistrationResponse>> listGeofences(Authentication auth) {
        String userId = userContextUtil.getCurrentUserId(auth);
        List<GeofenceRegistrationResponse> response = geofenceService.listGeofencesForRegistration(userId);
        return ResponseEntity.ok(response);
    }
}
