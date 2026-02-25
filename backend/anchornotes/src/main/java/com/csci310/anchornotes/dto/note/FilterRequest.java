package com.csci310.anchornotes.dto.note;

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
public class FilterRequest {
    private List<Long> tagIds;
    private Instant editedStart;
    private Instant editedEnd;
    private Boolean hasPhoto;
    private Boolean hasAudio;
    private Boolean hasLocation;

    @Builder.Default
    private Integer limit = 50;

    @Builder.Default
    private Integer offset = 0;
}
