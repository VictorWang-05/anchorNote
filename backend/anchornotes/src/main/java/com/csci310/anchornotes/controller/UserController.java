package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.ApiResponse;
import com.csci310.anchornotes.dto.ChangePasswordRequest;
import com.csci310.anchornotes.service.SupabaseAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final SupabaseAuthService supabaseAuthService;

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {

        log.info("Change password request received");

        if (auth == null) {
            log.error("Authentication is null");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        }

        log.info("User authenticated: {}", auth.getPrincipal());

        // Extract JWT token from Authorization header
        String token = getJwtFromRequest(request);
        if (token == null) {
            log.error("No JWT token found in request");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Authorization token is required"));
        }

        log.info("Calling Supabase to change password");
        try {
            supabaseAuthService.changePassword(
                token,
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
            );
            log.info("Password changed successfully");
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
        } catch (Exception e) {
            log.error("Error changing password", e);
            throw e;
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
