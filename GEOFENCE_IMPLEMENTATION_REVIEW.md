# Geofence Feature Implementation Review

## 📋 Feature Requirements (From Spec)

### Core Requirements:
1. **Mutually Exclusive Reminders**: Each note can have EITHER a time reminder OR a geofence reminder (not both)
2. **Geofence Trigger**: Reminder fires when user ENTERS a defined region
3. **Relevant Notes Section**: Triggered notes appear in a "Relevant Notes" section on home page
4. **Auto-Remove Logic**:
   - Time-based: Note leaves "Relevant Notes" after 1 hour
   - Geofence-based: Note leaves when user EXITS the geofence

---

## ✅ What Already Exists

### Backend APIs (Fully Implemented)
1. **`PUT /api/notes/{id}/reminder/geofence`** - Set geofence reminder
   - Body: `{ latitude, longitude, radius }`
   - Returns: Full `NoteResponse` with geofence data
   
2. **`DELETE /api/notes/{id}/reminder`** - Clear all reminders (time + geofence)

3. **`GET /api/geofences`** - List all geofences for Android registration
   - Returns: `List<GeofenceRegistrationResponse>`
   - Format: `[{ geofenceId: "note_123", latitude, longitude, radiusMeters }]`

### Android - Data Layer ✅
- ✅ `app/src/main/java/com/example/anchornotes_team3/model/Geofence.java` - Model class
- ✅ `app/src/main/java/com/example/anchornotes_team3/dto/GeofenceRequest.java` - API request DTO
- ✅ `app/src/main/java/com/example/anchornotes_team3/api/ApiService.java` - Has `setGeofence()` and `clearReminders()`
- ✅ `app/src/main/java/com/example/anchornotes_team3/repository/NoteRepository.java` - Has `setGeofence()` and `clearReminders()`
- ✅ `app/src/main/java/com/example/anchornotes_team3/model/Note.java` - Has separate `reminderTime` and `geofence` fields

### Android - UI Components ✅
- ✅ `app/src/main/res/layout/dialog_add_reminder.xml` - Complete dialog with TabLayout for Time/Geofence
  - Has address input field (`et_geo_address`)
  - Has radius slider (`slider_radius`) with 50-1000m range
  - Has proper tab switching layout
- ✅ `app/src/main/res/layout/activity_note_editor.xml` - Has reminder chip (`chip_reminder`)
- ✅ `app/src/main/java/com/example/anchornotes_team3/NoteEditorActivity.java` - Has reminder chip logic with click/long-press handlers

### Android - Permissions ✅
- ✅ `AndroidManifest.xml` has `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`

---

## ❌ What's Missing (Must Implement)

### 1. **Android DTO for GET /api/geofences**
**File to create**: `app/src/main/java/com/example/anchornotes_team3/dto/GeofenceRegistrationResponse.java`

```java
public class GeofenceRegistrationResponse {
    private String geofenceId;      // e.g., "note_123"
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;
    
    // Getters and setters...
}
```

**Update**: `ApiService.java` - Add endpoint:
```java
@GET("api/geofences")
Call<List<GeofenceRegistrationResponse>> listGeofences();
```

---

### 2. **Platform Service - GeofenceManager** ⚠️ CRITICAL
**File to create**: `app/src/main/java/com/example/anchornotes_team3/geofence/GeofenceManager.java`

**Purpose**: Wrapper around Google Play Services `GeofencingClient`

**Key Methods**:
- `void add(String noteId, double lat, double lng, float radiusM)` - Register a geofence
- `void remove(String noteId)` - Unregister a geofence
- `void syncAllGeofences()` - Called on app start to sync all geofences from backend

**Dependencies**:
```gradle
// Add to app/build.gradle.kts
implementation("com.google.android.gms:play-services-location:21.0.1")
```

**Key Implementation Points**:
- Uses `PendingIntent` to trigger `GeofenceReceiver`
- Geofence ID format: `"note_{noteId}"` (matches backend)
- Must request location permissions at runtime before registration

---

### 3. **Platform Service - GeofenceReceiver** ⚠️ CRITICAL
**File to create**: `app/src/main/java/com/example/anchornotes_team3/geofence/GeofenceReceiver.java`

**Purpose**: `BroadcastReceiver` that handles ENTER/EXIT transitions

**What it does**:
```
ON ENTER:
  1. Parse noteId from geofenceId
  2. Call RelevantNotesStore.add(noteId)
  3. Show notification: "Relevant note nearby"
  4. (Optional) POST telemetry to backend

ON EXIT:
  1. Parse noteId from geofenceId
  2. Call RelevantNotesStore.remove(noteId)
  3. (Optional) POST telemetry to backend
```

**Register in AndroidManifest.xml**:
```xml
<receiver
    android:name=".geofence.GeofenceReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.example.anchornotes_team3.GEOFENCE_EVENT"/>
    </intent-filter>
</receiver>
```

---

### 4. **Data Store - RelevantNotesStore** ⚠️ CRITICAL
**File to create**: `app/src/main/java/com/example/anchornotes_team3/store/RelevantNotesStore.java`

**Purpose**: In-memory singleton that tracks which notes are currently "relevant"

**Key Methods**:
- `void add(String noteId)` - Mark note as relevant
- `void remove(String noteId)` - Mark note as not relevant
- `LiveData<List<String>> getRelevantNoteIds()` - Observable list for UI
- `void scheduleRemoval(String noteId, long delayMillis)` - For 1-hour timeout (time-based reminders)

**Implementation Options**:
1. Simple singleton with `Set<String>` + callbacks
2. ViewModel-backed with LiveData (preferred for lifecycle awareness)
3. Room database for persistence across app restarts

---

### 5. **UI Component - Home Screen "Relevant Notes" Section**
**Files to modify/create**:
- `app/src/main/java/com/example/anchornotes_team3/MainActivity.java`
  - Add a "Relevant Notes" section ABOVE pinned notes
  - Observe `RelevantNotesStore.getRelevantNoteIds()`
  - Fetch note details and display them
  
- `app/src/main/res/layout/activity_main.xml`
  - Add a new `RecyclerView` or `LinearLayout` for relevant notes section
  - Add a section header: "Relevant Notes 📍"

---

### 6. **Complete Geofence Dialog Implementation**
**File to modify**: `app/src/main/java/com/example/anchornotes_team3/NoteEditorActivity.java`

**Current Status**: Dialog shows but only displays a toast for geofence tab

**What to implement**:
```java
private void showAddReminderDialog() {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);
    
    TabLayout tabLayout = dialogView.findViewById(R.id.tab_reminder_type);
    LinearLayout layoutTime = dialogView.findViewById(R.id.layout_time_reminder);
    LinearLayout layoutGeofence = dialogView.findViewById(R.id.layout_geofence_reminder);
    
    // Tab switching logic
    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (tab.getPosition() == 0) {
                layoutTime.setVisibility(View.VISIBLE);
                layoutGeofence.setVisibility(View.GONE);
            } else {
                layoutTime.setVisibility(View.GONE);
                layoutGeofence.setVisibility(View.VISIBLE);
            }
        }
        // ... other methods
    });
    
    // Geofence controls
    TextInputEditText etAddress = dialogView.findViewById(R.id.et_geo_address);
    Slider sliderRadius = dialogView.findViewById(R.id.slider_radius);
    TextView tvRadiusValue = dialogView.findViewById(R.id.tv_radius_value);
    
    sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
        tvRadiusValue.setText((int)value + " meters");
    });
    
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setView(dialogView)
        .setPositiveButton("Save", (d, which) -> {
            if (tabLayout.getSelectedTabPosition() == 1) {
                // Geofence selected
                String address = etAddress.getText().toString();
                int radius = (int) sliderRadius.getValue();
                
                // Geocode address to lat/lng (implement geocoding helper)
                geocodeAndSaveGeofence(address, radius);
            } else {
                // Time reminder (existing logic)
                saveTimeReminder();
            }
        })
        .setNegativeButton("Cancel", null)
        .create();
    dialog.show();
}

private void geocodeAndSaveGeofence(String address, int radius) {
    // Use Android Geocoder or Google Places API
    // Then call:
    saveGeofence(lat, lng, radius);
}

private void saveGeofence(double lat, double lng, int radius) {
    Geofence geofence = new Geofence(lat, lng, radius, address);
    currentNote.setGeofence(geofence);
    currentNote.setReminderTime(null); // Clear time reminder (mutual exclusivity)
    
    noteRepository.setGeofence(currentNote.getId(), geofence, new NoteCallback() {
        @Override
        public void onSuccess(Note note) {
            currentNote = note;
            updateReminderChip();
            
            // Register with GeofenceManager
            geofenceManager.add(currentNote.getId(), lat, lng, radius);
            
            Toast.makeText(NoteEditorActivity.this, 
                "Geofence reminder set", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onError(String message) {
            // Rollback on error
            currentNote.setGeofence(null);
            Toast.makeText(NoteEditorActivity.this, 
                "Failed to save geofence: " + message, Toast.LENGTH_LONG).show();
        }
    });
}
```

**Update `updateReminderChip()`**:
```java
private void updateReminderChip() {
    if (currentNote.getGeofence() != null) {
        // Show geofence info
        Geofence geo = currentNote.getGeofence();
        chipReminder.setText(geo.getDisplayText());
        chipReminder.setCloseIconVisible(true);
        chipReminder.setOnCloseIconClickListener(v -> clearReminder());
    } else if (currentNote.getReminderTime() != null) {
        // Show time reminder info (existing logic)
        Instant reminderTime = currentNote.getReminderTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                .withZone(ZoneId.systemDefault());
        String text = formatter.format(reminderTime);
        chipReminder.setText(text);
        chipReminder.setCloseIconVisible(true);
        chipReminder.setOnCloseIconClickListener(v -> clearReminder());
    } else {
        chipReminder.setText(R.string.chip_no_reminder);
        chipReminder.setCloseIconVisible(false);
    }
}
```

**Update `clearReminder()`**:
```java
private void clearReminder() {
    String noteId = currentNote.getId();
    boolean hadGeofence = currentNote.getGeofence() != null;
    
    currentNote.setReminderTime(null);
    currentNote.setGeofence(null);
    
    noteRepository.clearReminders(noteId, new SimpleCallback() {
        @Override
        public void onSuccess() {
            updateReminderChip();
            
            // Unregister geofence if it was set
            if (hadGeofence) {
                geofenceManager.remove(noteId);
            }
            
            Toast.makeText(NoteEditorActivity.this, 
                "Reminder cleared", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onError(String message) {
            Toast.makeText(NoteEditorActivity.this, 
                "Failed to clear reminder: " + message, Toast.LENGTH_LONG).show();
        }
    });
}
```

---

### 7. **Geocoding Helper**
**File to create**: `app/src/main/java/com/example/anchornotes_team3/util/GeocoderHelper.java`

**Purpose**: Convert address string to lat/lng coordinates

**Two Options**:
1. **Android Geocoder** (free, no API key, but less reliable):
```java
Geocoder geocoder = new Geocoder(context);
List<Address> addresses = geocoder.getFromLocationName(address, 1);
if (!addresses.isEmpty()) {
    Address address = addresses.get(0);
    double lat = address.getLatitude();
    double lng = address.getLongitude();
}
```

2. **Google Places API** (requires API key, more reliable):
   - Requires `implementation("com.google.android.libraries.places:places:3.2.0")`
   - Need to enable Places API in Google Cloud Console

---

### 8. **Runtime Permissions**
**File to modify**: `app/src/main/java/com/example/anchornotes_team3/NoteEditorActivity.java`

**Add permission checking**:
```java
private void saveGeofence(double lat, double lng, int radius) {
    // Check location permission first
    if (ActivityCompat.checkSelfPermission(this, 
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            REQUEST_CODE_LOCATION_PERMISSION);
        
        // Store pending geofence data to apply after permission granted
        pendingGeofence = new Geofence(lat, lng, radius);
        return;
    }
    
    // Continue with geofence registration...
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, apply pending geofence
            if (pendingGeofence != null) {
                saveGeofence(pendingGeofence.getLatitude(), 
                    pendingGeofence.getLongitude(), 
                    pendingGeofence.getRadius());
                pendingGeofence = null;
            }
        } else {
            Toast.makeText(this, "Location permission required for geofence", 
                Toast.LENGTH_LONG).show();
        }
    }
}
```

---

### 9. **Background Location Permission (Optional)**
If you want geofences to work when the app is fully closed:

**Update AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**Request at runtime** (Android 10+):
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
        REQUEST_CODE_BACKGROUND_LOCATION);
}
```

**Note**: This is optional for MVP. Geofences will still work if app is in background (not killed).

---

### 10. **Sync Geofences on App Start**
**File to modify**: `app/src/main/java/com/example/anchornotes_team3/MainActivity.java`

**Add in `onCreate()`**:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // ... existing initialization ...
    
    // Sync all geofences from backend
    if (authManager.isLoggedIn()) {
        syncGeofencesFromBackend();
    }
}

private void syncGeofencesFromBackend() {
    apiService.listGeofences().enqueue(new Callback<List<GeofenceRegistrationResponse>>() {
        @Override
        public void onResponse(Call<List<GeofenceRegistrationResponse>> call, 
                             Response<List<GeofenceRegistrationResponse>> response) {
            if (response.isSuccessful() && response.body() != null) {
                for (GeofenceRegistrationResponse geo : response.body()) {
                    // Extract noteId from "note_123" format
                    String noteId = geo.getGeofenceId().replace("note_", "");
                    
                    geofenceManager.add(noteId, 
                        geo.getLatitude(), 
                        geo.getLongitude(), 
                        geo.getRadiusMeters().floatValue());
                }
                Log.d(TAG, "Synced " + response.body().size() + " geofences");
            }
        }
        
        @Override
        public void onFailure(Call<List<GeofenceRegistrationResponse>> call, Throwable t) {
            Log.e(TAG, "Failed to sync geofences", t);
        }
    });
}
```

---

## 🔧 Implementation Priority

### Phase 1: Core Infrastructure (Must Have)
1. ✅ Add Google Play Services dependency
2. ✅ Create `GeofenceRegistrationResponse` DTO
3. ✅ Add `listGeofences()` to `ApiService`
4. ✅ Create `GeofenceManager`
5. ✅ Create `GeofenceReceiver`
6. ✅ Update `AndroidManifest.xml` with receiver
7. ✅ Create `RelevantNotesStore`

### Phase 2: UI Integration (Must Have)
8. ✅ Complete geofence dialog implementation in `NoteEditorActivity`
9. ✅ Update `updateReminderChip()` to show geofence info
10. ✅ Update `clearReminder()` to unregister geofences
11. ✅ Add runtime permission handling
12. ✅ Create `GeocoderHelper` for address → lat/lng conversion

### Phase 3: Home Screen (Must Have)
13. ✅ Add "Relevant Notes" section to `MainActivity`
14. ✅ Connect to `RelevantNotesStore` observable
15. ✅ Display triggered notes

### Phase 4: Sync & Polish (Should Have)
16. ✅ Implement geofence sync on app start
17. ✅ Add notification when entering geofence
18. ✅ Add 1-hour timeout for time-based reminders
19. ✅ Test with emulator location spoofing

---

## 🚨 Key Architectural Notes

### Mutual Exclusivity
- **SPEC SAYS**: "Each note can only have a time reminder or a geofence reminder, not both."
- **BACKEND BEHAVIOR**: According to `api-tester.html` lines 571-572 and 594-595, the backend does NOT enforce mutual exclusivity. Both can coexist.
- **FRONTEND MUST ENFORCE**: When setting a geofence, clear `reminderTime`. When setting time reminder, clear `geofence`.

```java
// When saving geofence:
currentNote.setGeofence(geofence);
currentNote.setReminderTime(null); // Enforce mutual exclusivity

// When saving time reminder:
currentNote.setReminderTime(instant);
currentNote.setGeofence(null); // Enforce mutual exclusivity
```

### Retention Windows
- **Time-based**: Note stays in "Relevant Notes" for 1 hour after trigger
- **Geofence-based**: Note stays while inside geofence, leaves immediately on EXIT

### Geofence ID Format
- Backend uses: `"note_{noteId}"` (e.g., `"note_123"`)
- Must match this format in `GeofenceManager` to ensure proper sync

---

## 🧪 Testing Strategy

### Unit Tests
1. `GeofenceManager` - Mock `GeofencingClient`
2. `RelevantNotesStore` - Test add/remove/timeout logic
3. `GeocoderHelper` - Mock Geocoder

### Integration Tests
1. Set geofence via UI → Verify API call
2. Clear geofence → Verify unregister + API call
3. App restart → Verify geofence sync

### Manual Tests (Emulator)
1. **Create geofence note**:
   - Open note → Add reminder → Geofence tab
   - Enter address → Set radius → Save
   - Verify chip shows geofence info

2. **Enter geofence** (emulator):
   - Settings → Location → Set to geofence center
   - Verify notification appears
   - Verify note appears in "Relevant Notes"

3. **Exit geofence** (emulator):
   - Settings → Location → Set to outside geofence
   - Verify note disappears from "Relevant Notes"

4. **Clear geofence**:
   - Long-press reminder chip → Clear
   - Verify chip resets
   - Verify geofence unregistered (no trigger when entering area)

5. **App restart**:
   - Force stop app
   - Relaunch
   - Verify geofences are re-registered (enter geofence → still triggers)

---

## 📝 Missing String Resources

Add to `app/src/main/res/values/strings_note_editor.xml`:
```xml
<string name="dialog_geofence_address">Address or location</string>
<string name="dialog_geofence_radius">Geofence radius</string>
<string name="notification_relevant_note_title">Relevant Note Nearby</string>
<string name="notification_relevant_note_body">You have a note for this location</string>
<string name="error_geocoding_failed">Could not find location. Please try a different address.</string>
<string name="permission_location_rationale">Location access is needed to create geofence reminders</string>
```

---

## 📚 External Resources

### Google Play Services Documentation
- [Geofencing API Guide](https://developer.android.com/training/location/geofencing)
- [Requesting Location Permissions](https://developer.android.com/training/location/permissions)

### Dependencies to Add
```kotlin
// app/build.gradle.kts
dependencies {
    // Geofencing
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Optional: Google Places for better geocoding
    implementation("com.google.android.libraries.places:places:3.2.0")
}
```

---

## ✅ Summary Checklist

### Backend (Complete)
- [x] Geofence entity
- [x] Set geofence endpoint
- [x] Clear reminders endpoint
- [x] List geofences endpoint

### Android - Already Done
- [x] Geofence model
- [x] GeofenceRequest DTO
- [x] API service methods
- [x] Repository methods
- [x] Dialog XML layout
- [x] Reminder chip in note editor
- [x] Location permissions in manifest

### Android - To Implement
- [ ] GeofenceRegistrationResponse DTO
- [ ] GeofenceManager (platform service)
- [ ] GeofenceReceiver (broadcast receiver)
- [ ] RelevantNotesStore (data store)
- [ ] Complete geofence dialog logic
- [ ] GeocoderHelper
- [ ] Runtime permission handling
- [ ] "Relevant Notes" section on home screen
- [ ] Geofence sync on app start
- [ ] Notification on geofence entry
- [ ] Update reminder chip for geofence display
- [ ] Update clear reminder to unregister geofence
- [ ] Add Google Play Services dependency
- [ ] Register receiver in AndroidManifest.xml
- [ ] Add missing string resources

---

**Estimated Implementation Time**: 8-12 hours for a complete, polished implementation.

**Critical Path**: GeofenceManager → GeofenceReceiver → RelevantNotesStore → UI Integration

