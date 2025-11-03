package com.example.anchornotes_team3.model;

/**
 * Model class representing a Geofence (location-based reminder)
 * Aligned with backend GeofenceResponse structure
 */
public class Geofence {
    private String id;  // Backend uses String IDs
    private Double latitude;
    private Double longitude;
    private Integer radius;  // In meters
    private String address;  // Human-readable address for display

    public Geofence() {
    }

    public Geofence(Double latitude, Double longitude, Integer radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public Geofence(Double latitude, Double longitude, Integer radius, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDisplayText() {
        if (address != null && !address.isEmpty()) {
            return address + " (" + radius + "m)";
        }
        return "Lat: " + latitude + ", Lng: " + longitude + " (" + radius + "m)";
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}

