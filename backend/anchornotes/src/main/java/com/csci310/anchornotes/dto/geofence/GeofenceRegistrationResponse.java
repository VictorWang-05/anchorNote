package com.csci310.anchornotes.dto.geofence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceRegistrationResponse {
    private String geofenceId; // e.g., "note_123"
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;
}
