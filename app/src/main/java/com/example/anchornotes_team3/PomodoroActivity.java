package com.example.anchornotes_team3;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.content.Intent;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.timer.PomodoroReceiver;
import com.example.anchornotes_team3.util.BottomNavigationHelper;
import com.example.anchornotes_team3.util.ThemeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PomodoroActivity extends AppCompatActivity {

    private static final long ONE_SECOND_MS = 1000L;
    private static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final String PREFS = "pomodoro_prefs";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_INITIAL_MS = "initial_ms";

    private TextView tvTimer;
    private MaterialButton btnStartPause;
    private MaterialButton btnReset;
    private MaterialButton btnSetTime;
    private Chip chipPomodoro;
    private Chip chipShortBreak;
    private Chip chipLongBreak;

    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private long initialDurationMs = 25 * ONE_MINUTE_MS;
    private long timeLeftMs = initialDurationMs;

    private AuthManager authManager;
    private android.os.CountDownTimer uiTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);
        setContentView(R.layout.activity_pomodoro);

        authManager = AuthManager.getInstance(this);
        BottomNavigationHelper.setupBottomNavigation(this, authManager, BottomNavigationHelper.NavItem.POMODORO);

        tvTimer = findViewById(R.id.tv_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnReset = findViewById(R.id.btn_reset);
        btnSetTime = findViewById(R.id.btn_set_time);
        chipPomodoro = findViewById(R.id.chip_pomodoro);
        chipShortBreak = findViewById(R.id.chip_short_break);
        chipLongBreak = findViewById(R.id.chip_long_break);

        restoreState();
        updateTimerText();
        if (isRunning) startUiTicker();

        chipPomodoro.setOnClickListener(v -> setDurationAndReset(25, 0));
        chipShortBreak.setOnClickListener(v -> setDurationAndReset(5, 0));
        chipLongBreak.setOnClickListener(v -> setDurationAndReset(15, 0));

        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnSetTime.setOnClickListener(v -> openSetTimeDialog());

        ensureNotificationPermission();
    }

    private void setDurationAndReset(int minutes, int seconds) {
        cancelExistingTimer();
        initialDurationMs = minutes * ONE_MINUTE_MS + seconds * ONE_SECOND_MS;
        timeLeftMs = initialDurationMs;
        isRunning = false;
        saveState();
        btnStartPause.setText(getString(R.string.pomodoro_start));
        updateTimerText();
    }

    private void startTimer() {
        if (timeLeftMs <= 0) timeLeftMs = initialDurationMs;
        long endTime = System.currentTimeMillis() + timeLeftMs;
        scheduleAlarm(endTime);
        saveRunning(endTime, true);
        startUiTicker();
        isRunning = true;
        btnStartPause.setText(getString(R.string.pomodoro_pause));
    }

    private void pauseTimer() {
        cancelAlarm();
        // compute remaining based on end time
        long end = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_END_TIME, 0);
        if (end > 0) {
            timeLeftMs = Math.max(0, end - System.currentTimeMillis());
        }
        stopUiTicker();
        saveRunning(0, false);
        isRunning = false;
        btnStartPause.setText(getString(R.string.pomodoro_start));
    }

    private void resetTimer() {
        cancelAlarm();
        stopUiTicker();
        timeLeftMs = initialDurationMs;
        isRunning = false;
        saveRunning(0, false);
        btnStartPause.setText(getString(R.string.pomodoro_start));
        updateTimerText();
    }

    private void openSetTimeDialog() {
        final NumberPicker minutesPicker = new NumberPicker(this);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(180);
        minutesPicker.setValue((int) (initialDurationMs / ONE_MINUTE_MS));

        final NumberPicker secondsPicker = new NumberPicker(this);
        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(59);
        secondsPicker.setValue((int) ((initialDurationMs / ONE_SECOND_MS) % 60));

        // Container for two pickers side by side
        final androidx.appcompat.widget.LinearLayoutCompat container = new androidx.appcompat.widget.LinearLayoutCompat(this);
        container.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.HORIZONTAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(minutesPicker, new androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(0, androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT, 1));
        container.addView(secondsPicker, new androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(0, androidx.appcompat.widget.LinearLayoutCompat.LayoutParams.WRAP_CONTENT, 1));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pomodoro_set_time)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    setDurationAndReset(minutesPicker.getValue(), secondsPicker.getValue());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startUiTicker() {
        stopUiTicker();
        long end = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_END_TIME, 0);
        uiTimer = new CountDownTimer(Math.max(0, end - System.currentTimeMillis()), 250L) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = Math.max(0, getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_END_TIME, 0) - System.currentTimeMillis());
                updateTimerText();
            }
            @Override
            public void onFinish() {
                timeLeftMs = 0;
                isRunning = false;
                updateTimerText();
                btnStartPause.setText(getString(R.string.pomodoro_start));
            }
        }.start();
    }

    private void stopUiTicker() {
        if (uiTimer != null) {
            uiTimer.cancel();
            uiTimer = null;
        }
    }

    private void scheduleAlarm(long endTimeMs) {
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(this, PomodoroReceiver.class).setAction(PomodoroReceiver.ACTION_POMODORO_DONE);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                this, 1001, intent,
                android.os.Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT : android.app.PendingIntent.FLAG_UPDATE_CURRENT
        );
        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, endTimeMs, pi);
    }

    private void cancelAlarm() {
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(this, PomodoroReceiver.class).setAction(PomodoroReceiver.ACTION_POMODORO_DONE);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                this, 1001, intent,
                android.os.Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT : android.app.PendingIntent.FLAG_UPDATE_CURRENT
        );
        am.cancel(pi);
    }

    private void saveRunning(long endTime, boolean running) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_RUNNING, running)
                .putLong(KEY_END_TIME, endTime)
                .putLong(KEY_INITIAL_MS, initialDurationMs)
                .apply();
    }

    private void saveState() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putLong(KEY_INITIAL_MS, initialDurationMs)
                .putBoolean(KEY_RUNNING, isRunning)
                .apply();
    }

    private void restoreState() {
        long savedInitial = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_INITIAL_MS, initialDurationMs);
        initialDurationMs = savedInitial;
        boolean running = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_RUNNING, false);
        long end = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_END_TIME, 0);
        if (running && end > 0) {
            timeLeftMs = Math.max(0, end - System.currentTimeMillis());
            isRunning = timeLeftMs > 0;
            if (!isRunning) {
                // finished while away
                timeLeftMs = 0;
            }
        } else {
            timeLeftMs = Math.min(timeLeftMs, initialDurationMs);
            isRunning = false;
        }
        btnStartPause.setText(isRunning ? getString(R.string.pomodoro_pause) : getString(R.string.pomodoro_start));
    }

    private void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    private void updateTimerText() {
        long totalSeconds = timeLeftMs / ONE_SECOND_MS;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String text = String.format("%02d:%02d", minutes, seconds);
        tvTimer.setText(text);
    }

    private void cancelExistingTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelExistingTimer();
        stopUiTicker();
    }
}


