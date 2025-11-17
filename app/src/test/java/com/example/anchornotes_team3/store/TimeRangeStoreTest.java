package com.example.anchornotes_team3.store;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * White box tests for TimeRangeStore
 * Tests the internal logic of time range validation and storage using JUnit 4
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TimeRangeStoreTest {

    private Context context;
    private TimeRangeStore timeRangeStore;

    private static final int DEFAULT_MINUTES = 60;

    @Before
    public void setUp() {
        // Use Robolectric's Android context
        context = RuntimeEnvironment.getApplication();

        // Get instance with real Android context
        timeRangeStore = TimeRangeStore.getInstance(context);
    }

    /**
     * Test 36: Test time range validation
     * White box test: Verifies that setRangeMinutes enforces minimum value constraint
     * and that the store correctly validates and stores time range values
     */
    @Test
    public void testTimeRangeValidation_MinimumValue() {
        // Arrange
        String noteId = "note_12345";

        // Test Case 1: Attempt to set value below minimum (should be clamped to 1)
        timeRangeStore.setRangeMinutes(noteId, 0);

        // Assert: Verify that value is validated to minimum 1 minute
        assertEquals("Value below minimum should be clamped to 1",
                1, timeRangeStore.getRangeMinutes(noteId));

        // Test Case 2: Attempt to set negative value (should be clamped to 1)
        timeRangeStore.setRangeMinutes(noteId, -10);

        // Assert: Verify that negative values are validated to 1
        assertEquals("Negative value should be clamped to 1",
                1, timeRangeStore.getRangeMinutes(noteId));

        // Test Case 3: Set exactly minimum valid value (1)
        timeRangeStore.setRangeMinutes(noteId, 1);

        // Assert: Verify that 1 is accepted as valid
        assertEquals("Minimum valid value (1) should be stored correctly",
                1, timeRangeStore.getRangeMinutes(noteId));
    }

    /**
     * Test 36 (Extended): Test valid time range values
     * White box test: Verifies that valid values (>= 1) are stored correctly
     */
    @Test
    public void testTimeRangeValidation_ValidValues() {
        // Arrange
        String noteId = "note_67890";

        // Test Case 1: Normal value
        timeRangeStore.setRangeMinutes(noteId, 30);
        assertEquals("Normal value should be stored correctly",
                30, timeRangeStore.getRangeMinutes(noteId));

        // Test Case 2: Large value
        timeRangeStore.setRangeMinutes(noteId, 1440); // 24 hours
        assertEquals("Large value should be stored correctly",
                1440, timeRangeStore.getRangeMinutes(noteId));

        // Test Case 3: Default value (60 minutes)
        timeRangeStore.setRangeMinutes(noteId, 60);
        assertEquals("Default value should be stored correctly",
                60, timeRangeStore.getRangeMinutes(noteId));
    }

    /**
     * Test 36 (Extended): Test retrieving time ranges with defaults
     * White box test: Verifies that getRangeMinutes returns default when no value set
     */
    @Test
    public void testTimeRangeValidation_DefaultRetrieval() {
        // Arrange
        String noteId = "note_new";

        // Act: Retrieve without setting a value first
        int retrievedRange = timeRangeStore.getRangeMinutes(noteId);

        // Assert: Should return default value (60) when no value is set
        assertEquals("Should return default value (60) when no value is set",
                DEFAULT_MINUTES, retrievedRange);
    }

    /**
     * Test 36 (Extended): Test boundary validation
     * White box test: Verifies behavior at boundary conditions
     */
    @Test
    public void testTimeRangeValidation_BoundaryConditions() {
        String noteId = "note_boundary";

        // Test exactly at minimum (1 minute)
        timeRangeStore.setRangeMinutes(noteId, 1);
        assertEquals("Exactly minimum value (1) should be stored",
                1, timeRangeStore.getRangeMinutes(noteId));

        // Test just below minimum (0 - should be clamped to 1)
        timeRangeStore.setRangeMinutes(noteId, 0);
        assertEquals("Below minimum (0) should be clamped to 1",
                1, timeRangeStore.getRangeMinutes(noteId));

        // Test negative (should be clamped to 1)
        timeRangeStore.setRangeMinutes(noteId, -100);
        assertEquals("Negative value should be clamped to 1",
                1, timeRangeStore.getRangeMinutes(noteId));

        // Test just above minimum (2 minutes)
        timeRangeStore.setRangeMinutes(noteId, 2);
        assertEquals("Just above minimum (2) should be stored correctly",
                2, timeRangeStore.getRangeMinutes(noteId));
    }
}
