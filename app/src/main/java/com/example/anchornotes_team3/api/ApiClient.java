package com.example.anchornotes_team3.api;

import android.content.Context;

import com.example.anchornotes_team3.auth.AuthInterceptor;
import com.example.anchornotes_team3.auth.AuthManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit API client configuration
 */
public class ApiClient {
    
    // Production backend deployed on Railway
    private static final String BASE_URL = "https://anchornotesteam3-production.up.railway.app/";
    
    // For local testing, use:
    // private static final String BASE_URL = "http://10.0.2.2:8080/"; // Android emulator localhost
    
    private static ApiService apiService;
    
    /**
     * Get singleton instance of ApiService
     */
    public static synchronized ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = createApiService(context);
        }
        return apiService;
    }
    
    /**
     * Create ApiService with configured Retrofit instance
     */
    private static ApiService createApiService(Context context) {
        // Configure Gson with custom Instant adapter for ISO-8601 timestamps
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        
        // Create AuthManager and AuthInterceptor
        AuthManager authManager = AuthManager.getInstance(context);
        AuthInterceptor authInterceptor = new AuthInterceptor(authManager);
        
        // Create logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Configure OkHttpClient with interceptors
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)       // Add JWT token to requests
                .addInterceptor(loggingInterceptor)    // Log requests/responses
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        return retrofit.create(ApiService.class);
    }
    
    /**
     * Update base URL if needed (for settings/config)
     */
    public static void resetClient() {
        apiService = null;
    }
}

