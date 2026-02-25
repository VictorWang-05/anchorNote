package com.csci310.anchornotes.util;

import com.csci310.anchornotes.exception.UnauthorizedException;
import com.csci310.anchornotes.security.SupabaseJwtVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContextUtil {

    private final SupabaseJwtVerifier jwtVerifier;

    /**
     * Extract user ID from the current authentication context
     * The JWT filter stores the userId as the principal
     */
    public String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        // Get userId from principal (set by JwtAuthenticationFilter)
        Object principal = authentication.getPrincipal();

        if (principal == null || !(principal instanceof String)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        String userId = (String) principal;

        if (userId.isEmpty()) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        return userId;
    }

    /**
     * Extract email from the current authentication context
     * The JWT filter stores the email in credentials
     */
    public String getCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        // Get email from credentials (set by JwtAuthenticationFilter)
        Object credentials = authentication.getCredentials();

        if (credentials == null || !(credentials instanceof String)) {
            throw new UnauthorizedException("Invalid authentication credentials");
        }

        String email = (String) credentials;

        if (email.isEmpty()) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        return email;
    }
}
