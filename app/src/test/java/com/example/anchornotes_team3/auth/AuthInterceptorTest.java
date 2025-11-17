package com.example.anchornotes_team3.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * White box tests for AuthInterceptor
 * Tests the internal logic of adding authentication headers to HTTP requests
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthInterceptorTest {

    @Mock
    private AuthManager mockAuthManager;

    @Mock
    private Interceptor.Chain mockChain;

    @Mock
    private Response mockResponse;

    private AuthInterceptor authInterceptor;

    @Before
    public void setUp() {
        authInterceptor = new AuthInterceptor(mockAuthManager);
    }

    /**
     * Test 32: Test adding auth header to request
     * White box test: Verifies that when a token exists in AuthManager,
     * the interceptor correctly adds the "Authorization: Bearer {token}" header
     * to the outgoing HTTP request.
     */
    @Test
    public void testAddAuthHeaderToRequest() throws Exception {
        // Arrange: Set up test data
        String testToken = "test_jwt_token_12345";
        Request originalRequest = new Request.Builder()
                .url("https://api.example.com/endpoint")
                .build();

        // Mock AuthManager to return a valid token
        when(mockAuthManager.getToken()).thenReturn(testToken);

        // Mock chain to return original request and mock response
        when(mockChain.request()).thenReturn(originalRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        // Act: Execute the interceptor
        Response response = authInterceptor.intercept(mockChain);

        // Assert: Verify the interceptor behavior
        // 1. Verify getToken() was called to retrieve the token
        verify(mockAuthManager, times(1)).getToken();

        // 2. Capture the modified request that was sent
        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain, times(1)).proceed(requestCaptor.capture());

        Request modifiedRequest = requestCaptor.getValue();

        // 3. Verify Authorization header was added with correct format
        String authHeader = modifiedRequest.header("Authorization");
        assertNotNull("Authorization header should be present", authHeader);
        assertEquals("Authorization header should contain Bearer token",
                "Bearer " + testToken, authHeader);

        // 4. Verify the URL remains unchanged
        assertEquals("Request URL should remain unchanged",
                originalRequest.url(), modifiedRequest.url());

        // 5. Verify response was returned
        assertSame("Response should be returned from chain", mockResponse, response);
    }

    /**
     * Test 33: Test handling missing token
     * White box test: Verifies that when no token is available (AuthManager returns null),
     * the interceptor correctly handles this case by proceeding with the original request
     * without adding any Authorization header.
     */
    @Test
    public void testHandlingMissingToken() throws Exception {
        // Arrange: Set up test data with no token
        Request originalRequest = new Request.Builder()
                .url("https://api.example.com/endpoint")
                .build();

        // Mock AuthManager to return null (no token available)
        when(mockAuthManager.getToken()).thenReturn(null);

        // Mock chain to return original request and mock response
        when(mockChain.request()).thenReturn(originalRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        // Act: Execute the interceptor
        Response response = authInterceptor.intercept(mockChain);

        // Assert: Verify the interceptor behavior
        // 1. Verify getToken() was called
        verify(mockAuthManager, times(1)).getToken();

        // 2. Capture the request that was sent
        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain, times(1)).proceed(requestCaptor.capture());

        Request sentRequest = requestCaptor.getValue();

        // 3. Verify NO Authorization header was added
        String authHeader = sentRequest.header("Authorization");
        assertNull("Authorization header should NOT be present when token is missing",
                authHeader);

        // 4. Verify the request is the original unmodified request
        assertEquals("Request should be original when no token",
                originalRequest.url(), sentRequest.url());

        // 5. Verify response was returned
        assertSame("Response should be returned from chain", mockResponse, response);
    }

    /**
     * Additional white box test: Test handling empty string token
     * Verifies edge case where token is empty string instead of null
     */
    @Test
    public void testHandlingEmptyToken() throws Exception {
        // Arrange
        Request originalRequest = new Request.Builder()
                .url("https://api.example.com/endpoint")
                .build();

        // Mock AuthManager to return empty string
        when(mockAuthManager.getToken()).thenReturn("");
        when(mockChain.request()).thenReturn(originalRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        // Act
        Response response = authInterceptor.intercept(mockChain);

        // Assert
        verify(mockAuthManager, times(1)).getToken();

        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain, times(1)).proceed(requestCaptor.capture());

        Request sentRequest = requestCaptor.getValue();

        // Empty string is considered as "no token" in most implementations
        String authHeader = sentRequest.header("Authorization");
        assertNull("Authorization header should NOT be present for empty token",
                authHeader);
    }
}
