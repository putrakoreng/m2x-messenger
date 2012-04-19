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

import org.openymsg.network.YahooProtocol;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.datastructures.FriendRequest;
import com.sir_m2x.messenger.helpers.NotificationHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Utils;

/**
 * Lists the friend requests which are currently pending.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class FriendRequestsActivity extends ListActivity
{
	BaseAdapter adapter = new BaseAdapter()
	{
		
		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent)
		{
			View v = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.request_row, parent, false);
			TextView txtId = (TextView) v.findViewById(R.id.txtId);
			TextView txtTimestamp = (TextView) v.findViewById(R.id.txtTimestamp);
			TextView txtMessage = (TextView) v.findViewById(R.id.txtMessage);
			ImageView img = (ImageView) v.findViewById(R.id.imgAvatar);
			
			FriendRequest request = (FriendRequest) getItem(position);
			String id = request.getFrom();
			
			v.setTag(id);
			txtId.setText(request.idToHtml());
			if (request.getMessage() == null || request.getMessage().equals(""))
				txtMessage.setVisibility(View.GONE);
			else
				txtMessage.setText(request.getMessage());
			
			txtTimestamp.setText(request.timeToHtml());

			//TODO load the requester avatar
			//			if (id.equals(MessengerService.getMyId()) && MessengerService.getMyAvatar() != null)
//				img.setImageBitmap(MessengerService.getMyAvatar());
//			else if (MessengerService.getFriendAvatars().containsKey(id))
//				img.setImageBitmap(MessengerService.getFriendAvatars().get(id));
//			else if (id.contains("M2X Messenger"))
//				img.setImageResource(R.drawable.ic_launcher_noborder);
//			else
//				img.setImageResource(R.drawable.yahoo_no_avatar);
			
			return v;
		}
		
		@Override
		public long getItemId(final int position)
		{
			return position;
		}
		
		@Override
		public Object getItem(final int position)
		{
			return MessengerService.getYahooList().getFriendRequests().values().toArray()[position];
		}
		
		@Override
		public int getCount()
		{
			return MessengerService.getYahooList().getFriendRequests().size();
		}
	};
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friend_requests);
		setListAdapter(this.adapter);
		registerForContextMenu(getListView());
		
		if (this.adapter.getCount() == 0)
		{
			TextView txtPrompt = (TextView) findViewById(R.id.txtPrompt);
			txtPrompt.setText("There are no requests to show");
			((LinearLayout.LayoutParams)txtPrompt.getLayoutParams()).gravity = Gravity.CENTER;
			getListView().setVisibility(View.GONE);
		}
		
		// cancel status bar notification if there are no more requests
		if (MessengerService.getYahooList().getFriendRequests().size() == 0)
			MessengerService.getNotificationHelper().cancelNotification(NotificationHelper.NOTIFICATION_CONTACT_REQUEST);
	}
	
	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id)
	{
		openContextMenu(v);
	}
	
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.friend_requests_context, menu);
	}
	
	
	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		int position = menuInfo.position;
		FriendRequest request = (FriendRequest) this.adapter.getItem(position);
		final String id = request.getFrom();
		
		switch (item.getItemId())
		{
			case R.id.mnuPm:
				Intent intent = new Intent(this, ChatWindowPager.class);
				intent.putExtra(Utils.qualify("friendId"), id);
				startActivity(intent);
				break;
			case R.id.mnuAccept:
				try
				{
					MessengerService.getSession().acceptFriendAuthorization(id, YahooProtocol.YAHOO);
					MessengerService.getYahooList().getFriendRequests().remove(id);
					this.adapter.notifyDataSetChanged();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
				
			case R.id.mnuAcceptAndAdd:
				
				try
				{
					final EditText txtGroup = new EditText(this);
					AlertDialog.Builder dlgGetGroupName = new AlertDialog.Builder(this);
					// get the group name for this new contact 
					dlgGetGroupName.setTitle("New contact").setMessage("Enter a group name for this contact:").setView(txtGroup).setPositiveButton("Add", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							String group = txtGroup.getText().toString();
							if (group == null || group.equals(""))
								return;
							
							try
							{
								MessengerService.getSession().acceptFriendAuthorization(id, YahooProtocol.YAHOO);
								MessengerService.getYahooList().getFriendRequests().remove(id);
								FriendRequestsActivity.this.adapter.notifyDataSetChanged();
								
								MessengerService.getSession().sendNewFriendRequest(id, group, YahooProtocol.YAHOO);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							
						}
					}).setNegativeButton("Cancel", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							dialog.cancel();
						}
					}).show();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;

			case R.id.mnuReject:
				final EditText et = new EditText(this);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Enter decline reason (optional):").setView(et).setPositiveButton("OK", new OnClickListener()
				{
					
					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						try
						{
							MessengerService.getSession().rejectFriendAuthorization(id, et.getText().toString());
							MessengerService.getYahooList().getFriendRequests().remove(id);
							FriendRequestsActivity.this.adapter.notifyDataSetChanged();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}						
					}
				}).setNegativeButton("Cancel", new OnClickListener()
				{
					
					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						dialog.cancel();
					}
				}).show();
				
				break;
		}
		
		// cancel status bar notification if there are no more requests
		if (MessengerService.getYahooList().getFriendRequests().size() == 0)
			MessengerService.getNotificationHelper().cancelNotification(NotificationHelper.NOTIFICATION_CONTACT_REQUEST);
		
		if (this.adapter.getCount() == 0)
		{
			TextView txtPrompt = (TextView) findViewById(R.id.txtPrompt);
			txtPrompt.setText("There are no requests to show");
			((LinearLayout.LayoutParams)txtPrompt.getLayoutParams()).gravity = Gravity.CENTER;
			getListView().setVisibility(View.GONE);
		}
		
		return true;
	}
}