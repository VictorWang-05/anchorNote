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
 * White box tests for ActiveGeofencesStore
 * Tests the internal logic of geofence state management using JUnit 4
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ActiveGeofencesStoreTest {

    private Context context;
    private ActiveGeofencesStore activeGeofencesStore;

    @Before
    public void setUp() {
        // Use Robolectric's Android context
        context = RuntimeEnvironment.getApplication();

        // Get instance with real Android context
        activeGeofencesStore = ActiveGeofencesStore.getInstance(context);

        // Clear any existing data
        activeGeofencesStore.clearAll();
    }

    /**
     * Test 37: Test geofence state management
     * White box test: Verifies that geofence states are correctly managed:
     * 1. Adding active geofences (ENTER event)
     * 2. Removing active geofences (EXIT event)
     * 3. Checking if geofence is active
     * 4. Persisting state changes
     * 5. Notifying listeners of state changes
     */
    @Test
    public void testGeofenceStateManagement_AddAndRemove() {
        // Arrange: Set up listener to capture state changes
        Set<String> capturedGeofenceIds = new HashSet<>();
        ActiveGeofencesStore.ActiveGeofencesListener listener =
                geofenceIds -> {
                    capturedGeofenceIds.clear();
                    capturedGeofenceIds.addAll(geofenceIds);
                };

        activeGeofencesStore.addListener(listener);

        String geofenceId1 = "note_12345";
        String geofenceId2 = "template_67890";

        // Act & Assert: Test adding geofences (ENTER events)

        // Add first geofence
        activeGeofencesStore.addActiveGeofence(geofenceId1);

        // Process pending messages on the main thread (for listener notifications)
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Verify: Geofence is marked as active
        assertTrue("Geofence should be active after adding",
                activeGeofencesStore.isActive(geofenceId1));

        // Verify: Count is correct
        assertEquals("Count should be 1 after adding one geofence",
                1, activeGeofencesStore.getCount());

        // Verify: Geofence appears in active set
        Set<String> activeGeofences = activeGeofencesStore.getActiveGeofenceIds();
        assertTrue("Active geofences should contain added geofence",
                activeGeofences.contains(geofenceId1));

        // Verify: Listener was notified
        assertTrue("Listener should be notified with active geofence",
                capturedGeofenceIds.contains(geofenceId1));

        // Add second geofence
        activeGeofencesStore.addActiveGeofence(geofenceId2);

        // Verify: Both geofences are active
        assertTrue("First geofence should still be active",
                activeGeofencesStore.isActive(geofenceId1));
        assertTrue("Second geofence should be active",
                activeGeofencesStore.isActive(geofenceId2));
        assertEquals("Count should be 2", 2, activeGeofencesStore.getCount());

        // Act & Assert: Test removing geofences (EXIT events)

        // Remove first geofence
        activeGeofencesStore.removeActiveGeofence(geofenceId1);

        // Verify: First geofence is no longer active
        assertFalse("First geofence should not be active after removal",
                activeGeofencesStore.isActive(geofenceId1));

        // Verify: Second geofence is still active
        assertTrue("Second geofence should still be active",
                activeGeofencesStore.isActive(geofenceId2));

        // Verify: Count decreased
        assertEquals("Count should be 1 after removing one geofence",
                1, activeGeofencesStore.getCount());

        // Verify: Active set updated correctly
        Set<String> updatedGeofences = activeGeofencesStore.getActiveGeofenceIds();
        assertFalse("Removed geofence should not be in active set",
                updatedGeofences.contains(geofenceId1));
        assertTrue("Remaining geofence should be in active set",
                updatedGeofences.contains(geofenceId2));

        // Clean up
        activeGeofencesStore.removeListener(listener);
    }

    /**
     * Test 37 (Extended): Test multiple geofence state transitions
     * White box test: Verifies complex state transitions with multiple geofences
     */
    @Test
    public void testGeofenceStateManagement_MultipleTransitions() {
        // Arrange
        String noteGeofence1 = "note_001";
        String noteGeofence2 = "note_002";
        String templateGeofence1 = "template_001";
        String templateGeofence2 = "template_002";

        // Act: Add multiple geofences
        activeGeofencesStore.addActiveGeofence(noteGeofence1);
        activeGeofencesStore.addActiveGeofence(noteGeofence2);
        activeGeofencesStore.addActiveGeofence(templateGeofence1);
        activeGeofencesStore.addActiveGeofence(templateGeofence2);

        // Assert: All geofences are active
        assertEquals("Should have 4 active geofences",
                4, activeGeofencesStore.getCount());
        assertTrue("Note geofence 1 should be active",
                activeGeofencesStore.isActive(noteGeofence1));
        assertTrue("Note geofence 2 should be active",
                activeGeofencesStore.isActive(noteGeofence2));
        assertTrue("Template geofence 1 should be active",
                activeGeofencesStore.isActive(templateGeofence1));
        assertTrue("Template geofence 2 should be active",
                activeGeofencesStore.isActive(templateGeofence2));

        // Act: Remove some geofences
        activeGeofencesStore.removeActiveGeofence(noteGeofence1);
        activeGeofencesStore.removeActiveGeofence(templateGeofence1);

        // Assert: Correct geofences remain active
        assertEquals("Should have 2 active geofences after removals",
                2, activeGeofencesStore.getCount());
        assertFalse("Removed note geofence should not be active",
                activeGeofencesStore.isActive(noteGeofence1));
        assertTrue("Note geofence 2 should still be active",
                activeGeofencesStore.isActive(noteGeofence2));
        assertFalse("Removed template geofence should not be active",
                activeGeofencesStore.isActive(templateGeofence1));
        assertTrue("Template geofence 2 should still be active",
                activeGeofencesStore.isActive(templateGeofence2));

        Set<String> remainingGeofences = activeGeofencesStore.getActiveGeofenceIds();
        assertEquals("Active set should contain 2 geofences",
                2, remainingGeofences.size());
        assertTrue("Active set should contain note_002",
                remainingGeofences.contains(noteGeofence2));
        assertTrue("Active set should contain template_002",
                remainingGeofences.contains(templateGeofence2));
    }

    /**
     * Test 37 (Extended): Test listener notifications on state changes
     * White box test: Verifies that listeners are notified correctly
     * when geofence states change
     */
    @Test
    public void testGeofenceStateManagement_ListenerNotifications() {
        // Arrange: Track all notifications
        final int[] notificationCount = {0};
        final Set<String> lastNotifiedSet = new HashSet<>();

        ActiveGeofencesStore.ActiveGeofencesListener listener = geofenceIds -> {
            notificationCount[0]++;
            lastNotifiedSet.clear();
            lastNotifiedSet.addAll(geofenceIds);
        };

        activeGeofencesStore.addListener(listener);

        // Act & Assert: Each state change should trigger notification

        // Add first geofence
        activeGeofencesStore.addActiveGeofence("geofence_1");

        // Process pending messages
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue("First notification should include geofence_1",
                lastNotifiedSet.contains("geofence_1"));
        int notificationsAfterFirst = notificationCount[0];
        assertTrue("Should have at least one notification",
                notificationsAfterFirst >= 1);

        // Add second geofence
        activeGeofencesStore.addActiveGeofence("geofence_2");
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue("Second notification should include both geofences",
                lastNotifiedSet.contains("geofence_1") &&
                lastNotifiedSet.contains("geofence_2"));
        assertTrue("Notification count should increase",
                notificationCount[0] > notificationsAfterFirst);

        // Remove one geofence
        activeGeofencesStore.removeActiveGeofence("geofence_1");
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse("After removal, notification should not include removed geofence",
                lastNotifiedSet.contains("geofence_1"));
        assertTrue("After removal, notification should still include remaining geofence",
                lastNotifiedSet.contains("geofence_2"));

        // Clean up
        activeGeofencesStore.removeListener(listener);
    }

    /**
     * Test 37 (Extended): Test clearAll functionality
     * White box test: Verifies that clearAll removes all active geofences
     */
    @Test
    public void testGeofenceStateManagement_ClearAll() {
        // Arrange: Add multiple geofences
        activeGeofencesStore.addActiveGeofence("note_1");
        activeGeofencesStore.addActiveGeofence("note_2");
        activeGeofencesStore.addActiveGeofence("template_1");

        assertEquals("Should have 3 active geofences before clear",
                3, activeGeofencesStore.getCount());

        // Act: Clear all geofences
        activeGeofencesStore.clearAll();

        // Assert: All geofences should be inactive
        assertEquals("Count should be 0 after clearAll",
                0, activeGeofencesStore.getCount());

        assertFalse("note_1 should not be active",
                activeGeofencesStore.isActive("note_1"));
        assertFalse("note_2 should not be active",
                activeGeofencesStore.isActive("note_2"));
        assertFalse("template_1 should not be active",
                activeGeofencesStore.isActive("template_1"));

        Set<String> geofencesAfterClear = activeGeofencesStore.getActiveGeofenceIds();
        assertTrue("Active geofences set should be empty",
                geofencesAfterClear.isEmpty());
    }

    /**
     * Test 37 (Extended): Test duplicate add handling
     * White box test: Verifies that adding the same geofence multiple times
     * doesn't create duplicates (Set behavior)
     */
    @Test
    public void testGeofenceStateManagement_DuplicateAdd() {
        // Arrange
        String geofenceId = "note_duplicate";

        // Act: Add same geofence multiple times
        activeGeofencesStore.addActiveGeofence(geofenceId);
        activeGeofencesStore.addActiveGeofence(geofenceId);
        activeGeofencesStore.addActiveGeofence(geofenceId);

        // Assert: Should only have one instance
        assertEquals("Count should be 1 despite multiple additions",
                1, activeGeofencesStore.getCount());

        Set<String> activeGeofences = activeGeofencesStore.getActiveGeofenceIds();
        assertEquals("Active set should contain only one geofence",
                1, activeGeofences.size());
        assertTrue("Active set should contain the geofence",
                activeGeofences.contains(geofenceId));
    }

    /**
     * Test 37 (Extended): Test removing non-existent geofence
     * White box test: Verifies that removing a geofence that isn't active
     * doesn't cause errors
     */
    @Test
    public void testGeofenceStateManagement_RemoveNonExistent() {
        // Arrange: Add one geofence
        activeGeofencesStore.addActiveGeofence("note_exists");
        assertEquals("Should have 1 active geofence",
                1, activeGeofencesStore.getCount());

        // Act: Try to remove a geofence that was never added
        activeGeofencesStore.removeActiveGeofence("note_does_not_exist");

        // Assert: Existing geofence should be unaffected
        assertEquals("Count should still be 1",
                1, activeGeofencesStore.getCount());
        assertTrue("Existing geofence should still be active",
                activeGeofencesStore.isActive("note_exists"));
    }

    /**
     * Test 37 (Extended): Test empty store behavior
     * White box test: Verifies correct behavior when store is empty
     */
    @Test
    public void testGeofenceStateManagement_EmptyStore() {
        // Assert: Empty store behavior
        assertEquals("Count should be 0 for empty store",
                0, activeGeofencesStore.getCount());

        Set<String> emptyGeofences = activeGeofencesStore.getActiveGeofenceIds();
        assertNotNull("Should return non-null set", emptyGeofences);
        assertTrue("Should return empty set", emptyGeofences.isEmpty());

        assertFalse("Any geofence should return false for isActive",
                activeGeofencesStore.isActive("any_geofence"));
    }
}
