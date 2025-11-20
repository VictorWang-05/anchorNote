package com.example.anchornotes_team3;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.repository.NoteRepository;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Black-box Espresso tests for SearchResultsActivity
 *
 * Location: app/src/androidTest/java/com/example/anchornotes_team3/SearchResultsActivityTest.java
 *
 * These tests verify the core functionality of the Search Results screen:
 * 1. Searching with valid keyword returns results
 * 2. Searching with no matches shows empty state
 * 3. Searching with empty query shows error
 *
 * IMPORTANT: These tests create test notes in the backend as part of test setup.
 * The notes use unique identifiers to avoid conflicts.
 *
 * Test execution: Run via Android Studio or command line with:
 * ./gradlew connectedAndroidTest --tests "com.example.anchornotes_team3.SearchResultsActivityTest"
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SearchResultsActivityTest {

    private ActivityScenario<SearchResultsActivity> scenario;
    private Context context;

    /**
     * Helper method to build a search intent with a specific query
     */
    private Intent buildSearchIntent(String query) {
        context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, SearchResultsActivity.class);
        intent.putExtra(SearchResultsActivity.EXTRA_SEARCH_QUERY, query);
        return intent;
    }

    /**
     * Helper method to create a test note in the backend
     * This ensures we have predictable search results
     */
    private void createNoteForSearch(String title, String body) {
        context = ApplicationProvider.getApplicationContext();
        NoteRepository repo = NoteRepository.getInstance(context);
        CountDownLatch latch = new CountDownLatch(1);

        // Create a new Note object
        Note testNote = new Note();
        testNote.setTitle(title);
        testNote.setText(body);

        repo.createNote(testNote, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note note) {
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method for sleeping (to wait for async operations)
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test 17: Search - Test searching with valid keyword returns results
     *
     * Description: This test verifies that when a user searches for a term that
     * matches existing notes, the search results are displayed correctly in the
     * RecyclerView with the proper result count.
     *
     * Rationale: Search is a core feature for finding notes. We test the happy path
     * where a search query matches existing content. We create a test note with a
     * unique identifier to ensure predictable results. This validates that:
     * - The search query is sent to the backend
     * - Results are received and displayed
     * - The UI updates correctly (RecyclerView visible, empty state hidden)
     *
     * Test Input: Search for "EspressoSearchNote" (a note we create in setup)
     * Expected Result:
     * - Search query label displays the query
     * - Empty layout is hidden
     * - RecyclerView is visible with results
     *
     * Coverage: Tests successful search workflow (positive equivalence class test)
     *
     * Note: This test creates a note in the backend, so it requires network access
     * and a running backend server.
     */
    @Test
    public void testSearchingWithValidKeywordReturnsResults() {
        // Create a test note with unique identifier
        String uniqueTitle = "EspressoSearchNote_" + System.currentTimeMillis();
        createNoteForSearch(uniqueTitle, "This is a test note for search testing");

        // Wait for note creation to complete
        sleep(2000);

        // Launch SearchResultsActivity with our search query
        scenario = ActivityScenario.launch(buildSearchIntent(uniqueTitle));

        // Wait for search results to load
        sleep(3000);

        // Verify the search query is displayed
        onView(withId(R.id.tv_search_query))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString(uniqueTitle))));

        // Verify empty state is NOT shown (we have results)
        onView(withId(R.id.layout_empty))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Verify RecyclerView is displayed (contains results)
        onView(withId(R.id.rv_search_results))
                .check(matches(isDisplayed()));

        // The test passes if results are displayed correctly
    }

    /**
     * Test 18: Search - Test searching with no matches shows empty state
     *
     * Description: This test verifies that when a search query returns no results,
     * the empty state UI is displayed appropriately with the "No results found" message.
     *
     * Rationale: Handling empty search results is important for UX. Users need clear
     * feedback when their search doesn't match any notes. We test with a query string
     * that is extremely unlikely to match any existing notes. This validates that:
     * - The empty state layout becomes visible
     * - The RecyclerView is hidden
     * - Appropriate messaging is shown
     *
     * Test Input: Search for "no_results_e2e_!@#" (a nonsense string)
     * Expected Result:
     * - Empty layout is visible
     * - RecyclerView is hidden or shows no content
     * - "No results" message is displayed
     *
     * Coverage: Tests empty results handling (boundary test with no matches)
     */
    @Test
    public void testSearchingWithNoMatchesShowsEmptyState() {
        // Use a search query that will never match any notes
        String impossibleQuery = "no_results_e2e_!@#_" + System.currentTimeMillis();

        // Launch SearchResultsActivity with the impossible query
        scenario = ActivityScenario.launch(buildSearchIntent(impossibleQuery));

        // Wait for search to complete and return no results
        sleep(3000);

        // Verify empty state layout is displayed
        onView(withId(R.id.layout_empty))
                .check(matches(isDisplayed()));

        // Verify RecyclerView is hidden (no results to show)
        onView(withId(R.id.rv_search_results))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // The test passes if empty state is shown correctly
    }

    /**
     * Test 19: Search - Test search with empty query
     *
     * Description: This test verifies that when SearchResultsActivity is launched
     * with an empty or null search query, it displays an appropriate error message
     * and immediately finishes the activity.
     *
     * Rationale: Input validation is important to prevent invalid states. An empty
     * search query is meaningless, so the activity should gracefully handle this
     * edge case by:
     * - Showing a toast message "No search query provided"
     * - Immediately finishing the activity
     * This is a defensive programming test for invalid input.
     *
     * Test Input: Launch activity with empty string query ("")
     * Expected Result:
     * - Toast message is displayed (optional verification)
     * - Activity finishes immediately
     *
     * Coverage: Tests input validation (negative test case, boundary condition)
     *
     * Note: The activity finishes in onCreate(), so it closes almost immediately.
     * We verify the finishing state rather than UI elements since the activity
     * is short-lived.
     */
    @Test
    public void testSearchWithEmptyQuery() {
        // Launch activity with empty query
        scenario = ActivityScenario.launch(buildSearchIntent(""));

        // Wait a moment for onCreate to execute
        sleep(500);

        // Verify that the activity is finishing (due to empty query validation)
        scenario.onActivity(activity -> {
            assertTrue("Activity should finish when query is empty",
                    activity.isFinishing());
        });

        // The test passes if the activity finishes as expected
        // Note: We could also verify the toast message "No search query provided"
        // using ToastMatcher, but checking isFinishing() is more reliable
    }
}

