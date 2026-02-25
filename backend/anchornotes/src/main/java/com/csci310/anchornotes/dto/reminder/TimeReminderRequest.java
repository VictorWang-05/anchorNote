package com.csci310.anchornotes.dto.reminder;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeReminderRequest {
    @NotBlank(message = "Local date time is required")
    private String localDateTime; // Format: "2025-11-03T09:00:00"

    @NotBlank(message = "Time zone is required")
    private String timeZone; // Format: "America/Los_Angeles"
}
