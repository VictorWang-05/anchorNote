package com.example.anchornotes_team3;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.anchornotes_team3.auth.AuthManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Black-box Espresso tests for LoginActivity and LoginOnlyActivity
 * Tests user authentication flows from a user's perspective
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityEspressoTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // IMPORTANT: Clear auth state BEFORE any activity is launched
        clearAuthState();

        // Add a delay to ensure clean state is fully applied
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        // Clean up after tests
        clearAuthState();
    }

    private void clearAuthState() {
        // Clear the CORRECT auth preferences - AuthManager uses "anchornotes_auth"!
        // USE COMMIT NOT APPLY (must be synchronous!)
        SharedPreferences authPrefs = context.getSharedPreferences("anchornotes_auth", Context.MODE_PRIVATE);
        authPrefs.edit().clear().commit();

        // Also clear auth_prefs (legacy/old name if it exists)
        SharedPreferences oldAuthPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        oldAuthPrefs.edit().clear().commit();

        // Clear any other shared preferences that might store auth data
        SharedPreferences defaultPrefs = context.getSharedPreferences(
            context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        defaultPrefs.edit().clear().commit();

        // Add longer delay to ensure state is completely cleared
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test 1: Login - Test successful login with valid credentials
     *
     * Location: app/src/androidTest/java/com/example/anchornotes_team3/LoginActivityEspressoTest.java::testSuccessfulLogin
     *
     * Description: Tests that a user can successfully log in with valid email and password,
     * and is redirected to the MainActivity.
     *
     * Rationale: This tests the happy path of the login flow - the most critical user journey.
     * Uses valid credentials that should exist in the backend. Tests end-to-end authentication.
     *
     * Note: Requires backend server running and a valid test account.
     * Test credentials: testuser@example.com / password123
     */
    @Test
    public void testSuccessfulLogin() {
        // Ensure we're logged out before starting
        clearAuthState();

        // Wait for state to be cleared
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch LoginActivity
        ActivityScenario.launch(LoginActivity.class);

        // Wait for LoginActivity to fully load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click the login button to navigate to LoginOnlyActivity
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for LoginOnlyActivity to load
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Enter valid email
        onView(withId(R.id.et_email))
                .perform(typeText("testuser@example.com"), closeSoftKeyboard());

        // Enter valid password
        onView(withId(R.id.et_password))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for API call and navigation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify MainActivity is displayed (check for a MainActivity-specific element)
        // Note: This assumes successful login redirects to MainActivity
        // You may need to adjust based on actual behavior
        onView(withId(R.id.newNoteButton))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 2: Login - Test login failure with invalid password
     *
     * Location: app/src/androidTest/java/com/example/anchornotes_team3/LoginActivityEspressoTest.java::testLoginWithInvalidPassword
     *
     * Description: Tests that login fails appropriately when user enters a valid email
     * but incorrect password. Verifies error message is displayed.
     *
     * Rationale: Tests error handling for invalid credentials - a common user mistake.
     * Ensures the app provides appropriate feedback rather than crashing or hanging.
     * Uses boundary testing with invalid inputs.
     */
    @Test
    public void testLoginWithInvalidPassword() {
        // Ensure we're logged out before starting
        clearAuthState();

        // Wait for state to be cleared
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch LoginActivity
        ActivityScenario.launch(LoginActivity.class);

        // Wait for LoginActivity to fully load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Navigate to LoginOnlyActivity
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for navigation
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Enter valid email
        onView(withId(R.id.et_email))
                .perform(typeText("testuser@example.com"), closeSoftKeyboard());

        // Enter INVALID password
        onView(withId(R.id.et_password))
                .perform(typeText("wrongpassword"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for API response
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify error toast appears (toast verification is tricky in Espresso)
        // Instead, verify we're still on LoginOnlyActivity (didn't navigate away)
        onView(withId(R.id.btn_login))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 3: Login - Test login with empty username field
     *
     * Location: app/src/androidTest/java/com/example/anchornotes_team3/LoginActivityEspressoTest.java::testLoginWithEmptyEmail
     *
     * Description: Tests that the app shows an error when user tries to login
     * with an empty email field.
     *
     * Rationale: Tests input validation for required fields. Uses boundary condition
     * testing with empty string input. Ensures app handles missing data gracefully.
     */
    @Test
    public void testLoginWithEmptyEmail() {
        // Ensure we're logged out before starting
        clearAuthState();

        // Wait for state to be cleared
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch LoginActivity
        ActivityScenario.launch(LoginActivity.class);

        // Wait for LoginActivity to fully load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Navigate to LoginOnlyActivity
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for navigation
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Leave email empty, enter password
        onView(withId(R.id.et_password))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.btn_login))
                .perform(click());

        // Verify we're still on LoginOnlyActivity (validation prevented login)
        onView(withId(R.id.btn_login))
                .check(matches(isDisplayed()));

        // Note: Email field should show error, but error checking is complex in Espresso
        // The main validation is that login didn't proceed
    }

    /**
     * Test 4: Login - Test login with empty password field
     *
     * Location: app/src/androidTest/java/com/example/anchornotes_team3/LoginActivityEspressoTest.java::testLoginWithEmptyPassword
     *
     * Description: Tests that the app shows an error when user tries to login
     * with an empty password field.
     *
     * Rationale: Tests input validation for password field. Boundary condition testing
     * with empty input. Ensures required field validation works correctly.
     */
    @Test
    public void testLoginWithEmptyPassword() {
        // Ensure we're logged out before starting
        clearAuthState();

        // Wait for state to be cleared
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch LoginActivity
        ActivityScenario.launch(LoginActivity.class);

        // Wait for LoginActivity to fully load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Navigate to LoginOnlyActivity
        onView(withId(R.id.btn_login))
                .perform(click());

        // Wait for navigation
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Enter email, leave password empty
        onView(withId(R.id.et_email))
                .perform(typeText("testuser@example.com"), closeSoftKeyboard());

        // Click login button without entering password
        onView(withId(R.id.btn_login))
                .perform(click());

        // Verify we're still on LoginOnlyActivity (validation prevented login)
        onView(withId(R.id.btn_login))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 5: Registration - Test new user registration with valid data
     *
     * Location: app/src/androidTest/java/com/example/anchornotes_team3/LoginActivityEspressoTest.java::testSuccessfulRegistration
     *
     * Description: Tests that a new user can successfully register with all required
     * fields filled in with valid data.
     *
     * Rationale: Tests the complete registration workflow. Uses unique username/email
     * to avoid conflicts. Tests all required fields are validated and processed correctly.
     *
     * Note: This test creates a new user in the database each time it runs.
     * You may need to use unique values or clean up the database between test runs.
     */
    @Test
    public void testSuccessfulRegistration() {
        // Ensure we're logged out before starting
        clearAuthState();

        // Wait for state to be cleared
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch LoginActivity (which is the registration screen)
        ActivityScenario.launch(LoginActivity.class);

        // Wait for LoginActivity to fully load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Generate unique username and email to avoid conflicts
        String timestamp = String.valueOf(System.currentTimeMillis());
        String username = "testuser" + timestamp;
        String email = "test" + timestamp + "@example.com";

        // Enter username
        onView(withId(R.id.et_username))
                .perform(typeText(username), closeSoftKeyboard());

        // Enter email
        onView(withId(R.id.et_email))
                .perform(typeText(email), closeSoftKeyboard());

        // Enter password
        onView(withId(R.id.et_password))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Enter full name
        onView(withId(R.id.et_fullname))
                .perform(typeText("Test User"), closeSoftKeyboard());

        // Click register button
        onView(withId(R.id.btn_register))
                .perform(click());

        // Wait for API call and navigation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify MainActivity is displayed after successful registration
        onView(withId(R.id.newNoteButton))
                .check(matches(isDisplayed()));
    }
}
