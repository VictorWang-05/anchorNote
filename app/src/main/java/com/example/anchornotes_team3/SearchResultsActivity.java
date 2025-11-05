package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.NoteAdapter;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity {

    private static final String TAG = "SearchResults";

    public static final String EXTRA_SEARCH_QUERY = "search_query";

    private MaterialToolbar toolbar;
    private com.google.android.material.button.MaterialButton btnFilter;
    private TextView tvSearchQuery;
    private TextView tvResultCount;
    private RecyclerView rvSearchResults;
    private View layoutEmpty;

    private NoteAdapter searchAdapter;
    private NoteRepository noteRepository;
    private String searchQuery;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Initialize repository
        noteRepository = NoteRepository.getInstance(this);

        // Get search query from intent
        searchQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            Toast.makeText(this, "No search query provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        btnFilter = findViewById(R.id.btn_filter);
        tvSearchQuery = findViewById(R.id.tv_search_query);
        tvResultCount = findViewById(R.id.tv_result_count);
        rvSearchResults = findViewById(R.id.rv_search_results);
        layoutEmpty = findViewById(R.id.layout_empty);

        // Setup toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup filter button
        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(SearchResultsActivity.this, FilterOptionsActivity.class);
            startActivity(intent);
        });

        // Setup RecyclerView
        setupRecyclerView();

        // Display search query
        tvSearchQuery.setText("\"" + searchQuery + "\"");

        // Perform search
        performSearch();
    }

    private void setupRecyclerView() {
        searchAdapter = new NoteAdapter(false);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchAdapter);

        // Set click listener
        searchAdapter.setOnNoteClickListener(new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                openNote(note);
            }

            @Override
            public void onAddTagClick(Note note) {
                Toast.makeText(SearchResultsActivity.this, "Open note to add tags", Toast.LENGTH_SHORT).show();
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

    private void performSearch() {
        Log.d(TAG, "Performing search with query: " + searchQuery);

        noteRepository.searchNotes(searchQuery, new NoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<Note> notes) {
                Log.d(TAG, "Search returned " + notes.size() + " notes");
                displayResults(notes);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Search failed: " + error);
                Toast.makeText(SearchResultsActivity.this, "Search failed: " + error, Toast.LENGTH_SHORT).show();
                displayResults(new ArrayList<>());
            }
        });
    }

    private void displayResults(List<Note> notes) {
        if (notes.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvResultCount.setText("0 results");
        } else {
            rvSearchResults.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvResultCount.setText(notes.size() + " result" + (notes.size() == 1 ? "" : "s"));
            searchAdapter.setNotes(notes);
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
                Toast.makeText(SearchResultsActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                performSearch();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SearchResultsActivity.this, "Failed to delete note: " + error, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SearchResultsActivity.this, message, Toast.LENGTH_SHORT).show();
                performSearch();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SearchResultsActivity.this, "Failed to " + (newPinStatus ? "pin" : "unpin") + " note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Skip search on initial load (already done in onCreate)
        // But refresh on subsequent resumes (e.g., returning from NoteEditorActivity)
        if (!isInitialLoad) {
            Log.d(TAG, "Refreshing search results on resume");
            performSearch();
        } else {
            isInitialLoad = false;
        }
    }
}
