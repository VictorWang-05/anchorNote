package com.example.anchornotes_team3.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Utility Classes (8 tests) - Jeffrey
 *
 * 21. ThemeUtils - Test theme mode validation (persistence mapping)
 */
@RunWith(RobolectricTestRunner.class)
public class ThemeUtilsTest {

    /**
     * Test 21: ThemeUtils - Test theme color validation (mode mapping/persistence)
     * White-box: validates SharedPreferences int mapping for modes and retrieval via API
     */
    @Test
    public void testThemeModePersistenceAndMapping() {
        Context context = RuntimeEnvironment.getApplication();

        // Set DARK -> expect stored int = 2 and retrieval = DARK
        ThemeUtils.setMode(context, ThemeUtils.ThemeMode.DARK);
        assertEquals(ThemeUtils.ThemeMode.DARK, ThemeUtils.getSavedMode(context));
        assertEquals(2, readRawModeInt(context));

        // Set LIGHT -> expect stored int = 1 and retrieval = LIGHT
        ThemeUtils.setMode(context, ThemeUtils.ThemeMode.LIGHT);
        assertEquals(ThemeUtils.ThemeMode.LIGHT, ThemeUtils.getSavedMode(context));
        assertEquals(1, readRawModeInt(context));

        // FOLLOW_SYSTEM -> expect stored int = 0 and retrieval = FOLLOW_SYSTEM
        ThemeUtils.setMode(context, ThemeUtils.ThemeMode.FOLLOW_SYSTEM);
        assertEquals(ThemeUtils.ThemeMode.FOLLOW_SYSTEM, ThemeUtils.getSavedMode(context));
        assertEquals(0, readRawModeInt(context));
    }

    private static int readRawModeInt(Context context) {
        SharedPreferences sp = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        return sp.getInt("mode", 0);
    }
}


