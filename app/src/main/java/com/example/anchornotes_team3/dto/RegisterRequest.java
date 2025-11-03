package com.example.anchornotes_team3.dto;

/**
 * Request DTO for registration
 */
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;

    public RegisterRequest(String username, String email, String password, String fullName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }
}

