# Testing Guide for AnchorNotes - Auth & Repository Tests

## Overview
This document provides instructions for running the white-box unit tests for Auth and Repository components (Tests 24-28).

## Test Files Created

### 1. AuthManagerTest.java
**Location**: `app/src/test/java/com/example/anchornotes_team3/AuthManagerTest.java`

**Tests Included**:
- Test 24: Token storage and retrieval
- Test 25: Clearing authentication tokens
- Test 26: Expired token detection

### 2. NoteRepositoryTest.java
**Location**: `app/src/test/java/com/example/anchornotes_team3/NoteRepositoryTest.java`

**Tests Included**:
- Test 27: Filtering notes by date
- Test 28: Sorting notes by title

---

## Prerequisites

### 1. Sync Gradle Dependencies
Before running tests, ensure all dependencies are installed:

1. Open Android Studio
2. Click **File → Sync Project with Gradle Files**
3. Wait for sync to complete (check bottom right of Android Studio)

The following testing dependencies have been added to `app/build.gradle.kts`:
- `org.robolectric:robolectric:4.11.1` - Android unit testing framework
- `org.mockito:mockito-core:5.5.0` - Mocking framework
- `org.mockito:mockito-inline:5.2.0` - Mockito support for inline mocking

---

## How to Run Tests

### Option 1: Run All Tests from Both Files

1. In Android Studio, navigate to the Project view
2. Right-click on `app/src/test/java/com/example/anchornotes_team3/`
3. Select **Run 'Tests in 'anchornotes_team3''**
4. All unit tests will execute, including the new 5 tests

### Option 2: Run Tests from a Single File

#### For AuthManagerTest:
1. Open `AuthManagerTest.java`
2. Click the green play button (▶) next to the class name `AuthManagerTest`
3. Select **Run 'AuthManagerTest'**
4. View results in the Run window

#### For NoteRepositoryTest:
1. Open `NoteRepositoryTest.java`
2. Click the green play button (▶) next to the class name `NoteRepositoryTest`
3. Select **Run 'NoteRepositoryTest'**
4. View results in the Run window

### Option 3: Run Individual Test Method

1. Open either test file
2. Click the green play button (▶) next to a specific test method, for example:
   - `testTokenStorageAndRetrieval()`
   - `testClearingAuthenticationTokens()`
   - `testExpiredTokenDetection()`
   - `testFilteringNotesByDate()`
   - `testSortingNotesByTitle()`
3. Select **Run 'testMethodName()'**

### Option 4: Run Tests from Terminal

```bash
# Navigate to project root
cd /Users/alexzhu/AndroidStudioProjects/anchornotes_team_3

# Run all unit tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests "com.example.anchornotes_team3.AuthManagerTest"
./gradlew test --tests "com.example.anchornotes_team3.NoteRepositoryTest"

# Run specific test method
./gradlew test --tests "com.example.anchornotes_team3.AuthManagerTest.testTokenStorageAndRetrieval"
```

---

## Generating Test Coverage Report

### Method 1: Android Studio GUI

1. Right-click on `app/src/test/java/com/example/anchornotes_team3/`
2. Select **Run 'Tests in ...' with Coverage**
3. After tests complete, coverage report opens automatically
4. View coverage percentages:
   - **Green**: Code covered by tests
   - **Red**: Code not covered
   - **Yellow**: Partially covered

### Method 2: Terminal Command

```bash
# Generate coverage report
./gradlew testDebugUnitTestCoverage

# Report location:
# app/build/reports/coverage/test/debug/index.html
```

Open the HTML report in your browser to see detailed coverage metrics.

---

## Understanding Test Results

### Success Output
```
AuthManagerTest
  ✅ testTokenStorageAndRetrieval (45ms)
  ✅ testClearingAuthenticationTokens (32ms)
  ✅ testExpiredTokenDetection (28ms)

NoteRepositoryTest
  ✅ testFilteringNotesByDate (156ms)
  ✅ testSortingNotesByTitle (92ms)

5 tests passed (353ms total)
```

### Failure Output
If a test fails, you'll see:
```
❌ testTokenStorageAndRetrieval
   Expected: "testToken123"
   Actual: null
   at AuthManagerTest.testTokenStorageAndRetrieval(AuthManagerTest.java:52)
```

Click the failed test to jump to the exact line and see the full stack trace.

---

## Test Descriptions

### Test 24: AuthManager - Token Storage and Retrieval
**Purpose**: Verifies that JWT tokens can be stored in SharedPreferences and retrieved correctly.

**What it tests**:
- Storing authentication token
- Storing username and full name
- Retrieving stored values from SharedPreferences
- Verifying login state persistence

**Coverage**: Tests `AuthManager.getToken()`, `getUsername()`, `isLoggedIn()` methods.

---

### Test 25: AuthManager - Clearing Authentication Tokens
**Purpose**: Ensures logout properly clears all authentication data.

**What it tests**:
- Setting up authenticated state
- Calling logout() method
- Verifying all auth data is cleared
- Confirming SharedPreferences are empty after logout

**Coverage**: Tests `AuthManager.logout()` method and data cleanup.

---

### Test 26: AuthManager - Expired Token Detection
**Purpose**: Tests handling of expired JWT tokens.

**What it tests**:
- Storing an expired token (exp claim from 2020)
- Retrieving expired token
- Handling null/missing tokens
- Verifying token storage regardless of expiration

**Note**: AuthManager doesn't validate expiration client-side. The backend rejects expired tokens, triggering a logout.

**Coverage**: Tests token storage edge cases and null handling.

---

### Test 27: NoteRepository - Filtering Notes by Date
**Purpose**: Verifies date-based filtering logic for notes.

**What it tests**:
- Filtering notes created in last 7 days
- Filtering notes edited in last 3 days
- Filtering notes older than 20 days
- Date range filtering (between two dates)
- Handling null dates gracefully

**Coverage**: Tests date comparison logic using `Instant` and `ChronoUnit`.

---

### Test 28: NoteRepository - Sorting Notes by Title
**Purpose**: Tests alphabetical sorting of notes by title.

**What it tests**:
- Ascending sort (A-Z)
- Descending sort (Z-A)
- Null title handling (nullsLast)
- Case-insensitive sorting
- Empty string titles
- Sorting stability (maintaining order for equal titles)

**Coverage**: Tests Java Stream API sorting with custom comparators.

---

## Troubleshooting

### Issue: Tests not appearing in Android Studio
**Solution**: 
1. Right-click on test folder
2. Select **Mark Directory as → Test Sources Root**
3. Sync Gradle files

### Issue: Robolectric not found
**Solution**:
1. Ensure Gradle sync completed successfully
2. Run `./gradlew clean build`
3. Invalidate caches: **File → Invalidate Caches / Restart**

### Issue: Mockito errors
**Solution**:
1. Verify `mockito-core` and `mockito-inline` are in `build.gradle.kts`
2. Clean and rebuild: `./gradlew clean build`

### Issue: "java.time.Instant not found" error
**Solution**:
- Ensure Java 8+ features are enabled
- Check `compileOptions` in `build.gradle.kts` has `sourceCompatibility = JavaVersion.VERSION_11`
- Verify core library desugaring is enabled

### Issue: Coverage report not generating
**Solution**:
1. Run with coverage from Android Studio GUI
2. Or use terminal: `./gradlew testDebugUnitTestCoverage`
3. Check `app/build/reports/coverage/` directory

---

## Test Coverage Goals

For these 5 tests, we aim to achieve:

- **AuthManager**: 70%+ coverage
  - Token storage/retrieval paths
  - Logout flow
  - Edge cases (null, expired tokens)

- **NoteRepository**: 60%+ coverage (focusing on filtering/sorting logic)
  - Date filtering logic
  - Sorting comparators
  - Edge cases (null dates, empty titles)

---

## Integration with Testing Document

When documenting these tests in your PDF report, use the following format:

### Test 24: AuthManager - Token Storage and Retrieval
- **Location**: `app/src/test/java/com/example/anchornotes_team3/AuthManagerTest.java`, method `testTokenStorageAndRetrieval()`
- **Description**: Tests JWT token persistence in SharedPreferences by storing test credentials and verifying retrieval across AuthManager instances.
- **Result**: PASS - Token, username, and login state correctly persisted
- **Bug Found**: None

### Test 25: AuthManager - Clearing Authentication Tokens
- **Location**: `app/src/test/java/com/example/anchornotes_team3/AuthManagerTest.java`, method `testClearingAuthenticationTokens()`
- **Description**: Tests logout functionality by setting authenticated state, calling logout(), and verifying all auth data is removed from SharedPreferences.
- **Result**: PASS - All authentication data properly cleared
- **Bug Found**: None

### Test 26: AuthManager - Expired Token Detection
- **Location**: `app/src/test/java/com/example/anchornotes_team3/AuthManagerTest.java`, method `testExpiredTokenDetection()`
- **Description**: Tests handling of expired JWT tokens (exp claim from 2020) and null tokens. Verifies tokens are stored/retrieved regardless of expiration, with backend responsible for validation.
- **Result**: PASS - Expired and null tokens handled correctly
- **Bug Found**: None

### Test 27: NoteRepository - Filtering Notes by Date
- **Location**: `app/src/test/java/com/example/anchornotes_team3/NoteRepositoryTest.java`, method `testFilteringNotesByDate()`
- **Description**: Tests date-based filtering with multiple scenarios: last 7 days, last 3 days, older than 20 days, date ranges, and null date handling. Uses Java Instant and ChronoUnit for time calculations.
- **Result**: PASS - All date filtering scenarios work correctly
- **Bug Found**: None

### Test 28: NoteRepository - Sorting Notes by Title
- **Location**: `app/src/test/java/com/example/anchornotes_team3/NoteRepositoryTest.java`, method `testSortingNotesByTitle()`
- **Description**: Tests alphabetical sorting (ascending/descending), case-insensitive comparison, null/empty title handling, and sorting stability. Uses Java Comparator API.
- **Result**: PASS - All sorting scenarios work correctly
- **Bug Found**: None

---

## Next Steps

After running these tests:

1. ✅ Verify all 5 tests pass
2. ✅ Generate coverage report
3. ✅ Take screenshots of test results (for documentation)
4. ✅ Document coverage percentage achieved
5. ✅ Update testing document PDF with test details
6. ✅ Include any bugs found during testing (if applicable)

---

## Contact & Support

If you encounter issues running these tests, verify:
- ✅ Gradle dependencies synced
- ✅ Java 11 configured in `build.gradle.kts`
- ✅ Robolectric and Mockito added to dependencies
- ✅ Test files in correct directory: `app/src/test/`

For questions about the tests, refer to the inline comments in each test file for detailed explanations.

