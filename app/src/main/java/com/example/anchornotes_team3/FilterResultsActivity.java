package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.NoteAdapter;
import com.example.anchornotes_team3.model.FilterCriteria;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FilterResultsActivity extends AppCompatActivity {

    private static final String TAG = "FilterResults";

    public static final String EXTRA_FILTER_CRITERIA = "filter_criteria";

    private MaterialToolbar toolbar;
    private MaterialButton btnChangeFilter;
    private TextView tvFilterSummary;
    private TextView tvResultCount;
    private RecyclerView rvFilterResults;
    private View layoutEmpty;

    private NoteAdapter filterAdapter;
    private NoteRepository noteRepository;
    private FilterCriteria filterCriteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_results);

        noteRepository = NoteRepository.getInstance(this);

        filterCriteria = getIntent().getParcelableExtra(EXTRA_FILTER_CRITERIA);
        if (filterCriteria == null) {
            Toast.makeText(this, "No filter criteria provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        displayFilterSummary();
        performFilter();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        btnChangeFilter = findViewById(R.id.btn_change_filter);
        tvFilterSummary = findViewById(R.id.tv_filter_summary);
        tvResultCount = findViewById(R.id.tv_result_count);
        rvFilterResults = findViewById(R.id.rv_filter_results);
        layoutEmpty = findViewById(R.id.layout_empty);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // Navigate back to MainActivity instead of FilterOptionsActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnChangeFilter.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterOptionsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupRecyclerView() {
        filterAdapter = new NoteAdapter(false);
        rvFilterResults.setLayoutManager(new LinearLayoutManager(this));
        rvFilterResults.setAdapter(filterAdapter);

        filterAdapter.setOnNoteClickListener(new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                openNote(note);
            }

            @Override
            public void onAddTagClick(Note note) {
                Toast.makeText(FilterResultsActivity.this, "Open note to add tags", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Note note) {
                confirmDeleteNote(note);
            }

            @Override
            public void onPinClick(Note note) {
                togglePinNote(note);
            }
        });
    }

    private void displayFilterSummary() {
        List<String> summaryParts = new ArrayList<>();

        if (filterCriteria.getTagIds() != null && !filterCriteria.getTagIds().isEmpty()) {
            summaryParts.add(filterCriteria.getTagIds().size() + " tag(s)");
        }

        if (filterCriteria.getHasPhoto() != null && filterCriteria.getHasPhoto()) {
            summaryParts.add("Has Photo");
        }

        if (filterCriteria.getHasAudio() != null && filterCriteria.getHasAudio()) {
            summaryParts.add("Has Audio");
        }

        String summary = String.join(", ", summaryParts);
        tvFilterSummary.setText(summary.isEmpty() ? "None" : summary);
    }

    private void performFilter() {
        noteRepository.filterNotes(
                filterCriteria.getTagIds(),
                null,
                filterCriteria.getHasPhoto(),
                filterCriteria.getHasAudio(),
                new NoteRepository.NotesCallback() {
                    @Override
                    public void onSuccess(List<Note> notes) {
                        displayResults(notes);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(FilterResultsActivity.this, "Filter failed: " + error, Toast.LENGTH_SHORT).show();
                        displayResults(new ArrayList<>());
                    }
                });
    }

    private void displayResults(List<Note> notes) {
        if (notes.isEmpty()) {
            rvFilterResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvResultCount.setText("0 results");
        } else {
            rvFilterResults.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvResultCount.setText(notes.size() + " result" + (notes.size() == 1 ? "" : "s"));
            filterAdapter.setNotes(notes);
        }
    }

    private void openNote(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }

        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }

    private void confirmDeleteNote(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }

        String noteTitle = note.getTitle();
        if (noteTitle == null || noteTitle.trim().isEmpty()) {
            noteTitle = "Untitled Note";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?\n\n" + noteTitle)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNote(note.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(String noteId) {
        noteRepository.deleteNote(noteId, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(FilterResultsActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                performFilter();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(FilterResultsActivity.this, "Failed to delete note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePinNote(Note note) {
        if (note == null || note.getId() == null) {
            return;
        }

        boolean newPinStatus = !note.isPinned();

        noteRepository.pinNote(note.getId(), newPinStatus, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note updatedNote) {
                String message = newPinStatus ? "Note pinned" : "Note unpinned";
                Toast.makeText(FilterResultsActivity.this, message, Toast.LENGTH_SHORT).show();
                performFilter();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(FilterResultsActivity.this, "Failed to " + (newPinStatus ? "pin" : "unpin") + " note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        performFilter();
    }
}
