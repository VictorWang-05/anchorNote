package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.NoteAdapter;
import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private AuthManager authManager;
    private NoteRepository noteRepository;
    
    // UI elements
    private MaterialButton loginButton;
    private TextView usernameDisplay;
    private MaterialButton newNoteButton;
    
    // RecyclerViews
    private RecyclerView pinnedRecyclerView;
    private RecyclerView relevantRecyclerView;
    private RecyclerView allNotesRecyclerView;
    
    // Adapters
    private NoteAdapter pinnedAdapter;
    private NoteAdapter relevantAdapter;
    private NoteAdapter allNotesAdapter;
    
    // Empty state TextViews
    private TextView pinnedEmpty;
    private TextView relevantNotesEmpty;
    private TextView allNotesEmpty;

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
        
        // Initialize managers and repositories
        authManager = AuthManager.getInstance(this);
        noteRepository = NoteRepository.getInstance(this);
        
        // Find UI elements
        loginButton = findViewById(R.id.loginButton);
        usernameDisplay = findViewById(R.id.usernameDisplay);
        newNoteButton = findViewById(R.id.newNoteButton);
        
        // Find RecyclerViews
        pinnedRecyclerView = findViewById(R.id.pinnedRecyclerView);
        relevantRecyclerView = findViewById(R.id.relevantNotesRecyclerView);
        allNotesRecyclerView = findViewById(R.id.allNotesRecyclerView);
        
        // Find empty state TextViews
        pinnedEmpty = findViewById(R.id.pinnedEmpty);
        relevantNotesEmpty = findViewById(R.id.relevantNotesEmpty);
        allNotesEmpty = findViewById(R.id.allNotesEmpty);
        
        // Set up RecyclerViews
        setupRecyclerViews();
        
        // Update UI based on login status
        updateLoginUI();
        
        // Set click listeners
        setupClickListeners();
        
        // Load notes if logged in
        if (authManager.isLoggedIn()) {
            loadNotes();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notes when returning to this activity
        if (authManager.isLoggedIn()) {
            loadNotes();
        } else {
            // Clear all notes if logged out
            clearAllNotes();
        }
    }
    
    private void setupRecyclerViews() {
        // Create a shared listener for all adapters
        NoteAdapter.OnNoteClickListener sharedListener = new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                openNote(note);
            }
            
            @Override
            public void onAddTagClick(Note note) {
                showAddTagDialog(note);
            }
            
            @Override
            public void onDeleteClick(Note note) {
                confirmDeleteNote(note);
            }
            
            @Override
            public void onPinClick(Note note) {
                togglePinNote(note);
            }
        };
        
        // Pinned Notes - Horizontal
        pinnedAdapter = new NoteAdapter(true);
        pinnedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        pinnedRecyclerView.setAdapter(pinnedAdapter);
        pinnedAdapter.setOnNoteClickListener(sharedListener);
        
        // Relevant Notes - Horizontal
        relevantAdapter = new NoteAdapter(true);
        relevantRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relevantRecyclerView.setAdapter(relevantAdapter);
        relevantAdapter.setOnNoteClickListener(sharedListener);
        
        // All Notes - Vertical
        allNotesAdapter = new NoteAdapter(false);
        allNotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allNotesRecyclerView.setAdapter(allNotesAdapter);
        allNotesAdapter.setOnNoteClickListener(sharedListener);
    }
    
    private void setupClickListeners() {
        // Login button click
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        
        // Username display click (navigate to account page)
        usernameDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AccountActivity.class);
            startActivity(intent);
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
    
    private void updateLoginUI() {
        if (authManager.isLoggedIn()) {
            // User is logged in - show username
            String username = authManager.getUsername();
            loginButton.setVisibility(View.GONE);
            usernameDisplay.setVisibility(View.VISIBLE);
            usernameDisplay.setText(username != null ? username : "User");
        } else {
            // User is not logged in - show login button
            loginButton.setVisibility(View.VISIBLE);
            usernameDisplay.setVisibility(View.GONE);
            // Clear notes when logged out
            clearAllNotes();
        }
    }
    
    private void loadNotes() {
        Log.d(TAG, "📥 Loading notes...");
        
        noteRepository.getAllNotes(new NoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<Note> notes) {
                Log.d(TAG, "✅ Loaded " + notes.size() + " notes");
                updateNoteLists(notes);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Failed to load notes: " + error);
                Toast.makeText(MainActivity.this, "Failed to load notes: " + error, Toast.LENGTH_SHORT).show();
                // Show empty states on error
                clearAllNotes();
            }
        });
    }
    
    private void updateNoteLists(List<Note> allNotes) {
        // Filter pinned notes
        List<Note> pinnedNotes = allNotes.stream()
            .filter(Note::isPinned)
            .collect(Collectors.toList());
        
        // Filter relevant notes - only notes with active reminders that have been triggered
        // According to Feature 4 requirements:
        // - Time reminders: notes should appear only when reminder time is within ±1 hour
        // - Geofence reminders: notes should appear only when user is inside the geofence
        // Since reminders are not yet properly implemented, Relevant Notes section will be empty
        // TODO: When reminders are implemented, use NoteRepository.getRelevantNotes() API call
        // For now, show empty list - notes will only appear here when reminders are actually triggered
        List<Note> relevantNotes = new ArrayList<>();
        
        // All notes - include ALL notes (both pinned and unpinned)
        // This ensures pinned notes still appear in the All Notes section
        List<Note> allNotesList = new ArrayList<>(allNotes);
        
        // Update adapters
        pinnedAdapter.setNotes(pinnedNotes);
        relevantAdapter.setNotes(relevantNotes);
        allNotesAdapter.setNotes(allNotesList); // Show all notes, including pinned
        
        // Update empty states
        updateEmptyStates(pinnedNotes, relevantNotes, allNotesList);
    }
    
    private void updateEmptyStates(List<Note> pinned, List<Note> relevant, List<Note> all) {
        pinnedEmpty.setVisibility(pinned.isEmpty() ? View.VISIBLE : View.GONE);
        relevantNotesEmpty.setVisibility(relevant.isEmpty() ? View.VISIBLE : View.GONE);
        allNotesEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    private void clearAllNotes() {
        pinnedAdapter.setNotes(new ArrayList<>());
        relevantAdapter.setNotes(new ArrayList<>());
        allNotesAdapter.setNotes(new ArrayList<>());
        updateEmptyStates(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
    
    private void openNote(Note note) {
        try {
            Log.d(TAG, "openNote called with note: " + (note != null ? note.getTitle() : "null"));
            
            if (note == null) {
                Log.e(TAG, "Cannot open note - note is null");
                Toast.makeText(this, "Unable to open note: Note is null", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String noteId = note.getId();
            if (noteId == null || noteId.isEmpty()) {
                Log.e(TAG, "Cannot open note - note ID is null or empty. Note title: " + note.getTitle());
                Toast.makeText(this, "Unable to open note: Invalid note ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Opening note - ID: " + noteId + ", Title: " + note.getTitle());
            
            // Verify the activity is still valid
            if (isFinishing() || isDestroyed()) {
                Log.e(TAG, "Activity is finishing or destroyed, cannot start NoteEditorActivity");
                return;
            }
            
            Intent intent = new Intent(this, NoteEditorActivity.class);
            intent.putExtra("note_id", noteId);
            
            // Use startActivity instead of startActivityForResult for compatibility
            startActivity(intent);
            Log.d(TAG, "Started NoteEditorActivity successfully");
            
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "NoteEditorActivity not found", e);
            Toast.makeText(this, "Note editor not found. Please check installation.", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting NoteEditorActivity", e);
            Toast.makeText(this, "Permission denied to open note", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error opening note", e);
            e.printStackTrace();
            Toast.makeText(this, "Error opening note: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // Notes are refreshed automatically in onResume() when returning from other activities
    
    /**
     * Show context menu when note is long-pressed
     */
    private void showNoteContextMenu(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }
        
        String[] options = {
            getString(R.string.add_tag),
            getString(R.string.delete_note)
        };
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.note_options)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Add tag
                    showAddTagDialog(note);
                } else if (which == 1) {
                    // Delete note
                    confirmDeleteNote(note);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show dialog to add a tag to the note (allows creating new tags or selecting existing ones)
     */
    private void showAddTagDialog(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }
        
        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        com.google.android.material.textfield.TextInputEditText etTag = dialogView.findViewById(R.id.et_tag);
        com.google.android.material.chip.ChipGroup chipGroupExisting = dialogView.findViewById(R.id.chip_group_existing_tags);
        
        // Load all available tags
        noteRepository.getAllTags(new NoteRepository.TagsCallback() {
            @Override
            public void onSuccess(List<com.example.anchornotes_team3.model.Tag> tags) {
                // Populate existing tags as chips
                chipGroupExisting.removeAllViews();
                for (com.example.anchornotes_team3.model.Tag tag : tags) {
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(MainActivity.this);
                    chip.setText(tag.getName());
                    chip.setCheckable(true);
                    chip.setClickable(true);
                    
                    // Set tag color if available
                    if (tag.getColor() != null && !tag.getColor().isEmpty()) {
                        try {
                            int color = android.graphics.Color.parseColor(tag.getColor());
                            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
                            chip.setTextColor(android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.WHITE));
                        } catch (Exception e) {
                            // Use default color if parsing fails
                        }
                    }
                    
                    // Chip click listener will be set after dialog is created
                    // (stored in tag for later reference)
                    chip.setTag(tag);
                    
                    chipGroupExisting.addView(chip);
                }
                
                // Create dialog
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.add_tag)
                    .setView(dialogView)
                    .setPositiveButton("Create New Tag", (d, which) -> {
                        String tagName = etTag.getText().toString().trim();
                        if (!tagName.isEmpty()) {
                            createAndAddTag(note, tagName);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a tag name", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
                
                // Store dialog reference for chip click handlers
                final androidx.appcompat.app.AlertDialog dialogRef = dialog;
                
                // Update chip click listeners to close dialog
                for (int i = 0; i < chipGroupExisting.getChildCount(); i++) {
                    View chip = chipGroupExisting.getChildAt(i);
                    if (chip instanceof com.google.android.material.chip.Chip) {
                        com.google.android.material.chip.Chip tagChip = (com.google.android.material.chip.Chip) chip;
                        Object tagObj = tagChip.getTag();
                        if (tagObj instanceof com.example.anchornotes_team3.model.Tag) {
                            com.example.anchornotes_team3.model.Tag selectedTag = (com.example.anchornotes_team3.model.Tag) tagObj;
                            tagChip.setOnClickListener(v -> {
                                dialogRef.dismiss();
                                addTagToNote(note, selectedTag);
                            });
                        }
                    }
                }
                
                dialog.show();
            }
            
            @Override
            public void onError(String error) {
                // Still show dialog even if loading tags fails (user can create new tag)
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.add_tag)
                    .setView(dialogView)
                    .setPositiveButton("Create New Tag", (d, which) -> {
                        String tagName = etTag.getText().toString().trim();
                        if (!tagName.isEmpty()) {
                            createAndAddTag(note, tagName);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a tag name", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
                
                dialog.show();
            }
        });
    }
    
    /**
     * Create a new tag and add it to the note
     */
    private void createAndAddTag(Note note, String tagName) {
        // Default color for new tags
        String defaultColor = "#FF8C42"; // Use orange primary color
        
        noteRepository.createTag(tagName, defaultColor, new retrofit2.Callback<com.example.anchornotes_team3.model.Tag>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.anchornotes_team3.model.Tag> call, 
                                 @NonNull retrofit2.Response<com.example.anchornotes_team3.model.Tag> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.anchornotes_team3.model.Tag newTag = response.body();
                    // Add the newly created tag to the note
                    addTagToNote(note, newTag);
                } else {
                    String errorMsg = "Failed to create tag";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull retrofit2.Call<com.example.anchornotes_team3.model.Tag> call, 
                                @NonNull Throwable t) {
                Log.e(TAG, "Error creating tag", t);
                Toast.makeText(MainActivity.this, "Failed to create tag: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Add a tag to a note
     */
    private void addTagToNote(Note note, com.example.anchornotes_team3.model.Tag tag) {
        // Get current note tags
        List<String> currentTagIds = note.getTags().stream()
            .map(com.example.anchornotes_team3.model.Tag::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        
        // Add new tag if not already present
        if (!currentTagIds.contains(tag.getId())) {
            currentTagIds.add(tag.getId());
            
            // Update note with new tags
            noteRepository.setNoteTags(note.getId(), currentTagIds, new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(com.example.anchornotes_team3.model.Note updatedNote) {
                    Toast.makeText(MainActivity.this, "Tag \"" + tag.getName() + "\" added successfully", Toast.LENGTH_SHORT).show();
                    // Refresh notes
                    loadNotes();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, "Failed to add tag: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "Tag \"" + tag.getName() + "\" already added to this note", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Confirm and delete a note
     */
    private void confirmDeleteNote(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }
        
        String noteTitle = note.getTitle();
        if (noteTitle == null || noteTitle.trim().isEmpty()) {
            noteTitle = "Untitled Note";
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_note)
            .setMessage(getString(R.string.delete_note_confirmation) + "\n\n" + noteTitle)
            .setPositiveButton(R.string.delete_note, (dialog, which) -> {
                deleteNote(note.getId());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Delete a note by ID
     */
    private void deleteNote(String noteId) {
        if (noteId == null || noteId.isEmpty()) {
            return;
        }
        
        noteRepository.deleteNote(noteId, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                // Refresh notes
                loadNotes();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Failed to delete note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Toggle pin status of a note
     */
    private void togglePinNote(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }
        
        // Toggle pin status
        boolean newPinStatus = !note.isPinned();
        
        noteRepository.pinNote(note.getId(), newPinStatus, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note updatedNote) {
                String message = newPinStatus ? "Note pinned" : "Note unpinned";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                // Refresh notes to update sections
                loadNotes();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Failed to " + (newPinStatus ? "pin" : "unpin") + " note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
