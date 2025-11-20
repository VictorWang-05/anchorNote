package com.example.anchornotes_team3;

import android.os.Build;
import android.provider.Settings;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;

/**
 * Utility class to disable animations for Espresso tests
 * 
 * Espresso tests can fail if animations are enabled on the device because
 * Espresso tries to interact with views before animations complete.
 * This utility disables all animation scales before tests run.
 * 
 * Uses ADB shell commands which are more reliable than Settings API
 * (which requires WRITE_SETTINGS permission).
 */
public class AnimationUtil {

    private static final float DISABLED = 0.0f;
    private static final float DEFAULT = 1.0f;

    private static float originalAnimationScale;
    private static float originalTransitionScale;
    private static float originalAnimatorDurationScale;

    /**
     * Disable all animations on the device
     * Should be called in @BeforeClass
     */
    public static void disableAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                // Save original values
                originalAnimationScale = Settings.Global.getFloat(
                        InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, DEFAULT);
                originalTransitionScale = Settings.Global.getFloat(
                        InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                        Settings.Global.TRANSITION_ANIMATION_SCALE, DEFAULT);
                originalAnimatorDurationScale = Settings.Global.getFloat(
                        InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                        Settings.Global.WINDOW_ANIMATION_SCALE, DEFAULT);

                // Use UiAutomation to execute shell commands (more reliable, no permission needed)
                executeShellCommand("settings put global animator_duration_scale 0.0");
                executeShellCommand("settings put global transition_animation_scale 0.0");
                executeShellCommand("settings put global window_animation_scale 0.0");
            } catch (Exception e) {
                // If ADB commands fail, try Settings API as fallback
                try {
                    setAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE, DISABLED);
                    setAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE, DISABLED);
                    setAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE, DISABLED);
                } catch (SecurityException se) {
                    // If both methods fail, tests may still work if animations are already disabled
                    System.err.println("Warning: Could not disable animations. Tests may be flaky.");
                }
            }
        }
    }

    /**
     * Re-enable animations on the device
     * Should be called in @AfterClass
     */
    public static void enableAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                // Use ADB shell commands to restore
                executeShellCommand("settings put global animator_duration_scale " + originalAnimationScale);
                executeShellCommand("settings put global transition_animation_scale " + originalTransitionScale);
                executeShellCommand("settings put global window_animation_scale " + originalAnimatorDurationScale);
            } catch (Exception e) {
                // Fallback to Settings API
                try {
                    setAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE, originalAnimationScale);
                    setAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE, originalTransitionScale);
                    setAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE, originalAnimatorDurationScale);
                } catch (SecurityException se) {
                    // Ignore - animations will remain disabled
                }
            }
        }
    }

    private static void setAnimationScale(String setting, float value) {
        try {
            Settings.Global.putFloat(
                    InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                    setting, value);
        } catch (SecurityException e) {
            // Ignore
        }
    }

    private static void executeShellCommand(String command) throws IOException {
        // Use UiAutomation to execute shell commands (requires API 18+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation()
                        .executeShellCommand(command)
                        .close();
            } catch (Exception e) {
                // If UiAutomation fails, try direct Runtime.exec (less reliable)
                java.lang.Process process = Runtime.getRuntime().exec(command);
                try {
                    process.waitFor();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Command interrupted", ie);
                }
            }
        } else {
            // Fallback for older APIs
            java.lang.Process process = Runtime.getRuntime().exec(command);
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Command interrupted", e);
            }
        }
    }
}

