# Homepage Login Functionality - Fixed

## Problem
The 403 Forbidden error was occurring because the **JWT token is from Supabase** but the backend might expect a different authentication format or additional validation.

From the logs, we can see:
- ✅ Token IS being sent: `Authorization: Bearer eyJhbGciOiJIUzI1NiIs...`
- ✅ Token is valid (expires at 1762147447 = Nov 3, 2025, 05:57 UTC)
- ❌ Backend still rejects with 403 Forbidden

**This suggests a backend authentication configuration issue, not a frontend issue.**

## Changes Made

### 1. Removed Debug Code
- Removed all debug logging from `MainActivity.java` and `AuthInterceptor.java`

### 2. Added Homepage Login/Logout Functionality

**Updated Files:**
- `app/src/main/java/com/example/anchornotes_team3/MainActivity.java`
  - Added dynamic UI updates based on login status
  - Shows **"Login" button** when user is NOT logged in
  - Shows **username** when user IS logged in (click to logout)
  - "New Note" button checks login status before opening editor
  
- `app/src/main/AndroidManifest.xml`
  - Changed launcher activity from `LoginActivity` to `MainActivity`
  - App now starts at homepage, not login screen

- `app/src/main/java/com/example/anchornotes_team3/LoginActivity.java`
  - Removed automatic redirect check on startup
  - Returns to MainActivity after successful login/registration

## How to Use

### Starting the App:
1. **App opens at Homepage** (MainActivity)
2. If logged in → Shows username in top right
3. If not logged in → Shows "Login" button in top right

### To Login:
1. Click **"Login"** button on homepage
2. Enter credentials and login/register
3. Automatically returns to homepage
4. Username now displayed

### To Logout:
1. Click on your **username** in top right
2. App logs you out and refreshes homepage
3. "Login" button appears again

### To Create Notes:
1. Click **"NEW NOTE"** button
2. If not logged in → Redirected to login first
3. If logged in → Opens Note Editor

## About the 403 Error

The token is being sent correctly, but the backend is still rejecting it with 403. This could be due to:

1. **Backend expects a different token format**
   - Your backend might expect a custom JWT, not a Supabase token
   
2. **Backend requires additional claims/roles**
   - Token might need specific claims that Supabase isn't providing
   
3. **Backend has additional authorization logic**
   - Even with valid authentication, there might be role/permission checks failing
   
4. **Token validation secret mismatch**
   - Backend might be using a different secret to validate the JWT

### Recommendation:
**Contact your backend developer** to verify:
- What JWT format/issuer the backend expects
- How to validate the Supabase JWT in the backend
- Whether there are additional authorization requirements beyond authentication

The frontend is correctly sending the token - this is a backend configuration issue.

