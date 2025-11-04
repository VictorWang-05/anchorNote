package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AccountActivity extends AppCompatActivity {

    private AuthManager authManager;
    private NoteRepository noteRepository;
    private TextView tvUsername;
    private MaterialButton btnLogout;
    private MaterialButton btnChangePassword;
    private MaterialButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize managers and repositories
        authManager = AuthManager.getInstance(this);
        noteRepository = NoteRepository.getInstance(this);

        // Find views
        tvUsername = findViewById(R.id.tv_username);
        btnLogout = findViewById(R.id.btn_logout);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnBack = findViewById(R.id.btn_back);

        // Check if user is logged in
        if (!authManager.isLoggedIn()) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Display username
        String username = authManager.getUsername();
        tvUsername.setText(username != null ? username : "User");

        // Back button click
        btnBack.setOnClickListener(v -> finish());

        // Logout button click
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Change password button click
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        TextView tvError = dialogView.findViewById(R.id.tv_error);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change Password", null) // Set to null to handle click manually
            .setNegativeButton("Cancel", null)
            .create();

        // Override positive button to validate before closing
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                // Hide error message
                tvError.setVisibility(View.GONE);
                
                // Get input values
                String currentPassword = etCurrentPassword.getText() != null 
                    ? etCurrentPassword.getText().toString().trim() : "";
                String newPassword = etNewPassword.getText() != null 
                    ? etNewPassword.getText().toString().trim() : "";
                String confirmPassword = etConfirmPassword.getText() != null 
                    ? etConfirmPassword.getText().toString().trim() : "";

                // Validate inputs
                if (TextUtils.isEmpty(currentPassword)) {
                    showError(tvError, "Current password is required");
                    return;
                }

                if (TextUtils.isEmpty(newPassword)) {
                    showError(tvError, "New password is required");
                    return;
                }

                if (newPassword.length() < 6) {
                    showError(tvError, "New password must be at least 6 characters");
                    return;
                }

                if (TextUtils.isEmpty(confirmPassword)) {
                    showError(tvError, "Please confirm your new password");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showError(tvError, "New passwords do not match");
                    return;
                }

                if (currentPassword.equals(newPassword)) {
                    showError(tvError, "New password must be different from current password");
                    return;
                }

                // All validation passed, proceed with password change
                dialog.dismiss();
                changePassword(currentPassword, newPassword);
            });
        });

        dialog.show();
    }

    private void showError(TextView tvError, String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void changePassword(String currentPassword, String newPassword) {
        // Show loading indicator (optional - you can add a progress dialog here)
        
        noteRepository.changePassword(currentPassword, newPassword, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AccountActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    String errorMessage = error != null && !error.isEmpty() 
                        ? error 
                        : "Failed to change password. Please try again.";
                    Toast.makeText(AccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage(getString(R.string.logout_confirmation))
            .setPositiveButton("Logout", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performLogout() {
        // Clear authentication
        authManager.clearAuth();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Go back to main activity and refresh it
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
