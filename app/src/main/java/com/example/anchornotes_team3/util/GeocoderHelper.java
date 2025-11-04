package com.example.anchornotes_team3.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for geocoding addresses to lat/lng coordinates
 * Uses Android's built-in Geocoder API
 */
public class GeocoderHelper {
    private static final String TAG = "GeocoderHelper";
    private static final int MAX_RESULTS = 1;
    
    private final Context context;
    private final Geocoder geocoder;
    private final ExecutorService executor;
    private final Handler mainHandler;

    /**
     * Callback interface for geocoding results
     */
    public interface GeocodeCallback {
        void onSuccess(double latitude, double longitude, String formattedAddress);
        void onError(String error);
    }

    /**
     * Simple data class for location results
     */
    public static class Location {
        public final double latitude;
        public final double longitude;
        public final String formattedAddress;

        public Location(double latitude, double longitude, String formattedAddress) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.formattedAddress = formattedAddress;
        }
    }

    public GeocoderHelper(Context context) {
        this.context = context.getApplicationContext();
        this.geocoder = new Geocoder(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Check if geocoding is available on this device
     * 
     * @return true if Geocoder is present
     */
    public boolean isGeocoderAvailable() {
        return Geocoder.isPresent();
    }

    /**
     * Geocode an address string to lat/lng coordinates
     * This operation is asynchronous and runs on a background thread
     * 
     * @param address Address string to geocode (e.g., "1600 Amphitheatre Parkway, Mountain View, CA")
     * @param callback Callback to receive results
     */
    public void geocodeAddress(String address, GeocodeCallback callback) {
        if (address == null || address.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("Address cannot be empty"));
            return;
        }

        if (!isGeocoderAvailable()) {
            Log.e(TAG, "Geocoder.isPresent() returned false");
            mainHandler.post(() -> callback.onError("Geocoder not available on this device. Please check Google Play Services."));
            return;
        }

        Log.d(TAG, "Starting geocoding for address: '" + address + "'");

        // Run geocoding on background thread
        executor.execute(() -> {
            try {
                Log.d(TAG, "Calling geocoder.getFromLocationName() for: " + address);
                List<Address> addresses = geocoder.getFromLocationName(address, MAX_RESULTS);
                
                Log.d(TAG, "Geocoder returned " + (addresses != null ? addresses.size() : "null") + " results");
                
                if (addresses == null || addresses.isEmpty()) {
                    Log.w(TAG, "No results found for address: " + address);
                    mainHandler.post(() -> 
                        callback.onError("Could not find location. Try adding city/state (e.g., 'Los Angeles, CA') or check your internet connection."));
                    return;
                }

                // Get first result
                Address result = addresses.get(0);
                double latitude = result.getLatitude();
                double longitude = result.getLongitude();
                String formattedAddress = formatAddress(result);
                
                Log.d(TAG, "✅ Geocoded '" + address + "' to: " + latitude + ", " + longitude);
                Log.d(TAG, "Formatted address: " + formattedAddress);
                
                // Return result on main thread
                mainHandler.post(() -> 
                    callback.onSuccess(latitude, longitude, formattedAddress));
                    
            } catch (IOException e) {
                Log.e(TAG, "❌ IOException during geocoding for: " + address, e);
                String errorMsg = "Geocoding failed: " + e.getMessage() + 
                    ". Check internet connection and Google Play Services.";
                mainHandler.post(() -> callback.onError(errorMsg));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "❌ IllegalArgumentException for address: " + address, e);
                mainHandler.post(() -> 
                    callback.onError("Invalid address format. Please try a more specific address."));
            } catch (Exception e) {
                Log.e(TAG, "❌ Unexpected error during geocoding", e);
                mainHandler.post(() -> 
                    callback.onError("Unexpected error: " + e.getMessage()));
            }
        });
    }

    /**
     * Reverse geocode lat/lng coordinates to an address
     * This operation is asynchronous and runs on a background thread
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @param callback Callback to receive results
     */
    public void reverseGeocode(double latitude, double longitude, GeocodeCallback callback) {
        if (!isGeocoderAvailable()) {
            mainHandler.post(() -> callback.onError("Geocoder not available on this device"));
            return;
        }

        // Run reverse geocoding on background thread
        executor.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, MAX_RESULTS);
                
                if (addresses == null || addresses.isEmpty()) {
                    mainHandler.post(() -> 
                        callback.onError("Could not find address for this location"));
                    return;
                }

                // Get first result
                Address result = addresses.get(0);
                String formattedAddress = formatAddress(result);
                
                Log.d(TAG, "Reverse geocoded " + latitude + ", " + longitude + " to: " + formattedAddress);
                
                // Return result on main thread
                mainHandler.post(() -> 
                    callback.onSuccess(latitude, longitude, formattedAddress));
                    
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                mainHandler.post(() -> 
                    callback.onError("Reverse geocoding failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Format an Address object into a human-readable string
     * 
     * @param address Address object from Geocoder
     * @return Formatted address string
     */
    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        
        // Add address lines
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(address.getAddressLine(i));
        }
        
        // If no address lines, build from components
        if (sb.length() == 0) {
            if (address.getFeatureName() != null) {
                sb.append(address.getFeatureName());
            }
            if (address.getLocality() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getLocality());
            }
            if (address.getAdminArea() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getAdminArea());
            }
            if (address.getCountryName() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getCountryName());
            }
        }
        
        return sb.toString();
    }

    /**
     * Clean up resources
     * Call this when done with the helper
     */
    public void shutdown() {
        executor.shutdown();
    }
}

