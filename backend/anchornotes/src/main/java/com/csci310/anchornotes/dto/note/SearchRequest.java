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
public class SearchRequest {
    private String q; // keyword search for title and content only

    @Builder.Default
    private Integer limit = 50;

    @Builder.Default
    private Integer offset = 0;

    // Deprecated fields - kept for backward compatibility
    // Use the new /filter endpoint instead
    @Deprecated
    private List<Long> tagIds;
    @Deprecated
    private Instant editedStart;
    @Deprecated
    private Instant editedEnd;
    @Deprecated
    private Boolean hasPhoto;
    @Deprecated
    private Boolean hasAudio;
    @Deprecated
    private Boolean hasLocation;
    @Deprecated
    private String sort;
}
