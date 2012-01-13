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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.EventLogger;
import com.sir_m2x.messenger.utils.Utils;

/**
 * A simple log window to show various events during the execution of the
 * program.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class LogWindowActivity extends ListActivity
{
	BaseAdapter adapter = new BaseAdapter()
	{

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent)
		{
			View v = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_window_row_friend, parent, false);
			TextView txtMessage = (TextView) v.findViewById(R.id.friendMessageTextView);
			TextView txtTimeStamp = (TextView) v.findViewById(R.id.timeStampTextView);
			ImageView img = (ImageView) v.findViewById(R.id.imgFriendAvatarChat);

			EventLogger.LogFormat log = (EventLogger.LogFormat) getItem(position);
			String id = log.getWho();

			txtMessage.setText(log.eventToHtml());
			txtTimeStamp.setText(log.timeToHtml());

			if (id.equals(MessengerService.getMyId()) && MessengerService.getMyAvatar() != null)
				img.setImageBitmap(MessengerService.getMyAvatar());
			else if (MessengerService.getFriendAvatars().containsKey(id))
				img.setImageBitmap(MessengerService.getFriendAvatars().get(id));
			else if (id.contains("M2X Messenger"))
				img.setImageResource(R.drawable.ic_launcher_noborder);
			else
				img.setImageResource(R.drawable.yahoo_no_avatar);

			return v;
		}

		@Override
		public long getItemId(final int arg0)
		{
			return arg0;
		}

		@Override
		public Object getItem(final int arg0)
		{
			return MessengerService.getEventLog().getEventLog().get(arg0);
		}

		@Override
		public int getCount()
		{
			return MessengerService.getEventLog().getEventLog().size();
		}
	};

	BroadcastReceiver updateReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED) || intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_ON)
					|| intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF))
				LogWindowActivity.this.adapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_window);
		setListAdapter(this.adapter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		unregisterReceiver(this.updateReceiver);
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.log_window_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.mnuClearLog:
				MessengerService.getEventLog().getEventLog().clear();
				this.adapter.notifyDataSetChanged();
				break;
			case R.id.mnuSaveLog:
				Utils.saveEventLog(MessengerService.getEventLog());
				break;
		}
		return true;
	}

}
