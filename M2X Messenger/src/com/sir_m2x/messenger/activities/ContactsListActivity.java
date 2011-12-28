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
import java.util.ArrayList;
import java.util.TreeMap;

import org.openymsg.network.Status;
import org.openymsg.network.YahooUser;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.FriendsList;
import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Utils;

/**
 * The buddy list of the messenger
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class ContactsListActivity extends ExpandableListActivity
{
	private ContactsListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.contacts_list);
		adapter = new ContactsListAdapter();
		setListAdapter(adapter);
		registerForContextMenu(getExpandableListView());
	}

	@Override
	protected void onResume()
	{
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_IS_TYPING));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_LIST_CHANGED));
		registerReceiver(listener, new IntentFilter(MessengerService.INTENT_DESTROY));
		adapter.notifyDataSetChanged();
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		unregisterReceiver(listener); // to be able to close the activity via
										// INTENT_DESTROY broadcast
		super.onDestroy();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
	{
		Intent intent = new Intent(ContactsListActivity.this, ChatWindowTabActivity.class);
		intent.putExtra(Utils.qualify("friendId"), adapter.getChild(groupPosition, childPosition).toString());
		startActivity(intent);

		return true;
	};

	private class ContactsListAdapter extends BaseExpandableListAdapter
	{
		@Override
		public Object getChild(int groupPosition, int childPosition)
		{
			return FriendsList.getMasterList().get(FriendsList.getMasterList().keySet().toArray()[groupPosition]).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition)
		{
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
		{
			YahooUser user = (YahooUser) getChild(groupPosition, childPosition);
			String friendId = user.getId().toString();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.contacts_list_child_view, parent, false);

			TextView txtId = (TextView) v.findViewById(R.id.txtId);
			TextView txtUnreadCount = (TextView) v.findViewById(R.id.txtNewImCount);
			ImageView imgIsTyping = (ImageView) v.findViewById(R.id.imgIsTyping);
			ImageView imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			ImageView imgAvatar = (ImageView) v.findViewById(R.id.imgAvatar);
			boolean isOnline = false;
			int unread = 0;

			if (MessengerService.getFriendAvatars().containsKey(friendId))
				imgAvatar.setImageBitmap(MessengerService.getFriendAvatars().get(friendId));

			if (MessengerService.getUnreadIMs().containsKey(friendId))
			{
				unread = MessengerService.getUnreadIMs().get(friendId).intValue();
				txtUnreadCount.setText(String.valueOf(unread));
			}

			if (user.getStatus() != Status.OFFLINE)
				isOnline = true;

			String friendIdAndStatus = isOnline ? "<b>" + friendId + "</b>" : "<i>" + friendId + "</i>";
			if (user.getCustomStatus() != null)
				friendIdAndStatus += " -- <small>" + user.getCustomStatusMessage() + "</small>";
			if (user.isPending())
				friendIdAndStatus += "<br/><small><i>[Add request pending]</i></small>";

			txtId.setText(Html.fromHtml(friendIdAndStatus));

			if (!isOnline)
			{
				txtId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				txtId.setTextColor(txtId.getTextColors().withAlpha(130));
				imgAvatar.setAlpha(130);
			}

			txtUnreadCount.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
			imgEnvelope.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);

			imgIsTyping.setVisibility(user.isTyping() == true ? View.VISIBLE : View.GONE);
			v.setTag(friendIdAndStatus);
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition)
		{
			return FriendsList.getMasterList().get(FriendsList.getMasterList().keySet().toArray()[groupPosition]).size();
		}

		@Override
		public Object getGroup(int groupPosition)
		{
			return FriendsList.getMasterList().keySet().toArray()[groupPosition];
		}

		@SuppressWarnings("finally")
		@Override
		public int getGroupCount()
		{
			// TODO What's with the random NullPoinderException??!
			try
			{
				return FriendsList.getMasterList().keySet().size();

			}
			catch (Exception ex)
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				finally
				{
					return FriendsList.getMasterList().keySet().size();
				}

			}
		}

		@Override
		public long getGroupId(int groupPosition)
		{
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
		{
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.contacts_list_group_view, parent, false);

			TextView txtGroupName = (TextView) v.findViewById(R.id.txtGroupName);
			TextView txtGroupCount = (TextView) v.findViewById(R.id.txtGroupCount);
			// TextView txtNewImCount = (TextView)
			// v.findViewById(R.id.txtNewImCount);
			ImageView imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			int unreadCount = 0;

			String groupName = getGroup(groupPosition).toString();
			txtGroupName.setText(groupName);

			// Integer totalCount =
			// MessengerService.getSession().getFriendList().get(groupName).size();
			Integer totalCount = FriendsList.getMasterList().get(groupName).size();
			Integer onlineCount = 0;

			for (YahooUser user : FriendsList.getMasterList().get(groupName))
			{
				if (user.getStatus() != Status.OFFLINE)
					onlineCount++;
				if (MessengerService.getUnreadIMs().keySet().contains(user.getId()))
					unreadCount += MessengerService.getUnreadIMs().get(user.getId());
			}

			txtGroupCount.setText(onlineCount + "/" + totalCount);
			imgEnvelope.setVisibility(unreadCount == 0 ? View.GONE : View.VISIBLE);
			// txtNewImCount.setText(unreadCount);
			// txtNewImCount.setVisibility(unreadCount == 0 ? View.GONE :
			// View.VISIBLE);

			return v;
		}

		@Override
		public boolean hasStableIds()
		{
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition)
		{
			return true;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.contacts_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.mnuSignOut:
				stopService(new Intent(this, MessengerService.class));
				finish();
				break;

			case R.id.mnuShowConversations:
				if (MessengerService.getFriendsInChat().isEmpty())
					break;
				startActivity(new Intent(this, ChatWindowTabActivity.class));
				break;

			case R.id.mnuLog:
				startActivity(new Intent(this, LogWindowActivity.class));
				break;
			case R.id.mnuStatus:
				startActivityForResult((new Intent(this, ChangeStatusActivity.class)), 0);
				break;
			default:
				break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) // Show the pop-up menu for a friend
		{
			YahooUser user = (YahooUser) adapter.getChild(group, child);
			menu.setHeaderTitle(user.getId());
			menu.add(1, 1, 0, "Remove from friends"); // the first choice is to remove this friend

			int i = 2;
			TreeMap<String, ArrayList<YahooUser>> groups = FriendsList.getMasterList();

			for (String groupName : groups.keySet()) // generate the list of all groups
			{
				MenuItem item = menu.add(2, i, 0, groupName);
				for (String g : user.getGroupIds())
				{
					if (g.equals(groupName))
					{
						item.setChecked(true); // check off the default group
						break;
					}
				}
				i++;
			}

			menu.setGroupCheckable(2, true, true);
		}
		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) // Show the pop-up menu for a group
		{
			String groupName = adapter.getGroup(group).toString();
			menu.setHeaderTitle(groupName);
			menu.add(1, 1, 0, "Add a friend");
			menu.add(1, 2, 0, "Delete");
			menu.add(1, 3, 0, "Rename");
		}
	};

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) //a child item is selected
		{
			final YahooUser user = (YahooUser) adapter.getChild(group, child);
			final String sourceGroup = user.getGroup();

			if (item.getItemId() == 1) //Delete
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Remove friend");
				builder.setMessage("Are you sure you want to remove \"" + user.getId() + "\" from your friends list?");
				builder.setCancelable(true);
				builder.setPositiveButton("Yes", new OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						try
						{
							FriendsList.removeFriendFromGroup(user.getId(), sourceGroup);
							adapter.notifyDataSetChanged();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				});

				builder.setNegativeButton("No", new OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				});

				builder.create().show();
				return true;
			}

			String destinationGroup = item.getTitle().toString();

			try
			{
				FriendsList.moveFriend(user.getId(), sourceGroup, destinationGroup);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) //a group item is selected
		{

			final String groupName = adapter.getGroup(group).toString();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			switch (item.getItemId())
			{
				case 1://add friend
					final EditText txtId = new EditText(this);
					builder.setTitle("Add friend").setMessage("Enter the ID of the person you want to add:").setView(txtId).setPositiveButton("Add", new OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String friendId = txtId.getText().toString();
							if (friendId == null || friendId.isEmpty())
								return;
							try
							{
								FriendsList.addFriend(friendId, groupName);
								adapter.notifyDataSetChanged();
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					}).show();
					break;
				case 2: //delete this group and all its contents
					builder.setTitle("Confrim delete").setMessage("Are you sure you want to delete group \"" + groupName + "\"?").setPositiveButton("Yes", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							try
							{
								FriendsList.deleteGroup(groupName);
								adapter.notifyDataSetChanged();
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}

						}
					}).setNegativeButton("No", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					}).show();

					break;
				case 3: //rename this group
					final EditText txtGroupNewName = new EditText(this);
					builder.setTitle("Group rename").setMessage("Enter a new name for this group:").setView(txtGroupNewName).setPositiveButton("OK", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String groupNewName = txtGroupNewName.getText().toString();
							if (groupName == null || groupName.isEmpty())
								return;
							try
							{
								FriendsList.renameGroup(groupName, groupNewName);
								adapter.notifyDataSetChanged();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}).setNegativeButton("Cancel", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					}).show();
					break;

			}
		}

		adapter.notifyDataSetChanged();
		return true;
	};

	BroadcastReceiver listener = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_NEW_IM) || intent.getAction().equals(MessengerService.INTENT_IS_TYPING)
					|| intent.getAction().equals(MessengerService.INTENT_BUZZ) || intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_ON)
					|| intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF) || intent.getAction().equals(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED)
					|| intent.getAction().equals(MessengerService.INTENT_LIST_CHANGED))
				adapter.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
			{
				finish();
			}
		}
	};

}