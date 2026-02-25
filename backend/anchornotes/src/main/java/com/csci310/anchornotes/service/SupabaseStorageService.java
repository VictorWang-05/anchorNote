package com.csci310.anchornotes.service;

import com.csci310.anchornotes.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final SupabaseConfig supabaseConfig;

    /**
     * Generate a pre-signed upload URL for file upload
     * @param bucket - "Photo" or "Voice"
     * @param fileName - original file name
     * @param mimeType - file MIME type
     * @return upload URL and file path
     */
    public UploadUrlResponse generateUploadUrl(String bucket, String fileName, String mimeType) {
        // Generate unique file path
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

        // Construct upload URL using Supabase Storage API
        // The client will upload directly to this URL
        String uploadUrl = String.format(
            "%s/storage/v1/object/%s/%s",
            supabaseConfig.getUrl(),
            bucket,
            uniqueFileName
        );

        log.info("Generated upload URL for file: {} in bucket: {}", uniqueFileName, bucket);

        return new UploadUrlResponse(uploadUrl, uniqueFileName);
    }

    /**
     * Get public URL for an uploaded file
     * @param bucket - "Photo" or "Voice"
     * @param filePath - file path returned from generateUploadUrl
     * @return public URL
     */
    public String getPublicUrl(String bucket, String filePath) {
        String publicUrl = String.format(
            "%s/storage/v1/object/public/%s/%s",
            supabaseConfig.getUrl(),
            bucket,
            filePath
        );

        log.info("Generated public URL for file: {} in bucket: {}", filePath, bucket);

        return publicUrl;
    }

    /**
     * Upload file directly to Supabase Storage from backend
     * @param bucket - bucket name (e.g., "attachment")
     * @param file - multipart file from request
     * @return FileUploadResponse with file path and public URL
     */
    public FileUploadResponse uploadFile(String bucket, MultipartFile file) throws IOException {
        // Generate unique file path
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // Construct upload URL
        String uploadUrl = String.format(
            "%s/storage/v1/object/%s/%s",
            supabaseConfig.getUrl(),
            bucket,
            uniqueFileName
        );

        log.info("Uploading file: {} to bucket: {} (size: {} bytes)", uniqueFileName, bucket, file.getSize());

        try {
            // Create HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build request with file content
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getServiceRole())
                .header("Content-Type", file.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("File uploaded successfully: {}", uniqueFileName);
                String publicUrl = getPublicUrl(bucket, uniqueFileName);
                return new FileUploadResponse(uniqueFileName, publicUrl);
            } else {
                log.error("Failed to upload file. Status: {}, Response: {}", response.statusCode(), response.body());
                throw new IOException("Failed to upload file to Supabase Storage: " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Upload interrupted", e);
            throw new IOException("Upload interrupted", e);
        }
    }

    /**
     * Delete a file from storage
     * @param bucket - "Photo" or "Voice"
     * @param filePath - file path
     */
    public void deleteFile(String bucket, String filePath) {
        // In a real implementation, you would call the Supabase Storage API
        // to delete the file using the service role key
        log.info("Deleting file: {} from bucket: {}", filePath, bucket);
        // TODO: Implement actual deletion via Supabase Storage API
    }

    // Inner class for upload URL response
    public static class UploadUrlResponse {
        private final String uploadUrl;
        private final String filePath;

        public UploadUrlResponse(String uploadUrl, String filePath) {
            this.uploadUrl = uploadUrl;
            this.filePath = filePath;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    // Inner class for file upload response
    public static class FileUploadResponse {
        private final String filePath;
        private final String publicUrl;

        public FileUploadResponse(String filePath, String publicUrl) {
            this.filePath = filePath;
            this.publicUrl = publicUrl;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getPublicUrl() {
            return publicUrl;
        }
    }
}
