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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.IM;
import com.sir_m2x.messenger.utils.Utils;

/**
 * A single chat window with all the features
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ChatWindowActivity extends Activity
{
	public String friendId;
	private ChatWindowListener imListener;
	private ListView lv;
	private EditText txtMessage = null;
	private boolean isTypingSelf = false;
	private NotificationCanceler notificationCanceler = null;

	@Override
	protected void onResume()
	{
		this.listAdapter.notifyDataSetChanged();
		this.lv.setAdapter(this.listAdapter);

		registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_DESTROY));

		// indicating that all the messages from this contact has been read
		synchronized (MessengerService.getUnreadIMs())
		{
			HashMap<String, Integer> unreadIMs = MessengerService.getUnreadIMs();
			unreadIMs.remove(this.friendId);
		}

		super.onResume();
	}

	@Override
	protected void onPause()
	{
		unregisterReceiver(this.imListener);
		super.onPause();
	}

	@Override
	protected void onCreate(final android.os.Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_window);
		this.notificationCanceler = new NotificationCanceler();
		this.notificationCanceler.start();

		Button btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(this.btnSend_Click);
		this.imListener = new ChatWindowListener();
		this.friendId = getIntent().getExtras().getString(Utils.qualify("friendId"));

		this.lv = (ListView) findViewById(R.id.listView1);

		this.txtMessage = (EditText) findViewById(R.id.txtMessage);

		this.txtMessage.setOnEditorActionListener(this.txtMessage_EditorActionListener);
		this.txtMessage.addTextChangedListener(this.txtMessage_TextChangedListener);
	}

	/*
	 * Handle "enter" on the TextView
	 */
	OnEditorActionListener txtMessage_EditorActionListener = new OnEditorActionListener()
	{

		@Override
		public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event)
		{
			sendIM();
			return true;
		}
	};
	/*
	 * Handle sending typing notifications to the other party
	 */
	TextWatcher txtMessage_TextChangedListener = new TextWatcher()
	{

		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
		{
			if (ChatWindowActivity.this.isTypingSelf || ChatWindowActivity.this.txtMessage.getText().toString().equals(""))
			{
				ChatWindowActivity.this.notificationCanceler.reschedule = true; // Reschedule
														// canceling typing
														// notification
				return;
			}
			try
			{
				ChatWindowActivity.this.isTypingSelf = true;
				MessengerService.getSession().sendTypingNotification(ChatWindowActivity.this.friendId, true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		}

		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
		{
		}

		@Override
		public void afterTextChanged(final Editable s)
		{
		}
	};

	@Override
	protected void onDestroy()
	{
		// unregisterReceiver(imListener);
		this.listAdapter.notifyDataSetChanged();
		this.notificationCanceler.shouldRun = false;
		super.onDestroy();
	};

	View.OnClickListener btnSend_Click = new OnClickListener()
	{

		@Override
		public void onClick(final View v)
		{
			sendIM();
		}
	};

	private void sendIM()
	{
		String sender = MessengerService.getSession().getLoginID().getId();
		String message = this.txtMessage.getText().toString();
		Date timeStamp = Calendar.getInstance().getTime();
		IM im = new IM(sender, message, timeStamp);

		if (im.getMessage().equals(""))
			return;
		try
		{
			MessengerService.getSession().sendMessage(ChatWindowActivity.this.friendId, im.getMessage());
			MessengerService.getFriendsInChat().get(this.friendId).add(im);
			this.listAdapter.notifyDataSetChanged();
			// lv.smoothScrollToPosition(MessengerService.getFriendsInChat()
			// .get(friendId).size() - 1);
			this.txtMessage.setText("");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	protected class ChatWindowListener extends BroadcastReceiver
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if ((intent.getAction().equals(MessengerService.INTENT_NEW_IM) || intent.getAction().equals(MessengerService.INTENT_BUZZ))
					&& intent.getExtras().getString(Utils.qualify("from")).equals(ChatWindowActivity.this.friendId))
			{
				ChatWindowActivity.this.listAdapter.notifyDataSetChanged();

				synchronized (MessengerService.getUnreadIMs())
				{
					HashMap<String, Integer> unreadIMs = MessengerService.getUnreadIMs();
					unreadIMs.remove(ChatWindowActivity.this.friendId);
				}
			}
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
				finish();
		}
	}

	/*
	 * An adapter to show the messages of the current conversation in the a
	 * ListView widget
	 */

	BaseAdapter listAdapter = new BaseAdapter()
	{

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent)
		{
			synchronized (MessengerService.getFriendsInChat())
			{

				String sender = "";
				boolean isOfflineMessage = false;

				try
				//TODO fix?
				{
					sender = MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position).getSender();
					isOfflineMessage = MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position).isOfflineMessage();

				}
				catch (Exception e)
				{
					e.toString();
				}
				boolean isSenderSelf = sender.equals(MessengerService.getMyId());

				LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v;

				if (isSenderSelf)
					v = inflator.inflate(R.layout.chat_window_row_self, parent, false);
				else
				{
					v = inflator.inflate(R.layout.chat_window_row_friend, parent, false);
					LinearLayout l = (LinearLayout) v.findViewById(R.id.layoutHolder);
					if (isOfflineMessage)
						l.setBackgroundColor(Color.parseColor("#FFFFF0"));
					else if (MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position).isBuzz())
						l.setBackgroundColor(Color.parseColor("#FFF0F0"));

				}

				TextView tv = (TextView) v.findViewById(R.id.friendMessageTextView);
				tv.setText(MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position).toHtml());
				TextView timeStamp = (TextView) v.findViewById(R.id.timeStampTextView);
				timeStamp.setText(MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position).getTime());
				ImageView iv = (ImageView) v.findViewById(R.id.imgFriendAvatarChat);

				// Filling the correct avatar

				if (isSenderSelf && MessengerService.getMyAvatar() != null)
					iv.setImageBitmap(MessengerService.getMyAvatar());
				else if (!isSenderSelf)
				{
					Bitmap friendAvatar = MessengerService.getFriendAvatars().get(ChatWindowActivity.this.friendId);
					if (friendAvatar != null)
						iv.setImageBitmap(friendAvatar);
				}
				return v;
			}
		}

		@Override
		public long getItemId(final int position)
		{
			return position;
		}

		@Override
		public Object getItem(final int position)
		{
			synchronized (MessengerService.getFriendsInChat())
			{

				return MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).get(position);
			}
		}

		@Override
		public int getCount()
		{
			synchronized (MessengerService.getFriendsInChat())
			{
				if (MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId) != null)
					return MessengerService.getFriendsInChat().get(ChatWindowActivity.this.friendId).size();
				return 0;
			}
		}
	};

	private class NotificationCanceler extends Thread
	{
		public boolean reschedule = false;
		public boolean shouldRun = true;

		@Override
		public void run()
		{
			while (this.shouldRun)
			{
				if (ChatWindowActivity.this.isTypingSelf)
				{
					while (this.reschedule)
						try
						{
							this.reschedule = false;
							Thread.sleep(1000);
							continue;
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}

					try
					{
						Thread.sleep(2000);
					}
					catch (InterruptedException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try
					{
						ChatWindowActivity.this.isTypingSelf = false;
						MessengerService.getSession().sendTypingNotification(ChatWindowActivity.this.friendId, false);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		startActivity(new Intent(this, ContactsListActivity.class));
		super.onBackPressed();
	};

}