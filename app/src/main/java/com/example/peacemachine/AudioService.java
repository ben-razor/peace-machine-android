package com.example.peacemachine;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.jsyn.data.FloatSample;

public class AudioService extends Service {
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private Audio mAudio;
    public boolean audioInitialized;

    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public void onCreate() {
        mAudio = new Audio();
        mAudio.start();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    public Audio getAudio() {
        return mAudio;
    }

    @Override
    public void onDestroy() {
        Log.d("AudioService", "onDestroy");
        destroy();
    }

    public void destroy() {
        Log.d("AudioService", "destroy");
        mAudio.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    private void showNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        MockDatabase.MockNotificationData mnd = MockDatabase.getBigTextStyleData();
        String channelId = NotificationUtil.createNotificationChannel(this, mnd);

        Notification notificationMain =
                new Notification.Builder(this, channelId)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .setOngoing(true)
                        .build();

        startForeground(31324, notificationMain);
    }
}
