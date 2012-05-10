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
package com.sir_m2x.messenger.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.classes.LogWindowAdapter;
import com.sir_m2x.messenger.services.MessengerService;
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
	LogWindowAdapter adapter = new LogWindowAdapter(this);
	
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

	@Override
	protected void onResume()
	{
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.updateReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		unregisterReceiver(this.updateReceiver);
		super.onPause();
	}

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
