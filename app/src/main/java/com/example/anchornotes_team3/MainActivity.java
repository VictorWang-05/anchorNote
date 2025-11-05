package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.content.pm.PackageManager;

import com.example.anchornotes_team3.adapter.NoteAdapter;
import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.util.ThemeUtils;
import com.example.anchornotes_team3.dto.GeofenceRegistrationResponse;
import com.example.anchornotes_team3.geofence.GeofenceManager;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.example.anchornotes_team3.store.RelevantNotesStore;
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
    private GeofenceManager geofenceManager;
    private RelevantNotesStore relevantNotesStore;
    
    // UI elements
    private MaterialButton loginButton;
    private TextView usernameDisplay;
    private TextView avatarText;
    private MaterialButton newNoteButton;
    private com.google.android.material.textfield.TextInputLayout searchInputLayout;
    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private View navHome;
    private View navFilter;
    private View navTemplates;
    private View navStats;
    
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

    // Flag to prevent duplicate note loading
    private boolean isLoadingNotes = false;
    private boolean notesLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);
        
        // Initialize auth manager first to check login status
        authManager = AuthManager.getInstance(this);
        
        // Check if user is logged in - redirect to login if not
        if (!authManager.isLoggedIn()) {
            // User not logged in, redirect to login page immediately
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // User is logged in, continue with normal setup
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize repositories
        noteRepository = NoteRepository.getInstance(this);
        geofenceManager = new GeofenceManager(this);
        relevantNotesStore = RelevantNotesStore.getInstance(this);
        
        // Find UI elements
        loginButton = findViewById(R.id.loginButton);
        usernameDisplay = findViewById(R.id.usernameDisplay);
        avatarText = findViewById(R.id.avatarText);
        newNoteButton = findViewById(R.id.newNoteButton);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        searchEditText = findViewById(R.id.searchEditText);
        navHome = findViewById(R.id.nav_home);
        navFilter = findViewById(R.id.nav_filter);
        navTemplates = findViewById(R.id.nav_templates);
        navStats = findViewById(R.id.nav_stats);

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

        // Set up search functionality
        setupSearch();
        
        // Set up RelevantNotesStore listener
        setupRelevantNotesListener();

        // Load notes if logged in (will be loaded in onResume to avoid duplicate calls)
        // Don't load here to prevent race condition with onResume()
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if user is still logged in (in case they logged out elsewhere)
        if (!authManager.isLoggedIn()) {
            // User logged out, redirect to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // User is logged in, refresh notes
        // Only load if logged in and not already loading
        loadNotes();
        syncGeofencesFromBackend();
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

        // Attach swipe actions
        attachSwipeHandlers(pinnedRecyclerView, pinnedAdapter);
        attachSwipeHandlers(relevantRecyclerView, relevantAdapter);
        attachSwipeHandlers(allNotesRecyclerView, allNotesAdapter);
    }

    private void attachSwipeHandlers(RecyclerView recyclerView, NoteAdapter adapter) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                com.example.anchornotes_team3.model.Note note = adapter.getNoteAt(position);
                if (note == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    // Immediately reset swipe UI to avoid temporary gap
                    adapter.notifyItemChanged(position);
                    // Delete with confirm
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_note)
                        .setMessage(getString(R.string.delete_note_confirmation) + "\n\n" + (note.getTitle() == null ? "Untitled Note" : note.getTitle()))
                        .setPositiveButton(R.string.delete_note, (d, which) -> {
                            deleteNote(note.getId());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .setOnDismissListener(null)
                        .show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Reset swipe right state immediately; perform action in background
                    adapter.notifyItemChanged(position);
                    boolean newPinStatus = !note.isPinned();
                    noteRepository.pinNote(note.getId(), newPinStatus, new NoteRepository.NoteCallback() {
                        @Override
                        public void onSuccess(Note updatedNote) {
                            String message = newPinStatus ? "Note pinned" : "Note unpinned";
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            loadNotes();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, "Failed to " + (newPinStatus ? "pin" : "unpin") + ": " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
    
    private void setupClickListeners() {
        // Login button click
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Avatar click (navigate to account page)
        avatarText.setOnClickListener(v -> {
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

        // Bottom Navigation click listeners
        navHome.setOnClickListener(v -> {
            // Scroll to top when on home page
            androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView)
                ((android.view.ViewGroup) findViewById(R.id.main)).getChildAt(0);
            if (scrollView != null) {
                scrollView.smoothScrollTo(0, 0);
            }
        });

        navFilter.setOnClickListener(v -> {
            if (authManager.isLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, FilterOptionsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please login to filter notes", Toast.LENGTH_SHORT).show();
            }
        });

        navTemplates.setOnClickListener(v -> {
            if (authManager.isLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, TemplateActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        navStats.setOnClickListener(v -> {
            try {
                if (authManager.isLoggedIn()) {
                    Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening statistics", e);
                Toast.makeText(MainActivity.this, "Unable to open Statistics: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSearch() {
        // Click listener for search icon
        searchInputLayout.setStartIconOnClickListener(v -> {
            performSearch();
            // Hide keyboard after search
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        });

        // Keyboard search action listener
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to search notes", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            return;
        }

        Log.d(TAG, "Launching search results for query: " + query);

        Intent intent = new Intent(MainActivity.this, SearchResultsActivity.class);
        intent.putExtra(SearchResultsActivity.EXTRA_SEARCH_QUERY, query);
        startActivity(intent);
    }
    
    private void updateLoginUI() {
        if (authManager.isLoggedIn()) {
            // User is logged in - show username and avatar
            String username = authManager.getUsername();
            String displayName = username != null ? username : "User";
            loginButton.setVisibility(View.GONE);
            usernameDisplay.setVisibility(View.VISIBLE);
            usernameDisplay.setText(displayName);

            // Set avatar initial
            if (avatarText != null) {
                String initial = displayName.length() > 0 ? displayName.substring(0, 1).toUpperCase() : "U";
                avatarText.setText(initial);
            }
        } else {
            // User is not logged in - show login button
            loginButton.setVisibility(View.VISIBLE);
            usernameDisplay.setVisibility(View.GONE);
            if (avatarText != null) {
                avatarText.setText("?");
            }
            // Clear notes when logged out
            clearAllNotes();
        }
    }
    
    private void loadNotes() {
        // Prevent duplicate concurrent calls
        if (isLoadingNotes) {
            Log.d(TAG, "📥 Already loading notes, skipping duplicate call");
            return;
        }

        // Double-check login status
        if (!authManager.isLoggedIn()) {
            Log.w(TAG, "📥 Cannot load notes - user not logged in");
            clearAllNotes();
            return;
        }

        // Verify token exists
        String token = authManager.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "📥 Cannot load notes - no token found");
            Toast.makeText(MainActivity.this, "Please login again", Toast.LENGTH_SHORT).show();
            clearAllNotes();
            return;
        }

        isLoadingNotes = true;
        Log.d(TAG, "📥 Loading notes... (Token exists: " + (token != null) + ")");
        
        noteRepository.getAllNotes(new NoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<Note> notes) {
                isLoadingNotes = false;
                notesLoaded = true;
                Log.d(TAG, "✅ Loaded " + notes.size() + " notes");
                evaluateTimeRelevantNotes(notes);
                updateNoteLists(notes);
            }
            
            @Override
            public void onError(String error) {
                isLoadingNotes = false;
                notesLoaded = false;
                Log.e(TAG, "❌ Failed to load notes: " + error);

                // Check if it's an authentication error
                if (error != null && (error.contains("401") || error.contains("403") || error.contains("Unauthorized"))) {
                    Log.e(TAG, "🔐 Authentication error - token may be invalid");
                    Toast.makeText(MainActivity.this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load notes: " + error, Toast.LENGTH_SHORT).show();
                }

                // Show empty states on error
                clearAllNotes();
            }
        });
    }

    /**
     * Mark notes as relevant when current time is within ±1 hour of their reminder time.
     * Adds with a timeout that auto-removes after +1h from reminder time.
     */
    private void evaluateTimeRelevantNotes(List<Note> notes) {
        try {
            relevantNotesStore.clearExpired();
            long now = System.currentTimeMillis();
            final long ONE_MIN = 60L * 1000L;

            for (Note n : notes) {
                if (n == null || n.getId() == null) continue;
                java.time.Instant rt = n.getReminderTime();
                if (rt == null) {
                    // If reminder cleared and note was time-relevant, remove it (geofence handled separately)
                    if (relevantNotesStore.isRelevant(n.getId()) && !n.hasGeofence()) {
                        relevantNotesStore.removeRelevantNote(n.getId());
                    }
                    continue;
                }

                long reminder = rt.toEpochMilli();
                int rangeMin = com.example.anchornotes_team3.store.TimeRangeStore.getInstance(this)
                        .getRangeMinutes(n.getId());
                long rangeMillis = Math.max(1, rangeMin) * ONE_MIN;
                long diff = Math.abs(reminder - now);
                if (diff <= rangeMillis) {
                    long windowEnd = reminder + rangeMillis;
                    long duration = Math.max(0, windowEnd - now);
                    relevantNotesStore.addRelevantNoteWithTimeout(n.getId(), duration);
                } else {
                    // Outside window: remove if present
                    if (relevantNotesStore.isRelevant(n.getId()) && !n.hasGeofence()) {
                        relevantNotesStore.removeRelevantNote(n.getId());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating time relevant notes", e);
        }
    }
    
    private void updateNoteLists(List<Note> allNotes) {
        // Filter pinned notes
        List<Note> pinnedNotes = allNotes.stream()
            .filter(Note::isPinned)
            .collect(Collectors.toList());
        
        // Filter relevant notes based on RelevantNotesStore
        // Notes appear here when:
        // - User enters a geofence (removed when user exits)
        // - Time reminder is triggered (removed after 1 hour)
        java.util.Set<String> relevantNoteIds = relevantNotesStore.getRelevantNoteIds();
        List<Note> relevantNotes = allNotes.stream()
            .filter(note -> relevantNoteIds.contains(note.getId()))
            .collect(Collectors.toList());
        
        Log.d(TAG, "Relevant notes: " + relevantNotes.size() + " (from " + relevantNoteIds.size() + " IDs)");
        
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
    
    /**
     * Set up listener for RelevantNotesStore changes
     * When geofences are entered/exited, the UI will automatically update
     */
    private void setupRelevantNotesListener() {
        relevantNotesStore.addListener(relevantNoteIds -> {
            Log.d(TAG, "RelevantNotesStore changed: " + relevantNoteIds.size() + " relevant notes");
            // Reload notes to update the relevant notes section
            if (authManager.isLoggedIn()) {
                loadNotes();
            }
        });
    }
    
    /**
     * Manually check proximity to geofences and mark relevant notes
     * This ensures notes appear in "Relevant Notes" even if we're already inside when geofence is registered
     */
    private void checkProximityToGeofences(List<GeofenceManager.GeofenceData> geofences) {
        if (geofences == null || geofences.isEmpty()) {
            return;
        }
        
        // Check if we have location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No location permission for proximity check");
            return;
        }
        
        Log.d(TAG, "Checking proximity to " + geofences.size() + " geofences...");
        
        // Get current location
        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = 
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
        
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null  // CancellationToken - null is acceptable
        ).addOnSuccessListener(location -> {
            if (location != null) {
                Log.d(TAG, "Current location: " + location.getLatitude() + ", " + location.getLongitude());
                
                // Check distance to each geofence
                for (GeofenceManager.GeofenceData geofence : geofences) {
                    float[] distance = new float[1];
                    android.location.Location.distanceBetween(
                        location.getLatitude(), 
                        location.getLongitude(),
                        geofence.latitude, 
                        geofence.longitude, 
                        distance
                    );
                    
                String noteId = geofence.geofenceId.replace("note_", "");
                
                if (distance[0] <= geofence.radiusMeters) {
                    Log.d(TAG, "✅ Within range of geofence " + geofence.geofenceId + " (distance: " + distance[0] + "m, radius: " + geofence.radiusMeters + "m)");
                    relevantNotesStore.addRelevantNote(noteId);
                } else {
                    Log.d(TAG, "❌ Outside range of geofence " + geofence.geofenceId + " (distance: " + distance[0] + "m, radius: " + geofence.radiusMeters + "m)");
                    // Remove note from relevant notes if it's now outside the range
                    relevantNotesStore.removeRelevantNote(noteId);
                }
                }
            } else {
                Log.w(TAG, "Could not get current location for proximity check");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get current location for proximity check", e);
        });
    }
    
    /**
     * Sync all geofences from backend and register with device
     * Called on app start to ensure all geofences are registered
     */
    private void syncGeofencesFromBackend() {
        Log.d(TAG, "Syncing geofences from backend...");
        
        noteRepository.getApiService().listGeofences().enqueue(new Callback<List<GeofenceRegistrationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<GeofenceRegistrationResponse>> call,
                                 @NonNull Response<List<GeofenceRegistrationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GeofenceRegistrationResponse> geofences = response.body();
                    Log.d(TAG, "Received " + geofences.size() + " geofences from backend");
                    
                    if (!geofences.isEmpty()) {
                        // Convert to GeofenceManager.GeofenceData list
                        List<GeofenceManager.GeofenceData> geofenceDataList = new ArrayList<>();
                        for (GeofenceRegistrationResponse geo : geofences) {
                            geofenceDataList.add(new GeofenceManager.GeofenceData(
                                geo.getGeofenceId(),
                                geo.getLatitude(),
                                geo.getLongitude(),
                                geo.getRadiusMeters().floatValue()
                            ));
                        }
                        
                        // Register all geofences with device
                        geofenceManager.addGeofences(geofenceDataList, new GeofenceManager.GeofenceCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Log.d(TAG, "Successfully synced " + geofences.size() + " geofences");
                                // After syncing, check if we're currently near any geofences
                                checkProximityToGeofences(geofenceDataList);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Failed to sync geofences: " + error);
                            }
                        });
                    } else {
                        Log.d(TAG, "No geofences to sync");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch geofences: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<GeofenceRegistrationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching geofences", t);
            }
        });
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
        com.google.android.material.textfield.TextInputLayout etTagLayout = dialogView.findViewById(R.id.et_tag_layout);
        com.google.android.material.chip.ChipGroup chipGroupExisting = dialogView.findViewById(R.id.chip_group_existing_tags);
        
        // Selected color state (default to app orange)
        final String[] selectedColor = new String[]{"#FF8C42"};
        try {
            etTagLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(selectedColor[0])));
        } catch (Exception ignored) {}
        
        // Color picker on start icon
        etTagLayout.setStartIconOnClickListener(v -> {
            com.example.anchornotes_team3.util.ColorPickerDialog.show(MainActivity.this, selectedColor[0], hex -> {
                selectedColor[0] = hex;
                try {
                    etTagLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor(hex)));
                } catch (Exception ignored4) {}
            });
        });
        
        // Create new tag on plus (end icon)
        etTagLayout.setEndIconOnClickListener(v -> {
            String tagName = etTag.getText().toString().trim();
            if (tagName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a tag name", Toast.LENGTH_SHORT).show();
                return;
            }
            noteRepository.createTag(tagName, selectedColor[0], new retrofit2.Callback<com.example.anchornotes_team3.model.Tag>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<com.example.anchornotes_team3.model.Tag> call,
                                       @NonNull retrofit2.Response<com.example.anchornotes_team3.model.Tag> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.example.anchornotes_team3.model.Tag newTag = response.body();
                        // Add a chip for the newly created tag
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(MainActivity.this);
                        chip.setText(newTag.getName());
                        chip.setCheckable(true);
                        chip.setClickable(true);
                        if (newTag.getColor() != null && !newTag.getColor().isEmpty()) {
                            try {
                                int c = android.graphics.Color.parseColor(newTag.getColor());
                                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(c));
                                chip.setTextColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                            } catch (Exception ignored) {}
                        }
                        chip.setTag(newTag);
                        chip.setOnClickListener(view -> {
                            addTagToNote(note, newTag);
                        });
                        chipGroupExisting.addView(chip);
                        etTag.setText("");
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to create tag", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(@NonNull retrofit2.Call<com.example.anchornotes_team3.model.Tag> call,
                                      @NonNull Throwable t) {
                    Toast.makeText(MainActivity.this, "Failed to create tag: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        
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
                    .setPositiveButton("Done", null)
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
                    .setPositiveButton("Done", null)
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
