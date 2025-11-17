package com.csci310.anchornotes.dto.note;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoteRequest {
    private String title;
    private String text;
    private Boolean pinned;
    private List<Long> tagIds;
    private String backgroundColor;
}
