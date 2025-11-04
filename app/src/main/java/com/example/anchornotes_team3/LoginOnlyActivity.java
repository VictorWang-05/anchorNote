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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login-only activity with just email and password
 */
public class LoginOnlyActivity extends AppCompatActivity {
    private static final String TAG = "LoginOnlyActivity";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnBackToRegister;
    private ProgressBar progressBar;

    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_only);

        // Initialize API and Auth
        apiService = ApiClient.getApiService(this);
        authManager = AuthManager.getInstance(this);

        // Find views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnBackToRegister = findViewById(R.id.btn_back_to_register);
        progressBar = findViewById(R.id.progress_bar);

        // Set up click listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        btnBackToRegister.setOnClickListener(v -> {
            finish(); // Go back to registration screen
        });
        
        // Back button to return to home
        MaterialButton btnBackToHome = findViewById(R.id.btn_back_to_home);
        btnBackToHome.setOnClickListener(v -> finish());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
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

        // Show loading
        setLoading(true);

        // Create login request
        AuthRequest request = new AuthRequest(email, password);

        Log.d(TAG, "🔐 Attempting login for: " + email);

        // Make API call
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Login successful!");
                    handleLoginResponse(response.body());
                } else {
                    String errorMsg = "Login failed";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "❌ Login failed: " + response.code() + " - " + errorMsg);
                    Toast.makeText(LoginOnlyActivity.this, 
                        "Login failed: Invalid credentials", 
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "❌ Login network error: " + t.getMessage(), t);
                Toast.makeText(LoginOnlyActivity.this, 
                    "Network error: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLoginResponse(AuthResponse authResponse) {
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
                "Welcome back, " + authResponse.getUsername() + "!", 
                Toast.LENGTH_SHORT).show();

            // Go to main activity
            goToMainActivity();
        } else {
            Toast.makeText(this, 
                "Login failed: " + authResponse.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnBackToRegister.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }
}

