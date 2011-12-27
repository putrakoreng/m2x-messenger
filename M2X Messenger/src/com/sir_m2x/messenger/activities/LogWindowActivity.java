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
package com.sir_m2x.messenger.activities;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.EventLogger;

/**
 * A simple log window to show various events during the execution of the program.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class LogWindowActivity extends ListActivity
{
	BaseAdapter adapter = new BaseAdapter()
	{

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.chat_window_row_friend, parent, false);
			TextView txtMessage = (TextView) v.findViewById(R.id.friendMessageTextView);
			TextView txtTimeStamp = (TextView) v.findViewById(R.id.timeStampTextView);
			ImageView img = (ImageView) v.findViewById(R.id.imgFriendAvatarChat);
			
			EventLogger.LogFormat log = (EventLogger.LogFormat)getItem(position);
			String id = log.getWho();
			
			txtMessage.setText(log.eventToHtml());
			txtTimeStamp.setText(log.timeToHtml());
			
			if (id.equals(MessengerService.getMyId()) && MessengerService.getMyAvatar()!=null)
				img.setImageBitmap(MessengerService.getMyAvatar());
			else if (MessengerService.getFriendAvatars().containsKey(id))
				img.setImageBitmap(MessengerService.getFriendAvatars().get(id));
			else
				img.setImageResource(R.drawable.yahoo_no_avatar);
			
			
			return v;
		}

		@Override
		public long getItemId(int arg0)
		{
			return arg0;
		}

		@Override
		public Object getItem(int arg0)
		{
			return MessengerService.getEventLog().getEventLog().get(arg0);
		}

		@Override
		public int getCount()
		{
			return MessengerService.getEventLog().getEventLog().size();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_window);
		setListAdapter(adapter);
	}

}
