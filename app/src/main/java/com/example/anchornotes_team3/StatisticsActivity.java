package com.example.anchornotes_team3;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.anchornotes_team3.util.BottomNavigationHelper;
import com.example.anchornotes_team3.util.ThemeUtils;
import java.util.Comparator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private NoteRepository repository;
    private AuthManager authManager;

    private TextView tvTotalNotes;
    private TextView tvPinnedNotes;
    private TextView tvTimeReminders;
    private TextView tvGeofences;
    private TextView tvPhotoNotes;
    private TextView tvAudioNotes;
    private TextView tvTotalTags;
    private TextView tvAvgTags;
    private TextView tvLastUpdated;
    private android.widget.LinearLayout recentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);
        setContentView(R.layout.activity_statistics);

        repository = NoteRepository.getInstance(this);
        authManager = AuthManager.getInstance(this);

        // Back button
        android.view.View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, authManager);

        tvLastUpdated = findViewById(R.id.lastUpdated);
        tvTotalNotes = findViewById(R.id.tvTotalNotes);
        tvPinnedNotes = findViewById(R.id.tvPinnedNotes);
        tvTimeReminders = findViewById(R.id.tvTimeReminders);
        tvGeofences = findViewById(R.id.tvGeofences);
        tvPhotoNotes = findViewById(R.id.tvPhotoNotes);
        tvAudioNotes = findViewById(R.id.tvAudioNotes);
        tvTotalTags = findViewById(R.id.tvTotalTags);
        tvAvgTags = findViewById(R.id.tvAvgTags);
        recentContainer = findViewById(R.id.recentNotesContainer);

        try {
            loadStats();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load stats: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats in case notes changed while this screen was in the background
        loadStats();
    }

    private void loadStats() {
        // Fetch notes then tags; compute in callbacks
        repository.getAllNotes(new NoteRepository.NotesCallback() {
            @Override
            public void onSuccess(@NonNull List<Note> notes) {
                applyNoteStats(notes);
                repository.getAllTags(new NoteRepository.TagsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Tag> tags) {
                        tvTotalTags.setText(String.valueOf(tags.size()));
                        updateLastUpdated();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(StatisticsActivity.this, error, Toast.LENGTH_SHORT).show();
                        tvTotalTags.setText("-");
                        updateLastUpdated();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(StatisticsActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyNoteStats(List<Note> notes) {
        int total = notes.size();
        int pinned = 0;
        int timeRem = 0;
        int geofence = 0;
        int withPhoto = 0;
        int withAudio = 0;
        int totalTagsAssigned = 0;

        for (Note n : notes) {
            if (n.isPinned()) pinned++;
            if (n.hasTimeReminder()) timeRem++;
            if (n.hasGeofence()) geofence++;
            if (Boolean.TRUE.equals(n.getHasPhoto())) withPhoto++;
            if (Boolean.TRUE.equals(n.getHasAudio())) withAudio++;
            if (n.getTags() != null) totalTagsAssigned += n.getTags().size();
        }

        double avgTags = total == 0 ? 0.0 : (double) totalTagsAssigned / (double) total;

        tvTotalNotes.setText(String.valueOf(total));
        tvPinnedNotes.setText(String.valueOf(pinned));
        tvTimeReminders.setText(String.valueOf(timeRem));
        tvGeofences.setText(String.valueOf(geofence));
        tvPhotoNotes.setText(String.valueOf(withPhoto));
        tvAudioNotes.setText(String.valueOf(withAudio));
        tvAvgTags.setText(String.format(Locale.getDefault(), "%.2f", avgTags));

        // Populate recent notes (top 3 by lastEdited or createdAt)
        if (recentContainer != null) {
            recentContainer.removeAllViews();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault());
            notes.stream()
                .sorted(Comparator.comparing((Note n) -> n.getLastEdited() != null ? n.getLastEdited() : (n.getCreatedAt() != null ? n.getCreatedAt() : java.time.Instant.EPOCH)).reversed())
                .limit(3)
                .forEach(n -> {
                    android.view.View item = getLayoutInflater().inflate(R.layout.item_recent_note, recentContainer, false);
                    TextView t1 = item.findViewById(R.id.tvRecentTitle);
                    TextView t2 = item.findViewById(R.id.tvRecentDate);
                    String title = (n.getTitle() != null && !n.getTitle().isEmpty()) ? n.getTitle() : "Untitled";
                    t1.setText(title);
                    java.time.Instant ts = (n.getLastEdited() != null ? n.getLastEdited() : n.getCreatedAt());
                    if (ts != null) {
                        t2.setText(sdf.format(new java.util.Date(ts.toEpochMilli())));
                    } else {
                        t2.setText("");
                    }
                    item.setOnClickListener(v -> {
                        if (n.getId() != null) {
                            android.content.Intent intent = new android.content.Intent(this, NoteEditorActivity.class);
                            intent.putExtra("note_id", n.getId());
                            startActivity(intent);
                        }
                    });
                    recentContainer.addView(item);
                });
        }
    }

    private void updateLastUpdated() {
        String ts = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date());
        tvLastUpdated.setText(getString(R.string.last_updated_fmt, ts));
    }
}


