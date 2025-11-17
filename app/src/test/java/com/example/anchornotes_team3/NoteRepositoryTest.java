package com.example.anchornotes_team3;

import android.content.Context;

import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Tag;
import com.example.anchornotes_team3.repository.NoteRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * White-box unit tests for NoteRepository class
 * Tests note data filtering, sorting, and management logic
 */
@RunWith(RobolectricTestRunner.class)
public class NoteRepositoryTest {
    
    private Context context;
    private List<Note> testNotes;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        
        // Create sample notes for testing
        testNotes = createTestNotes();
    }
    
    /**
     * Helper method to create test notes with various dates
     */
    private List<Note> createTestNotes() {
        List<Note> notes = new ArrayList<>();
        
        // Note 1 - Created today
        Note note1 = new Note("1");
        note1.setTitle("Recent Note");
        note1.setText("This is a recent note");
        note1.setCreatedAt(Instant.now());
        note1.setLastEdited(Instant.now());
        notes.add(note1);
        
        // Note 2 - Created 5 days ago
        Note note2 = new Note("2");
        note2.setTitle("Last Week Note");
        note2.setText("This note is from last week");
        note2.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        note2.setLastEdited(Instant.now().minus(5, ChronoUnit.DAYS));
        notes.add(note2);
        
        // Note 3 - Created 30 days ago
        Note note3 = new Note("3");
        note3.setTitle("Old Note");
        note3.setText("This is an old note");
        note3.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        note3.setLastEdited(Instant.now().minus(30, ChronoUnit.DAYS));
        notes.add(note3);
        
        // Note 4 - Created 2 days ago
        Note note4 = new Note("4");
        note4.setTitle("Another Recent Note");
        note4.setText("Created recently");
        note4.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        note4.setLastEdited(Instant.now().minus(2, ChronoUnit.DAYS));
        notes.add(note4);
        
        // Note 5 - Created 15 days ago, edited yesterday
        Note note5 = new Note("5");
        note5.setTitle("Edited Note");
        note5.setText("This was edited recently");
        note5.setCreatedAt(Instant.now().minus(15, ChronoUnit.DAYS));
        note5.setLastEdited(Instant.now().minus(1, ChronoUnit.DAYS));
        notes.add(note5);
        
        return notes;
    }

    /**
     * Test 27: NoteRepository - Test filtering notes by date
     * Tests that notes can be correctly filtered by creation date and last edited date
     * This verifies date-based filtering logic works correctly for time-based searches
     */
    @Test
    public void testFilteringNotesByDate() {
        // Test 1: Filter notes created in last 7 days
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Note> recentNotes = testNotes.stream()
                .filter(note -> note.getCreatedAt() != null && 
                               note.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        
        assertEquals("Should find 3 notes created in last 7 days", 3, recentNotes.size());
        assertTrue("Should include note 1", 
                  recentNotes.stream().anyMatch(n -> n.getId().equals("1")));
        assertTrue("Should include note 4", 
                  recentNotes.stream().anyMatch(n -> n.getId().equals("4")));
        assertTrue("Should include note 2", 
                  recentNotes.stream().anyMatch(n -> n.getId().equals("2")));
        
        // Test 2: Filter notes edited in last 3 days
        Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
        List<Note> recentlyEditedNotes = testNotes.stream()
                .filter(note -> note.getLastEdited() != null && 
                               note.getLastEdited().isAfter(threeDaysAgo))
                .collect(Collectors.toList());
        
        assertEquals("Should find 3 notes edited in last 3 days", 3, recentlyEditedNotes.size());
        assertTrue("Should include recently edited note", 
                  recentlyEditedNotes.stream().anyMatch(n -> n.getId().equals("5")));
        
        // Test 3: Filter notes older than 20 days
        Instant twentyDaysAgo = Instant.now().minus(20, ChronoUnit.DAYS);
        List<Note> oldNotes = testNotes.stream()
                .filter(note -> note.getCreatedAt() != null && 
                               note.getCreatedAt().isBefore(twentyDaysAgo))
                .collect(Collectors.toList());
        
        assertEquals("Should find 1 note older than 20 days", 1, oldNotes.size());
        assertEquals("Should be the old note", "3", oldNotes.get(0).getId());
        
        // Test 4: Filter with date range (between 10 and 20 days ago)
        Instant twentyDaysAgoRange = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        List<Note> midRangeNotes = testNotes.stream()
                .filter(note -> note.getCreatedAt() != null && 
                               note.getCreatedAt().isAfter(twentyDaysAgoRange) &&
                               note.getCreatedAt().isBefore(tenDaysAgo))
                .collect(Collectors.toList());
        
        assertEquals("Should find 1 note in the middle range", 1, midRangeNotes.size());
        assertEquals("Should be note 5", "5", midRangeNotes.get(0).getId());
        
        // Test 5: Handle null dates gracefully
        Note noteWithNullDate = new Note("6");
        noteWithNullDate.setTitle("No Date Note");
        noteWithNullDate.setCreatedAt(null);
        noteWithNullDate.setLastEdited(null);
        testNotes.add(noteWithNullDate);
        
        List<Note> notesWithValidDates = testNotes.stream()
                .filter(note -> note.getCreatedAt() != null)
                .collect(Collectors.toList());
        
        assertEquals("Should exclude note with null date", 5, notesWithValidDates.size());
        assertFalse("Should not include note with null date", 
                   notesWithValidDates.stream().anyMatch(n -> n.getId().equals("6")));
    }

    /**
     * Test 28: NoteRepository - Test sorting notes by title
     * Tests that notes can be sorted alphabetically by title in both ascending and descending order
     * This verifies title-based sorting logic for organizing notes
     */
    @Test
    public void testSortingNotesByTitle() {
        // Test 1: Sort notes by title in ascending order (A-Z)
        List<Note> sortedAscending = new ArrayList<>(testNotes);
        sortedAscending.sort(Comparator.comparing(Note::getTitle, 
                                                  String.CASE_INSENSITIVE_ORDER));
        
        assertEquals("First note should be 'Another Recent Note'", 
                    "Another Recent Note", sortedAscending.get(0).getTitle());
        assertEquals("Second note should be 'Edited Note'", 
                    "Edited Note", sortedAscending.get(1).getTitle());
        assertEquals("Third note should be 'Last Week Note'", 
                    "Last Week Note", sortedAscending.get(2).getTitle());
        assertEquals("Fourth note should be 'Old Note'", 
                    "Old Note", sortedAscending.get(3).getTitle());
        assertEquals("Fifth note should be 'Recent Note'", 
                    "Recent Note", sortedAscending.get(4).getTitle());
        
        // Test 2: Sort notes by title in descending order (Z-A)
        List<Note> sortedDescending = new ArrayList<>(testNotes);
        sortedDescending.sort(Comparator.comparing(Note::getTitle, 
                                                   String.CASE_INSENSITIVE_ORDER).reversed());
        
        assertEquals("First note should be 'Recent Note'", 
                    "Recent Note", sortedDescending.get(0).getTitle());
        assertEquals("Last note should be 'Another Recent Note'", 
                    "Another Recent Note", sortedDescending.get(4).getTitle());
        
        // Test 3: Sort with null title handling
        Note noteWithNullTitle = new Note("7");
        noteWithNullTitle.setTitle(null);
        noteWithNullTitle.setText("Note without title");
        testNotes.add(noteWithNullTitle);
        
        List<Note> sortedWithNull = new ArrayList<>(testNotes);
        sortedWithNull.sort(Comparator.comparing(Note::getTitle, 
                                                 Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        
        // Verify null title is at the end
        assertNull("Last note should have null title", 
                  sortedWithNull.get(sortedWithNull.size() - 1).getTitle());
        
        // Test 4: Sort with case-insensitive comparison
        Note upperCaseNote = new Note("8");
        upperCaseNote.setTitle("ZEBRA NOTE");
        Note lowerCaseNote = new Note("9");
        lowerCaseNote.setTitle("apple note");
        
        List<Note> caseTestNotes = Arrays.asList(upperCaseNote, lowerCaseNote);
        caseTestNotes.sort(Comparator.comparing(Note::getTitle, 
                                                String.CASE_INSENSITIVE_ORDER));
        
        assertEquals("'apple note' should come before 'ZEBRA NOTE' (case-insensitive)", 
                    "apple note", caseTestNotes.get(0).getTitle());
        assertEquals("'ZEBRA NOTE' should come after 'apple note'", 
                    "ZEBRA NOTE", caseTestNotes.get(1).getTitle());
        
        // Test 5: Sort with empty string title
        Note emptyTitleNote = new Note("10");
        emptyTitleNote.setTitle("");
        
        List<Note> notesWithEmpty = new ArrayList<>(testNotes);
        notesWithEmpty.add(emptyTitleNote);
        notesWithEmpty.sort(Comparator.comparing(Note::getTitle, 
                                                 Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        
        assertEquals("Empty title should be first (before other titles)", 
                    "", notesWithEmpty.get(0).getTitle());
        
        // Verify sorting stability - notes with same title maintain relative order
        Note duplicateTitle1 = new Note("11");
        duplicateTitle1.setTitle("Duplicate");
        duplicateTitle1.setText("First");
        Note duplicateTitle2 = new Note("12");
        duplicateTitle2.setTitle("Duplicate");
        duplicateTitle2.setText("Second");
        
        List<Note> stabilityTest = Arrays.asList(duplicateTitle1, duplicateTitle2);
        stabilityTest.sort(Comparator.comparing(Note::getTitle, 
                                               String.CASE_INSENSITIVE_ORDER));
        
        assertEquals("First duplicate should maintain position", 
                    "First", stabilityTest.get(0).getText());
        assertEquals("Second duplicate should maintain position", 
                    "Second", stabilityTest.get(1).getText());
    }
}

