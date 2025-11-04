package com.example.anchornotes_team3.dto;

/**
 * Response DTO for GET /api/geofences endpoint
 * Used for syncing all geofences from backend to register with device
 */
public class GeofenceRegistrationResponse {
    private String geofenceId;      // Format: "note_123"
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;

    public GeofenceRegistrationResponse() {
    }

    public GeofenceRegistrationResponse(String geofenceId, Double latitude, Double longitude, Integer radiusMeters) {
        this.geofenceId = geofenceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
    }

    // Getters and setters
    public String getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(String geofenceId) {
        this.geofenceId = geofenceId;
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

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Integer radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    /**
     * Extract noteId from geofenceId format "note_123"
     */
    public String getNoteId() {
        if (geofenceId != null && geofenceId.startsWith("note_")) {
            return geofenceId.substring(5); // Remove "note_" prefix
        }
        return geofenceId;
    }
}

