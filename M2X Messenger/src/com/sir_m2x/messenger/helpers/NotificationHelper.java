/*
 * M2X Messenger, an implementation of the Yahoo Instant Messaging Client based on OpenYMSG for Android.
 * Copyright (C) 2011  Mehran Maghoumi [aka SirM2X], maghoumi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sir_m2x.messenger.helpers;

import org.openymsg.network.SessionState;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.activities.ContactsListActivity;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;

/**
 * This is a helper class to show and manage various notifications.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class NotificationHelper
{
	/*
	 * Notification constants
	 */
	public static final int NOTIFICATION_SIGNED_IN = 0;
	public static final int NOTIFICATION_CONTACT_REQUEST = 1;
	public static final int NOTIFICATION_CONTACT_ACCEPTED = 2;
	public static final int NOTIFICATION_CONTACT_REJECTED = 3;

	private Context context = null;
	private NotificationManager notificationManager = null;

	/**
	 * Public accessor 
	 */
	public Context getContext()
	{
		return this.context;
	}

	/**
	 * Public accessor 
	 */
	public void setContext(final Context context)
	{
		this.context = context;
	}

	public NotificationHelper(final Context context, final NotificationManager notificationManager)
	{
		this.context = context;
		this.notificationManager = notificationManager;
	}

	/**
	 * Shows the default notification in the status bar.
	 * @param firstTime
	 * 		Indicated weather it is the first time the notification is being shown
	 * @param statusChanged
	 * 		Indicated weather this notification has been shown to indicate that user's status has changed
	 */
	public void showDefaultNotification(final boolean firstTime, final boolean statusChanged)
	{
		if (MessengerService.getSession().getSessionStatus() == SessionState.UNSTARTED)
			return;

		Notification notify = null;
		String currentStatus;
		int notificationIcon = R.drawable.ic_stat_notify;

		switch (MessengerService.getSession().getStatus())
		{
			case AVAILABLE:
				currentStatus = "Online";
				break;
			case INVISIBLE:
				currentStatus = "Invisible";
				notificationIcon = R.drawable.ic_stat_notify_invisible;
				break;
			case IDLE:
				currentStatus = "Away";
				notificationIcon = R.drawable.ic_stat_notify_away;
				break;
			case CUSTOM:
				currentStatus = MessengerService.getSession().getCustomStatusMessage();
				if (MessengerService.getSession().isCustomBusy())
					notificationIcon = R.drawable.ic_stat_notify_busy;
				break;
			default:
				currentStatus = "Busy";
				notificationIcon = R.drawable.ic_stat_notify_busy;
		}

		if (firstTime)
			notify = new Notification(notificationIcon, "Connected to Yahoo!", System.currentTimeMillis());
		else if (statusChanged)
			notify = new Notification(notificationIcon, "New status has been set: " + currentStatus, System.currentTimeMillis());
		else
			notify = new Notification(notificationIcon, null, System.currentTimeMillis());

		notify.flags = Notification.FLAG_ONGOING_EVENT;
		Intent intent2 = new Intent(this.context, ContactsListActivity.class);
		PendingIntent i = PendingIntent.getActivity(this.context, 0, intent2, 0);

		notify.setLatestEventInfo(this.context, "M2X Messenger", MessengerService.getMyId() + " -- " + currentStatus, i);
		this.notificationManager.notify(NOTIFICATION_SIGNED_IN, notify);
	}

	public void updateNotification(final String tickerText, final String title, final String message, final int notificationId, final int icon, final Intent intent,
			final int notificationCount, final int flags, final boolean defaults)
	{
		Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		if (defaults)
			notification.defaults = Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND;
		notification.audioStreamType = Preferences.audibleStream;
		notification.flags = flags | Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xFFFFFFFF;
		notification.ledOnMS = 1;
		notification.ledOffMS = 0;
		PendingIntent pending = PendingIntent.getActivity(this.context, 0, intent, 0);
		notification.setLatestEventInfo(this.context, title, message, pending);
		notification.number = notificationCount > 1 ? notificationCount : 0;
		this.notificationManager.notify(notificationId, notification);
	}
	
	public void playAudio(final Uri uri, final int streamType, final boolean isIm)
	{
		if (Preferences.audibles.equals(Preferences.AUDIBLE_DONT_PLAY))
			return;
		else if (Preferences.audibles.equals(Preferences.AUDIBLE_PLAY_IM))
		{
			if (!isIm)
				return;
		}
		else if (Preferences.audibles.equals(Preferences.AUDIBLE_PLAY_STATUS))
			if (isIm)
				return;

		MediaPlayer mp = new MediaPlayer();
		try
		{
			mp.setDataSource(this.context, uri);
			mp.setAudioStreamType(streamType);
			mp.prepare();
			mp.start();
		}
		catch (Exception e)
		{
			Log.w("M2X-Messenger", "Unable to play " + uri);
		}

	}
	
	public void vibrate(final int duration)
	{
		if (Preferences.vibration.equals(Preferences.VIBRATE_OFF))
			return;

		AudioManager am = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
		if (!am.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION))
			return;

		Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(duration);
	}
}
