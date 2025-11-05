package com.example.anchornotes_team3.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    private static final String PREFS = "theme_prefs";
    private static final String KEY_MODE = "mode"; // 0 follow, 1 light, 2 dark

    public enum ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

    public static void applySavedTheme(Context context) {
        ThemeMode mode = getSavedMode(context);
        applyMode(mode);
    }

    public static void setMode(Context context, ThemeMode mode) {
        saveMode(context, mode);
        applyMode(mode);
    }

    public static ThemeMode getSavedMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int v = sp.getInt(KEY_MODE, 0);
        if (v == 1) return ThemeMode.LIGHT;
        if (v == 2) return ThemeMode.DARK;
        return ThemeMode.FOLLOW_SYSTEM;
    }

    private static void saveMode(Context context, ThemeMode mode) {
        int v = 0;
        if (mode == ThemeMode.LIGHT) v = 1;
        else if (mode == ThemeMode.DARK) v = 2;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MODE, v)
            .apply();
    }

    private static void applyMode(ThemeMode mode) {
        int nightMode;
        switch (mode) {
            case LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case FOLLOW_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}


