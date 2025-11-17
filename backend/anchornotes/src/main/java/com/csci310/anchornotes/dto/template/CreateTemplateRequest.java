package com.csci310.anchornotes.dto.template;

import com.csci310.anchornotes.dto.geofence.GeofenceRequest;
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
public class CreateTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String name;

    private String text;

    private Boolean pinned;

    private List<Long> tagIds;

    private GeofenceRequest geofence;

    private String backgroundColor;
}
