package com.example.anchornotes_team3;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.AttachmentsAdapter;
import com.example.anchornotes_team3.model.Attachment;
import com.example.anchornotes_team3.model.Geofence;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.example.anchornotes_team3.util.FormattingTextWatcher;
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
    private MenuItem pinMenuItem;
    private List<Tag> availableTags = new ArrayList<>();
    private boolean isNewNote = true;
    
    // Formatting toggle support
    private FormattingTextWatcher formattingTextWatcher;
    
    // Track pending uploads
    private int pendingUploads = 0;
    private boolean shouldFinishAfterUploads = false;
    
    // State persistence keys
    private static final String STATE_PHOTO_URI = "photo_uri";
    private static final String STATE_IS_NEW_NOTE = "is_new_note";
    private static final String STATE_NOTE_ID = "note_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Initialize repository and helpers
        repository = NoteRepository.getInstance(this);
        mediaHelper = new MediaHelper(this);
        
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
        if (noteId != null && !noteId.isEmpty()) {
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
        etBody.setText(currentNote.getText());
        
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
            chipLocation.setText(geofence.getAddress() != null ? geofence.getAddress() : "Location Set");
            chipLocation.setCloseIconVisible(true);
            chipLocation.setOnCloseIconClickListener(v -> clearLocation());
        } else {
            chipLocation.setText(R.string.chip_no_location);
            chipLocation.setCloseIconVisible(false);
        }
    }

    private void updateReminderChip() {
        if (currentNote.hasTimeReminder()) {
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
                .setPositiveButton("Create New", (d, which) -> {
                    String tagName = etTag.getText().toString().trim();
                    if (!tagName.isEmpty()) {
                        createAndAddTag(tagName);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.show();
    }
    
    private void createAndAddTag(String tagName) {
        // Create tag with random color (or default)
        String defaultColor = "#FF6B6B";
        repository.createTag(tagName, defaultColor, new retrofit2.Callback<Tag>() {
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

    private void showAddLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null);
        TextInputEditText etAddress = dialogView.findViewById(R.id.et_address);
        MaterialButton btnDetectAuto = dialogView.findViewById(R.id.btn_detect_auto);
        
        btnDetectAuto.setOnClickListener(v -> {
            if (mediaHelper.hasLocationPermission()) {
                // TODO: Get current location and geocode to address
                etAddress.setText("Current Location (detected)");
            } else {
                mediaHelper.requestLocationPermission();
            }
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_ok, (d, which) -> {
                    String address = etAddress.getText().toString().trim();
                    if (!address.isEmpty()) {
                        // TODO: Geocode address to lat/long using Geocoder or backend API
                        // For now, use dummy coordinates
                        double lat = 34.0522; // Los Angeles example
                        double lng = -118.2437;
                        int radius = 100; // meters
                        
                        Geofence geofence = new Geofence(lat, lng, radius, address);
                        currentNote.setGeofence(geofence);
                        updateLocationChip();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.show();
    }

    private void showAddReminderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null);
        TabLayout tabReminderType = dialogView.findViewById(R.id.tab_reminder_type);
        LinearLayout layoutTimeReminder = dialogView.findViewById(R.id.layout_time_reminder);
        LinearLayout layoutGeofenceReminder = dialogView.findViewById(R.id.layout_geofence_reminder);
        TextView tvSelectedDatetime = dialogView.findViewById(R.id.tv_selected_datetime);
        TextInputEditText etGeoAddress = dialogView.findViewById(R.id.et_geo_address);
        Slider sliderRadius = dialogView.findViewById(R.id.slider_radius);
        TextView tvRadiusValue = dialogView.findViewById(R.id.tv_radius_value);
        
        final Calendar calendar = Calendar.getInstance();
        
        // Tab selection listener
        tabReminderType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutTimeReminder.setVisibility(View.VISIBLE);
                    layoutGeofenceReminder.setVisibility(View.GONE);
                } else {
                    layoutTimeReminder.setVisibility(View.GONE);
                    layoutGeofenceReminder.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
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
        
        // Radius slider listener
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> 
            tvRadiusValue.setText((int)value + " meters")
        );
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_ok, (d, which) -> {
                    int selectedTab = tabReminderType.getSelectedTabPosition();
                    
                    if (selectedTab == 0) {
                        // Time reminder
                        Instant reminderTime = Instant.ofEpochMilli(calendar.getTimeInMillis());
                        currentNote.setReminderTime(reminderTime);
                    } else {
                        // Geofence reminder (handled by location dialog)
                        Toast.makeText(this, "Please use Location button to set geofence", Toast.LENGTH_SHORT).show();
                    }
                    updateReminderChip();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.show();
    }

    private void clearLocation() {
        if (currentNote.getId() != null && (currentNote.hasGeofence() || currentNote.hasTimeReminder())) {
            // Clear ALL reminders (including location) from backend if note exists
            repository.clearReminders(currentNote.getId(), new NoteRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    currentNote.clearGeofence();
                    currentNote.clearTimeReminder();
                    updateLocationChip();
                    updateReminderChip();
                    Toast.makeText(NoteEditorActivity.this, "Location and reminders cleared", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, "Failed to clear location: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            currentNote.clearGeofence();
            updateLocationChip();
        }
    }

    private void clearReminder() {
        if (currentNote.getId() != null && (currentNote.hasTimeReminder() || currentNote.getGeofence() != null)) {
            // Clear ALL reminders from backend if note exists
            repository.clearReminders(currentNote.getId(), new NoteRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    currentNote.clearTimeReminder();
                    currentNote.setGeofence(null);
                    updateReminderChip();
                    updateLocationChip();
                    Toast.makeText(NoteEditorActivity.this, "Reminders cleared", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, "Failed to clear reminders: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            currentNote.clearTimeReminder();
            currentNote.setGeofence(null);
            updateReminderChip();
            updateLocationChip();
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
        
        // TODO: Implement audio recording
        Toast.makeText(this, "Audio recording - to be implemented", Toast.LENGTH_SHORT).show();
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
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
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
        String text = etBody.getText().toString();
        
        currentNote.setTitle(title);
        currentNote.setText(text);
        
        android.util.Log.d("NoteEditor", "🚀 Attempting to save note: " + title);
        android.util.Log.d("NoteEditor", "📝 Is new note: " + isNewNote);
        
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
                    currentNote = note;
                    
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
        
        pendingUploads = 0;
        
        // Save time reminder if set
        if (currentNote.hasTimeReminder()) {
            repository.setTimeReminder(currentNote.getId(), currentNote.getReminderTime(), new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    // Reminder saved
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, "Failed to set reminder: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Save geofence if set
        if (currentNote.hasGeofence()) {
            repository.setGeofence(currentNote.getId(), currentNote.getGeofence(), new NoteRepository.NoteCallback() {
                @Override
                public void onSuccess(Note note) {
                    // Geofence saved
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(NoteEditorActivity.this, "Failed to set geofence: " + error, Toast.LENGTH_SHORT).show();
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
        currentNote.setPinned(!currentNote.isPinned());
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
        currentNote.removeAttachment(attachment);
        updateAttachments();
    }

    @Override
    public void onPlayAudio(Attachment attachment) {
        // TODO: Implement audio playback
        Toast.makeText(this, "Playing audio...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPauseAudio(Attachment attachment) {
        // TODO: Implement audio pause
        Toast.makeText(this, "Paused audio", Toast.LENGTH_SHORT).show();
    }
}

