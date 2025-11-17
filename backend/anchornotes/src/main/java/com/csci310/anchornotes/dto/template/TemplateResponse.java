package com.csci310.anchornotes.dto.template;

import com.csci310.anchornotes.dto.tag.TagResponse;
import com.csci310.anchornotes.dto.geofence.GeofenceResponse;
import com.csci310.anchornotes.dto.attachment.AttachmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private String id;
    private String name;
    private String text;
    private Boolean pinned;
    private List<TagResponse> tags;
    private GeofenceResponse geofence;
    private AttachmentResponse image;
    private AttachmentResponse audio;
    private String backgroundColor;
}
