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
import org.openymsg.network.StealthStatus;
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
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ContactsListActivity extends ExpandableListActivity
{
	private ContactsListAdapter adapter;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.contacts_list);
		this.adapter = new ContactsListAdapter();
		setListAdapter(this.adapter);
		registerForContextMenu(getExpandableListView());
	}

	@Override
	protected void onResume()
	{
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_IS_TYPING));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_LIST_CHANGED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_DESTROY));
		this.adapter.notifyDataSetChanged();
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
		unregisterReceiver(this.listener); // to be able to close the activity via
											// INTENT_DESTROY broadcast
		super.onDestroy();
	}

	@Override
	public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id)
	{
		Intent intent = new Intent(ContactsListActivity.this, ChatWindowTabActivity.class);
		intent.putExtra(Utils.qualify("friendId"), this.adapter.getChild(groupPosition, childPosition).toString());
		startActivity(intent);

		return true;
	};

	private class ContactsListAdapter extends BaseExpandableListAdapter
	{
		@Override
		public Object getChild(final int groupPosition, final int childPosition)
		{
			return FriendsList.getMasterList().get(FriendsList.getMasterList().keySet().toArray()[groupPosition]).get(childPosition);
		}

		@Override
		public long getChildId(final int groupPosition, final int childPosition)
		{
			return childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView, final ViewGroup parent)
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
			ImageView imgBulb = (ImageView) v.findViewById(R.id.imgBulb);
			boolean isOnline = false;
			boolean isBusy = false;
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

			String friendIdAndStatus = isOnline ? Utils.toBold(friendId) : friendId;
			if (user.getStatus() == Status.BUSY)
				isBusy = true;

			if (user.getStealth() == StealthStatus.STEALTH_PERMENANT)
				friendIdAndStatus = Utils.toItalic(friendIdAndStatus);

			if (user.getCustomStatusMessage() != null)
			{
				friendIdAndStatus += " -- <small>" + user.getCustomStatusMessage() + "</small>";
				isBusy = user.isCustomStatusBusy();
			}
			if (user.isPending())
				friendIdAndStatus += "<br/><small><i>[Add request pending]</i></small>";

			txtId.setText(Html.fromHtml(friendIdAndStatus));

			if (!isOnline)
			{
				txtId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				txtId.setTextColor(txtId.getTextColors().withAlpha(130));
				imgAvatar.setAlpha(130);
			}

			if (isBusy)
				imgBulb.setVisibility(View.VISIBLE);
			else
				imgBulb.setVisibility(View.GONE);

			txtUnreadCount.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
			imgEnvelope.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);

			imgIsTyping.setVisibility(user.isTyping() == true ? View.VISIBLE : View.GONE);
			v.setTag(friendIdAndStatus);
			return v;
		}

		@Override
		public int getChildrenCount(final int groupPosition)
		{
			return FriendsList.getMasterList().get(FriendsList.getMasterList().keySet().toArray()[groupPosition]).size();
		}

		@Override
		public Object getGroup(final int groupPosition)
		{
			return FriendsList.getMasterList().keySet().toArray()[groupPosition];
		}

		@Override
		public int getGroupCount()
		{
			// TODO What's with the random NullPoinderException??!
			// TODO What's with the random NullPoinderException??!
			// TODO What's with the random NullPoinderException??!
			// TODO What's with the random NullPoinderException??!
			while (true)
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
				}
		}

		@Override
		public long getGroupId(final int groupPosition)
		{
			return groupPosition;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent)
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
		public boolean isChildSelectable(final int groupPosition, final int childPosition)
		{
			return true;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.contacts_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.mnuSignOut:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				//confirm sign out
				builder.setTitle("Confirm sign out").setMessage("Are you sure you want to sing out?").setPositiveButton("Yes", new OnClickListener()
				{

					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						stopService(new Intent(ContactsListActivity.this, MessengerService.class));
						finish();
					}
				}).setNegativeButton("No", new OnClickListener()
				{

					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						dialog.cancel();
					}
				}).show();
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
			case R.id.mnuNewIm:
				final EditText txtId = new EditText(this);
				AlertDialog.Builder dlgGetId = new AlertDialog.Builder(this);
				// get the ID of the recipient 
				dlgGetId.setTitle("New IM").setMessage("Enter the recipient's ID:").setView(txtId).setPositiveButton("OK", new OnClickListener()
				{
					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						String id = txtId.getText().toString();
						if (id == null || id.isEmpty())
							return;

						Intent intent = new Intent(ContactsListActivity.this, ChatWindowTabActivity.class);
						intent.putExtra(Utils.qualify("friendId"), id);
						startActivity(intent);
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
			default:
				break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(final android.view.ContextMenu menu, final View v, final android.view.ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) // Show the pop-up menu for a friend
		{
			YahooUser user = (YahooUser) this.adapter.getChild(group, child);
			menu.setHeaderTitle(user.getId());

			//The stealth choices
			menu.add(1, 1, 0, "Appear online to " + user.getId()).setChecked(user.getStealth() == StealthStatus.NO_STEALTH);
			//TODO implement this for session stealth support
			//menu.add(1, 2, 0, "Appear offline to " + user.getId()).setChecked(user.getStealth() == StealthStatus.STEALTH_SESSION);
			menu.add(1, 3, 0, "Appear permenantly offline to " + user.getId()).setChecked(user.getStealth() == StealthStatus.STEALTH_PERMENANT);

			menu.add(2, 4, 0, "Remove from friends"); // the next choice is to remove this friend

			int i = 5;
			TreeMap<String, ArrayList<YahooUser>> groups = FriendsList.getMasterList();

			for (String groupName : groups.keySet()) // generate the list of all groups
			{
				MenuItem item = menu.add(3, i, 0, groupName);
				for (String g : user.getGroupIds())
					if (g.equals(groupName))
					{
						item.setChecked(true); // check off the default group
						break;
					}
				i++;
			}

			menu.setGroupCheckable(1, true, true);
			menu.setGroupCheckable(3, true, true);
		}
		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) // Show the pop-up menu for a group
		{
			String groupName = this.adapter.getGroup(group).toString();
			menu.setHeaderTitle(groupName);
			menu.add(1, 1, 0, "Add a friend");
			menu.add(1, 2, 0, "Delete");
			menu.add(1, 3, 0, "Rename");
		}
	};

	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) //a child item is selected
		{
			final YahooUser user = (YahooUser) this.adapter.getChild(group, child);
			final String sourceGroup = user.getGroup();

			if (item.getItemId() == 1) // Appear online
			{
				try
				{
					FriendsList.changeStealth(user.getId(), StealthStatus.NO_STEALTH);
					this.adapter.notifyDataSetChanged();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return true;
			}
			//TODO implement this for session stealth support
			//			else if (item.getItemId() == 2)	// Appear offline
			//			{
			//				try
			//				{
			//					FriendsList.changeStealth(user.getId(), StealthStatus.STEALTH_SESSION);
			//				}
			//				catch (IOException e)
			//				{
			//					e.printStackTrace();
			//				}
			//				return true;
			//			}
			else if (item.getItemId() == 3) //Appear permenantly offline
			{
				try
				{
					FriendsList.changeStealth(user.getId(), StealthStatus.STEALTH_PERMENANT);
					this.adapter.notifyDataSetChanged();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return true;
			}
			else if (item.getItemId() == 4) //Delete
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Remove friend");
				builder.setMessage("Are you sure you want to remove \"" + user.getId() + "\" from your friends list?");
				builder.setCancelable(true);
				builder.setPositiveButton("Yes", new OnClickListener()
				{

					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						try
						{
							FriendsList.removeFriendFromGroup(user.getId(), sourceGroup);
							ContactsListActivity.this.adapter.notifyDataSetChanged();
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
					public void onClick(final DialogInterface dialog, final int which)
					{
						dialog.cancel();
					}
				});

				builder.create().show();
				return true;
			}

			// Else : change the group of the current user
			String destinationGroup = item.getTitle().toString();

			try
			{
				FriendsList.moveFriend(user.getId(), sourceGroup, destinationGroup);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}

		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) //a group item is selected
		{

			final String groupName = this.adapter.getGroup(group).toString();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			switch (item.getItemId())
			{
				case 1://add friend
					final EditText txtId = new EditText(this);
					builder.setTitle("Add friend").setMessage("Enter the ID of the person you want to add:").setView(txtId).setPositiveButton("Add", new OnClickListener()
					{

						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							String friendId = txtId.getText().toString();
							if (friendId == null || friendId.isEmpty())
								return;
							try
							{
								FriendsList.addFriend(friendId, groupName);
								ContactsListActivity.this.adapter.notifyDataSetChanged();
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
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
				case 2: //delete this group and all its contents
					builder.setTitle("Confrim delete").setMessage("Are you sure you want to delete group \"" + groupName + "\"?").setPositiveButton("Yes", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							try
							{
								FriendsList.deleteGroup(groupName);
								ContactsListActivity.this.adapter.notifyDataSetChanged();
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}

						}
					}).setNegativeButton("No", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
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
						public void onClick(final DialogInterface dialog, final int which)
						{
							String groupNewName = txtGroupNewName.getText().toString();
							if (groupName == null || groupName.isEmpty())
								return;
							try
							{
								FriendsList.renameGroup(groupName, groupNewName);
								ContactsListActivity.this.adapter.notifyDataSetChanged();
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
		}

		this.adapter.notifyDataSetChanged();
		return true;
	};

	BroadcastReceiver listener = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_NEW_IM) || intent.getAction().equals(MessengerService.INTENT_IS_TYPING)
					|| intent.getAction().equals(MessengerService.INTENT_BUZZ) || intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_ON)
					|| intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF) || intent.getAction().equals(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED)
					|| intent.getAction().equals(MessengerService.INTENT_LIST_CHANGED))
				ContactsListActivity.this.adapter.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
				finish();
		}
	};

}