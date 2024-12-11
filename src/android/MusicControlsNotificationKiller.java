package com.homerours.musiccontrols;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.os.Build;
import android.os.IBinder;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.util.Log;

public class MusicControlsNotificationKiller extends Service {

	private static int NOTIFICATION_ID;
	private NotificationManager mNM;
	private final IBinder mBinder = new KillBinder(this);

    @Override
	public IBinder onBind(Intent intent) {
		NOTIFICATION_ID = intent.getIntExtra("notificationID", 1);
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(NOTIFICATION_ID);
	}

	@Override
	public void onDestroy() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(NOTIFICATION_ID);
	}

    public void setForeground(Notification notification) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(MusicControlsNotificationKiller.NOTIFICATION_ID, notification);
            } else {
				startForeground(MusicControlsNotificationKiller.NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            }
        } catch (Exception e) {
            Log.e("MusicControlsNotificationKiller", "Failed to start foreground service: " + e);
        }
    }

	@SuppressLint("ObsoleteSdkInt")
    public void clearForeground() {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return;
		}

		this.stopForeground(STOP_FOREGROUND_DETACH);
	}
}
