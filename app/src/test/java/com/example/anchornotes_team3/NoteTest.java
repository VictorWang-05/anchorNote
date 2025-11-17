package com.example.anchornotes_team3;

import com.example.anchornotes_team3.model.Note;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * White-box unit tests for the Note model class
 * Tests internal logic and state management of Note objects
 */
public class NoteTest {
    private Note note;

    @Before
    public void setUp() {
        note = new Note();
    }

    /**
     * Test 1: Note - Test setting and getting title
     * Tests that the title getter returns the exact value set by the setter
     */
    @Test
    public void testSetAndGetTitle() {
        System.out.println("\n--- Test 1: testSetAndGetTitle ---");
        String testTitle = "My Test Note Title";
        note.setTitle(testTitle);
        System.out.println("✓ Set title to: " + testTitle);
        assertEquals("Title should match the set value", testTitle, note.getTitle());
        System.out.println("✓ Assert passed: Title matches '" + note.getTitle() + "'");
    }

    /**
     * Test 2: Note - Test setting and getting content
     * Tests that the text content getter returns the exact value set by the setter
     */
    @Test
    public void testSetAndGetContent() {
        System.out.println("\n--- Test 2: testSetAndGetContent ---");
        String testContent = "This is my note content with some text.";
        note.setText(testContent);
        System.out.println("✓ Set content to: " + testContent);
        assertEquals("Content should match the set value", testContent, note.getText());
        System.out.println("✓ Assert passed: Content matches '" + note.getText() + "'");
    }

    /**
     * Test 3: Note - Test note with null title handles gracefully
     * Tests that setting a null title doesn't cause exceptions and can be retrieved
     */
    @Test
    public void testNullTitleHandling() {
        System.out.println("\n--- Test 3: testNullTitleHandling ---");
        note.setTitle(null);
        System.out.println("✓ Set title to null");
        assertNull("Null title should be stored as null", note.getTitle());
        System.out.println("✓ Assert passed: Title is null");

        // Verify note can still function with null title
        note.setText("Some content");
        System.out.println("✓ Set content to: 'Some content'");
        assertEquals("Content should still work with null title", "Some content", note.getText());
        System.out.println("✓ Assert passed: Content works with null title");
    }

    /**
     * Test 4: Note - Test note with empty content
     * Tests that empty content is handled correctly and doesn't cause issues
     */
    @Test
    public void testEmptyContent() {
        System.out.println("\n--- Test 4: testEmptyContent ---");
        note.setText("");
        System.out.println("✓ Set content to empty string");
        assertEquals("Empty content should be stored as empty string", "", note.getText());
        System.out.println("✓ Assert passed: Content is empty string");
        assertTrue("Empty content should return empty string", note.getText().isEmpty());
        System.out.println("✓ Assert passed: isEmpty() returns true");

        // Verify note can have title with empty content
        note.setTitle("Title with empty content");
        System.out.println("✓ Set title to: 'Title with empty content'");
        assertEquals("Title should be set even with empty content", "Title with empty content", note.getTitle());
        System.out.println("✓ Assert passed: Title set with empty content");
    }

    /**
     * Test 5: Note - Test note ID validation
     * Tests that note ID can be set and retrieved correctly
     */
    @Test
    public void testNoteIdValidation() {
        System.out.println("\n--- Test 5: testNoteIdValidation ---");
        // Test setting ID through setter
        String testId = "note-12345";
        note.setId(testId);
        System.out.println("✓ Set ID to: " + testId);
        assertEquals("ID should match the set value", testId, note.getId());
        System.out.println("✓ Assert passed: ID matches '" + note.getId() + "'");

        // Test creating note with ID in constructor
        Note noteWithId = new Note("note-67890");
        System.out.println("✓ Created note with ID in constructor: 'note-67890'");
        assertEquals("ID should be set from constructor", "note-67890", noteWithId.getId());
        System.out.println("✓ Assert passed: Constructor ID matches");

        // Test null ID
        note.setId(null);
        System.out.println("✓ Set ID to null");
        assertNull("Null ID should be handled", note.getId());
        System.out.println("✓ Assert passed: Null ID handled correctly");
    }
}
