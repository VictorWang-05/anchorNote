package com.csci310.anchornotes.controller;

import com.csci310.anchornotes.dto.ApiResponse;
import com.csci310.anchornotes.dto.AuthRequest;
import com.csci310.anchornotes.dto.AuthResponse;
import com.csci310.anchornotes.dto.ChangePasswordRequest;
import com.csci310.anchornotes.dto.RegisterRequest;
import com.csci310.anchornotes.service.SupabaseAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SupabaseAuthService supabaseAuthService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        AuthResponse response = supabaseAuthService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request received for: {}", request.getUsername());
        AuthResponse response = supabaseAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
