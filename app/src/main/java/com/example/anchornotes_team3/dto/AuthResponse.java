package com.example.anchornotes_team3.dto;

/**
 * Response DTO for login/register
 * Backend returns: { success, message, data: { token, username, email, fullName } }
 */
public class AuthResponse {
    private boolean success;
    private String message;
    private AuthData data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public AuthData getData() {
        return data;
    }

    // Convenience methods to access data fields
    public String getToken() {
        return data != null ? data.token : null;
    }

    public String getUsername() {
        return data != null ? data.username : null;
    }

    public String getUserId() {
        // Backend doesn't return userId in the response, but we can extract from token if needed
        return data != null ? data.username : null; // Using username as ID for now
    }

    public String getEmail() {
        return data != null ? data.email : null;
    }

    public String getFullName() {
        return data != null ? data.fullName : null;
    }

    /**
     * Inner class for the "data" field
     */
    public static class AuthData {
        private String token;
        private String username;
        private String email;
        private String fullName;

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }
    }
}

