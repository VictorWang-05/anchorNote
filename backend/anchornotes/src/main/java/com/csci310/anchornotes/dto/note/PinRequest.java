package com.csci310.anchornotes.dto.note;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinRequest {
    @NotNull(message = "Pinned status is required")
    private Boolean pinned;
}
