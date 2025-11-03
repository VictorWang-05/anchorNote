package com.example.anchornotes_team3.dto;

/**
 * Request DTO for setting a time-based reminder
 */
public class TimeReminderRequest {
    private String triggerAtUtc;  // ISO-8601 format

    public TimeReminderRequest(String triggerAtUtc) {
        this.triggerAtUtc = triggerAtUtc;
    }

    public String getTriggerAtUtc() {
        return triggerAtUtc;
    }
}

