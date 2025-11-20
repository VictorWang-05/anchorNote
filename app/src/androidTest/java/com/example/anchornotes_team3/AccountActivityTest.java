package com.example.anchornotes_team3;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.anchornotes_team3.auth.AuthManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box Espresso tests for AccountActivity
 *
 * Location: app/src/androidTest/java/com/example/anchornotes_team3/AccountActivityTest.java
 *
 * These tests verify the core functionality of the Account management feature:
 * 1. Changing password with valid input
 * 2. Changing password with mismatched confirmation
 * 3. Logout functionality
 * 4. Displaying user account information
 *
 * IMPORTANT: These tests require a valid test account to exist in the backend.
 * Test credentials used:
 * - Email: test@example.com
 * - Password: testpass123
 *
 * Before running these tests, ensure:
 * 1. The backend server is running
 * 2. A test account exists with the above credentials
 * 3. The device/emulator has network connectivity
 *
 * Test execution: Run via Android Studio or command line with:
 * ./gradlew connectedAndroidTest --tests "com.example.anchornotes_team3.AccountActivityTest"
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountActivityTest {

    // Test account credentials - CHANGE THESE to match your test account
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "testpass123";

    private ActivityScenario<?> scenario;
    private Context context;

    @BeforeClass
    public static void setUpClass() {
        // Disable animations for Espresso tests
        // This is critical - Espresso can fail if animations are enabled
        AnimationUtil.disableAnimations();

        // This runs once before all tests
        Context context = ApplicationProvider.getApplicationContext();
        AuthManager authManager = AuthManager.getInstance(context);

        // Clear any existing auth state
        authManager.clearAuth();
    }

    @AfterClass
    public static void tearDownClass() {
        // Re-enable animations after all tests complete
        AnimationUtil.enableAnimations();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AuthManager authManager = AuthManager.getInstance(context);

        // Clear auth state before each test to ensure clean state
        authManager.clearAuth();

        // Wait a moment for the clear to take effect
        sleep(500);

        // Launch LoginOnlyActivity and perform login
        performLogin();

        // Wait for login to complete and verify login succeeded
        // Login is asynchronous, so we need to wait and verify
        int maxWaitTime = 10000; // 10 seconds max
        int waited = 0;
        while (!authManager.isLoggedIn() && waited < maxWaitTime) {
            sleep(500);
            waited += 500;
        }

        // Verify login actually succeeded
        if (!authManager.isLoggedIn()) {
            throw new AssertionError("Login failed - user is not logged in after login attempt");
        }

        // Now launch AccountActivity
        scenario = ActivityScenario.launch(AccountActivity.class);

        // Give time for the activity to load user info
        sleep(1500);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Helper method to perform login before each test
     */
    private void performLogin() {
        // Launch LoginOnlyActivity
        ActivityScenario<LoginOnlyActivity> loginScenario = ActivityScenario.launch(LoginOnlyActivity.class);

        // Wait for activity to load
        sleep(1000);

        try {
            // Enter email
            onView(withId(R.id.et_email))
                    .perform(replaceText(TEST_EMAIL), closeSoftKeyboard());

            sleep(300);

            // Enter password
            onView(withId(R.id.et_password))
                    .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());

            sleep(300);

            // Click login button
            onView(withId(R.id.btn_login))
                    .perform(click());

            // Wait for login API call to start
            // The login is asynchronous, so we wait for the API response
            // Don't close the scenario immediately - let the login complete
            sleep(5000); // Increased wait time for API call

        } finally {
            // Close the login scenario after login attempt
            // Note: The login activity may have already navigated away
            loginScenario.close();
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

    /**
     * Test 27: Account - Test changing password with valid input
     *
     * Description: This test verifies that users can change their password by entering
     * a current password, new password, and matching confirmation. The dialog should
     * validate the input and call the backend API to change the password.
     *
     * Rationale: Password change is a critical security feature. We test with valid
     * inputs to ensure the happy path works: current password "testpass123", new
     * password "newpass456" (meeting the 6-character minimum), and matching confirmation.
     * This is a positive test case for the password change workflow.
     *
     * Test Input:
     * - Current password: "testpass123" (the actual test account password)
     * - New password: "newpass456"
     * - Confirm password: "newpass456"
     * Expected Result: Password change dialog should accept the input and attempt
     * to change the password via the backend API
     *
     * Coverage: Tests password change with valid input (positive equivalence class test).
     * The password "newpass456" is in the valid equivalence class (>= 6 characters).
     *
     * NOTE: This test changes the password to "newpass456", then changes it back to
     * "testpass123" to maintain the test account state.
     */
    @Test
    public void testChangePasswordWithValidInput() {
        // First verify we're actually on AccountActivity (not redirected to LoginActivity)
        // Verify the change password button is displayed - this confirms AccountActivity loaded
        onView(withId(R.id.btn_change_password))
                .check(matches(isDisplayed()));

        // Click the change password button to open dialog
        onView(withId(R.id.btn_change_password))
                .perform(click());

        // Verify dialog fields are displayed (Espresso waits automatically)
        onView(withId(R.id.et_current_password))
                .check(matches(isDisplayed()));

        onView(withId(R.id.et_new_password))
                .check(matches(isDisplayed()));

        onView(withId(R.id.et_confirm_password))
                .check(matches(isDisplayed()));

        // Enter current password (use the actual test account password)
        onView(withId(R.id.et_current_password))
                .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());

        // Enter new password (valid: >= 6 characters)
        onView(withId(R.id.et_new_password))
                .perform(replaceText("newpass456"), closeSoftKeyboard());

        // Enter matching confirmation password
        onView(withId(R.id.et_confirm_password))
                .perform(replaceText("newpass456"), closeSoftKeyboard());

        // Click the Change Password button in the dialog
        // Use android.R.id.button1 to target the positive button specifically
        // This avoids ambiguity with the dialog title which also says "Change Password"
        onView(allOf(withId(android.R.id.button1), withText("Change Password"), isClickable()))
                .perform(click());

        // Wait for backend API call
        sleep(3000);

        // The password has been changed. Now change it back to maintain test state.
        // Click change password button again
        onView(withId(R.id.btn_change_password))
                .perform(click());

        sleep(500);

        // Enter the new password as current
        onView(withId(R.id.et_current_password))
                .perform(replaceText("newpass456"), closeSoftKeyboard());

        sleep(200);

        // Change back to original password
        onView(withId(R.id.et_new_password))
                .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());

        sleep(200);

        onView(withId(R.id.et_confirm_password))
                .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());

        sleep(300);

        // Click Change Password button (positive button in dialog)
        onView(allOf(withId(android.R.id.button1), withText("Change Password"), isClickable()))
                .perform(click());

        // Wait for API call
        sleep(3000);

        // Test passes if both password changes executed successfully
    }

    /**
     * Test 28: Account - Test changing password with mismatched confirmation
     *
     * Description: This test verifies that the password change dialog properly
     * validates that the new password and confirmation password match. When they
     * don't match, an error should be displayed.
     *
     * Rationale: Input validation is critical for password security. We test with
     * mismatched passwords ("newpass456" vs "wrongpass789") to ensure the validation
     * catches this error. This is a negative test case that verifies error handling.
     *
     * Test Input:
     * - Current password: "testpass123"
     * - New password: "newpass456"
     * - Confirm password: "wrongpass789" (mismatch!)
     * Expected Result: Error message should be displayed indicating passwords don't match
     *
     * Coverage: Tests password validation (negative test case, error boundary test).
     */
    @Test
    public void testChangePasswordWithMismatchedConfirmation() {
        // Click the change password button
        onView(withId(R.id.btn_change_password))
                .check(matches(isDisplayed()))
                .perform(click());

        // Enter current password (Espresso waits automatically for dialog to appear)
        onView(withId(R.id.et_current_password))
                .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());

        // Enter new password
        onView(withId(R.id.et_new_password))
                .perform(replaceText("newpass456"), closeSoftKeyboard());

        // Enter DIFFERENT confirmation password (this should trigger validation error)
        onView(withId(R.id.et_confirm_password))
                .perform(replaceText("wrongpass789"), closeSoftKeyboard());

        // Click the Change Password button (positive button in dialog)
        onView(allOf(withId(android.R.id.button1), withText("Change Password"), isClickable()))
                .perform(click());

        // The error message TextView should now be visible with an error message
        // The dialog should still be open (not dismissed) because validation failed
        onView(withId(R.id.tv_error))
                .check(matches(isDisplayed()));

        // Cancel the dialog (negative button)
        onView(allOf(withId(android.R.id.button2), withText("Cancel"), isClickable()))
                .perform(click());

        // The test passes if validation error was shown and dialog remained open
    }

    /**
     * Test 29: Account - Test logout button logs user out
     *
     * Description: This test verifies that clicking the logout button shows a
     * confirmation dialog and, upon confirmation, logs the user out and navigates
     * to the MainActivity.
     *
     * Rationale: Logout is a critical session management feature. Users must be able
     * to securely end their session. We test the complete logout workflow including
     * the confirmation dialog (to prevent accidental logouts) and the final logout action.
     * This is a critical path test for user session management.
     *
     * Test Input: Click logout button, then confirm logout
     * Expected Result: Confirmation dialog should appear, then user should be logged
     * out and returned to MainActivity
     *
     * Coverage: Tests logout workflow (positive critical path test).
     */
    @Test
    public void testLogoutButtonLogsUserOut() {
        // Verify logout button is displayed
        onView(withId(R.id.btn_logout))
                .check(matches(isDisplayed()));

        // Click the logout button
        onView(withId(R.id.btn_logout))
                .perform(click());

        // Verify the confirmation dialog appears
        // The dialog title says "Logout", but we need to click the positive button
        // Use android.R.id.button1 to target the positive button specifically
        // This avoids ambiguity with the dialog title which also says "Logout"
        onView(allOf(withId(android.R.id.button1), withText("Logout"), isClickable()))
                .check(matches(isDisplayed()))
                .perform(click());

        // Wait for logout to complete and navigation to MainActivity
        sleep(2000);

        // The test passes if the logout action executes successfully
        // Note: After logout, the app navigates to MainActivity
        // The AuthManager should have cleared the auth state

        // Verify we're logged out by checking AuthManager
        AuthManager authManager = AuthManager.getInstance(context);
        assert !authManager.isLoggedIn() : "User should be logged out";
    }

    /**
     * Additional test: Test theme selection button
     *
     * Description: This test verifies that clicking the theme button opens a dialog
     * allowing users to select their preferred theme (Follow System, Light, or Dark).
     *
     * Rationale: Theme customization is an important UX feature. Users should be able
     * to change the app's appearance. This tests the theme selection dialog interaction.
     *
     * Test Input: Click theme button
     * Expected Result: Theme selection dialog should appear with options
     *
     * Coverage: Tests theme selection UI (positive interaction test).
     */
    @Test
    public void testThemeSelectionButton() {
        // Verify theme button is displayed
        onView(withId(R.id.btn_theme))
                .check(matches(isDisplayed()));

        // Click the theme button
        onView(withId(R.id.btn_theme))
                .perform(click());

        // Wait a moment for the dialog to appear
        sleep(300);

        // The theme dialog uses setSingleChoiceItems, which creates a ListView
        // The string is "Follow system" (lowercase 's') not "Follow System"
        // Use onData() to interact with ListView items
        // Try without specifying the adapter view - Espresso will find it automatically
        onData(allOf(is(instanceOf(String.class)), is("Follow system")))
                .perform(click());

        // Click OK button to confirm theme selection
        onView(allOf(withId(android.R.id.button1), withText("OK"), isClickable()))
                .perform(click());

        // Wait for theme to be applied (activity may recreate)
        sleep(1500);

        // The test passes if the theme dialog opened and an option was selectable
    }
}
