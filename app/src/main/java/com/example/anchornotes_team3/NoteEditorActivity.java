package com.example.anchornotes_team3;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.AttachmentsAdapter;
import com.example.anchornotes_team3.geofence.GeofenceManager;
import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Geofence;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.example.anchornotes_team3.util.FormattingTextWatcher;
import com.example.anchornotes_team3.util.GeocoderHelper;
import com.example.anchornotes_team3.util.MarkdownConverter;
import com.example.anchornotes_team3.util.MediaHelper;
import com.example.anchornotes_team3.util.TextSpanUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating and editing notes
 */
public class NoteEditorActivity extends AppCompatActivity implements AttachmentsAdapter.OnAttachmentActionListener {

    // UI Components
    private MaterialToolbar toolbar;
    private EditText etTitle;
    private EditText etBody;
    private ChipGroup chipGroupMeta;
    private Chip chipAddTag;
    private Chip chipLocation;
    private Chip chipReminder;
    private RecyclerView rvAttachments;
    private LinearLayout layoutAttachments;
    
    // Formatting buttons
    private MaterialButton btnBold;
    private MaterialButton btnItalic;
    private MaterialButton btnSizeSmall;
    private MaterialButton btnSizeMedium;
    private MaterialButton btnSizeLarge;
    private MaterialButton btnAddPhoto;
    private MaterialButton btnAddAudio;

    // Data
    private Note currentNote;
    private NoteRepository repository;
    private AttachmentsAdapter attachmentsAdapter;
    private MediaHelper mediaHelper;
    private GeofenceManager geofenceManager;
    private GeocoderHelper geocoderHelper;
    private MenuItem pinMenuItem;
    private List<Tag> availableTags = new ArrayList<>();
    private boolean isNewNote = true;
    private boolean isTemplateMode = false; // Flag to indicate if creating/editing a template
    
    // Formatting toggle support
    private FormattingTextWatcher formattingTextWatcher;
    
    // Track pending uploads
    private int pendingUploads = 0;
    private boolean shouldFinishAfterUploads = false;

    // Audio playback
    private MediaPlayer mediaPlayer;
    private Attachment currentlyPlayingAttachment;

    // Track attachments to delete from backend
    private List<Attachment> attachmentsToDelete = new ArrayList<>();
    
    // Pending geofence data (for permission request)
    private Geofence pendingGeofence = null;
    
    // State persistence keys
    private static final String STATE_PHOTO_URI = "photo_uri";
    private static final String STATE_IS_NEW_NOTE = "is_new_note";
    private static final String STATE_NOTE_ID = "note_id";
    
    // Permission request codes
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 101;
    
    // Activity request codes
    private static final int REQUEST_CODE_MAP_LOCATION_PICKER = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Initialize repository and helpers
        repository = NoteRepository.getInstance(this);
        mediaHelper = new MediaHelper(this);
        geofenceManager = new GeofenceManager(this);
        geocoderHelper = new GeocoderHelper(this);
        
        // Restore photo URI if activity was recreated (e.g., after camera app)
        if (savedInstanceState != null) {
            Uri restoredPhotoUri = savedInstanceState.getParcelable(STATE_PHOTO_URI);
            isNewNote = savedInstanceState.getBoolean(STATE_IS_NEW_NOTE, true);
            String savedNoteId = savedInstanceState.getString(STATE_NOTE_ID);
            
            if (restoredPhotoUri != null) {
                mediaHelper.setCurrentPhotoUri(restoredPhotoUri);
                android.util.Log.d("NoteEditor", "🔄 Restored photo URI: " + restoredPhotoUri);
            }
            
            if (savedNoteId != null && currentNote != null) {
                currentNote.setId(savedNoteId);
            }
            
            android.util.Log.d("NoteEditor", "🔄 State restored - isNewNote: " + isNewNote);
        }

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupChips();
        setupFormattingToggle();
        setupFormattingBar();
        setupAttachmentsRecyclerView();

        // Load available tags
        loadAvailableTags();

        // Load or create note
        String noteId = getIntent().getStringExtra("note_id");
        boolean templateMode = getIntent().getBooleanExtra("is_template_mode", false);
        
        if (templateMode) {
            // Template creation mode
            isTemplateMode = true;
            isNewNote = true;
            currentNote = new Note();
            loadNoteIntoUI();
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.create_template);
            }
        } else if (noteId != null && !noteId.isEmpty()) {
            isNewNote = false;
            loadNoteFromBackend(noteId);
        } else {
            isNewNote = true;
            currentNote = new Note();
            loadNoteIntoUI();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.et_title);
        etBody = findViewById(R.id.et_body);
        chipGroupMeta = findViewById(R.id.chip_group_meta);
        chipAddTag = findViewById(R.id.chip_add_tag);
        chipLocation = findViewById(R.id.chip_location);
        chipReminder = findViewById(R.id.chip_reminder);
        rvAttachments = findViewById(R.id.rv_attachments);
        layoutAttachments = findViewById(R.id.layout_attachments);
        
        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnSizeSmall = findViewById(R.id.btn_size_small);
        btnSizeMedium = findViewById(R.id.btn_size_medium);
        btnSizeLarge = findViewById(R.id.btn_size_large);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnAddAudio = findViewById(R.id.btn_add_audio);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_note);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupChips() {
        chipAddTag.setOnClickListener(v -> showAddTagDialog());
        chipLocation.setOnClickListener(v -> showAddLocationDialog());
        chipLocation.setOnLongClickListener(v -> {
            clearLocation();
            return true;
        });
        chipReminder.setOnClickListener(v -> showAddReminderDialog());
        chipReminder.setOnLongClickListener(v -> {
            clearReminder();
            return true;
        });
    }

    private void setupFormattingToggle() {
        // Initialize formatting text watcher for toggle mode
        formattingTextWatcher = new FormattingTextWatcher(etBody);
        etBody.addTextChangedListener(formattingTextWatcher);
    }

    private void setupFormattingBar() {
        btnBold.setOnClickListener(v -> {
            int start = etBody.getSelectionStart();
            int end = etBody.getSelectionEnd();
            
            if (start >= 0 && end > start) {
                // Has selection - apply to selected text
                TextSpanUtils.toggleBold(etBody);
            } else {
                // No selection - toggle mode for future typing
                formattingTextWatcher.toggleBold();
                updateButtonStates();
            }
        });
        
        btnItalic.setOnClickListener(v -> {
            int start = etBody.getSelectionStart();
            int end = etBody.getSelectionEnd();
            
            if (start >= 0 && end > start) {
                // Has selection - apply to selected text
                TextSpanUtils.toggleItalic(etBody);
            } else {
                // No selection - toggle mode for future typing
                formattingTextWatcher.toggleItalic();
                updateButtonStates();
            }
        });
        
        btnSizeSmall.setOnClickListener(v -> {
            int start = etBody.getSelectionStart();
            int end = etBody.getSelectionEnd();
            
            if (start >= 0 && end > start) {
                // Has selection - apply to selected text
                TextSpanUtils.applyTextSize(etBody, TextSpanUtils.TextSize.SMALL);
            } else {
                // No selection - toggle mode for future typing
                formattingTextWatcher.setSize(TextSpanUtils.TextSize.SMALL);
                updateButtonStates();
            }
        });
        
        btnSizeMedium.setOnClickListener(v -> {
            int start = etBody.getSelectionStart();
            int end = etBody.getSelectionEnd();
            
            if (start >= 0 && end > start) {
                // Has selection - apply to selected text
                TextSpanUtils.applyTextSize(etBody, TextSpanUtils.TextSize.MEDIUM);
            } else {
                // No selection - toggle mode for future typing
                formattingTextWatcher.setSize(TextSpanUtils.TextSize.MEDIUM);
                updateButtonStates();
            }
        });
        
        btnSizeLarge.setOnClickListener(v -> {
            int start = etBody.getSelectionStart();
            int end = etBody.getSelectionEnd();
            
            if (start >= 0 && end > start) {
                // Has selection - apply to selected text
                TextSpanUtils.applyTextSize(etBody, TextSpanUtils.TextSize.LARGE);
            } else {
                // No selection - toggle mode for future typing
                formattingTextWatcher.setSize(TextSpanUtils.TextSize.LARGE);
                updateButtonStates();
            }
        });
        
        btnAddPhoto.setOnClickListener(v -> handleAddPhoto());
        btnAddAudio.setOnClickListener(v -> handleAddAudio());
    }
    
    /**
     * Update button appearance to show which formatting is active
     */
    private void updateButtonStates() {
        // Update bold button
        if (formattingTextWatcher.isBoldActive()) {
            btnBold.setBackgroundColor(getColor(R.color.format_active));
        } else {
            btnBold.setBackgroundColor(getColor(android.R.color.transparent));
        }
        
        // Update italic button
        if (formattingTextWatcher.isItalicActive()) {
            btnItalic.setBackgroundColor(getColor(R.color.format_active));
        } else {
            btnItalic.setBackgroundColor(getColor(android.R.color.transparent));
        }
        
        // Update size buttons - reset all first
        btnSizeSmall.setBackgroundColor(getColor(android.R.color.transparent));
        btnSizeMedium.setBackgroundColor(getColor(android.R.color.transparent));
        btnSizeLarge.setBackgroundColor(getColor(android.R.color.transparent));
        
        // Highlight active size button
        if (formattingTextWatcher.isSizeActive()) {
            // We need to check which size is active - for now just show indication
            // You could extend FormattingTextWatcher to return which size if needed
        }
    }

    private void setupAttachmentsRecyclerView() {
        attachmentsAdapter = new AttachmentsAdapter();
        attachmentsAdapter.setOnAttachmentActionListener(this);
        rvAttachments.setAdapter(attachmentsAdapter);
        rvAttachments.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    /**
     * Load available tags from backend
     */
    private void loadAvailableTags() {
        repository.getAllTags(new NoteRepository.TagsCallback() {
            @Override
            public void onSuccess(List<Tag> tags) {
                availableTags = tags;
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NoteEditorActivity.this, "Failed to load tags: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load note from backend
     */
    private void loadNoteFromBackend(String noteId) {
        repository.getNoteById(noteId, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note note) {
                currentNote = note;
                loadNoteIntoUI();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NoteEditorActivity.this, "Failed to load note: " + error, Toast.LENGTH_SHORT).show();
                currentNote = new Note();
                loadNoteIntoUI();
            }
        });
    }

    private void loadNoteIntoUI() {
        if (currentNote == null) return;

        etTitle.setText(currentNote.getTitle());

        // Convert Markdown to formatted text (Spanned) when loading
        String noteText = currentNote.getText();
        if (noteText != null && !noteText.isEmpty()) {
            android.util.Log.d("NoteEditor", "📖 Loading markdown: " + noteText);
            android.text.Spanned formattedText = MarkdownConverter.fromMarkdown(this, noteText);
            etBody.setText(formattedText);
            android.util.Log.d("NoteEditor", "✅ Loaded formatted text, length: " + formattedText.length());
        } else {
            etBody.setText("");
        }

        // Update chips
        updateTagChips();
        updateLocationChip();
        updateReminderChip();
        updateAttachments();

        // Update pin icon in menu
        updatePinIcon();
    }

    private void updateTagChips() {
        // Remove existing tag chips (keep the add tag chip)
        for (int i = chipGroupMeta.getChildCount() - 1; i >= 0; i--) {
            View child = chipGroupMeta.getChildAt(i);
            if (child instanceof Chip && child.getId() != R.id.chip_add_tag 
                    && child.getId() != R.id.chip_location 
                    && child.getId() != R.id.chip_reminder) {
                chipGroupMeta.removeView(child);
            }
        }
        
        // Add tag chips
        for (Tag tag : currentNote.getTags()) {
            Chip tagChip = new Chip(this);
            tagChip.setText(tag.getName());
            tagChip.setCloseIconVisible(true);
            tagChip.setOnCloseIconClickListener(v -> {
                currentNote.removeTag(tag);
                updateTagChips();
            });
            chipGroupMeta.addView(tagChip, chipGroupMeta.indexOfChild(chipAddTag));
        }
    }

    private void updateLocationChip() {
        if (currentNote.hasGeofence()) {
            Geofence geofence = currentNote.getGeofence();
            String displayText = geofence.getAddress() != null && !geofence.getAddress().isEmpty() 
                ? geofence.getAddress() 
                : "Location (" + geofence.getRadius() + "m)";
            chipLocation.setText(displayText);
            chipLocation.setCloseIconVisible(true);
            chipLocation.setOnCloseIconClickListener(v -> clearLocation());
        } else {
            chipLocation.setText(R.string.chip_add_location);
            chipLocation.setCloseIconVisible(false);
        }
    }

    private void updateReminderChip() {
        if (currentNote.hasTimeReminder()) {
            // Show time reminder info
            Instant reminderTime = currentNote.getReminderTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                    .withZone(ZoneId.systemDefault());
            String text = formatter.format(reminderTime);
            chipReminder.setText(text);
            chipReminder.setCloseIconVisible(true);
            chipReminder.setOnCloseIconClickListener(v -> clearReminder());
        } else {
            chipReminder.setText(R.string.chip_no_reminder);
            chipReminder.setCloseIconVisible(false);
        }
    }

    private void updateAttachments() {
        if (currentNote.getAttachments().isEmpty()) {
            layoutAttachments.setVisibility(View.GONE);
        } else {
            layoutAttachments.setVisibility(View.VISIBLE);
            attachmentsAdapter.setAttachments(currentNote.getAttachments());
        }
    }

    private void showAddTagDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null);
        TextInputEditText etTag = dialogView.findViewById(R.id.et_tag);
        com.google.android.material.textfield.TextInputLayout etTagLayout = dialogView.findViewById(R.id.et_tag_layout);
        
        // Selected color state (default)
        final String[] selectedColor = new String[]{"#FF8C42"};
        try {
            etTagLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(selectedColor[0])));
        } catch (Exception ignored) {}
        
        // Color picker on start icon (visual chips)
        etTagLayout.setStartIconOnClickListener(v -> {
            com.example.anchornotes_team3.util.ColorPickerDialog.show(NoteEditorActivity.this, selectedColor[0], hex -> {
                selectedColor[0] = hex;
                try {
                    etTagLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor(hex)));
                } catch (Exception ignored4) {}
            });
        });
        
        // Create tag on plus icon
        etTagLayout.setEndIconOnClickListener(v -> {
            String tagName = etTag.getText().toString().trim();
            if (!tagName.isEmpty()) {
                createAndAddTag(tagName, selectedColor[0]);
                etTag.setText("");
            } else {
                android.widget.Toast.makeText(NoteEditorActivity.this, "Please enter a tag name", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Show existing tags as suggestions
        String[] tagNames = availableTags.stream()
                .map(Tag::getName)
                .toArray(String[]::new);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Tag")
                .setView(dialogView)
                .setItems(tagNames, (d, which) -> {
                    Tag selectedTag = availableTags.get(which);
                    currentNote.addTag(selectedTag);
                    updateTagChips();
                })
                .setPositiveButton("Done", null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.show();
    }
    
    private void createAndAddTag(String tagName) {
        createAndAddTag(tagName, "#FF6B6B");
    }

    private void createAndAddTag(String tagName, String color) {
        String chosen = color != null ? color : "#FF6B6B";
        repository.createTag(tagName, chosen, new retrofit2.Callback<Tag>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Tag> call, @NonNull retrofit2.Response<Tag> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Tag newTag = response.body();
                    availableTags.add(newTag);
                    currentNote.addTag(newTag);
                    updateTagChips();
                } else {
                    Toast.makeText(NoteEditorActivity.this, "Failed to create tag", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<Tag> call, @NonNull Throwable t) {
                Toast.makeText(NoteEditorActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Launch Google Maps picker to select location (geofence)
     */
    private void showAddLocationDialog() {
        Intent intent = new Intent(this, MapLocationPickerActivity.class);
        
        // If geofence already exists, pass it to the map picker
        if (currentNote.hasGeofence()) {
            Geofence geofence = currentNote.getGeofence();
            intent.putExtra(MapLocationPickerActivity.EXTRA_LATITUDE, geofence.getLatitude());
            intent.putExtra(MapLocationPickerActivity.EXTRA_LONGITUDE, geofence.getLongitude());
            intent.putExtra(MapLocationPickerActivity.EXTRA_RADIUS, geofence.getRadius());
            intent.putExtra(MapLocationPickerActivity.EXTRA_ADDRESS, geofence.getAddress());
        }
        
        startActivityForResult(intent, REQUEST_CODE_MAP_LOCATION_PICKER);
    }

    /**
     * Show dialog to add/edit time reminder
     */
    private void showAddReminderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null);
        TextView tvSelectedDatetime = dialogView.findViewById(R.id.tv_selected_datetime);
        
        final Calendar calendar = Calendar.getInstance();
        
        // Pre-fill if time reminder exists
        if (currentNote.hasTimeReminder()) {
            Instant reminderTime = currentNote.getReminderTime();
            calendar.setTimeInMillis(reminderTime.toEpochMilli());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
            tvSelectedDatetime.setText(sdf.format(calendar.getTime()));
        }
        
        // Date/Time picker for time reminder
        tvSelectedDatetime.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        
                        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                                (view1, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
                                    tvSelectedDatetime.setText(sdf.format(calendar.getTime()));
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                        timePickerDialog.show();
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_ok, (d, which) -> {
                    // Save time reminder
                    Instant reminderTime = Instant.ofEpochMilli(calendar.getTimeInMillis());
                    saveTimeReminder(reminderTime);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.show();
    }
    
    /**
     * Save time reminder
     */
    private void saveTimeReminder(Instant reminderTime) {
        currentNote.setReminderTime(reminderTime);
        updateReminderChip();
    }
    
    /**
     * Geocode address and save geofence
     */
    private void saveGeofence(String address, int radiusMeters) {
        // Show progress
        Toast.makeText(this, R.string.geocoding_in_progress, Toast.LENGTH_SHORT).show();
        
        // Geocode address to lat/lng
        geocoderHelper.geocodeAddress(address, new GeocoderHelper.GeocodeCallback() {
            @Override
            public void onSuccess(double latitude, double longitude, String formattedAddress) {
                // Create geofence object
                Geofence geofence = new Geofence(latitude, longitude, radiusMeters, formattedAddress);
                
                // Check if note exists on backend first
                if (currentNote.getId() == null || currentNote.getId().isEmpty()) {
                    // Note doesn't exist yet - save locally and will be sent to backend when note is saved
                    currentNote.setGeofence(geofence);
                    updateLocationChip();
                    Toast.makeText(NoteEditorActivity.this, "Location set (will be saved with note)", Toast.LENGTH_SHORT).show();
                } else {
                    // Note exists - save location to backend and register with device
                    saveGeofenceToBackend(geofence);
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(NoteEditorActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Save geofence to backend and register with device
     */
    private void saveGeofenceToBackend(Geofence geofence) {
        // Check location permission first
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
        boolean locationEnabled = geofenceManager.isLocationEnabled();
        
        android.util.Log.d("NoteEditor", "Geofence check - Permission: " + hasPermission + ", Location enabled: " + locationEnabled);
        
        if (!hasPermission) {
            // Store pending geofence and request permission
            pendingGeofence = geofence;
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_LOCATION_PERMISSION);
            return;
        }
        
        // Check if location services are enabled
        if (!locationEnabled) {
            Toast.makeText(this, 
                "Location services are disabled. Please enable location in Settings → Location.", 
                Toast.LENGTH_LONG).show();
            // Still save to backend - will work when location is enabled
        }
        
        // Save to backend
        repository.setGeofence(currentNote.getId(), geofence, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note note) {
                currentNote = note;
                updateLocationChip();
                
                // Register geofence with device
                geofenceManager.addGeofence(
                    currentNote.getId(),
                    geofence.getLatitude(),
                    geofence.getLongitude(),
                    geofence.getRadius().floatValue(),
                    new GeofenceManager.GeofenceCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(NoteEditorActivity.this, 
                                R.string.location_saved, Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(String error) {
                            // Geofence is saved to backend, but device registration failed
                            // This is common on emulators - geofence will work on real devices
                            android.util.Log.w("NoteEditor", "Geofence saved to backend but device registration failed: " + error);
                            
                            String message;
                            if (error.contains("Location services are disabled")) {
                                message = "Geofence saved. Enable location services in Settings for it to work.";
                            } else if (error.contains("not available")) {
                                message = "Geofence saved. Will work on real devices or when location is enabled.";
                            } else {
                                message = "Geofence saved to backend. Device registration failed: " + error;
                            }
                            
                            Toast.makeText(NoteEditorActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                );
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(NoteEditorActivity.this, 
                    "Failed to save geofence: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Clear location (geofence) only
     */
    private void clearLocation() {
        String noteId = currentNote.getId();
        boolean hasGeofence = currentNote.hasGeofence();
        
        if (noteId != null && hasGeofence) {
            // Note exists on backend - clear geofence
            repository.clearReminders(noteId, new NoteRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    currentNote.clearGeofence();
                    updateLocationChip();
                    
                    // Unregister geofence from device
                    geofenceManager.removeGeofence(noteId, new GeofenceManager.GeofenceCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(NoteEditorActivity.this, 
                                R.string.location_removed, Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Toast.makeText(NoteEditorActivity.this, 
                                "Location cleared but device unregister failed: " + error, 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, 
                        "Failed to clear location: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Note doesn't exist yet or no geofence - clear locally
            currentNote.clearGeofence();
            updateLocationChip();
            Toast.makeText(this, R.string.location_removed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clear time reminder only
     */
    private void clearReminder() {
        String noteId = currentNote.getId();
        boolean hasTimeReminder = currentNote.hasTimeReminder();
        
        if (noteId != null && hasTimeReminder) {
            // Note exists on backend - clear time reminder
            repository.clearReminders(noteId, new NoteRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    currentNote.clearTimeReminder();
                    updateReminderChip();
                    Toast.makeText(NoteEditorActivity.this, 
                        R.string.reminder_cleared, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, 
                        "Failed to clear reminder: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Note doesn't exist yet or no time reminder - clear locally
            currentNote.clearTimeReminder();
            updateReminderChip();
            Toast.makeText(this, R.string.reminder_cleared, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAddPhoto() {
        if (!mediaHelper.hasCameraPermission()) {
            mediaHelper.requestCameraPermission();
            return;
        }
        
        // Show options: Camera or Gallery
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    try {
                        if (which == 0) {
                            mediaHelper.launchCamera();
                        } else {
                            mediaHelper.launchGallery();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void handleAddAudio() {
        if (!mediaHelper.hasAudioPermission()) {
            mediaHelper.requestAudioPermission();
            return;
        }

        // Show audio recording dialog
        showAudioRecordingDialog();
    }

    private void showAudioRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_audio_recording, null);

        TextView tvRecordingStatus = dialogView.findViewById(R.id.tv_recording_status);
        MaterialButton btnStartStop = dialogView.findViewById(R.id.btn_start_stop);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        final boolean[] isRecording = {false};
        final android.os.Handler handler = new android.os.Handler();
        final Runnable[] updateTimer = new Runnable[1];

        updateTimer[0] = new Runnable() {
            @Override
            public void run() {
                if (mediaHelper.isRecording()) {
                    int duration = mediaHelper.getRecordingDuration();
                    tvRecordingStatus.setText(String.format(Locale.getDefault(),
                            "Recording... %d:%02d", duration / 60, duration % 60));
                    handler.postDelayed(this, 1000);
                }
            }
        };

        AlertDialog dialog = builder.setView(dialogView).create();

        btnStartStop.setOnClickListener(v -> {
            if (!isRecording[0]) {
                // Start recording
                try {
                    mediaHelper.startAudioRecording();
                    isRecording[0] = true;
                    btnStartStop.setText("Stop Recording");
                    btnStartStop.setIcon(getDrawable(android.R.drawable.ic_media_pause));
                    handler.post(updateTimer[0]);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to start recording: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                // Stop recording
                Uri audioUri = mediaHelper.stopAudioRecording();
                int duration = mediaHelper.getRecordingDuration();
                isRecording[0] = false;
                handler.removeCallbacks(updateTimer[0]);

                if (audioUri != null) {
                    // Add audio attachment to note
                    Attachment attachment = new Attachment(Attachment.AttachmentType.AUDIO, audioUri);
                    attachment.setDurationSec(duration);
                    currentNote.addAttachment(attachment);
                    updateAttachments();
                    Toast.makeText(this, "Audio recorded successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to save audio recording", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isRecording[0]) {
                mediaHelper.cancelAudioRecording();
                handler.removeCallbacks(updateTimer[0]);
            }
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            if (isRecording[0]) {
                mediaHelper.cancelAudioRecording();
                handler.removeCallbacks(updateTimer[0]);
            }
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        android.util.Log.d("NoteEditor", "📷 onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == MediaHelper.REQUEST_IMAGE_CAPTURE) {
                Uri photoUri = mediaHelper.getCurrentPhotoUri();
                android.util.Log.d("NoteEditor", "📷 Camera photo URI: " + photoUri);
                if (photoUri != null) {
                    Attachment attachment = new Attachment(Attachment.AttachmentType.PHOTO, photoUri);
                    currentNote.addAttachment(attachment);
                    android.util.Log.d("NoteEditor", "📷 Photo attachment added! Total attachments: " + currentNote.getAttachments().size());
                    updateAttachments();
                }
            } else if (requestCode == MediaHelper.REQUEST_IMAGE_PICK && data != null) {
                Uri photoUri = data.getData();
                android.util.Log.d("NoteEditor", "📷 Gallery photo URI: " + photoUri);
                if (photoUri != null) {
                    Attachment attachment = new Attachment(Attachment.AttachmentType.PHOTO, photoUri);
                    currentNote.addAttachment(attachment);
                    android.util.Log.d("NoteEditor", "📷 Photo attachment added! Total attachments: " + currentNote.getAttachments().size());
                    updateAttachments();
                }
            } else if (requestCode == REQUEST_CODE_MAP_LOCATION_PICKER && data != null) {
                // Handle location selected from map
                double latitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LATITUDE, 0);
                double longitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LONGITUDE, 0);
                int radius = data.getIntExtra(MapLocationPickerActivity.EXTRA_RADIUS, 200);
                String address = data.getStringExtra(MapLocationPickerActivity.EXTRA_ADDRESS);
                
                android.util.Log.d("NoteEditor", "📍 Map location selected: " + address);
                android.util.Log.d("NoteEditor", "📍 Coordinates: " + latitude + ", " + longitude + " (radius: " + radius + "m)");
                
                // Create geofence with selected location
                Geofence geofence = new Geofence(latitude, longitude, radius, address);
                
                // Check if note exists on backend
                if (currentNote.getId() == null || currentNote.getId().isEmpty()) {
                    // Note doesn't exist yet - save locally
                    currentNote.setGeofence(geofence);
                    updateLocationChip();
                    Toast.makeText(this, "Location set (will be saved with note)", Toast.LENGTH_SHORT).show();
                } else {
                    // Note exists - save to backend
                    saveGeofenceToBackend(geofence);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == MediaHelper.REQUEST_CAMERA_PERMISSION) {
                handleAddPhoto();
            } else if (requestCode == MediaHelper.REQUEST_AUDIO_PERMISSION) {
                handleAddAudio();
            } else if (requestCode == MediaHelper.REQUEST_LOCATION_PERMISSION) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
                // Location permission granted - retry pending geofence save
                if (pendingGeofence != null) {
                    saveGeofenceToBackend(pendingGeofence);
                    pendingGeofence = null;
                }
            }
        } else {
            if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
                Toast.makeText(this, R.string.permission_location_required, Toast.LENGTH_LONG).show();
                pendingGeofence = null; // Clear pending geofence
            } else {
                String permissionType = "Permission";
                if (requestCode == MediaHelper.REQUEST_CAMERA_PERMISSION) {
                    permissionType = "Camera permission";
                } else if (requestCode == MediaHelper.REQUEST_AUDIO_PERMISSION) {
                    permissionType = "Microphone permission";
                } else if (requestCode == MediaHelper.REQUEST_LOCATION_PERMISSION) {
                    permissionType = "Location permission";
                }
                Toast.makeText(this, permissionType + " denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        // Save photo URI before activity is destroyed (e.g., when camera app opens)
        Uri photoUri = mediaHelper.getCurrentPhotoUri();
        if (photoUri != null) {
            outState.putParcelable(STATE_PHOTO_URI, photoUri);
            android.util.Log.d("NoteEditor", "💾 Saving photo URI: " + photoUri);
        }
        
        // Save other state
        outState.putBoolean(STATE_IS_NEW_NOTE, isNewNote);
        if (currentNote != null && currentNote.getId() != null) {
            outState.putString(STATE_NOTE_ID, currentNote.getId());
        }
        
        android.util.Log.d("NoteEditor", "💾 State saved");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_editor, menu);
        pinMenuItem = menu.findItem(R.id.action_pin);
        updatePinIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_pin) {
            togglePin();
            return true;
        } else if (id == R.id.action_discard) {
            showDiscardDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();

        // Convert formatted text (Spanned) to Markdown when saving
        android.text.Editable bodyEditable = etBody.getText();
        android.util.Log.d("NoteEditor", "💾 Converting to markdown, text length: " + bodyEditable.length());
        String markdown = MarkdownConverter.toMarkdown(bodyEditable);
        android.util.Log.d("NoteEditor", "💾 Generated markdown: " + markdown);

        currentNote.setTitle(title);
        currentNote.setText(markdown);

        android.util.Log.d("NoteEditor", "🚀 Attempting to save note: " + title);
        android.util.Log.d("NoteEditor", "📝 Is new note: " + isNewNote);
        android.util.Log.d("NoteEditor", "📋 Is template mode: " + isTemplateMode);
        
        if (isTemplateMode) {
            // Create template instead of note
            android.util.Log.d("NoteEditor", "📤 Calling createTemplate API...");
            
            // Convert Note to Template model
            com.example.anchornotes_team3.model.Template template = new com.example.anchornotes_team3.model.Template();
            template.setName(title); // Use title as template name
            template.setText(markdown);
            template.setPinned(currentNote.isPinned());
            template.setTags(currentNote.getTags() != null ? currentNote.getTags() : new ArrayList<>());
            template.setGeofence(currentNote.getGeofence());
            // Note: Attachments are not copied to templates - templates are blueprints for content
            
            repository.createTemplate(template, new NoteRepository.TemplateCallback() {
                @Override
                public void onSuccess(com.example.anchornotes_team3.model.Template createdTemplate) {
                    android.util.Log.d("NoteEditor", "✅ Template saved successfully! ID: " + createdTemplate.getId());
                    Toast.makeText(NoteEditorActivity.this, "Template saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("NoteEditor", "❌ Failed to save template: " + error);
                    Toast.makeText(NoteEditorActivity.this, "Failed to save template: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        
        if (isNewNote) {
            // Create new note
            android.util.Log.d("NoteEditor", "📤 Calling createNote API...");
            repository.createNote(currentNote, new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    android.util.Log.d("NoteEditor", "✅ Note saved successfully! ID: " + note.getId());
                    
                    // Preserve local attachments before replacing currentNote
                    List<Attachment> localAttachments = new ArrayList<>(currentNote.getAttachments());
                    android.util.Log.d("NoteEditor", "💾 Preserving " + localAttachments.size() + " local attachments");
                    
                    currentNote = note;
                    isNewNote = false;
                    
                    // Restore local attachments that need to be uploaded
                    for (Attachment attachment : localAttachments) {
                        currentNote.addAttachment(attachment);
                    }
                    android.util.Log.d("NoteEditor", "💾 Restored attachments. Total: " + currentNote.getAttachments().size());
                    
                    // After creating note, save reminders and attachments
                    int uploads = saveRemindersAndAttachments();
                    
                    Toast.makeText(NoteEditorActivity.this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                    
                    // Only finish if no uploads are pending
                    if (uploads == 0) {
                        finish();
                    } else {
                        shouldFinishAfterUploads = true;
                        android.util.Log.d("NoteEditor", "⏳ Waiting for " + uploads + " uploads to complete...");
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("NoteEditor", "❌ Failed to save note: " + error);
                    Toast.makeText(NoteEditorActivity.this, "Failed to save note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Update existing note
            repository.updateNote(currentNote, new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    android.util.Log.d("NoteEditor", "✅ Note updated successfully! ID: " + note.getId());

                    // Preserve local attachments before replacing currentNote
                    List<Attachment> localAttachments = new ArrayList<>(currentNote.getAttachments());
                    android.util.Log.d("NoteEditor", "💾 Preserving " + localAttachments.size() + " local attachments");

                    currentNote = note;

                    // Restore local attachments that need to be uploaded
                    for (Attachment attachment : localAttachments) {
                        // Only add back attachments that haven't been uploaded yet
                        if (!attachment.isUploaded()) {
                            currentNote.addAttachment(attachment);
                        }
                    }
                    android.util.Log.d("NoteEditor", "💾 Restored attachments. Total: " + currentNote.getAttachments().size());

                    // After updating note, save reminders and attachments
                    int uploads = saveRemindersAndAttachments();

                    Toast.makeText(NoteEditorActivity.this, R.string.note_saved, Toast.LENGTH_SHORT).show();

                    // Only finish if no uploads are pending
                    if (uploads == 0) {
                        finish();
                    } else {
                        shouldFinishAfterUploads = true;
                        android.util.Log.d("NoteEditor", "⏳ Waiting for " + uploads + " uploads to complete...");
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, "Failed to update note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    /**
     * Save reminders and upload attachments after note is created/updated
     * @return number of uploads started
     */
    private int saveRemindersAndAttachments() {
        if (currentNote.getId() == null) return 0;

        android.util.Log.d("NoteEditor", "💾 saveRemindersAndAttachments called for note " + currentNote.getId());
        android.util.Log.d("NoteEditor", "💾 Total attachments: " + currentNote.getAttachments().size());
        android.util.Log.d("NoteEditor", "🗑️ Attachments to delete: " + attachmentsToDelete.size());

        pendingUploads = 0;

        // Delete attachments from backend that were removed by user
        for (Attachment attachment : attachmentsToDelete) {
            android.util.Log.d("NoteEditor", "🗑️ Deleting attachment from backend: " + attachment.getId() + " (type: " + attachment.getType() + ")");

            if (attachment.getType() == Attachment.AttachmentType.PHOTO) {
                repository.deletePhotoAttachment(currentNote.getId(), attachment.getId(), new NoteRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("NoteEditor", "✅ Photo attachment deleted from backend: " + attachment.getId());
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("NoteEditor", "❌ Failed to delete photo attachment: " + error);
                        // Don't show error to user - it's non-critical
                    }
                });
            } else if (attachment.getType() == Attachment.AttachmentType.AUDIO) {
                repository.deleteAudioAttachment(currentNote.getId(), attachment.getId(), new NoteRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("NoteEditor", "✅ Audio attachment deleted from backend: " + attachment.getId());
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("NoteEditor", "❌ Failed to delete audio attachment: " + error);
                        // Don't show error to user - it's non-critical
                    }
                });
            }
        }

        // Clear the deletion list after processing
        attachmentsToDelete.clear();
        
        // Save time reminder if set
        if (currentNote.hasTimeReminder()) {
            pendingUploads++;
            android.util.Log.d("NoteEditor", "⏰ Saving time reminder...");
            repository.setTimeReminder(currentNote.getId(), currentNote.getReminderTime(), new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    android.util.Log.d("NoteEditor", "✅ Time reminder saved successfully");
                    currentNote.setReminderTime(note.getReminderTime());
                    onUploadComplete();
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("NoteEditor", "❌ Failed to set reminder: " + error);
                    Toast.makeText(NoteEditorActivity.this, "Failed to set reminder: " + error, Toast.LENGTH_SHORT).show();
                    onUploadComplete();
                }
            });
        }
        
        // Save geofence if set
        if (currentNote.hasGeofence()) {
            pendingUploads++;
            android.util.Log.d("NoteEditor", "📍 Saving geofence...");
            repository.setGeofence(currentNote.getId(), currentNote.getGeofence(), new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    android.util.Log.d("NoteEditor", "✅ Geofence saved successfully");
                    currentNote.setGeofence(note.getGeofence());
                    updateLocationChip();
                    onUploadComplete();
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("NoteEditor", "❌ Failed to set geofence: " + error);
                    Toast.makeText(NoteEditorActivity.this, "Failed to set geofence: " + error, Toast.LENGTH_SHORT).show();
                    onUploadComplete();
                }
            });
        }
        
        // Upload attachments that aren't uploaded yet
        for (Attachment attachment : currentNote.getAttachments()) {
            android.util.Log.d("NoteEditor", "💾 Checking attachment: type=" + attachment.getType() + 
                    ", isUploaded=" + attachment.isUploaded() + ", hasUri=" + (attachment.getUri() != null));
            
            if (!attachment.isUploaded() && attachment.getUri() != null) {
                if (attachment.getType() == Attachment.AttachmentType.PHOTO) {
                    pendingUploads++;
                    android.util.Log.d("NoteEditor", "📸 Starting photo upload for note " + currentNote.getId());
                    repository.uploadPhoto(currentNote.getId(), attachment.getUri(), new NoteRepository.AttachmentCallback() {
                        @Override
                        public void onSuccess(String attachmentId, String mediaUrl) {
                            android.util.Log.d("NoteEditor", "✅ Photo uploaded successfully! ID: " + attachmentId);
                            attachment.setId(attachmentId);
                            attachment.setMediaUrl(mediaUrl);
                            attachment.setUploaded(true);
                            onUploadComplete();
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("NoteEditor", "❌ Photo upload failed: " + error);
                            Toast.makeText(NoteEditorActivity.this, "Failed to upload photo: " + error, Toast.LENGTH_SHORT).show();
                            onUploadComplete();
                        }
                    });
                } else if (attachment.getType() == Attachment.AttachmentType.AUDIO && attachment.getDurationSec() != null) {
                    pendingUploads++;
                    android.util.Log.d("NoteEditor", "🎵 Starting audio upload for note " + currentNote.getId());
                    repository.uploadAudio(currentNote.getId(), attachment.getUri(), attachment.getDurationSec(), new NoteRepository.AttachmentCallback() {
                        @Override
                        public void onSuccess(String attachmentId, String mediaUrl) {
                            android.util.Log.d("NoteEditor", "✅ Audio uploaded successfully! ID: " + attachmentId);
                            attachment.setId(attachmentId);
                            attachment.setMediaUrl(mediaUrl);
                            attachment.setUploaded(true);
                            onUploadComplete();
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("NoteEditor", "❌ Audio upload failed: " + error);
                            Toast.makeText(NoteEditorActivity.this, "Failed to upload audio: " + error, Toast.LENGTH_SHORT).show();
                            onUploadComplete();
                        }
                    });
                }
            }
        }
        
        return pendingUploads;
    }
    
    /**
     * Called when an upload completes (success or failure)
     */
    private void onUploadComplete() {
        pendingUploads--;
        android.util.Log.d("NoteEditor", "📊 Upload complete. Remaining: " + pendingUploads);
        
        if (pendingUploads <= 0 && shouldFinishAfterUploads) {
            android.util.Log.d("NoteEditor", "✅ All uploads complete. Finishing activity...");
            finish();
        }
    }

    private void togglePin() {
        if (currentNote == null) return;
        
        boolean newPinStatus = !currentNote.isPinned();
        
        // If note doesn't exist yet (new note), just update local state
        // The pin status will be saved when the note is saved
        if (isNewNote || currentNote.getId() == null || currentNote.getId().isEmpty()) {
            currentNote.setPinned(newPinStatus);
            updatePinIcon();
            String message = newPinStatus ? "Note will be pinned when saved" : "Note will be unpinned when saved";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For existing notes, immediately update on backend
        repository.pinNote(currentNote.getId(), newPinStatus, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note note) {
                // Update current note with the response
                currentNote.setPinned(note.isPinned());
                updatePinIcon();
                String message = note.isPinned() ? "Note pinned" : "Note unpinned";
                Toast.makeText(NoteEditorActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                // Revert the change on error
                currentNote.setPinned(!newPinStatus);
                updatePinIcon();
                Toast.makeText(NoteEditorActivity.this, "Failed to " + (newPinStatus ? "pin" : "unpin") + " note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Optimistically update UI
        currentNote.setPinned(newPinStatus);
        updatePinIcon();
    }

    private void updatePinIcon() {
        if (pinMenuItem != null && currentNote != null) {
            if (currentNote.isPinned()) {
                pinMenuItem.setIcon(android.R.drawable.btn_star_big_on);
                pinMenuItem.setTitle(R.string.action_unpin);
            } else {
                pinMenuItem.setIcon(android.R.drawable.btn_star_big_off);
                pinMenuItem.setTitle(R.string.action_pin);
            }
        }
    }

    private void showDiscardDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.discard_confirm_title)
                .setMessage(R.string.discard_confirm_message)
                .setPositiveButton(R.string.action_discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    // AttachmentsAdapter.OnAttachmentActionListener implementation
    @Override
    public void onRemoveAttachment(Attachment attachment) {
        android.util.Log.d("NoteEditor", "🗑️ Removing attachment: " + attachment.getId() + ", uploaded: " + attachment.isUploaded());

        // Remove from current note
        currentNote.removeAttachment(attachment);

        // If this attachment was uploaded to backend, track it for deletion
        if (attachment.getId() != null && !attachment.getId().isEmpty() && attachment.isUploaded()) {
            android.util.Log.d("NoteEditor", "📝 Tracking attachment for backend deletion: " + attachment.getId());
            attachmentsToDelete.add(attachment);
        }

        updateAttachments();
    }

    @Override
    public void onPlayAudio(Attachment attachment) {
        try {
            // Stop any currently playing audio
            stopAudioPlayback();

            // Create new MediaPlayer
            mediaPlayer = new MediaPlayer();
            currentlyPlayingAttachment = attachment;

            // Set data source (URI for local files, URL for remote files)
            if (attachment.getUri() != null) {
                android.util.Log.d("NoteEditor", "🎵 Playing audio from URI: " + attachment.getUri());
                mediaPlayer.setDataSource(this, attachment.getUri());
            } else if (attachment.getMediaUrl() != null) {
                android.util.Log.d("NoteEditor", "🎵 Playing audio from URL: " + attachment.getMediaUrl());
                mediaPlayer.setDataSource(attachment.getMediaUrl());
            } else {
                Toast.makeText(this, "No audio source available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare and start playback
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                android.util.Log.d("NoteEditor", "✅ Audio playback started");
                Toast.makeText(this, "Playing audio", Toast.LENGTH_SHORT).show();
            });

            // Handle completion
            mediaPlayer.setOnCompletionListener(mp -> {
                android.util.Log.d("NoteEditor", "🎵 Audio playback completed");
                stopAudioPlayback();
                updateAttachments(); // Refresh to reset play button
            });

            // Handle errors
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("NoteEditor", "❌ Audio playback error: what=" + what + ", extra=" + extra);
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
                stopAudioPlayback();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            android.util.Log.e("NoteEditor", "❌ Failed to play audio", e);
            Toast.makeText(this, "Failed to play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopAudioPlayback();
        }
    }

    @Override
    public void onPauseAudio(Attachment attachment) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            android.util.Log.d("NoteEditor", "⏸️ Audio playback paused");
            Toast.makeText(this, "Audio paused", Toast.LENGTH_SHORT).show();
        } else {
            stopAudioPlayback();
        }
    }

    /**
     * Stop and release audio playback resources
     */
    private void stopAudioPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            currentlyPlayingAttachment = null;
            android.util.Log.d("NoteEditor", "🛑 Audio playback stopped");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up media player
        stopAudioPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop playback when activity is paused
        stopAudioPlayback();
    }
}

