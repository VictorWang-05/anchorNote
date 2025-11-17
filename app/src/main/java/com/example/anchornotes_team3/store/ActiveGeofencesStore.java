package com.example.anchornotes_team3.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton store that tracks which geofences the user is currently inside
 * Used to prioritize templates with matching geofences
 */
public class ActiveGeofencesStore {
    private static final String TAG = "ActiveGeofencesStore";
    private static final String PREFS_NAME = "active_geofences_prefs";
    private static final String KEY_ACTIVE_GEOFENCES = "active_geofence_ids";
    
    private static ActiveGeofencesStore instance;
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Set<String> activeGeofenceIds; // Format: "note_123" or "template_456"
    private final List<ActiveGeofencesListener> listeners;
    private final Handler handler;

    /**
     * Listener interface for observing active geofences changes
     */
    public interface ActiveGeofencesListener {
        void onActiveGeofencesChanged(Set<String> geofenceIds);
    }

    private ActiveGeofencesStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.activeGeofenceIds = new HashSet<>();
        this.listeners = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        
        // Load persisted active geofences
        loadActiveGeofences();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ActiveGeofencesStore getInstance(Context context) {
        if (instance == null) {
            instance = new ActiveGeofencesStore(context);
        }
        return instance;
    }

    /**
     * Add a geofence as active (user entered)
     * 
     * @param geofenceId Geofence ID (e.g., "note_123" or "template_456")
     */
    public synchronized void addActiveGeofence(String geofenceId) {
        if (geofenceId == null || geofenceId.isEmpty()) {
            Log.w(TAG, "Cannot add null or empty geofenceId");
            return;
        }

        if (!activeGeofenceIds.contains(geofenceId)) {
            activeGeofenceIds.add(geofenceId);
            saveActiveGeofences();
            notifyListeners();
            Log.d(TAG, "Added active geofence: " + geofenceId);
        }
    }

    /**
     * Remove a geofence (user exited)
     * 
     * @param geofenceId Geofence ID to remove
     */
    public synchronized void removeActiveGeofence(String geofenceId) {
        if (geofenceId == null || geofenceId.isEmpty()) {
            Log.w(TAG, "Cannot remove null or empty geofenceId");
            return;
        }

        if (activeGeofenceIds.remove(geofenceId)) {
            saveActiveGeofences();
            notifyListeners();
            Log.d(TAG, "Removed active geofence: " + geofenceId);
        }
    }

    /**
     * Get all currently active geofence IDs
     * 
     * @return Set of active geofence IDs
     */
    public synchronized Set<String> getActiveGeofenceIds() {
        return new HashSet<>(activeGeofenceIds);
    }

    /**
     * Check if a geofence is currently active
     * 
     * @param geofenceId Geofence ID to check
     * @return true if geofence is active
     */
    public synchronized boolean isActive(String geofenceId) {
        return activeGeofenceIds.contains(geofenceId);
    }

    /**
     * Clear all active geofences
     */
    public synchronized void clearAll() {
        activeGeofenceIds.clear();
        saveActiveGeofences();
        notifyListeners();
        Log.d(TAG, "Cleared all active geofences");
    }

    /**
     * Add a listener to observe active geofences changes
     * 
     * @param listener Listener to add
     */
    public void addListener(ActiveGeofencesListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify new listener of current state
            listener.onActiveGeofencesChanged(getActiveGeofenceIds());
        }
    }

    /**
     * Remove a listener
     * 
     * @param listener Listener to remove
     */
    public void removeListener(ActiveGeofencesListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of changes
     */
    private void notifyListeners() {
        Set<String> geofenceIds = getActiveGeofenceIds();
        handler.post(() -> {
            for (ActiveGeofencesListener listener : new ArrayList<>(listeners)) {
                listener.onActiveGeofencesChanged(geofenceIds);
            }
        });
    }

    /**
     * Save active geofences to SharedPreferences for persistence
     */
    private void saveActiveGeofences() {
        prefs.edit()
            .putStringSet(KEY_ACTIVE_GEOFENCES, new HashSet<>(activeGeofenceIds))
            .apply();
    }

    /**
     * Load active geofences from SharedPreferences
     */
    private void loadActiveGeofences() {
        Set<String> saved = prefs.getStringSet(KEY_ACTIVE_GEOFENCES, new HashSet<>());
        if (saved != null) {
            activeGeofenceIds.addAll(saved);
            Log.d(TAG, "Loaded " + saved.size() + " active geofences from storage");
        }
    }

    /**
     * Get count of active geofences
     * 
     * @return Number of active geofences
     */
    public synchronized int getCount() {
        return activeGeofenceIds.size();
    }
}

