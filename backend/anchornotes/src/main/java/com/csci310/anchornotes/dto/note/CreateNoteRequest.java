package com.csci310.anchornotes.dto.note;

import com.csci310.anchornotes.dto.geofence.GeofenceRequest;
import com.csci310.anchornotes.dto.reminder.TimeReminderRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String text;

    private Boolean pinned;

    private List<Long> tagIds;

    private GeofenceRequest geofence;

    private TimeReminderRequest reminder;
}
