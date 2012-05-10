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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.openymsg.network.SessionState;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.classes.ChatWindowAdapter;
import com.sir_m2x.messenger.classes.IM;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Utils;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ChatWindowFragment extends Fragment
{
	public String friendId;
	private ListView lv;
	private EditText txtMessage = null;
	private boolean isTypingSelf = false;
	private NotificationCanceler notificationCanceler = null;
	public ChatFragmentListener imListener = new ChatFragmentListener();
	public ChatWindowAdapter listAdapter;

	public static ChatWindowFragment newInstance(final String friendId)
	{
		ChatWindowFragment chatWindowFragment = new ChatWindowFragment();
		Bundle bundle = new Bundle();
		bundle.putString("friendId", friendId);
		chatWindowFragment.setArguments(bundle);
		return chatWindowFragment;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.friendId = getArguments().getString("friendId");
		this.listAdapter = new ChatWindowAdapter(getActivity(), this.friendId);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.chat_window, container, false);

		this.notificationCanceler = new NotificationCanceler();
		this.notificationCanceler.start();
		this.friendId = getArguments().getString("friendId");

		Button btnSend = (Button) v.findViewById(R.id.btnSend);
		btnSend.setOnClickListener(this.btnSend_Click);

		this.lv = (ListView) v.findViewById(R.id.listView1);

		this.txtMessage = (EditText) v.findViewById(R.id.txtMessage);

		this.txtMessage.setOnEditorActionListener(this.txtMessage_EditorActionListener);
		this.txtMessage.addTextChangedListener(this.txtMessage_TextChangedListener);
		this.listAdapter.notifyDataSetChanged();
		return v;
	}

	@Override
	public void onResume()
	{
		this.listAdapter.notifyDataSetChanged();
		this.lv.setAdapter(this.listAdapter);

		getActivity().registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		getActivity().registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_INSERT_SMILEY));
		getActivity().registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_LIST_CHANGED));
		getActivity().registerReceiver(this.imListener, new IntentFilter(MessengerService.INTENT_BUZZ));
		super.onResume();
	};

	@Override
	public void onPause()
	{
		getActivity().unregisterReceiver(this.imListener);
		super.onPause();
	};

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
			if (ChatWindowFragment.this.isTypingSelf || ChatWindowFragment.this.txtMessage.getText().toString().equals(""))
			{
				ChatWindowFragment.this.notificationCanceler.reschedule = true; // Reschedule
				// canceling typing
				// notification
				return;
			}
			try
			{
				ChatWindowFragment.this.isTypingSelf = true;
				MessengerService.getSession().sendTypingNotification(ChatWindowFragment.this.friendId, true);
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

		if (im.getMessage().equals("") || MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON)
			return;
		try
		{
			MessengerService.getSession().sendMessage(this.friendId, im.getMessage());
			MessengerService.addIm(this.getActivity(), this.friendId, im);
			this.listAdapter.notifyDataSetChanged();
			TextKeyListener.clear(this.txtMessage.getText());	// to prevent the Android's bug.
			// if we use setText(""), the EditText will not show typed characters after a while!
			// strange bug, I confess
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	private class NotificationCanceler extends Thread
	{
		public boolean reschedule = false;
		public boolean shouldRun = true;

		@Override
		public void run()
		{
			while (this.shouldRun)
			{
				if (ChatWindowFragment.this.isTypingSelf)
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
						e1.printStackTrace();
					}
					try
					{
						ChatWindowFragment.this.isTypingSelf = false;
						MessengerService.getSession().sendTypingNotification(ChatWindowFragment.this.friendId, false);
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

	protected class ChatFragmentListener extends BroadcastReceiver
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if ((intent.getAction().equals(MessengerService.INTENT_NEW_IM) || intent.getAction().equals(MessengerService.INTENT_BUZZ))
					&& intent.getExtras().getString(Utils.qualify("from")).equals(ChatWindowFragment.this.friendId))
				ChatWindowFragment.this.listAdapter.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_LIST_CHANGED))
				ChatWindowFragment.this.listAdapter.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_INSERT_SMILEY)
					&& intent.getExtras().getString(Utils.qualify("from")).equals(ChatWindowFragment.this.friendId))
			{
				String smiley = intent.getExtras().getString(Utils.qualify("symbol"));
				// insert the smiley symbol at the current cursor location
				int start = ChatWindowFragment.this.txtMessage.getSelectionStart();
				int end = ChatWindowFragment.this.txtMessage.getSelectionEnd();

				ChatWindowFragment.this.txtMessage.getText().replace(Math.min(start, end), Math.max(start, end), smiley, 0, smiley.length());
			}
		}
	}
}