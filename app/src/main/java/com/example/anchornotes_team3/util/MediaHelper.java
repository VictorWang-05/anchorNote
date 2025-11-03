package com.example.anchornotes_team3.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
     * Note: This is a simplified stub. Real implementation would use MediaRecorder
     */
    public File startAudioRecording() throws IOException {
        currentAudioFile = createAudioFile();
        // TODO: Implement MediaRecorder setup and start
        // For now, return the file that would be used
        return currentAudioFile;
    }

    /**
     * Stop audio recording
     * Returns the URI of the recorded audio file
     */
    public Uri stopAudioRecording() {
        // TODO: Implement MediaRecorder stop and release
        if (currentAudioFile != null && currentAudioFile.exists()) {
            return Uri.fromFile(currentAudioFile);
        }
        return null;
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
     * Get the file of the current audio recording
     */
    public File getCurrentAudioFile() {
        return currentAudioFile;
    }
}

