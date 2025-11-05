package com.example.anchornotes_team3.dto;

/**
 * Request DTO for setting a geofence reminder
 */
public class GeofenceRequest {
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private String addressName;

    public GeofenceRequest(Double latitude, Double longitude, Integer radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public GeofenceRequest(Double latitude, Double longitude, Integer radius, String addressName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.addressName = addressName;
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

    public String getAddressName() {
        return addressName;
    }
}

