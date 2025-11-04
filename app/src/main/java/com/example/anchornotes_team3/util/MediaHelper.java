package com.example.anchornotes_team3.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for managing media operations: photo capture, audio recording, permissions
 */
public class MediaHelper {

    // Request codes
    public static final int REQUEST_IMAGE_CAPTURE = 1001;
    public static final int REQUEST_IMAGE_PICK = 1002;
    public static final int REQUEST_AUDIO_RECORD = 1003;
    public static final int REQUEST_CAMERA_PERMISSION = 2001;
    public static final int REQUEST_AUDIO_PERMISSION = 2002;
    public static final int REQUEST_LOCATION_PERMISSION = 2003;

    private final Activity activity;
    private Uri currentPhotoUri;
    private File currentAudioFile;
    private MediaRecorder mediaRecorder;
    private long recordingStartTime;

    public MediaHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check if camera permission is granted
     */
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if audio recording permission is granted
     */
    public boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if location permission is granted
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request camera permission
     */
    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    /**
     * Request audio recording permission
     */
    public void requestAudioPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_AUDIO_PERMISSION);
    }

    /**
     * Request location permission
     */
    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    /**
     * Launch camera to capture photo
     * Returns URI where photo will be saved
     */
    public Uri launchCamera() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Create file to save photo
        File photoFile = createImageFile();
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            return currentPhotoUri;
        }
        return null;
    }

    /**
     * Launch gallery to pick photo
     */
    public void launchGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    /**
     * Start audio recording
     */
    public File startAudioRecording() throws IOException {
        // Create audio file first
        currentAudioFile = createAudioFile();
        recordingStartTime = System.currentTimeMillis();

        android.util.Log.d("MediaHelper", "🎙️ Creating audio file: " + currentAudioFile.getAbsolutePath());
        android.util.Log.d("MediaHelper", "🎙️ File exists: " + currentAudioFile.exists());

        // Initialize MediaRecorder
        mediaRecorder = new MediaRecorder();

        try {
            // Set audio source FIRST
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // Set output format SECOND (must be before setOutputFile on some devices)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            // Set audio encoder THIRD
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Set output file LAST (this is critical!)
            mediaRecorder.setOutputFile(currentAudioFile.getAbsolutePath());

            // Prepare the recorder
            mediaRecorder.prepare();

            // Start recording
            mediaRecorder.start();

            android.util.Log.d("MediaHelper", "✅ Audio recording started successfully");
            android.util.Log.d("MediaHelper", "📁 File size after start: " + currentAudioFile.length());
        } catch (Exception e) {
            android.util.Log.e("MediaHelper", "❌ Failed to start audio recording", e);
            // Clean up on failure
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.release();
                } catch (Exception ignored) {}
                mediaRecorder = null;
            }
            if (currentAudioFile != null && currentAudioFile.exists()) {
                currentAudioFile.delete();
                currentAudioFile = null;
            }
            throw new IOException("Failed to start recording: " + e.getMessage(), e);
        }

        return currentAudioFile;
    }

    /**
     * Stop audio recording
     * Returns the URI of the recorded audio file
     */
    public Uri stopAudioRecording() {
        if (mediaRecorder != null) {
            try {
                android.util.Log.d("MediaHelper", "🎙️ Stopping audio recording...");

                // Stop recording
                mediaRecorder.stop();

                android.util.Log.d("MediaHelper", "🎙️ Recording stopped, releasing MediaRecorder");

                // Release the recorder
                mediaRecorder.release();
                mediaRecorder = null;

                // Check file validity
                if (currentAudioFile != null && currentAudioFile.exists()) {
                    long fileSize = currentAudioFile.length();
                    android.util.Log.d("MediaHelper", "✅ Audio file saved: " + currentAudioFile.getAbsolutePath());
                    android.util.Log.d("MediaHelper", "📁 File size: " + fileSize + " bytes");

                    if (fileSize > 0) {
                        return Uri.fromFile(currentAudioFile);
                    } else {
                        android.util.Log.e("MediaHelper", "❌ Audio file is empty (0 bytes)!");
                        return null;
                    }
                } else {
                    android.util.Log.e("MediaHelper", "❌ Audio file not found after recording");
                }
            } catch (RuntimeException e) {
                android.util.Log.e("MediaHelper", "❌ Error stopping audio recording", e);
                // If stop fails, release the recorder anyway
                if (mediaRecorder != null) {
                    try {
                        mediaRecorder.release();
                    } catch (Exception ignored) {}
                    mediaRecorder = null;
                }

                // Check if we got any data despite the error
                if (currentAudioFile != null && currentAudioFile.exists() && currentAudioFile.length() > 0) {
                    android.util.Log.w("MediaHelper", "⚠️ Recording had error but file exists with data, returning it anyway");
                    return Uri.fromFile(currentAudioFile);
                }
            }
        }
        return null;
    }

    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return mediaRecorder != null;
    }

    /**
     * Get the duration of current recording in seconds
     */
    public int getRecordingDuration() {
        if (recordingStartTime > 0) {
            return (int) ((System.currentTimeMillis() - recordingStartTime) / 1000);
        }
        return 0;
    }

    /**
     * Cancel audio recording without saving
     */
    public void cancelAudioRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                // Ignore if stop fails
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (currentAudioFile != null && currentAudioFile.exists()) {
            currentAudioFile.delete();
            currentAudioFile = null;
        }

        recordingStartTime = 0;
        android.util.Log.d("MediaHelper", "🎙️ Audio recording cancelled");
    }

    /**
     * Create a temporary image file
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir("Pictures");
        
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * Create a temporary audio file
     */
    private File createAudioFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String audioFileName = "AUDIO_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir("Audio");
        
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        return File.createTempFile(audioFileName, ".3gp", storageDir);
    }

    /**
     * Get the URI of the last captured photo
     */
    public Uri getCurrentPhotoUri() {
        return currentPhotoUri;
    }
    
    /**
     * Set the current photo URI (used when restoring state)
     */
    public void setCurrentPhotoUri(Uri uri) {
        this.currentPhotoUri = uri;
    }

    /**
     * Get the file of the current audio recording
     */
    public File getCurrentAudioFile() {
        return currentAudioFile;
    }
}

