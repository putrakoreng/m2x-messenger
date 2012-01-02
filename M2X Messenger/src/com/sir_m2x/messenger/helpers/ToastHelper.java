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

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sir_m2x.messenger.R;

/**
 * A helper class to facilitate working with toast notifications
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ToastHelper
{
	public static void showToast(final Context context, final int resIcon, final String message, final int Duration)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.toast_layout, null);
		
		ImageView imgIcon = (ImageView)view.findViewById(R.id.imgIcon);
		TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
		imgIcon.setImageResource(resIcon);
		txtMessage.setText(message);

		Toast toast = new Toast(context);
		toast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
		toast.setDuration(Duration);
		toast.setView(view);
		toast.show();
	}
}
