package com.example.anchornotes_team3.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton store that tracks which notes are currently "relevant"
 * A note is relevant when:
 * - User is inside its geofence (removed on EXIT)
 * - A time-based reminder was triggered (removed after 1 hour)
 */
public class RelevantNotesStore {
    private static final String TAG = "RelevantNotesStore";
    private static final String PREFS_NAME = "relevant_notes_prefs";
    private static final String KEY_RELEVANT_NOTES = "relevant_note_ids";
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000; // 1 hour in milliseconds
    
    private static RelevantNotesStore instance;
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Set<String> relevantNoteIds;
    private final Map<String, Long> noteAddedTimestamps; // Track when notes were added
    private final List<RelevantNotesListener> listeners;
    private final Handler handler;

    /**
     * Listener interface for observing relevant notes changes
     */
    public interface RelevantNotesListener {
        void onRelevantNotesChanged(Set<String> noteIds);
    }

    private RelevantNotesStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.relevantNoteIds = new HashSet<>();
        this.noteAddedTimestamps = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        
        // Load persisted relevant notes
        loadRelevantNotes();
    }

    /**
     * Get singleton instance
     */
    public static synchronized RelevantNotesStore getInstance(Context context) {
        if (instance == null) {
            instance = new RelevantNotesStore(context);
        }
        return instance;
    }

    /**
     * Add a note to relevant notes (from geofence ENTER)
     * 
     * @param noteId Note ID to add
     */
    public synchronized void addRelevantNote(String noteId) {
        if (noteId == null || noteId.isEmpty()) {
            Log.w(TAG, "Cannot add null or empty noteId");
            return;
        }

        if (!relevantNoteIds.contains(noteId)) {
            relevantNoteIds.add(noteId);
            noteAddedTimestamps.put(noteId, System.currentTimeMillis());
            saveRelevantNotes();
            notifyListeners();
            Log.d(TAG, "Added relevant note: " + noteId);
        }
    }

    /**
     * Add a note with a timeout (for time-based reminders)
     * Note will be automatically removed after the specified duration
     * 
     * @param noteId Note ID to add
     * @param durationMillis Duration in milliseconds before auto-removal
     */
    public synchronized void addRelevantNoteWithTimeout(String noteId, long durationMillis) {
        addRelevantNote(noteId);
        
        // Schedule automatic removal
        handler.postDelayed(() -> {
            removeRelevantNote(noteId);
            Log.d(TAG, "Auto-removed relevant note after timeout: " + noteId);
        }, durationMillis);
    }

    /**
     * Remove a note from relevant notes (from geofence EXIT or timeout)
     * 
     * @param noteId Note ID to remove
     */
    public synchronized void removeRelevantNote(String noteId) {
        if (noteId == null || noteId.isEmpty()) {
            Log.w(TAG, "Cannot remove null or empty noteId");
            return;
        }

        if (relevantNoteIds.remove(noteId)) {
            noteAddedTimestamps.remove(noteId);
            saveRelevantNotes();
            notifyListeners();
            Log.d(TAG, "Removed relevant note: " + noteId);
        }
    }

    /**
     * Get all currently relevant note IDs
     * 
     * @return Set of relevant note IDs
     */
    public synchronized Set<String> getRelevantNoteIds() {
        return new HashSet<>(relevantNoteIds);
    }

    /**
     * Check if a note is currently relevant
     * 
     * @param noteId Note ID to check
     * @return true if note is relevant
     */
    public synchronized boolean isRelevant(String noteId) {
        return relevantNoteIds.contains(noteId);
    }

    /**
     * Clear all relevant notes
     */
    public synchronized void clearAll() {
        relevantNoteIds.clear();
        noteAddedTimestamps.clear();
        saveRelevantNotes();
        notifyListeners();
        Log.d(TAG, "Cleared all relevant notes");
    }

    /**
     * Clear expired notes (added more than 1 hour ago)
     * Useful for cleaning up stale geofence entries
     */
    public synchronized void clearExpired() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : noteAddedTimestamps.entrySet()) {
            if (now - entry.getValue() > ONE_HOUR_MILLIS) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String noteId : toRemove) {
            relevantNoteIds.remove(noteId);
            noteAddedTimestamps.remove(noteId);
        }
        
        if (!toRemove.isEmpty()) {
            saveRelevantNotes();
            notifyListeners();
            Log.d(TAG, "Cleared " + toRemove.size() + " expired relevant notes");
        }
    }

    /**
     * Add a listener to observe relevant notes changes
     * 
     * @param listener Listener to add
     */
    public void addListener(RelevantNotesListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify new listener of current state
            listener.onRelevantNotesChanged(getRelevantNoteIds());
        }
    }

    /**
     * Remove a listener
     * 
     * @param listener Listener to remove
     */
    public void removeListener(RelevantNotesListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of changes
     */
    private void notifyListeners() {
        Set<String> noteIds = getRelevantNoteIds();
        handler.post(() -> {
            for (RelevantNotesListener listener : new ArrayList<>(listeners)) {
                listener.onRelevantNotesChanged(noteIds);
            }
        });
    }

    /**
     * Save relevant notes to SharedPreferences for persistence
     */
    private void saveRelevantNotes() {
        prefs.edit()
            .putStringSet(KEY_RELEVANT_NOTES, new HashSet<>(relevantNoteIds))
            .apply();
    }

    /**
     * Load relevant notes from SharedPreferences
     */
    private void loadRelevantNotes() {
        Set<String> saved = prefs.getStringSet(KEY_RELEVANT_NOTES, new HashSet<>());
        if (saved != null) {
            relevantNoteIds.addAll(saved);
            // Initialize timestamps for loaded notes
            long now = System.currentTimeMillis();
            for (String noteId : saved) {
                noteAddedTimestamps.put(noteId, now);
            }
            Log.d(TAG, "Loaded " + saved.size() + " relevant notes from storage");
        }
    }

    /**
     * Get count of relevant notes
     * 
     * @return Number of relevant notes
     */
    public synchronized int getCount() {
        return relevantNoteIds.size();
    }
}

