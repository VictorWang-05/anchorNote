package com.csci310.anchornotes.service;

import com.csci310.anchornotes.config.SupabaseConfig;
import com.csci310.anchornotes.dto.AuthRequest;
import com.csci310.anchornotes.dto.AuthResponse;
import com.csci310.anchornotes.dto.RegisterRequest;
import com.csci310.anchornotes.exception.BadRequestException;
import com.csci310.anchornotes.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseAuthService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with Supabase: {}", request.getEmail());

        try {
            String url = supabaseConfig.getUrl() + "/auth/v1/signup";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getAnonKey());

            Map<String, Object> body = new HashMap<>();
            body.put("email", request.getEmail());
            body.put("password", request.getPassword());

            Map<String, String> userData = new HashMap<>();
            userData.put("username", request.getUsername());
            userData.put("full_name", request.getFullName());
            body.put("data", userData);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("Supabase registration response: {}", response.getBody());
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String email = request.getEmail();
            String accessToken = null;

            // Check if email confirmation is required
            if (jsonResponse.has("confirmation_sent_at") && jsonResponse.get("confirmation_sent_at") != null) {
                // Email confirmation is required - user created but needs to verify email
                log.info("User registered successfully with Supabase, email confirmation required: {}", email);

                if (jsonResponse.has("email")) {
                    email = jsonResponse.get("email").asText();
                }

                // Return response without token - user needs to verify email first
                return AuthResponse.builder()
                        .token("EMAIL_VERIFICATION_REQUIRED")
                        .username(request.getUsername())
                        .email(email)
                        .fullName(request.getFullName())
                        .build();
            }

            // Check if we got an access_token (instant signup without email confirmation)
            if (jsonResponse.has("access_token")) {
                accessToken = jsonResponse.get("access_token").asText();
            }

            JsonNode user = jsonResponse.has("user") ? jsonResponse.get("user") : null;
            if (user == null) {
                // Response is the user object itself (no nested user)
                user = jsonResponse;
            }

            if (user.has("email")) {
                email = user.get("email").asText();
            }

            log.info("User registered successfully with Supabase: {}", email);

            return AuthResponse.builder()
                    .token(accessToken != null ? accessToken : "PLEASE_LOGIN")
                    .username(request.getUsername())
                    .email(email)
                    .fullName(request.getFullName())
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Error registering user with Supabase: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Registration failed: " + extractErrorMessage(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            throw new BadRequestException("Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse login(AuthRequest request) {
        log.info("Logging in user with Supabase: {}", request.getUsername());

        try {
            String url = supabaseConfig.getUrl() + "/auth/v1/token?grant_type=password";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getAnonKey());

            Map<String, String> body = new HashMap<>();
            body.put("email", request.getUsername()); // Assuming username is email
            body.put("password", request.getPassword());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("Supabase login response: {}", response.getBody());
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (!jsonResponse.has("access_token")) {
                log.error("No access_token in response: {}", response.getBody());
                throw new UnauthorizedException("Login failed: Invalid response from Supabase");
            }

            String accessToken = jsonResponse.get("access_token").asText();
            JsonNode user = jsonResponse.has("user") ? jsonResponse.get("user") : null;

            log.info("User logged in successfully with Supabase: {}", request.getUsername());

            // Extract metadata
            String username = request.getUsername();
            String email = request.getUsername();
            String fullName = null;

            if (user != null) {
                if (user.has("email")) {
                    email = user.get("email").asText();
                }
                if (user.has("user_metadata")) {
                    JsonNode metadata = user.get("user_metadata");
                    if (metadata.has("username")) {
                        username = metadata.get("username").asText();
                    }
                    if (metadata.has("full_name")) {
                        fullName = metadata.get("full_name").asText();
                    }
                }
            }

            return AuthResponse.builder()
                    .token(accessToken)
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Error logging in user with Supabase: {}", e.getResponseBodyAsString());
            throw new UnauthorizedException("Login failed: Invalid credentials");
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new UnauthorizedException("Login failed: " + e.getMessage());
        }
    }

    public void changePassword(String accessToken, String currentPassword, String newPassword) {
        log.info("Changing password for user");

        try {
            // First, get user email from the access token
            String userEmail = getUserEmailFromToken(accessToken);
            log.info("Extracted email from token: {}", userEmail);

            // Verify current password by attempting to login
            try {
                verifyCurrentPassword(userEmail, currentPassword);
                log.info("Current password verified successfully");
            } catch (UnauthorizedException e) {
                log.error("Current password verification failed");
                throw new BadRequestException("Current password is incorrect");
            }

            // If current password is correct, proceed with password change
            String url = supabaseConfig.getUrl() + "/auth/v1/user";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getAnonKey());
            headers.set("Authorization", "Bearer " + accessToken);

            Map<String, String> body = new HashMap<>();
            body.put("password", newPassword);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            log.info("Password changed successfully");

        } catch (BadRequestException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Error changing password with Supabase: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Password change failed: " + extractErrorMessage(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Unexpected error during password change", e);
            throw new BadRequestException("Password change failed: " + e.getMessage());
        }
    }

    private String getUserEmailFromToken(String accessToken) {
        try {
            String url = supabaseConfig.getUrl() + "/auth/v1/user";

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", supabaseConfig.getAnonKey());
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (jsonResponse.has("email")) {
                return jsonResponse.get("email").asText();
            }

            throw new BadRequestException("Could not extract email from token");
        } catch (Exception e) {
            log.error("Error getting user email from token", e);
            throw new BadRequestException("Invalid access token");
        }
    }

    private void verifyCurrentPassword(String email, String currentPassword) {
        try {
            String url = supabaseConfig.getUrl() + "/auth/v1/token?grant_type=password";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getAnonKey());

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", currentPassword);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (!jsonResponse.has("access_token")) {
                throw new UnauthorizedException("Current password is incorrect");
            }
        } catch (HttpClientErrorException e) {
            log.error("Current password verification failed: {}", e.getResponseBodyAsString());
            throw new UnauthorizedException("Current password is incorrect");
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying current password", e);
            throw new UnauthorizedException("Current password verification failed");
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.has("msg")) {
                return json.get("msg").asText();
            }
            if (json.has("error_description")) {
                return json.get("error_description").asText();
            }
            return "Unknown error";
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
