/*
 * M2X Messenger, an implementation of the Yahoo Instant Messaging Client based on OpenYMSG for Android.
 * Copyright (C) 2011-2012  Mehran Maghoumi [aka SirM2X], maghoumi@gmail.com
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.utils.Preferences;

/**
 * A helper class to facilitate working with toast notifications
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ToastHelper
{
	/**
	 * A static utility method to ease showing toast notifications.
	 * 
	 * @param context
	 * 		Android context
	 * @param resIcon
	 * 		The resource ID of the Icon to be shown in the toast message.
	 * @param message
	 * 		A string representing the message to be shown
	 * @param duration
	 * 		The duration of the toast (Toast.LONG or Toast.SHORT)
	 */
	public static void showToast(final Context context, final int resIcon, final String message, final int duration)
	{
		if (!Preferences.showToasts)
			return;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.toast_layout, null);
		
		ImageView imgIcon = (ImageView)view.findViewById(R.id.imgIcon);
		TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
		imgIcon.setImageResource(resIcon);
		txtMessage.setText(message);

		Toast toast = new Toast(context);
		if (Preferences.toastGravity != -1)
			toast.setGravity(Preferences.toastGravity, Preferences.xOffset, Preferences.yOffset);
		toast.setDuration(duration);
		toast.setView(view);
		toast.show();
	}
}
