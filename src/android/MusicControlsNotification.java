package com.homerours.musiccontrols;

import org.apache.cordova.CordovaInterface;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import android.util.Log;
import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.media.session.MediaSession.Token;

import android.app.NotificationChannel;

public class MusicControlsNotification {
	private Activity cordovaActivity;
	private NotificationManager notificationManager;
	private Notification.Builder notificationBuilder;
	private int notificationID;
	protected MusicControlsInfos infos;
	private Bitmap bitmapCover;
	private String CHANNEL_ID;
	private Token token;

	private String getChannelId(Activity cordovaActivity) {
		SharedPreferences sharedPreferences = cordovaActivity.getSharedPreferences("cordova-plugin-music-controls", Context.MODE_PRIVATE);
		
		String r = sharedPreferences.getString("channel_id", null);
		if (r == null) {
			r = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("channel_id", r);
            editor.commit();
		}
		return r;
	}

	// Public Constructor
	public MusicControlsNotification(Activity cordovaActivity, int id, Token token){
		this.CHANNEL_ID = this.getChannelId(cordovaActivity);
		this.notificationID = id;
		this.cordovaActivity = cordovaActivity;
		Context context = cordovaActivity;
		this.token = token;
		this.notificationManager = (NotificationManager) cordovaActivity.getSystemService(Context.NOTIFICATION_SERVICE);

		// use channelID for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			// The user-visible name of the channel.
			CharSequence name = "Audio Controls";
			// The user-visible description of the channel.
			String description = "Control Playing Audio";

			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel mChannel = new NotificationChannel(this.CHANNEL_ID, name,importance);

			// Configure the notification channel.
			mChannel.setDescription(description);

			// Don't show badges for this channel
			mChannel.setShowBadge(false);

			this.notificationManager.createNotificationChannel(mChannel);
    	}
	}

	// Show or update notification
	public void updateNotification(MusicControlsInfos newInfos){
		// Check if the cover has changed	
		if (!newInfos.cover.isEmpty() && (this.infos == null || !newInfos.cover.equals(this.infos.cover))){
			this.setBitmapCover(newInfos.cover);
		}
		this.infos = newInfos;
		this.createBuilder();
		Notification noti = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, noti);
		this.onNotificationUpdated(noti);
	}

	// Toggle the play/pause button
	public void updateIsPlaying(boolean isPlaying){
		this.infos.isPlaying=isPlaying;
		this.createBuilder();
		Notification noti = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, noti);
		this.onNotificationUpdated(noti);
	}

	// Toggle the dismissable status
	public void updateDismissable(boolean dismissable){
		this.infos.dismissable=dismissable;
		this.createBuilder();
		Notification noti = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, noti);
		this.onNotificationUpdated(noti);
	}

	private void setBitmapCover(String coverURL){
		try{
			this.bitmapCover = getBitmapFromURL(coverURL);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// Get image from url
	private Bitmap getBitmapFromURL(String url){
		if(url.matches("^(https?|ftp)://.*$"))
			// Remote image
			return getBitmapFromRemote(url);
		else{
			// Local image
			return getBitmapFromLocal(url);
		}
	}

	// get Local image
	private Bitmap getBitmapFromLocal(String localURL){
		try {
			Uri uri = Uri.parse(localURL);
			File file = new File(uri.getPath());
			FileInputStream fileStream = new FileInputStream(file);
			BufferedInputStream buf = new BufferedInputStream(fileStream);
			Bitmap myBitmap = BitmapFactory.decodeStream(buf);
			buf.close();
			return myBitmap;
		} catch (Exception ex) {
			try {
				InputStream fileStream = cordovaActivity.getAssets().open("www/" + localURL);
				BufferedInputStream buf = new BufferedInputStream(fileStream);
				Bitmap myBitmap = BitmapFactory.decodeStream(buf);
				buf.close();
				return myBitmap;
			} catch (Exception ex2) {
				ex.printStackTrace();
				ex2.printStackTrace();
				return null;
			}
		}
	}

	// get Remote image
	private Bitmap getBitmapFromRemote(String strURL) {
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			return BitmapFactory.decodeStream(input);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private void createBuilder(){
		Context context = cordovaActivity;
		Notification.Builder builder = new Notification.Builder(context);

		// use channelID for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			builder.setChannelId(this.CHANNEL_ID);
		}

		//Configure builder
		builder.setContentTitle(infos.track);
		if (!infos.artist.isEmpty()){
			builder.setContentText(infos.artist);
		}
		builder.setWhen(0);

		// set if the notification can be destroyed by swiping
		if (infos.dismissable){
			builder.setOngoing(false);
			Intent dismissIntent = new Intent("music-controls-destroy");
			dismissIntent.setPackage(context.getPackageName());
			PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.setDeleteIntent(dismissPendingIntent);
		} else {
			builder.setOngoing(true);
		}
		if (!infos.ticker.isEmpty()){
			builder.setTicker(infos.ticker);
		}

		builder.setSound(null);
		builder.setPriority(Notification.PRIORITY_MAX);

		//If 5.0 >= set the controls to be visible on lockscreen
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			builder.setVisibility(Notification.VISIBILITY_PUBLIC);
		}

		//Set SmallIcon
		boolean usePlayingIcon = infos.notificationIcon.isEmpty();
		if(!usePlayingIcon){
			try {
				int resId = this.getResourceId(infos.notificationIcon, 0);
				if (resId == 0) {
					builder.setSmallIcon(Icon.createWithBitmap(this.getBitmapFromURL(infos.notificationIcon)));
				} else {
					builder.setSmallIcon(resId);
				}
			} catch (Exception ex) {
				usePlayingIcon = true;
			}
		}

		if(usePlayingIcon){
			if (infos.isPlaying){
				builder.setSmallIcon(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play));
			} else {
				builder.setSmallIcon(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause));
			}
		}

		//Set LargeIcon
		if (!infos.cover.isEmpty() && this.bitmapCover != null){
			builder.setLargeIcon(this.bitmapCover);
		}

		//Open app if tapped
		Intent resultIntent = new Intent(context, cordovaActivity.getClass());
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
		builder.setContentIntent(resultPendingIntent);

		//Controls
		int nbControls=0;

		if (infos.hasPrev){
			/* Previous  */
			nbControls++;
			Intent previousIntent = new Intent("music-controls-previous");
			previousIntent.setPackage(context.getPackageName());
			PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, 1, previousIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.addAction(this.getResourceId(infos.prevIcon, android.R.drawable.ic_media_previous), "", previousPendingIntent);
		}
		if (infos.isPlaying){
			/* Pause  */
			nbControls++;
			Intent pauseIntent = new Intent("music-controls-pause");
			pauseIntent.setPackage(context.getPackageName());
			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.addAction(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause), "", pausePendingIntent);
		} else {
			/* Play  */
			nbControls++;
			Intent playIntent = new Intent("music-controls-play");
			playIntent.setPackage(context.getPackageName());
			PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 1, playIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.addAction(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play), "", playPendingIntent);
		}

		if (infos.hasNext){
			/* Next */
			nbControls++;
			Intent nextIntent = new Intent("music-controls-next");
			nextIntent.setPackage(context.getPackageName());
			PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.addAction(this.getResourceId(infos.nextIcon, android.R.drawable.ic_media_next), "", nextPendingIntent);
		}
		if (infos.hasClose){
			/* Close */
			nbControls++;
			Intent destroyIntent = new Intent("music-controls-destroy");
			destroyIntent.setPackage(context.getPackageName());
			PendingIntent destroyPendingIntent = PendingIntent.getBroadcast(context, 1, destroyIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
			builder.addAction(this.getResourceId(infos.closeIcon, android.R.drawable.ic_menu_close_clear_cancel), "", destroyPendingIntent);
		}

		//If 5.0 >= use MediaStyle
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			int[] args = new int[nbControls];
			for (int i = 0; i < nbControls; ++i) {
				args[i] = i;
			}
			builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(args).setMediaSession(this.token));
		}
		this.notificationBuilder = builder;
	}

	private int getResourceId(String name, int fallback){
		try{
			if(name.isEmpty()){
				return fallback;
			}

			int resId = this.cordovaActivity.getResources().getIdentifier(name, "drawable", this.cordovaActivity.getPackageName());
			return resId == 0 ? fallback : resId;
		}
		catch(Exception ex){
			return fallback;
		}
	}

	public void destroy(){
		this.onNotificationDestroyed();

        try { // this is a hack that has to wait on it to come fully out of background mode .. dunno how long to wait, or if there's a notification when it's time
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
		this.notificationManager.cancel(this.notificationID);
	}

	protected void onNotificationUpdated(Notification notification) {}
	protected void onNotificationDestroyed() {}
}
