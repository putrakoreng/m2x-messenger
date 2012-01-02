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
package com.sir_m2x.messenger.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.activities.ContactsListActivity;
import com.sir_m2x.messenger.services.MessengerService;

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

	public Context getContext()
	{
		return this.context;
	}

	public void setContext(final Context context)
	{
		this.context = context;
	}

	public NotificationHelper(final Context context, final NotificationManager notificationManager)
	{
		this.context = context;
		this.notificationManager = notificationManager;
	}

	public void showDefaultNotification(final boolean firstTime, final boolean statusChanged)
	{
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
			case NOTATDESK:
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

	public void updateNotification(final String tickerText, final String title, final String message, final int notificationId, final int resId, final Intent intent)
	{
		Notification notification = new Notification(resId, tickerText, System.currentTimeMillis());

		PendingIntent pending = PendingIntent.getActivity(this.context, 0, intent, 0);
		notification.setLatestEventInfo(this.context, title, message, pending);
		this.notificationManager.notify(notificationId, notification);
	}
}
