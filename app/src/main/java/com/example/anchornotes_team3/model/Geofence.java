package com.example.anchornotes_team3.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a Geofence (location-based reminder)
 * Aligned with backend GeofenceResponse structure
 */
public class Geofence {
    private String id;  // Backend uses String IDs
    private Double latitude;
    private Double longitude;
    private Integer radius;  // In meters
    
    @SerializedName("addressName")
    private String addressName;  // Human-readable address for display

    public Geofence() {
    }

    public Geofence(Double latitude, Double longitude, Integer radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public Geofence(Double latitude, Double longitude, Integer radius, String addressName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.addressName = addressName;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    /**
     * Get a shortened display name suitable for UI chips
     * Shows just the place name or first part of address
     */
    public String getShortDisplayName() {
        if (addressName == null || addressName.isEmpty()) {
            return "Location";
        }
        
        // If address contains a comma, take just the first part (usually the place name or street)
        int firstComma = addressName.indexOf(',');
        if (firstComma > 0) {
            String firstPart = addressName.substring(0, firstComma).trim();
            // If first part is reasonable length (< 40 chars), use it
            if (firstPart.length() <= 40) {
                return firstPart;
            }
        }
        
        // If no comma or first part too long, truncate at 40 characters
        if (addressName.length() > 40) {
            return addressName.substring(0, 37) + "...";
        }
        
        return addressName;
    }

    /**
     * Get full display text with radius (for detailed views)
     */
    public String getDisplayText() {
        if (addressName != null && !addressName.isEmpty()) {
            return addressName + " (" + radius + "m)";
        }
        return "Lat: " + latitude + ", Lng: " + longitude + " (" + radius + "m)";
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}

