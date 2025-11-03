package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.anchornotes_team3.auth.AuthManager;
import com.google.android.material.button.MaterialButton;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Get auth manager
        AuthManager authManager = AuthManager.getInstance(this);
        
        // Find UI elements
        MaterialButton loginButton = findViewById(R.id.loginButton);
        TextView usernameDisplay = findViewById(R.id.usernameDisplay);
        MaterialButton newNoteButton = findViewById(R.id.newNoteButton);
        
        // Update UI based on login status
        if (authManager.isLoggedIn()) {
            // User is logged in - show username
            String username = authManager.getUsername();
            loginButton.setVisibility(View.GONE);
            usernameDisplay.setVisibility(View.VISIBLE);
            usernameDisplay.setText(username != null ? username : "User");
            
            Toast.makeText(this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
        } else {
            // User is not logged in - show login button
            loginButton.setVisibility(View.VISIBLE);
            usernameDisplay.setVisibility(View.GONE);
        }
        
        // Login button click
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        
        // Username display click (for logout)
        usernameDisplay.setOnClickListener(v -> {
            authManager.clearAuth();
            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Refresh the activity
            recreate();
        });
        
        // New Note button click
        newNoteButton.setOnClickListener(v -> {
            if (authManager.isLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}