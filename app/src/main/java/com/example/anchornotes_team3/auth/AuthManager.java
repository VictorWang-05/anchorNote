package com.example.anchornotes_team3.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages JWT authentication token storage using SharedPreferences
 */
public class AuthManager {
    private static final String PREFS_NAME = "anchornotes_auth";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    
    private final SharedPreferences prefs;
    private static AuthManager instance;
    
    private AuthManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }
    
    /**
     * Save JWT token after successful login
     */
    public void saveToken(String token) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .apply();
    }
    
    /**
     * Get the current JWT token
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
    
    /**
     * Save user information
     */
    public void saveUserInfo(String userId, String username) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }
    
    /**
     * Get the current user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get the current username
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    /**
     * Clear all authentication data (logout)
     */
    public void clearAuth() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .apply();
    }
    
    /**
     * Get Bearer token format for Authorization header
     */
    public String getBearerToken() {
        String token = getToken();
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }
}

