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

import java.util.Date;
import java.util.LinkedList;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.IM;
import com.sir_m2x.messenger.utils.Utils;

/**
 * A tab activity to contain several ChatWindowActivity instances. Used for
 * creating tabbed chat window experience
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ChatWindowTabActivity extends TabActivity
{
	ChatWindowListener listener;
	static String lastOpen = null;
	public static boolean isActive = false;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_window_tabhost);
		this.listener = new ChatWindowListener();
		String friendId;
		if (getIntent().hasExtra(Utils.qualify("friendId")))
		{
			friendId = getIntent().getExtras().getString(Utils.qualify("friendId"));
			lastOpen = friendId;
		}
		else
			friendId = lastOpen;

		if (!MessengerService.getFriendsInChat().keySet().contains(friendId))
			MessengerService.getFriendsInChat().put(friendId, new LinkedList<IM>());
		createTabs(friendId);
		getTabHost().setOnTabChangedListener(this.tabChangedListener);
		
		// reshow the default notification
		MessengerService.getNotificationHelper().showDefaultNotification(false, false);
	}

	private void createTabs(final String friendIdToFocus)
	{
		if (MessengerService.getFriendsInChat().size() == 0)
			finish();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		for (String friendId : MessengerService.getFriendsInChat().keySet())
		{
			intent = new Intent().setClass(this, ChatWindowActivity.class);
			intent.putExtra(Utils.qualify("friendId"), friendId);

			spec = tabHost.newTabSpec(friendId).setIndicator(createTabView(friendId)).setContent(intent);
			tabHost.addTab(spec);
		}

		try
		{
			int id = Integer.parseInt(friendIdToFocus);
			tabHost.setCurrentTab(id);
		}
		catch (Exception ex)
		{
			tabHost.setCurrentTabByTag(friendIdToFocus);
			lastOpen = friendIdToFocus;
		}
	}

	private View createTabView(final String title)
	{
		LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.chat_tab_title, null);
		TextView tv = (TextView) v.findViewById(R.id.txtTabTitle);
		tv.setText(title);
		// ImageView iv = (ImageView) v.findViewById(R.id.imgIsTyping);
		v.setTag(title);
		return v;
	}

	@Override
	protected void onResume()
	{
		isActive = true;
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_IS_TYPING));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_NEW_IM_ADDED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_DESTROY));
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		isActive = false;
		unregisterReceiver(this.listener);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_chat_window, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		if (item.getItemId() == R.id.mnuClose)
		{
			TabHost tabHost = getTabHost();
			String tabTag = tabHost.getCurrentTabTag();
			int tabId = tabHost.getCurrentTab();
			MessengerService.getFriendsInChat().get(tabTag).clear();
			MessengerService.getFriendsInChat().remove(tabTag);
			// tabHost.getTabContentView().findViewById(R.id.listView1).invalidate();
			tabHost.setCurrentTab(0); // TODO **** VERY IMPORTANT
			tabHost.clearAllTabs();

			// Intent intent = new Intent("com.sir_m2x.messenger.DESTROY_WINDOW");
			// intent.putExtra("from", tabTag);
			// sendBroadcast(intent);

			Integer tabToChoose;
			if (tabId == 0)
				tabToChoose = 0;
			else
				tabToChoose = tabId - 1;
			createTabs(tabToChoose.toString());
		}
		else if (item.getItemId() == R.id.mnuBuzz)
		{
			TabHost tabHost = getTabHost();
			String tabTag = tabHost.getCurrentTabTag();
			try
			{
				IM im = new IM(MessengerService.getMyId(), "", new Date(System.currentTimeMillis()), false, true);
				MessengerService.getFriendsInChat().get(tabTag).add(im);
				MessengerService.getSession().sendBuzz(tabTag);
				Intent intent = new Intent();
				intent.setAction(MessengerService.INTENT_BUZZ);
				intent.putExtra(Utils.qualify("from"), MessengerService.getMyId());
				sendBroadcast(intent);

				intent = new Intent();
				intent.setAction(MessengerService.INTENT_NEW_IM);
				intent.putExtra(Utils.qualify("from"), tabTag);
				sendBroadcast(intent);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return true;
	}

	protected class ChatWindowListener extends BroadcastReceiver
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_IS_TYPING))
			{
				String from = intent.getExtras().getString(Utils.qualify("from"));
				View v = getTabHost().findViewWithTag(from);
				if (v == null)
					return;
				ImageView iv = (ImageView) v.findViewById(R.id.imgIsTyping);
				if (intent.getExtras().getBoolean(Utils.qualify("isTyping")))
					iv.setVisibility(View.VISIBLE);
				else
					iv.setVisibility(View.GONE);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_NEW_IM))
			{
				
				String from = intent.getExtras().getString(Utils.qualify("from"));
				View v = getTabHost().findViewWithTag(from);
				if (v == null)
					return;
				if (getTabHost().getCurrentTabTag().equals(from))
					return;
				ImageView iv = (ImageView) v.findViewById(R.id.imgUnreadIm);
				iv.setVisibility(View.VISIBLE);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_NEW_IM_ADDED))
			{
				TabHost tabHost = getTabHost();
				tabHost.setCurrentTab(0);
				tabHost.clearAllTabs();
				createTabs(lastOpen);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
				finish();
		}
	}

	private final OnTabChangeListener tabChangedListener = new OnTabChangeListener()
	{
		@Override
		public void onTabChanged(final String tabId)
		{
			lastOpen = tabId;
			View v = getTabHost().findViewWithTag(tabId);
			ImageView iv = (ImageView) v.findViewById(R.id.imgUnreadIm);
			if (iv == null)
				return;
			
			iv.setVisibility(View.GONE);
		}
	};
}