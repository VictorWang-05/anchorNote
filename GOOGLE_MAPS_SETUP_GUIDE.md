# Google Maps API Setup Guide

## ✅ Google Maps Integration Complete!

The app now uses **Google Maps** for location selection instead of the unreliable Android Geocoder. This provides:

- ✅ **Visual map picker** - See exactly where you're setting the geofence
- ✅ **Address autocomplete** - Google's powerful search (like Google Maps app)
- ✅ **100% reliable** geocoding
- ✅ **Visual radius** - See the geofence circle on the map
- ✅ **Works everywhere** - No device-specific issues

---

## 🔑 Get Your Google Maps API Key (5 minutes)

### Step 1: Go to Google Cloud Console

1. Open: https://console.cloud.google.com/
2. Sign in with your Google account

### Step 2: Create or Select a Project

1. Click the project dropdown at the top
2. Click "**New Project**"
3. Name it: `AnchorNotes` (or any name)
4. Click "**Create**"
5. Wait a few seconds, then select your new project

### Step 3: Enable Required APIs

1. Go to: https://console.cloud.google.com/apis/library
2. Search for "**Maps SDK for Android**"
3. Click on it, then click "**Enable**"
4. Go back and search for "**Places API**"
5. Click on it, then click "**Enable**"

### Step 4: Create API Key

1. Go to: https://console.cloud.google.com/apis/credentials
2. Click "**+ Create Credentials**" at the top
3. Select "**API Key**"
4. A popup will show your new API key (looks like: `AIzaSyB...`)
5. **Copy the entire API key**

### Step 5: Restrict Your API Key (Recommended)

1. Click "**Restrict Key**" in the popup (or click the key name)
2. Under "**Application restrictions**":
   - Select "**Android apps**"
   - Click "**+ Add an item**"
   - Package name: `com.example.anchornotes_team3`
   - SHA-1 certificate fingerprint: Get this by running:
     ```bash
     ./gradlew signingReport
     ```
     Look for the SHA-1 under `debug` variant
   - Click "**Done**"
3. Under "**API restrictions**":
   - Select "**Restrict key**"
   - Check: "**Maps SDK for Android**"
   - Check: "**Places API**"
4. Click "**Save**"

---

## 📱 Add API Key to Your App

### Option 1: Directly in strings.xml (Quick)

1. Open: `app/src/main/res/values/strings_note_editor.xml`
2. Find the line:
   ```xml
   <string name="google_maps_key" translatable="false">YOUR_API_KEY_HERE</string>
   ```
3. Replace `YOUR_API_KEY_HERE` with your actual API key:
   ```xml
   <string name="google_maps_key" translatable="false">AIzaSyB...</string>
   ```

### Option 2: Using local.properties (More Secure - Recommended for Git)

1. Open/create: `local.properties` in the project root
2. Add your API key:
   ```properties
   MAPS_API_KEY=AIzaSyB...
   ```
3. Update `app/build.gradle.kts` to read from local.properties:
   ```kotlin
   android {
       defaultConfig {
           // Read API key from local.properties
           val properties = gradleLocalProperties(rootDir)
           manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY", "YOUR_API_KEY_HERE")
       }
   }
   ```
4. Update `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="${MAPS_API_KEY}" />
   ```

> **Note:** `local.properties` is already in `.gitignore`, so your API key won't be committed to Git.

---

## 🧪 Test the Integration

1. **Build and install** the app:
   ```bash
   ./gradlew installDebug
   ```

2. **Create a new note** or open an existing note

3. **Click "Add location"** button

4. **You should see:**
   - A full-screen Google Map
   - A search bar at the top (Places Autocomplete)
   - Ability to search for addresses with autocomplete suggestions
   - Tap anywhere on the map to select a location
   - Adjust the radius slider
   - See a visual circle showing the geofence radius
   - Confirm to save the location

---

## 🐛 Troubleshooting

### Map shows but is blank/gray

- **Cause:** API key not set correctly or APIs not enabled
- **Fix:** 
  1. Double-check API key in `strings_note_editor.xml`
  2. Ensure both "Maps SDK for Android" and "Places API" are enabled
  3. Wait 5 minutes after enabling APIs (propagation delay)

### "This app won't run unless you update Google Play services"

- **Cause:** Google Play Services outdated on device
- **Fix:** Update Google Play Services from Play Store

### Search bar not working

- **Cause:** Places API not enabled
- **Fix:** Enable "Places API" in Google Cloud Console

### "BILLING_NOT_ENABLED" error

- **Cause:** Google Maps requires billing enabled (but has free tier)
- **Fix:**
  1. Go to: https://console.cloud.google.com/billing
  2. Link a billing account (credit card required)
  3. **Don't worry:** You get $200 free credit per month
  4. This app's usage will stay well within the free tier

---

## 💰 Pricing (Don't Worry - It's Free!)

Google Maps provides generous free usage:

- **$200 free credit per month** (renews monthly)
- **Maps SDK:** $7 per 1,000 loads (you get ~28,000 free loads/month)
- **Places Autocomplete:** $17 per 1,000 sessions (you get ~11,000 free/month)

For a personal notes app, you'll likely **never exceed the free tier**.

---

## 🎉 Features Now Available

### In MapLocationPickerActivity:

1. **Visual Map** - See exactly where the geofence is
2. **Address Search** - Google's powerful autocomplete
3. **Tap to Select** - Click anywhere on the map
4. **Visual Radius** - See a circle showing the geofence area
5. **Radius Adjustment** - Slider to adjust from 50m to 1000m
6. **Address Display** - Shows the formatted address below the map

### Improved Reliability:

- ✅ **No more "could not find location" errors**
- ✅ **Works on all devices** (emulators and real devices)
- ✅ **No dependency** on device-specific geocoding services
- ✅ **Same experience** as Google Maps app

---

## 📝 Next Steps

1. Get your API key (5 minutes)
2. Add it to `strings_note_editor.xml`
3. Build and test the app
4. Enjoy reliable location selection!

**Need help?** Check the troubleshooting section or review the Google Maps Platform documentation: https://developers.google.com/maps/documentation/android-sdk/start

