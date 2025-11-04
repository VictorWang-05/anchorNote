package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.anchornotes_team3.api.ApiClient;
import com.example.anchornotes_team3.api.ApiService;
import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.dto.AuthRequest;
import com.example.anchornotes_team3.dto.AuthResponse;
import com.example.anchornotes_team3.dto.RegisterRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login and Registration Activity
 */
public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etFullname;
    private MaterialButton btnRegister;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize auth manager first
        authManager = AuthManager.getInstance(this);
        
        // Check if user is already logged in - redirect to main activity
        if (authManager.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);
        
        // Initialize API service
        apiService = ApiClient.getApiService(this);
        
        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etFullname = findViewById(R.id.et_fullname);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set click listeners
        btnRegister.setOnClickListener(v -> handleRegister());
        btnLogin.setOnClickListener(v -> {
            // Redirect to login-only screen
            Intent intent = new Intent(LoginActivity.this, LoginOnlyActivity.class);
            startActivity(intent);
        });
    }
    
    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fullname = etFullname.getText().toString().trim();
        
        // Validation
        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (fullname.isEmpty()) {
            etFullname.setError("Full name is required");
            etFullname.requestFocus();
            return;
        }
        
        // Show loading
        showLoading(true);
        
        Log.d(TAG, "📝 Registering user: " + username);
        
        // Create register request
        RegisterRequest request = new RegisterRequest(username, email, password, fullname);
        
        // Call API
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Registration successful!");
                    handleRegistrationResponse(response.body());
                } else {
                    Log.e(TAG, "❌ Registration failed: " + response.code());
                    String errorMsg = "Registration failed";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "💥 Network error during registration", t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void handleRegistrationResponse(AuthResponse authResponse) {
        if (authResponse.isSuccess() && authResponse.getData() != null) {
            // Save token and user info
            authManager.saveToken(authResponse.getToken());
            authManager.saveUserInfo(
                authResponse.getUserId(),
                authResponse.getUsername()
            );
            
            // Reset API client to pick up the new token
            ApiClient.resetClient();
            
            Log.d(TAG, "✅ Token saved, API client reset");
            
            Toast.makeText(this, 
                "Welcome, " + authResponse.getUsername() + "!", 
                Toast.LENGTH_SHORT).show();
            
            // Go to main activity
            goToMainActivity();
        } else {
            Toast.makeText(this, 
                "Registration failed: " + authResponse.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        btnLogin.setEnabled(!show);
        etUsername.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etFullname.setEnabled(!show);
    }
    
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

