package com.example.anchornotes_team3.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.anchornotes_team3.MainActivity;
import com.example.anchornotes_team3.R;
import com.example.anchornotes_team3.store.RelevantNotesStore;
import com.example.anchornotes_team3.store.ActiveGeofencesStore;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * BroadcastReceiver that handles geofence transition events (ENTER/EXIT)
 * Triggered by Google Play Services when user enters or exits a registered geofence
 */
public class GeofenceReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    private static final String CHANNEL_ID = "geofence_notifications";
    private static final int NOTIFICATION_ID_BASE = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence event received");

        // Parse geofencing event
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get transition type (ENTER or EXIT)
        int transitionType = geofencingEvent.getGeofenceTransition();
        
        // Get triggered geofences
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "No triggering geofences");
            return;
        }

        // Get store instances
        RelevantNotesStore notesStore = RelevantNotesStore.getInstance(context);
        ActiveGeofencesStore geofencesStore = ActiveGeofencesStore.getInstance(context);

        // Process each triggered geofence
        for (Geofence geofence : triggeringGeofences) {
            String geofenceId = geofence.getRequestId();
            String noteId = extractNoteId(geofenceId);
            
            if (noteId == null) {
                Log.w(TAG, "Could not extract noteId from geofenceId: " + geofenceId);
                continue;
            }

            Log.d(TAG, "Geofence " + geofenceId + " transition: " + transitionType);

            switch (transitionType) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    handleGeofenceEnter(context, noteId);
                    notesStore.addRelevantNote(noteId);
                    // Also track the geofence ID itself for template matching
                    geofencesStore.addActiveGeofence(geofenceId);
                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    handleGeofenceExit(context, noteId);
                    notesStore.removeRelevantNote(noteId);
                    // Remove from active geofences
                    geofencesStore.removeActiveGeofence(geofenceId);
                    break;

                default:
                    Log.w(TAG, "Unknown transition type: " + transitionType);
            }
        }
    }

    /**
     * Handle geofence ENTER event
     */
    private void handleGeofenceEnter(Context context, String noteId) {
        Log.d(TAG, "Entered geofence for note: " + noteId);
        
        // Show notification
        showNotification(context, noteId, "Relevant Note Nearby", 
            "You have a note for this location");
    }

    /**
     * Handle geofence EXIT event
     */
    private void handleGeofenceExit(Context context, String noteId) {
        Log.d(TAG, "Exited geofence for note: " + noteId);
        
        // Note is automatically removed from RelevantNotesStore
        // No notification needed for exit
    }

    /**
     * Extract noteId from geofenceId format "note_123"
     * 
     * @param geofenceId Geofence ID in format "note_{noteId}"
     * @return Note ID, or null if format is invalid
     */
    private String extractNoteId(String geofenceId) {
        if (geofenceId != null && geofenceId.startsWith("note_")) {
            return geofenceId.substring(5); // Remove "note_" prefix
        }
        return null;
    }

    /**
     * Show notification when entering a geofence
     */
    private void showNotification(Context context, String noteId, String title, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Geofence Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for location-based reminders");
            notificationManager.createNotificationChannel(channel);
        }

        // Create intent to open MainActivity when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("fromGeofence", true);
        intent.putExtra("noteId", noteId);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE + noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        // Show notification (use noteId hash as notification ID to allow multiple notifications)
        notificationManager.notify(NOTIFICATION_ID_BASE + noteId.hashCode(), builder.build());
        
        Log.d(TAG, "Notification shown for note: " + noteId);
    }
}

