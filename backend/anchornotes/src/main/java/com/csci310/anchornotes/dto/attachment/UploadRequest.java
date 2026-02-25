package com.csci310.anchornotes.dto.attachment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "MIME type is required")
    private String mime;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long size;
}
