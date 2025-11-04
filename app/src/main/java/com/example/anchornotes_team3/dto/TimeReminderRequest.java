package com.example.anchornotes_team3.dto;

/**
 * Request DTO for setting a time-based reminder
 */
public class TimeReminderRequest {
    private String localDateTime;  // ISO-8601 format (e.g., "2024-11-04T10:30:00")
    private String timeZone;       // Time zone (e.g., "America/Los_Angeles")

    public TimeReminderRequest(String localDateTime, String timeZone) {
        this.localDateTime = localDateTime;
        this.timeZone = timeZone;
    }

    public String getLocalDateTime() {
        return localDateTime;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
}

