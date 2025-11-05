package com.example.anchornotes_team3.store;

import android.content.Context;
import android.content.SharedPreferences;

public class TimeRangeStore {
    private static final String PREFS = "time_range_store";
    private static final String KEY_PREFIX = "range_"; // minutes
    private static final int DEFAULT_MINUTES = 60;

    private static TimeRangeStore instance;
    private final SharedPreferences prefs;

    private TimeRangeStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized TimeRangeStore getInstance(Context context) {
        if (instance == null) instance = new TimeRangeStore(context);
        return instance;
    }

    public int getRangeMinutes(String noteId) {
        if (noteId == null) return DEFAULT_MINUTES;
        return prefs.getInt(KEY_PREFIX + noteId, DEFAULT_MINUTES);
    }

    public void setRangeMinutes(String noteId, int minutes) {
        if (noteId == null) return;
        prefs.edit().putInt(KEY_PREFIX + noteId, Math.max(1, minutes)).apply();
    }

    public void clear(String noteId) {
        if (noteId == null) return;
        prefs.edit().remove(KEY_PREFIX + noteId).apply();
    }
}


