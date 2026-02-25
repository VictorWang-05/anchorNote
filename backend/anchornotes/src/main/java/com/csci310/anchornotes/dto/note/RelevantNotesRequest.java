package com.csci310.anchornotes.dto.note;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelevantNotesRequest {
    @NotNull(message = "nowUtc is required")
    private Instant nowUtc;

    private List<String> insideGeofenceIds; // e.g., ["note_123", "note_456"]
}
