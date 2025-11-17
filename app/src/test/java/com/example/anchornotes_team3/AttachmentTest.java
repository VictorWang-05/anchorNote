package com.example.anchornotes_team3;

import android.net.Uri;

import com.example.anchornotes_team3.model.Attachment;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * White-box unit tests for the Attachment model class
 * Tests attachment creation and validation logic
 */
public class AttachmentTest {
    private Attachment photoAttachment;
    private Attachment audioAttachment;

    @Before
    public void setUp() {
        // Note: Uri.parse requires Android framework, but we're testing the logic
        // In a real scenario, you might need to mock Uri or use Robolectric
        // For now, we'll test what we can with null Uri
        photoAttachment = new Attachment(Attachment.AttachmentType.PHOTO, null);
        audioAttachment = new Attachment(Attachment.AttachmentType.AUDIO, null, 120);
    }

    /**
     * Test 9: Attachment - Test attachment file path validation
     * Tests that attachment correctly handles different URI scenarios
     */
    @Test
    public void testAttachmentFilePathValidation() {
        // Test that attachment can be created without URI
        assertNull("Photo attachment URI should be null when not set", photoAttachment.getUri());
        assertEquals("Photo attachment type should be PHOTO",
                Attachment.AttachmentType.PHOTO, photoAttachment.getType());

        // Test setting media URL (backend path)
        String testMediaUrl = "https://example.com/media/photo123.jpg";
        photoAttachment.setMediaUrl(testMediaUrl);
        assertEquals("Media URL should be set correctly", testMediaUrl, photoAttachment.getMediaUrl());
    }

    /**
     * Test 9 (additional): Attachment - Test attachment ID validation
     * Tests that attachment ID can be set and retrieved
     */
    @Test
    public void testAttachmentIdValidation() {
        String attachmentId = "attach-12345";
        photoAttachment.setId(attachmentId);

        assertEquals("Attachment ID should match set value", attachmentId, photoAttachment.getId());

        // Test null ID
        photoAttachment.setId(null);
        assertNull("Null ID should be handled", photoAttachment.getId());
    }

    /**
     * Test 9 (additional): Attachment - Test audio attachment duration formatting
     * Tests that audio duration is formatted correctly
     */
    @Test
    public void testAudioDurationFormatting() {
        // Test 2 minutes (120 seconds)
        assertEquals("120 seconds should format as 2:00", "2:00", audioAttachment.getFormattedDuration());

        // Test different duration
        Attachment shortAudio = new Attachment(Attachment.AttachmentType.AUDIO, null, 65);
        assertEquals("65 seconds should format as 1:05", "1:05", shortAudio.getFormattedDuration());

        // Test 0 seconds
        Attachment zeroAudio = new Attachment(Attachment.AttachmentType.AUDIO, null, 0);
        assertEquals("0 seconds should format as 0:00", "0:00", zeroAudio.getFormattedDuration());
    }

    /**
     * Test 9 (additional): Attachment - Test photo attachment has no duration
     * Tests that photo attachments return empty string for duration
     */
    @Test
    public void testPhotoAttachmentNoDuration() {
        assertEquals("Photo attachment should have empty duration string",
                "", photoAttachment.getFormattedDuration());
    }

    /**
     * Test 9 (additional): Attachment - Test upload status tracking
     * Tests that attachment upload status is tracked correctly
     */
    @Test
    public void testUploadStatusTracking() {
        assertFalse("New attachment should not be uploaded initially", photoAttachment.isUploaded());

        photoAttachment.setUploaded(true);
        assertTrue("Attachment should be marked as uploaded", photoAttachment.isUploaded());

        // Test backend attachment (created from server response)
        Attachment backendAttachment = new Attachment(
                "attach-999",
                Attachment.AttachmentType.PHOTO,
                "https://example.com/photo.jpg",
                null
        );
        assertTrue("Backend attachment should be marked as uploaded", backendAttachment.isUploaded());
    }

    /**
     * Test 9 (additional): Attachment - Test display name
     * Tests that attachment display name can be set and retrieved
     */
    @Test
    public void testAttachmentDisplayName() {
        assertEquals("Default display name should be empty", "", photoAttachment.getDisplayName());

        String displayName = "my_photo.jpg";
        photoAttachment.setDisplayName(displayName);
        assertEquals("Display name should match set value", displayName, photoAttachment.getDisplayName());
    }
}
