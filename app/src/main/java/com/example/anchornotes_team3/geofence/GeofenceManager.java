package com.example.anchornotes_team3.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager class for handling geofence registration with Google Play Services
 * Wraps GeofencingClient to provide simplified geofence operations
 */
public class GeofenceManager {
    private static final String TAG = "GeofenceManager";
    private static final String GEOFENCE_ACTION = "com.example.anchornotes_team3.GEOFENCE_EVENT";
    
    private final Context context;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    /**
     * Callback interface for geofence operation results
     */
    public interface GeofenceCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    /**
     * Add a geofence for a note
     * 
     * @param noteId Note ID (will be converted to "note_{noteId}" format)
     * @param latitude Latitude of geofence center
     * @param longitude Longitude of geofence center
     * @param radiusMeters Radius in meters
     * @param callback Success/error callback
     */
    public void addGeofence(String noteId, double latitude, double longitude, float radiusMeters, GeofenceCallback callback) {
        // Check location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, cannot add geofence");
            if (callback != null) {
                callback.onError("Location permission not granted");
            }
            return;
        }

        // Build geofence ID in format "note_{noteId}"
        String geofenceId = "note_" + noteId;
        
        // Create geofence object
        Geofence geofence = new Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
            .build();

        // Create geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build();

        // Register geofence with Play Services
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Geofence added successfully: " + geofenceId);
                if (callback != null) {
                    callback.onSuccess("Geofence registered for note " + noteId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to add geofence: " + geofenceId, e);
                
                String errorMessage = getGeofenceErrorMessage(e);
                
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            });
    }

    /**
     * Remove a geofence for a note
     * 
     * @param noteId Note ID
     * @param callback Success/error callback
     */
    public void removeGeofence(String noteId, GeofenceCallback callback) {
        String geofenceId = "note_" + noteId;
        
        geofencingClient.removeGeofences(Collections.singletonList(geofenceId))
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Geofence removed successfully: " + geofenceId);
                if (callback != null) {
                    callback.onSuccess("Geofence removed for note " + noteId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to remove geofence: " + geofenceId, e);
                if (callback != null) {
                    callback.onError("Failed to remove geofence: " + e.getMessage());
                }
            });
    }

    /**
     * Remove all geofences
     * 
     * @param callback Success/error callback
     */
    public void removeAllGeofences(GeofenceCallback callback) {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "All geofences removed successfully");
                if (callback != null) {
                    callback.onSuccess("All geofences removed");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to remove all geofences", e);
                if (callback != null) {
                    callback.onError("Failed to remove geofences: " + e.getMessage());
                }
            });
    }

    /**
     * Add multiple geofences at once (used for syncing from backend)
     * 
     * @param geofences List of geofences to add
     * @param callback Success/error callback
     */
    public void addGeofences(List<GeofenceData> geofences, GeofenceCallback callback) {
        if (geofences == null || geofences.isEmpty()) {
            if (callback != null) {
                callback.onSuccess("No geofences to add");
            }
            return;
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, cannot add geofences");
            if (callback != null) {
                callback.onError("Location permission not granted");
            }
            return;
        }

        // Build list of Geofence objects
        List<Geofence> geofenceList = new ArrayList<>();
        for (GeofenceData data : geofences) {
            Geofence geofence = new Geofence.Builder()
                .setRequestId(data.geofenceId)
                .setCircularRegion(data.latitude, data.longitude, data.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
            geofenceList.add(geofence);
        }

        // Create geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build();

        // Register geofences with Play Services
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Added " + geofenceList.size() + " geofences successfully");
                if (callback != null) {
                    callback.onSuccess("Registered " + geofenceList.size() + " geofences");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to add geofences", e);
                if (callback != null) {
                    callback.onError("Failed to register geofences: " + e.getMessage());
                }
            });
    }

    /**
     * Get or create the PendingIntent for geofence transitions
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.setAction(GEOFENCE_ACTION);
        
        geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        
        return geofencePendingIntent;
    }

    /**
     * Get user-friendly error message from geofence exception
     */
    private String getGeofenceErrorMessage(Exception e) {
        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            int statusCode = apiException.getStatusCode();
            
            switch (statusCode) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    // Error 1004: Location services disabled or not available
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    boolean isLocationEnabled = false;
                    if (locationManager != null) {
                        isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                           locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    }
                    if (!isLocationEnabled) {
                        return "Location services are disabled. Please enable GPS or Network location in Settings.";
                    }
                    return "Geofencing not available on this device. Please enable location services.";
                    
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    return "Too many geofences. Please remove some existing geofences.";
                    
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    return "Too many pending geofence requests. Please try again later.";
                    
                default:
                    return "Geofence error (code " + statusCode + "): " + apiException.getMessage();
            }
        }
        
        return "Failed to register geofence: " + e.getMessage();
    }
    
    /**
     * Check if location services are enabled
     */
    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                   locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking location services", e);
            return false;
        }
    }

    /**
     * Data class for geofence information
     */
    public static class GeofenceData {
        public final String geofenceId;
        public final double latitude;
        public final double longitude;
        public final float radiusMeters;

        public GeofenceData(String geofenceId, double latitude, double longitude, float radiusMeters) {
            this.geofenceId = geofenceId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radiusMeters = radiusMeters;
        }
    }
}

