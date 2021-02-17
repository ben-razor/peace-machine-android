package com.example.peacemachine;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.InputStream;

public class AudioService extends Service {
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private SineSynth mAudio;

    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public void onCreate() {
        mAudio = new SineSynth();
        mAudio.start();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    public void setLPFreq(float val, float t) { mAudio.setLPFreq(val, t); }

    public void setHPFreq(float val) {
        mAudio.setHPFreq(val);
    }

    public void setVolume(float val, float t) {
        mAudio.setVolume(val, t);
    }

    public void changeVibe(VibeInfo vibeInfo, InputStream iStr) {
        mAudio.changeVibe(vibeInfo, iStr);
    }

    @Override
    public void onDestroy() {
        Log.d("AudioService", "destroy");
        destroy();
    }

    public void destroy() {
        mAudio.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    private void showNotification() {
        CharSequence text = getText(R.string.local_service_started);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mNM.notify(NOTIFICATION, notification);

        MockDatabase.MockNotificationData mnd = MockDatabase.getBigTextStyleData();
        String channelId = NotificationUtil.createNotificationChannel(this, mnd);

        Notification notificationMain =
                new Notification.Builder(this, channelId)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        startForeground(31324, notificationMain);
    }
}
