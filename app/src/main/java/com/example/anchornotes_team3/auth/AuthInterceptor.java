package com.example.anchornotes_team3.auth;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor that adds JWT token to every API request
 */
public class AuthInterceptor implements Interceptor {
    
    private final AuthManager authManager;
    
    public AuthInterceptor(AuthManager authManager) {
        this.authManager = authManager;
    }
    
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        
        // Get the token from AuthManager
        String token = authManager.getToken();
        
        // If no token, proceed with original request
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        
        // Add Authorization header with Bearer token
        Request authenticated = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .method(original.method(), original.body())
                .build();
        
        return chain.proceed(authenticated);
    }
}

