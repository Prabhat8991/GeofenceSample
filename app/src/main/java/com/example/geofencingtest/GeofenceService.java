package com.example.geofencingtest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class GeofenceService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {

            NotificationChannel chan = new NotificationChannel(
                    getApplicationContext().getPackageName(),
                    "My Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                    this, getApplicationContext().getPackageName());
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running on foreground")
                    .setAutoCancel(false)
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setChannelId(getApplicationContext().getPackageName())
                    .build();
            startForeground(1, notification);
        }
        return START_NOT_STICKY;
    }
}

