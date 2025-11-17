package com.example.anchornotes_team3;

import com.example.anchornotes_team3.model.Template;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * White-box unit tests for the Template model class
 * Tests template initialization and default values
 */
public class TemplateTest {
    private Template template;

    @Before
    public void setUp() {
        template = new Template();
    }

    /**
     * Test 8: Template - Test template initialization with default values
     * Tests that a newly created template has appropriate default/null values
     */
    @Test
    public void testTemplateInitializationWithDefaults() {
        // Verify default values for new template
        assertNull("ID should be null by default", template.getId());
        assertNull("Name should be null by default", template.getName());
        assertNull("Text should be null by default", template.getText());
        assertNull("Tags should be null by default", template.getTags());
        assertNull("Geofence should be null by default", template.getGeofence());
        assertNull("Image should be null by default", template.getImage());
        assertNull("Audio should be null by default", template.getAudio());
        assertNull("Background color should be null by default", template.getBackgroundColor());

        // Test getPinned() which has special handling for null
        assertFalse("Pinned should default to false when null", template.getPinned());
    }

    /**
     * Test 8 (additional): Template - Test template with all fields set
     * Tests that template correctly stores all values when set through setters
     */
    @Test
    public void testTemplateWithAllFieldsSet() {
        template.setId("template-001");
        template.setName("Meeting Notes Template");
        template.setText("# Meeting Notes\n\n## Attendees\n\n## Agenda");
        template.setPinned(true);
        template.setBackgroundColor("#FFE4B5");

        assertEquals("ID should be set", "template-001", template.getId());
        assertEquals("Name should be set", "Meeting Notes Template", template.getName());
        assertNotNull("Text should be set", template.getText());
        assertTrue("Pinned should be true", template.getPinned());
        assertEquals("Background color should be set", "#FFE4B5", template.getBackgroundColor());
    }

    /**
     * Test 8 (additional): Template - Test pinned null handling
     * Tests that getPinned() returns false when pinned is null
     */
    @Test
    public void testPinnedNullHandling() {
        template.setPinned(null);
        assertFalse("getPinned should return false when null", template.getPinned());

        template.setPinned(true);
        assertTrue("getPinned should return true when set to true", template.getPinned());

        template.setPinned(false);
        assertFalse("getPinned should return false when set to false", template.getPinned());
    }
}
