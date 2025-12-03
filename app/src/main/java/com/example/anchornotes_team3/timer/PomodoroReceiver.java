package com.example.anchornotes_team3.timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.anchornotes_team3.PomodoroActivity;
import com.example.anchornotes_team3.R;

public class PomodoroReceiver extends BroadcastReceiver {

    public static final String ACTION_POMODORO_DONE = "com.example.anchornotes_team3.ACTION_POMODORO_DONE";
    public static final String CHANNEL_ID = "pomodoro_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_POMODORO_DONE.equals(intent.getAction())) {
            return;
        }

        // Show completion notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Pomodoro timer notifications");
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, PomodoroActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.pomodoro_title))
                .setContentText("Time's up! Take a break.")
                .setContentIntent(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify(1010, builder.build());
    }
}


