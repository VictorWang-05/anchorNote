package com.example.anchornotes_team3.dto;

/**
 * Request DTO for setting a geofence reminder
 */
public class GeofenceRequest {
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private String address;

    public GeofenceRequest(Double latitude, Double longitude, Integer radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public GeofenceRequest(Double latitude, Double longitude, Integer radius, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getRadius() {
        return radius;
    }

    public String getAddress() {
        return address;
    }
}

