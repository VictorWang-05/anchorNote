# Frontend-Backend Integration Summary

## Overview
This document summarizes all the changes made to align the Android frontend with the backend API.

## ✅ Completed Changes

### 1. Model Layer Updates

#### **Note.java**
- Changed ID type from `String` to `Long` to match backend
- Renamed `body` field to `text` to match backend
- Changed tags from `List<String>` to `List<Tag>` (full Tag objects)
- Separated reminders into:
  - `Instant reminderTime` for time-based reminders
  - `Geofence geofence` for location-based reminders
- Added `lastEdited` and `createdAt` timestamps
- Added helper flags `hasPhoto` and `hasAudio`

#### **Tag.java** (New)
- Full Tag model with `id`, `name`, and `color` fields
- Aligned with backend TagResponse structure

#### **Geofence.java** (New)
- Geofence model with `id`, `latitude`, `longitude`, `radius`, and `address`
- Aligned with backend GeofenceResponse structure

#### **Attachment.java**
- Added `Long id` field for backend attachment ID
- Added `mediaUrl` field for backend URL
- Added `isUploaded` flag to track upload status
- Added constructor for attachments loaded from backend

#### **Reminder.java** (Deleted)
- Removed old unified Reminder class
- Replaced with separate time reminder and geofence models

### 2. Networking Layer

#### **Dependencies (build.gradle.kts & libs.versions.toml)**
- Added Retrofit 2.9.0 for REST API calls
- Added OkHttp 4.12.0 for HTTP client
- Added Gson 2.10.1 for JSON serialization
- Added logging interceptor for debugging

#### **ApiService.java**
Complete Retrofit API interface with all endpoints:
- **Authentication**: register, login
- **Notes**: CRUD operations, pin/unpin, search, filter
- **Tags**: get all, create, delete, add/remove from note
- **Reminders**: set/delete time reminders and geofences
- **Attachments**: upload photo/audio, delete attachments

#### **ApiClient.java**
- Singleton Retrofit client configuration
- Configured with AuthInterceptor for JWT tokens
- Logging interceptor for debugging
- Base URL: `http://10.0.2.2:8080/` (Android emulator localhost)
- **⚠️ TODO**: Update BASE_URL for physical device testing

### 3. Authentication Layer

#### **AuthManager.java**
- Manages JWT token storage using SharedPreferences
- Methods:
  - `saveToken()` - Save JWT after login
  - `getToken()` - Get current token
  - `getBearerToken()` - Get formatted "Bearer {token}"
  - `isLoggedIn()` - Check login status
  - `clearAuth()` - Logout
  - `saveUserInfo()` / `getUserId()` / `getUsername()`

#### **AuthInterceptor.java**
- OkHttp interceptor that automatically adds JWT to all API requests
- Adds "Authorization: Bearer {token}" header to every request

### 4. Data Transfer Objects (DTOs)

Created DTOs for all API requests/responses:
- **Auth**: `AuthRequest`, `AuthResponse`, `RegisterRequest`
- **Notes**: `CreateNoteRequest`, `UpdateNoteRequest`, `NoteResponse`, `PinRequest`
- **Tags**: `CreateTagRequest`
- **Reminders**: `TimeReminderRequest`, `GeofenceRequest`

### 5. Repository Layer

#### **NoteRepository.java**
Complete rewrite with async HTTP calls:
- Changed singleton to require `Context` parameter
- Implemented all CRUD operations with callbacks
- **Note Operations**: `getAllNotes()`, `getNoteById()`, `createNote()`, `updateNote()`, `deleteNote()`, `pinNote()`
- **Tag Operations**: `getAllTags()`, `createTag()`
- **Reminder Operations**: `setTimeReminder()`, `setGeofence()`, `deleteTimeReminder()`, `deleteGeofence()`
- **Attachment Operations**: `uploadPhoto()`, `uploadAudio()`
- Callback interfaces: `NoteCallback`, `NotesCallback`, `TagsCallback`, `AttachmentCallback`, `SimpleCallback`

### 6. UI Layer

#### **NoteEditorActivity.java**
Major updates for async API integration:
- Changed repository initialization to pass `Context`
- Changed note ID handling from `String` to `Long`
- Added `isNewNote` flag to distinguish create vs update
- Added `loadAvailableTags()` to load tags from backend on startup
- Added `loadNoteFromBackend()` for async note loading
- Updated `saveNote()` to use async create/update with callbacks
- Added `saveRemindersAndAttachments()` to upload after note save
- Updated tag handling to use full `Tag` objects
- Updated location chip to use `Geofence` model
- Updated reminder chip to use `Instant` for time reminders
- Separated reminder/geofence clearing with backend API calls
- Fixed all `getBody()` calls to `getText()`

#### **AndroidManifest.xml**
- Added `INTERNET` permission
- Added `ACCESS_NETWORK_STATE` permission
- Added `android:usesCleartextTraffic="true"` for HTTP support (development)

## 🔧 Configuration Required

### 1. Backend URL
Update the BASE_URL in `ApiClient.java`:
```java
// For Android emulator (default)
private static final String BASE_URL = "http://10.0.2.2:8080/";

// For physical device (use your computer's IP)
private static final String BASE_URL = "http://YOUR_IP_ADDRESS:8080/";
```

### 2. Authentication Flow
The frontend now requires users to be logged in. You'll need to:
1. Create a login/register screen
2. Call `AuthManager.saveToken()` after successful login
3. All subsequent API calls will automatically include the JWT token

Example login flow:
```java
AuthRequest request = new AuthRequest(username, password);
apiService.login(request).enqueue(new Callback<AuthResponse>() {
    @Override
    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            AuthResponse auth = response.body();
            AuthManager.getInstance(context).saveToken(auth.getToken());
            AuthManager.getInstance(context).saveUserInfo(auth.getUserId(), auth.getUsername());
            // Navigate to main screen
        }
    }
    // ... error handling
});
```

### 3. Network Security (Production)
For production, remove `android:usesCleartextTraffic="true"` and use HTTPS only.

## 📝 Key Differences from Original Design

### What Changed:
1. **ID Type**: Backend uses `Long` IDs, not `String` UUIDs
2. **Field Names**: Backend uses `text` not `body` for note content
3. **Tags**: Backend uses full Tag objects with IDs, not just strings
4. **Reminders**: Backend separates time reminders and geofences (can have both!)
5. **Attachments**: Backend requires separate upload after note creation
6. **Async Operations**: All backend calls are asynchronous with callbacks

### Frontend Responsibilities:
- ✅ Store JWT token in SharedPreferences
- ✅ Attach token to all API requests (handled by AuthInterceptor)
- ✅ Handle token expiration (logout and redirect to login)
- ✅ Upload attachments after creating note
- ✅ Convert address to lat/long before sending geofence (TODO: implement geocoding)
- ✅ Display backend errors to users
- ❌ **NOT** validate/generate tokens (backend only)
- ❌ **NOT** manage user passwords (backend only)

### Backend Responsibilities:
- JWT generation and validation
- Password hashing and authentication
- File storage (Supabase)
- Database operations
- Input validation
- Authorization checks

## 🚀 Next Steps

### Immediate TODOs:
1. **Implement Login/Register Screen**
   - Create `LoginActivity.java` and layout
   - Call authentication APIs
   - Save token on successful login

2. **Update MainActivity**
   - Check if user is logged in (`AuthManager.isLoggedIn()`)
   - Redirect to login if not authenticated
   - Load notes from backend using `NoteRepository.getAllNotes()`

3. **Implement Geocoding**
   - Use Android Geocoder API to convert addresses to lat/long
   - Or use backend geocoding endpoint if available

4. **Handle Token Expiration**
   - Catch 401 Unauthorized responses
   - Clear token and redirect to login

5. **Test with Real Backend**
   - Start backend server
   - Update BASE_URL in ApiClient
   - Test all flows end-to-end

### Optional Enhancements:
- Add loading spinners during API calls
- Implement offline caching with Room database
- Add retry logic for failed network requests
- Implement pull-to-refresh on note list
- Add image preview for photo attachments using mediaUrl

## 🧪 Testing

### Backend Connection Test:
1. Ensure backend is running on `localhost:8080`
2. Register a new user through the frontend
3. Login with those credentials
4. Create a note and verify it appears in backend database

### API Test Checklist:
- [ ] Register new user
- [ ] Login existing user
- [ ] Create new note
- [ ] Update existing note
- [ ] Delete note
- [ ] Pin/unpin note
- [ ] Add tags to note
- [ ] Set time reminder
- [ ] Set geofence
- [ ] Upload photo attachment
- [ ] Upload audio attachment
- [ ] Search notes
- [ ] Filter notes

## 📚 Additional Resources

- [Retrofit Documentation](https://square.github.io/retrofit/)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Gson User Guide](https://github.com/google/gson/blob/master/UserGuide.md)
- [JWT Introduction](https://jwt.io/introduction)
- Backend API Documentation: See `API_DOCUMENTATION.md`

## ⚠️ Known Issues/Limitations

1. **Geocoding**: Address to lat/long conversion not implemented yet (uses dummy coordinates)
2. **Audio Recording**: Audio recording functionality not implemented yet
3. **Image Display**: Attachments from backend (mediaUrl) not displayed yet
4. **Error Handling**: Generic error messages, could be more specific
5. **No Offline Support**: App requires internet connection for all operations

## 💡 Tips

- Use Android Studio's Logcat with filter "OkHttp" to see all API requests/responses
- Check `HttpLoggingInterceptor` output for debugging network issues
- Use backend's `api-tester.html` to verify endpoints work before implementing in frontend
- Test on Android emulator first (easier networking setup)

---

**Last Updated**: November 3, 2025
**Status**: ✅ All frontend changes complete, ready for testing with backend

