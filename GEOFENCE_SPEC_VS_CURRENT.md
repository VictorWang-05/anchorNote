# Geofence Spec vs Current Implementation

## 📊 Comparison Matrix

| Component | Spec Requirement | Current Implementation | Gap | Action Needed |
|-----------|------------------|----------------------|-----|---------------|
| **Backend API** | ✅ Set geofence, clear, list | ✅ Fully implemented | ✅ None | None |
| **Geofence Model** | ✅ Lat/lng/radius | ✅ `Geofence.java` exists | ✅ None | None |
| **Reminder Chip** | ✅ Show geofence info | ⚠️ Shows time only | ❌ Missing geofence display | Update `updateReminderChip()` |
| **Reminder Dialog** | ✅ Time/Geofence tabs | ⚠️ Layout exists, logic incomplete | ❌ Geofence tab shows toast | Implement geofence save logic |
| **GeofenceManager** | ✅ Register with Play Services | ❌ Does not exist | ❌ Critical missing | Create `GeofenceManager.java` |
| **GeofenceReceiver** | ✅ Handle ENTER/EXIT | ❌ Does not exist | ❌ Critical missing | Create `GeofenceReceiver.java` |
| **RelevantNotesStore** | ✅ Track active notes | ❌ Does not exist | ❌ Critical missing | Create `RelevantNotesStore.java` |
| **Relevant Notes UI** | ✅ Section on home page | ❌ Does not exist | ❌ Critical missing | Add to `MainActivity` |
| **Geocoding** | ✅ Address → Lat/Lng | ❌ Does not exist | ❌ Missing | Create `GeocoderHelper.java` |
| **Permissions** | ✅ Runtime location permission | ⚠️ Manifest only | ❌ No runtime check | Add permission handling |
| **Geofence Sync** | ✅ Sync on app start | ❌ Does not exist | ❌ Missing | Add to `MainActivity.onCreate()` |
| **Notification** | ✅ Show on ENTER | ❌ Does not exist | ❌ Missing | Add to `GeofenceReceiver` |

---

## 🔍 Detailed Spec Analysis

### From Your Spec Images:

#### Presentation Layer
**Spec Says:**
- `NoteEditorScreen` calls `onSetGeofence(geo)` when user picks a location/radius
- `onAddReminder(r)` / `onDeleteReminder()` for saving/clearing the single reminder
- `RelevantNotesSection` shows/removes active items as geofence/time triggers happen

**Current State:**
- ✅ `NoteEditorActivity` has reminder chip
- ✅ Dialog layout exists
- ❌ `onSetGeofence()` logic not implemented
- ❌ `RelevantNotesSection` component doesn't exist
- ❌ No connection between geofence events and UI

---

#### Domain Layer
**Spec Says:**
- Models: `Reminder` (abstract) → `GeofenceReminder(geofence)`, `Geofence(center, radiusM=50)`
- Use cases: `AddReminder` (writes to repo + registers with platform), `DeleteReminder` (cancels), `TriggerRelevantNote` (surface + clear after window/exit)

**Current State:**
- ✅ `Geofence` model exists
- ✅ `Note` model has separate `reminderTime` and `geofence` fields (differs from spec's unified `Reminder` but works)
- ❌ No use case layer (but `NoteRepository` handles API calls)
- ❌ No `TriggerRelevantNote` logic

**Note**: Your architecture doesn't have a formal use case layer, but `NoteRepository` effectively serves this purpose. This is fine.

---

#### Data Layer
**Spec Says:**
- `ReminderRepository`: `setGeofence(noteId, lat, lng, radiusM)`, `remove(noteId)`, `get(noteId)`
- Also `listInside(lat,lng)` helpers if needed
- `LocationRepository`: stores note location fields (e.g., lat/lng/place text)

**Current State:**
- ✅ `NoteRepository.setGeofence()` exists
- ✅ `NoteRepository.clearReminders()` exists
- ❌ No `LocationRepository` (but not critical - location is just note metadata)

---

#### Platform Services
**Spec Says:**
- `GeofenceManager` (Google Play Services): `add(noteId, center, radiusM)`, `remove(noteId)`; callbacks on **enter/exit**
- `ReminderScheduler`: has `scheduleGeofence(noteId, center, radiusM)` and shows/clears "Relevant Notes"

**Current State:**
- ❌ No `GeofenceManager`
- ❌ No `ReminderScheduler`

**Recommendation**: 
- Create `GeofenceManager` (handles Play Services)
- Don't need separate `ReminderScheduler` - combine logic into `GeofenceReceiver` + `RelevantNotesStore`

---

### Workflow Analysis

#### 1) User sets a geofence on the Note screen

**Spec Flow:**
1. UI (NoteEditorScreen): opens dialog to enter address + radius (default 50m); when confirmed → `onSetGeofence(geo)` and `onAddReminder(GeofenceReminder(geo))`
2. VM/Use case: `AddReminder.execute(noteId, reminder)`:
   - Persists via `ReminderRepository.setGeofence(noteId, lat, lng, radius)` (removes any time reminder to enforce mutual exclusivity)
   - Calls platform `GeofenceManager.add(noteId, center, radius)` (or `ReminderScheduler.scheduleGeofence(...)`)
3. GeofencingClient (Play Services) registers the fence tied to a `PendingIntent`

**Current Implementation:**
- ✅ Step 1: Dialog layout exists
- ❌ Step 1: Dialog logic shows toast instead of saving
- ⚠️ Step 2: `NoteRepository.setGeofence()` exists but no platform registration
- ❌ Step 3: No `GeofenceManager` to call Play Services

**Gap**: Missing steps 1 (complete logic) and 3 (platform registration)

---

#### 2) User enters/exits the region

**Spec Flow:**
1. System → app: The `PendingIntent` fires on **ENTER** (and EXIT)
2. Platform service (`GeofenceManager`): handles the intent, resolves `noteId`, and:
   - On ENTER → invokes `TriggerRelevantNote.execute(noteId, now, where)` to:
     - mark the note **active** in "Relevant Notes,"
     - show a **notification**,
     - start a timer to clear after the retention window (e.g., 1 hour), *or* keep it visible while inside the fence
   - On EXIT → clears that note from "Relevant Notes" immediately
3. Home UI updates: `RelevantNotesSection.refresh()` pulls active items via `ReminderViewModel.getActiveRelevant()`. The note shows beneath the Pinned section; when the window ends or on EXIT, it disappears

**Current Implementation:**
- ❌ No `GeofenceReceiver` to handle PendingIntent
- ❌ No `RelevantNotesStore` to track active notes
- ❌ No notification logic
- ❌ No "Relevant Notes" section on home screen

**Gap**: Entire flow is missing

---

#### 3) Home UI updates

**Spec Flow:**
- `RelevantNotesSection.refresh()` pulls active items (e.g., from a small in-memory list or DB flag) via `ReminderViewModel.getActiveRelevant()`
- The note shows beneath the Pinned section; when the window ends or on EXIT, it disappears

**Current Implementation:**
- ❌ No "Relevant Notes" section
- ❌ No observable data source for active notes

**Gap**: Entire section missing

---

## ⚖️ Spec Divergences

### 1. Reminder Architecture
**Spec**: Unified `Reminder` abstract class with `TimeReminder` and `GeofenceReminder` subclasses

**Your Code**: Separate fields in `Note`:
```java
private Instant reminderTime;  // For time-based
private Geofence geofence;     // For location-based
```

**Verdict**: ✅ **This is fine**. Your approach is simpler and works well for the use case. Both approaches achieve mutual exclusivity if enforced in UI.

---

### 2. Backend Mutual Exclusivity
**Spec**: "Each note can only have a time reminder or a geofence reminder, not both."

**Backend**: According to `api-tester.html`:
- Line 571: "Note: Does NOT clear geofence reminder (both can coexist)"
- Line 594: "Note: Does NOT clear time reminder (both can coexist)"

**Verdict**: ⚠️ **Frontend must enforce**. When setting geofence, clear time reminder. When setting time reminder, clear geofence.

**Implementation**:
```java
// In saveGeofence():
currentNote.setGeofence(geofence);
currentNote.setReminderTime(null); // Enforce mutual exclusivity

// In saveTimeReminder():
currentNote.setReminderTime(instant);
currentNote.setGeofence(null); // Enforce mutual exclusivity
```

---

### 3. Use Case Layer
**Spec**: Has formal use case layer (`AddReminder`, `DeleteReminder`, `TriggerRelevantNote`)

**Your Code**: Direct repository calls from activities

**Verdict**: ✅ **This is fine**. Your architecture is simpler and appropriate for the app size. No need to add a formal use case layer.

---

### 4. ReminderScheduler vs GeofenceManager
**Spec**: Has both `GeofenceManager` and `ReminderScheduler`

**Your Code**: (Neither exists yet)

**Verdict**: ✅ **Simplify**. You only need:
- `GeofenceManager` - Wraps Play Services GeofencingClient
- `GeofenceReceiver` - Handles ENTER/EXIT events
- `RelevantNotesStore` - Tracks active notes

No need for a separate `ReminderScheduler`.

---

### 5. LocationRepository
**Spec**: Separate `LocationRepository` to store note location fields

**Your Code**: Location is just metadata in `Note` model

**Verdict**: ✅ **This is fine**. No need for separate repository. Location is just a string address field on the note.

---

## 🎯 Recommended Architecture (Your Version)

Based on your current codebase structure, here's the recommended architecture:

```
Presentation Layer:
├── NoteEditorActivity
│   ├── Shows reminder chip
│   ├── Opens reminder dialog (time/geofence tabs)
│   ├── Calls NoteRepository.setGeofence()
│   └── Calls GeofenceManager.add()
│
└── MainActivity
    └── RelevantNotesSection (new component)
        ├── Observes RelevantNotesStore
        └── Displays active notes

Data Layer:
├── NoteRepository (existing)
│   ├── setGeofence() ✅
│   ├── clearReminders() ✅
│   └── (No changes needed)
│
└── RelevantNotesStore (NEW)
    ├── add(noteId)
    ├── remove(noteId)
    └── getRelevantNoteIds(): LiveData<List<String>>

Platform Services:
├── GeofenceManager (NEW)
│   ├── add(noteId, lat, lng, radius)
│   ├── remove(noteId)
│   └── Wraps GeofencingClient
│
└── GeofenceReceiver (NEW)
    ├── onReceive()
    ├── Handles ENTER: RelevantNotesStore.add() + show notification
    └── Handles EXIT: RelevantNotesStore.remove()

Utilities:
└── GeocoderHelper (NEW)
    └── geocodeAddress(address): LatLng
```

---

## 📋 Implementation Checklist (Priority Order)

### High Priority (Core Functionality)
- [ ] 1. Add Google Play Services dependency
- [ ] 2. Create `GeofenceRegistrationResponse` DTO
- [ ] 3. Add `listGeofences()` to `ApiService`
- [ ] 4. Create `GeofenceManager.java`
- [ ] 5. Create `GeofenceReceiver.java`
- [ ] 6. Register receiver in `AndroidManifest.xml`
- [ ] 7. Create `RelevantNotesStore.java`
- [ ] 8. Implement geofence save logic in `NoteEditorActivity`
- [ ] 9. Update `updateReminderChip()` for geofence display
- [ ] 10. Update `clearReminder()` to unregister geofences

### Medium Priority (UI Polish)
- [ ] 11. Create `GeocoderHelper.java`
- [ ] 12. Add runtime permission handling
- [ ] 13. Add "Relevant Notes" section to `MainActivity`
- [ ] 14. Add geofence sync on app start
- [ ] 15. Add notification on geofence entry

### Low Priority (Nice to Have)
- [ ] 16. Add 1-hour timeout for time-based reminders
- [ ] 17. Add background location permission (optional)
- [ ] 18. Add error handling for geocoding failures
- [ ] 19. Add loading states for geofence operations

---

## 🚀 Next Steps

1. **Review this document** with your team to ensure everyone understands the gaps
2. **Start with Phase 1** (Infrastructure) from the implementation review
3. **Test incrementally** - Don't try to implement everything at once
4. **Use emulator location spoofing** to test geofence triggers without physically moving

---

## 📞 Questions for Your Team

1. **Geocoding**: Should we use Android's free Geocoder or invest in Google Places API (requires API key)?
2. **Background location**: Do we want geofences to work when app is fully closed? (Requires `ACCESS_BACKGROUND_LOCATION`)
3. **Notification style**: What should the notification look like when entering a geofence?
4. **Mutual exclusivity enforcement**: Backend allows both reminders - should we keep it that way or enforce in frontend only?
5. **Time-based reminders**: Do we also need to implement the 1-hour "Relevant Notes" window for time-based reminders, or is that out of scope?

---

**Last Updated**: Based on codebase analysis on November 4, 2025

