package com.example.anchornotes_team3;

import com.example.anchornotes_team3.model.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * White-box unit tests for the Tag model class
 * Tests tag creation, validation, and behavior
 */
public class TagTest {
    private Tag tag;

    @Before
    public void setUp() {
        tag = new Tag();
    }

    /**
     * Test 6: Tag - Test tag creation with valid name
     * Tests that a tag can be created with a valid name and retrieved correctly
     */
    @Test
    public void testTagCreationWithValidName() {
        String tagName = "Important";
        String tagColor = "#FF5733";

        Tag newTag = new Tag(tagName, tagColor);

        assertEquals("Tag name should match", tagName, newTag.getName());
        assertEquals("Tag color should match", tagColor, newTag.getColor());
    }

    /**
     * Test 6 (additional): Tag - Test tag creation with constructor including ID
     * Tests the full constructor with id, name, and color
     */
    @Test
    public void testTagCreationWithId() {
        String tagId = "tag-001";
        String tagName = "Work";
        String tagColor = "#3498db";

        Tag newTag = new Tag(tagId, tagName, tagColor);

        assertEquals("Tag ID should match", tagId, newTag.getId());
        assertEquals("Tag name should match", tagName, newTag.getName());
        assertEquals("Tag color should match", tagColor, newTag.getColor());
    }

    /**
     * Test 7: Tag - Test tag with empty name validation
     * Tests that a tag can handle an empty name without errors
     */
    @Test
    public void testTagWithEmptyName() {
        tag.setName("");
        assertEquals("Empty name should be stored as empty string", "", tag.getName());
        assertTrue("Tag name should be empty", tag.getName().isEmpty());

        // Verify tag can still have other properties with empty name
        tag.setColor("#FFFFFF");
        assertEquals("Color should be set even with empty name", "#FFFFFF", tag.getColor());
    }

    /**
     * Test 7 (additional): Tag - Test tag with null name
     * Tests that a tag can handle a null name
     */
    @Test
    public void testTagWithNullName() {
        tag.setName(null);
        assertNull("Null name should be stored as null", tag.getName());
    }

    /**
     * Test 7 (additional): Tag - Test tag equals method
     * Tests that two tags with the same ID are considered equal
     */
    @Test
    public void testTagEquality() {
        Tag tag1 = new Tag("tag-001", "Personal", "#FF0000");
        Tag tag2 = new Tag("tag-001", "Personal", "#FF0000");
        Tag tag3 = new Tag("tag-002", "Work", "#0000FF");

        assertEquals("Tags with same ID should be equal", tag1, tag2);
        assertNotEquals("Tags with different IDs should not be equal", tag1, tag3);
    }

    /**
     * Test 7 (additional): Tag - Test tag toString method
     * Tests that toString returns the tag name
     */
    @Test
    public void testTagToString() {
        String tagName = "Urgent";
        tag.setName(tagName);

        assertEquals("toString should return tag name", tagName, tag.toString());
    }
}
