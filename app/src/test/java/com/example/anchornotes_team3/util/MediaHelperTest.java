package com.example.anchornotes_team3.util;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Utility Classes (8 tests) - Jeffrey
 *
 * 23. MediaHelper - Test file extension validation (white-box via reflection)
 */
@RunWith(RobolectricTestRunner.class)
public class MediaHelperTest {

    /**
     * Test 23: MediaHelper - Validate created file extensions for image/audio
     * White-box: uses reflection to invoke private file-creation methods
     */
    @Test
    public void testFileExtensionValidation() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MediaHelper helper = new MediaHelper(activity);

        // Access private createImageFile()
        Method createImageFile = MediaHelper.class.getDeclaredMethod("createImageFile");
        createImageFile.setAccessible(true);
        File img = (File) createImageFile.invoke(helper);
        assertNotNull(img);
        assertTrue(img.getName().toLowerCase().endsWith(".jpg"));
        assertTrue(img.getParentFile().exists());

        // Access private createAudioFile()
        Method createAudioFile = MediaHelper.class.getDeclaredMethod("createAudioFile");
        createAudioFile.setAccessible(true);
        File aud = (File) createAudioFile.invoke(helper);
        assertNotNull(aud);
        assertTrue(aud.getName().toLowerCase().endsWith(".3gp"));
        assertTrue(aud.getParentFile().exists());
    }
}


