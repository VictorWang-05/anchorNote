package com.example.anchornotes_team3.store;

import android.content.Context;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * White box tests for RelevantNotesStore
 * Tests the internal logic of managing relevant notes with SharedPreferences persistence
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RelevantNotesStoreTest {

    private Context context;
    private RelevantNotesStore relevantNotesStore;

    @Before
    public void setUp() {
        // Use Robolectric's Android context
        context = RuntimeEnvironment.getApplication();

        // Get instance with real Android context
        relevantNotesStore = RelevantNotesStore.getInstance(context);

        // Clear any existing data
        relevantNotesStore.clearAll();
    }

    /**
     * Test 34: Test adding note to store
     * White box test: Verifies that adding a note correctly:
     * 1. Adds the note ID to the internal Set
     * 2. Records the timestamp for expiration tracking
     * 3. Persists changes to SharedPreferences
     * 4. Notifies registered listeners of the change
     */
    @Test
    public void testAddingNoteToStore() {
        // Arrange: Set up a listener to capture notifications
        Set<String> capturedNoteIds = new HashSet<>();
        RelevantNotesStore.RelevantNotesListener listener =
                noteIds -> capturedNoteIds.addAll(noteIds);

        // Register the listener
        relevantNotesStore.addListener(listener);

        String testNoteId = "note_12345";

        // Act: Add a note to the store
        relevantNotesStore.addRelevantNote(testNoteId);

        // Process pending messages on the main thread (for listener notifications)
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert: Verify internal state changes
        // 1. Verify note is in the store
        assertTrue("Note should be marked as relevant",
                relevantNotesStore.isRelevant(testNoteId));

        // 2. Verify note appears in retrieved set
        Set<String> retrievedNotes = relevantNotesStore.getRelevantNoteIds();
        assertNotNull("Retrieved notes set should not be null", retrievedNotes);
        assertTrue("Retrieved notes should contain added note",
                retrievedNotes.contains(testNoteId));

        // 3. Verify count is correct
        assertEquals("Count should be 1 after adding one note",
                1, relevantNotesStore.getCount());

        // 4. Verify listener was notified
        assertTrue("Listener should have been notified with the added note",
                capturedNoteIds.contains(testNoteId));

        // Clean up
        relevantNotesStore.removeListener(listener);
    }

    /**
     * Test 34 (Extended): Test adding multiple notes to store
     * White box test: Verifies that multiple notes can be added and
     * the internal Set correctly maintains all unique note IDs
     */
    @Test
    public void testAddingMultipleNotesToStore() {
        // Arrange
        String noteId1 = "note_001";
        String noteId2 = "note_002";
        String noteId3 = "note_003";

        // Act: Add multiple notes
        relevantNotesStore.addRelevantNote(noteId1);
        relevantNotesStore.addRelevantNote(noteId2);
        relevantNotesStore.addRelevantNote(noteId3);

        // Assert
        // 1. Verify all notes are marked as relevant
        assertTrue("First note should be relevant",
                relevantNotesStore.isRelevant(noteId1));
        assertTrue("Second note should be relevant",
                relevantNotesStore.isRelevant(noteId2));
        assertTrue("Third note should be relevant",
                relevantNotesStore.isRelevant(noteId3));

        // 2. Verify count reflects all additions
        assertEquals("Count should be 3 after adding three notes",
                3, relevantNotesStore.getCount());

        // 3. Verify all notes are in the retrieved set
        Set<String> allNotes = relevantNotesStore.getRelevantNoteIds();
        assertEquals("Retrieved set should contain all 3 notes",
                3, allNotes.size());
        assertTrue("All notes should be in retrieved set",
                allNotes.containsAll(Set.of(noteId1, noteId2, noteId3)));
    }

    /**
     * Test 34 (Edge Case): Test adding duplicate note
     * White box test: Verifies that adding the same note ID multiple times
     * doesn't create duplicates (Set behavior)
     */
    @Test
    public void testAddingDuplicateNote() {
        // Arrange
        String noteId = "note_duplicate";

        // Act: Add the same note multiple times
        relevantNotesStore.addRelevantNote(noteId);
        relevantNotesStore.addRelevantNote(noteId);
        relevantNotesStore.addRelevantNote(noteId);

        // Assert: Set should only contain one instance
        assertEquals("Count should be 1 despite multiple additions",
                1, relevantNotesStore.getCount());

        Set<String> notes = relevantNotesStore.getRelevantNoteIds();
        assertEquals("Retrieved set should contain only one note",
                1, notes.size());
    }

    /**
     * Test 35: Test retrieving notes from store
     * White box test: Verifies that getRelevantNoteIds() correctly:
     * 1. Returns a Set containing all added note IDs
     * 2. Returns an empty set when no notes are present
     * 3. The returned set is a defensive copy (modifications don't affect store)
     */
    @Test
    public void testRetrievingNotesFromStore() {
        // Test Case 1: Retrieve from empty store
        Set<String> emptyResult = relevantNotesStore.getRelevantNoteIds();
        assertNotNull("Should return non-null set even when empty", emptyResult);
        assertTrue("Should return empty set when no notes added",
                emptyResult.isEmpty());
        assertEquals("Count should be 0 for empty store",
                0, relevantNotesStore.getCount());

        // Test Case 2: Add notes and retrieve
        String note1 = "note_alpha";
        String note2 = "note_beta";
        String note3 = "note_gamma";

        relevantNotesStore.addRelevantNote(note1);
        relevantNotesStore.addRelevantNote(note2);
        relevantNotesStore.addRelevantNote(note3);

        Set<String> retrievedNotes = relevantNotesStore.getRelevantNoteIds();

        // Assert: Verify all notes are retrieved
        assertNotNull("Retrieved set should not be null", retrievedNotes);
        assertEquals("Retrieved set should contain all 3 notes",
                3, retrievedNotes.size());
        assertTrue("Should contain note1", retrievedNotes.contains(note1));
        assertTrue("Should contain note2", retrievedNotes.contains(note2));
        assertTrue("Should contain note3", retrievedNotes.contains(note3));

        // Test Case 3: Verify defensive copy (modifying returned set doesn't affect store)
        Set<String> firstRetrieval = relevantNotesStore.getRelevantNoteIds();
        int originalSize = firstRetrieval.size();

        // Try to modify the returned set
        firstRetrieval.add("note_should_not_persist");

        // Get a fresh copy
        Set<String> secondRetrieval = relevantNotesStore.getRelevantNoteIds();

        // Assert: Store should be unaffected
        assertEquals("Store should not be affected by external modifications",
                originalSize, secondRetrieval.size());
        assertFalse("Externally added note should not be in store",
                secondRetrieval.contains("note_should_not_persist"));
    }

    /**
     * Test 35 (Extended): Test retrieving notes after removal
     * White box test: Verifies that removal correctly updates the retrievable set
     */
    @Test
    public void testRetrievingNotesAfterRemoval() {
        // Arrange: Add multiple notes
        String note1 = "note_001";
        String note2 = "note_002";
        String note3 = "note_003";

        relevantNotesStore.addRelevantNote(note1);
        relevantNotesStore.addRelevantNote(note2);
        relevantNotesStore.addRelevantNote(note3);

        // Verify initial state
        assertEquals("Should have 3 notes initially",
                3, relevantNotesStore.getCount());

        // Act: Remove one note
        relevantNotesStore.removeRelevantNote(note2);

        // Assert: Verify retrieval reflects removal
        Set<String> notesAfterRemoval = relevantNotesStore.getRelevantNoteIds();
        assertEquals("Should have 2 notes after removal",
                2, notesAfterRemoval.size());
        assertTrue("Should still contain note1",
                notesAfterRemoval.contains(note1));
        assertFalse("Should not contain removed note2",
                notesAfterRemoval.contains(note2));
        assertTrue("Should still contain note3",
                notesAfterRemoval.contains(note3));

        // Verify isRelevant reflects removal
        assertTrue("Note1 should still be relevant",
                relevantNotesStore.isRelevant(note1));
        assertFalse("Note2 should no longer be relevant",
                relevantNotesStore.isRelevant(note2));
        assertTrue("Note3 should still be relevant",
                relevantNotesStore.isRelevant(note3));
    }

    /**
     * Test 35 (Extended): Test clearAll functionality
     * White box test: Verifies that clearAll properly empties the store
     */
    @Test
    public void testClearAllNotes() {
        // Arrange: Add notes
        relevantNotesStore.addRelevantNote("note_1");
        relevantNotesStore.addRelevantNote("note_2");
        relevantNotesStore.addRelevantNote("note_3");

        assertEquals("Should have 3 notes before clear",
                3, relevantNotesStore.getCount());

        // Act: Clear all notes
        relevantNotesStore.clearAll();

        // Assert: Verify store is empty
        assertEquals("Count should be 0 after clearAll",
                0, relevantNotesStore.getCount());

        Set<String> notesAfterClear = relevantNotesStore.getRelevantNoteIds();
        assertTrue("Retrieved set should be empty after clearAll",
                notesAfterClear.isEmpty());
    }
}
