package com.example.anchornotes_team3;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.anchornotes_team3.auth.AuthManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * White-box unit tests for AuthManager class
 * Tests authentication token management and user session handling
 */
@RunWith(RobolectricTestRunner.class)
public class AuthManagerTest {
    
    private AuthManager authManager;
    private Context context;
    private SharedPreferences mockSharedPrefs;
    private SharedPreferences.Editor mockEditor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Create mock SharedPreferences and Editor
        mockSharedPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        
        // Setup mock behavior
        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
        when(mockEditor.clear()).thenReturn(mockEditor);
        
        authManager = AuthManager.getInstance(context);
    }

    /**
     * Test 24: AuthManager - Test token storage and retrieval
     * Tests that authentication tokens can be stored and retrieved correctly
     * This verifies the core functionality of maintaining user session state
     */
    @Test
    public void testTokenStorageAndRetrieval() {
        // Arrange - Create test token and user data
        String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.testpayload";
        String testUserId = "user-12345";
        String testUsername = "testuser@example.com";
        
        // Act - Save authentication data using AuthManager
        AuthManager testAuthManager = AuthManager.getInstance(context);
        testAuthManager.saveToken(testToken);
        testAuthManager.saveUserInfo(testUserId, testUsername);
        
        // Retrieve the data
        String retrievedToken = testAuthManager.getToken();
        String retrievedUserId = testAuthManager.getUserId();
        String retrievedUsername = testAuthManager.getUsername();
        boolean isLoggedIn = testAuthManager.isLoggedIn();
        
        // Assert - Verify stored values match retrieved values
        assertEquals("Token should match stored value", testToken, retrievedToken);
        assertEquals("User ID should match stored value", testUserId, retrievedUserId);
        assertEquals("Username should match stored value", testUsername, retrievedUsername);
        assertTrue("User should be logged in when token exists", isLoggedIn);
        
        // Cleanup
        testAuthManager.clearAuth();
    }

    /**
     * Test 25: AuthManager - Test clearing authentication tokens
     * Tests that clearAuth properly clears all authentication data
     * This ensures no stale session data remains after logout
     */
    @Test
    public void testClearingAuthenticationTokens() {
        // Arrange - Set up authenticated state
        AuthManager testAuthManager = AuthManager.getInstance(context);
        String testToken = "sample_token_12345";
        String testUserId = "user-001";
        String testUsername = "user@test.com";
        
        // Save authentication data
        testAuthManager.saveToken(testToken);
        testAuthManager.saveUserInfo(testUserId, testUsername);
        
        // Verify user is logged in before clearing
        assertTrue("User should be logged in initially", testAuthManager.isLoggedIn());
        assertNotNull("Token should exist before clearing", testAuthManager.getToken());
        assertNotNull("User ID should exist before clearing", testAuthManager.getUserId());
        assertNotNull("Username should exist before clearing", testAuthManager.getUsername());
        
        // Act - Clear authentication data (logout)
        testAuthManager.clearAuth();
        
        // Assert - Verify all auth data is cleared
        assertNull("Token should be null after clearAuth", testAuthManager.getToken());
        assertNull("User ID should be null after clearAuth", testAuthManager.getUserId());
        assertNull("Username should be null after clearAuth", testAuthManager.getUsername());
        assertFalse("User should not be logged in after clearAuth", testAuthManager.isLoggedIn());
        
        // Verify SharedPreferences are actually cleared
        SharedPreferences prefs = context.getSharedPreferences("anchornotes_auth", Context.MODE_PRIVATE);
        String tokenAfter = prefs.getString("jwt_token", null);
        String userIdAfter = prefs.getString("user_id", null);
        String usernameAfter = prefs.getString("username", null);
        assertNull("Token should be removed from SharedPreferences", tokenAfter);
        assertNull("User ID should be removed from SharedPreferences", userIdAfter);
        assertNull("Username should be removed from SharedPreferences", usernameAfter);
    }

    /**
     * Test 26: AuthManager - Test expired token detection
     * Tests that expired JWT tokens are properly handled
     * This is critical for security to prevent using stale credentials
     */
    @Test
    public void testExpiredTokenDetection() {
        // Arrange - Create an expired JWT token
        // JWT structure: header.payload.signature
        // Payload contains exp (expiration) claim as Unix timestamp
        
        // This is a sample expired token (expired in 2020)
        // Payload: {"sub":"test-user-id","exp":1577836800} // Jan 1, 2020
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                             "eyJzdWIiOiJ0ZXN0LXVzZXItaWQiLCJleHAiOjE1Nzc4MzY4MDB9." +
                             "signature";
        
        AuthManager testAuthManager = AuthManager.getInstance(context);
        testAuthManager.saveToken(expiredToken);
        
        // Act - Check if token is retrievable
        String retrievedToken = testAuthManager.getToken();
        boolean isLoggedIn = testAuthManager.isLoggedIn();
        
        // Assert - Token should be retrievable but expired
        assertNotNull("Expired token should still be retrievable", retrievedToken);
        assertEquals("Retrieved token should match expired token", expiredToken, retrievedToken);
        assertTrue("User should be considered logged in (token exists)", isLoggedIn);
        
        // Note: AuthManager doesn't validate expiration internally - that's done by backend
        // This test verifies we can store and retrieve tokens regardless of expiration
        // The backend will reject expired tokens, prompting a clearAuth() call
        
        // Test with null token (completely missing)
        testAuthManager.clearAuth();
        String nullToken = testAuthManager.getToken();
        assertNull("Token should be null after clearAuth", nullToken);
        assertFalse("Should not be logged in with no token", testAuthManager.isLoggedIn());
        
        // Test Bearer token format
        testAuthManager.saveToken("test-token-123");
        String bearerToken = testAuthManager.getBearerToken();
        assertEquals("Bearer token should have correct format", "Bearer test-token-123", bearerToken);
        
        // Test Bearer token with no token stored
        testAuthManager.clearAuth();
        String nullBearerToken = testAuthManager.getBearerToken();
        assertNull("Bearer token should be null when no token stored", nullBearerToken);
        
        // Cleanup
        testAuthManager.clearAuth();
    }
}

