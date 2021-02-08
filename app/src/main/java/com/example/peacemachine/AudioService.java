package com.example.peacemachine;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class AudioService extends Service {
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private SineSynth mSineSynth;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    public AudioService() {
    }

    @Override
    public void onCreate() {
        mSineSynth = new SineSynth();
        mSineSynth.start();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    public void setLPFreq(float val) { mSineSynth.setLPFreq(val); }

    public void setHPFreq(float val) {
        mSineSynth.setHPFreq(val);
    }

    public void setVolume(float val) { mSineSynth.getAmplitudePort().set(val); }

    @Override
    public void onDestroy() {
        mSineSynth.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
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
