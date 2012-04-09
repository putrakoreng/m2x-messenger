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
import java.util.TreeMap;

import org.openymsg.network.SessionState;
import org.openymsg.network.Status;
import org.openymsg.network.StealthStatus;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooUser;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.ProgressCheckThread;
import com.sir_m2x.messenger.utils.Utils;

/**
 * The buddy list of the messenger
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ContactsListActivity extends ExpandableListActivity
{	
	private final int STEALTH_SETTINGS = 0;
	private final int STEALTH_ONLINE = 1;
	private final int STEALTH_OFFLINE = 2;
	private final int REMOVE_FIRNED = 3;
	private final int MOVE_FIRNED = 4;
	private final int MOVE_NEW_GROUP = 5;
	private final int ADD_FIRNED = 0;
	private final int DELETE_GROUP = 1;
	private final int RENAME_GROUP = 2;
	
	private ContactsListAdapter adapter;
	private ProgressDialog pd = null;
	private String lastLoginStatus = "Connection lost! Retrying...";
	private ProgressCheckThread progressCheck = null;

	// ContextMenuInfo for keeping track of the selected item between submenus. Apparently Android has a bug...
	ExpandableListContextMenuInfo cmInfo = null;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.contacts_list);
		this.adapter = new ContactsListAdapter();
		setListAdapter(this.adapter);
		registerForContextMenu(getExpandableListView());

		this.pd = new ProgressDialog(this);
		this.pd.setIndeterminate(true);
		this.pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pd.setMessage(this.lastLoginStatus);
		this.pd.setOnCancelListener(this.pdCancelListener);
	}

	@Override
	protected void onResume()
	{
		if (MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON)
			showProgressAndWaitLogin();
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
		if (this.pd != null)
			this.pd.dismiss();
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
//		Intent intent = new Intent(ContactsListActivity.this, ChatWindowTabActivity.class);
//		intent.putExtra(Utils.qualify("friendId"), this.adapter.getChild(groupPosition, childPosition).toString());
//		startActivity(intent);
		
		Intent intent = new Intent(ContactsListActivity.this, ChatWindowPager.class);
		intent.putExtra(Utils.qualify("friendId"), this.adapter.getChild(groupPosition, childPosition).toString());
		startActivity(intent);

		return true;
	};

	private class ContactsListAdapter extends BaseExpandableListAdapter
	{
		@Override
		public Object getChild(final int groupPosition, final int childPosition)
		{
			YahooGroup group = (YahooGroup) getGroup(groupPosition);
			int i = 0;

			for (YahooUser user : group.getUsers())
			{
				if (i == childPosition)
					if (Preferences.showOffline)
						return user;
					else if (user.getStatus() != Status.OFFLINE) // if this user is online, show it!
						return user;
					else
						continue; // this user is offline and based on the preferences, we should not list him/her
				if (Preferences.showOffline)
					i++;
				else if (user.getStatus() != Status.OFFLINE)
					i++;

			}

			return null;
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

			boolean isOnline = false;

			if (user.getStatus() != Status.OFFLINE)
				isOnline = true;

			TextView txtId = (TextView) v.findViewById(R.id.txtId);
			TextView txtUnreadCount = (TextView) v.findViewById(R.id.txtNewImCount);
			ImageView imgIsTyping = (ImageView) v.findViewById(R.id.imgIsTyping);
			ImageView imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			ImageView imgAvatar = (ImageView) v.findViewById(R.id.imgAvatar);
			ImageView imgBulb = (ImageView) v.findViewById(R.id.imgBulb);

			int unread = 0;

			if (MessengerService.getFriendAvatars().containsKey(friendId))
				imgAvatar.setImageBitmap(MessengerService.getFriendAvatars().get(friendId));
			else
			{
				Bitmap b = AvatarHelper.loadAvatarFromSD(friendId);
				if (b != null)
					imgAvatar.setImageBitmap(b);
			}
			
			if (MessengerService.getUnreadIMs().containsKey(friendId))
			{
				unread = MessengerService.getUnreadIMs().get(friendId).intValue();
				txtUnreadCount.setText(String.valueOf(unread));
			}

			String friendIdAndStatus = isOnline ? Utils.toBold(friendId) : friendId;

			if (user.getStealth() == StealthStatus.STEALTH_PERMENANT)
				friendIdAndStatus = Utils.toItalic(friendIdAndStatus);

			if (user.getCustomStatusMessage() != null)
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

			switch (user.getStatus())
			{
				case AVAILABLE:
					imgBulb.setImageResource(R.drawable.presence_online);
					break;
				case BUSY:
					imgBulb.setImageResource(R.drawable.presence_busy);
					break;
				case IDLE:
					imgBulb.setImageResource(R.drawable.presence_away);
					break;
				case CUSTOM:
					if (user.isCustomStatusBusy())
						imgBulb.setImageResource(R.drawable.presence_busy);
					else
						imgBulb.setImageResource(R.drawable.presence_online);
					break;
				case OFFLINE:
					imgBulb.setImageResource(R.drawable.presence_offline);
					break;
				default:
					imgBulb.setImageResource(R.drawable.presence_busy);
			}

			txtUnreadCount.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
			imgEnvelope.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);

			imgIsTyping.setVisibility(user.isTyping() == true ? View.VISIBLE : View.GONE);
			v.setTag(friendIdAndStatus);
			return v;
		}

		@Override
		public int getChildrenCount(final int groupPosition)
		{
			if (Preferences.showOffline)
				return ((YahooGroup) (MessengerService.getYahooList().getFriendsList().values().toArray()[groupPosition])).getUsers().size();

			int count = 0;
			YahooGroup group = (YahooGroup) getGroup(groupPosition);
			for (YahooUser user : group.getUsers())
				if (user.getStatus() != Status.OFFLINE)
					count++;

			return count;
		}

		/**
		 * The count of all of the contacts in this group (regardless of their
		 * status)
		 * 
		 * @param groupPosition
		 *            The index of the group;
		 * @return The total count of the contacts in this group.
		 */
		public int getTotalChildrenCount(final int groupPosition)
		{
			return ((YahooGroup) (MessengerService.getYahooList().getFriendsList().values().toArray()[groupPosition])).getUsers().size();
		}

		@Override
		public Object getGroup(final int groupPosition)
		{
			int i = 0;
			TreeMap<String, YahooGroup> friendsList = MessengerService.getYahooList().getFriendsList();

			for (String group : friendsList.keySet())
			{
				if (i == groupPosition)
					return friendsList.get(group);
				i++;
			}

			return null;
		}

		@Override
		public int getGroupCount()
		{
			if (MessengerService.getYahooList() == null)
			{
				Log.e("M2X", "YahooList is null");
				return 0;
			}
			if (MessengerService.getYahooList().getFriendsList() == null)
			{
				Log.e("M2X", "Friends list is null");
				return 0;
			}
			return MessengerService.getYahooList().getFriendsList().size();
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
			ImageView imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			int unreadCount = 0;

			YahooGroup group = (YahooGroup) getGroup(groupPosition);
			txtGroupName.setText(group.getName());

			Integer totalCount = getTotalChildrenCount(groupPosition);
			Integer onlineCount = 0;

			for (YahooUser user : group.getUsers())
			{
				if (user.getStatus() != Status.OFFLINE)
					onlineCount++;
				if (MessengerService.getUnreadIMs().keySet().contains(user.getId()))
					unreadCount += MessengerService.getUnreadIMs().get(user.getId());
			}

			txtGroupCount.setText(onlineCount + "/" + totalCount);
			imgEnvelope.setVisibility(unreadCount == 0 ? View.GONE : View.VISIBLE);
			
			return v;
		}

		@Override
		public boolean hasStableIds()
		{
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
		getMenuInflater().inflate(R.menu.contacts_list_menu, menu);
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
				startActivity(new Intent(this, ChatWindowPager.class));
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
						if (id == null || id.equals(""))
							return;

						Intent intent = new Intent(ContactsListActivity.this, ChatWindowPager.class);
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
			case R.id.mnuPreferences:
				startActivityForResult(new Intent(this, PreferencesActivity.class), 0);
				break;
			case R.id.mnuRequests:
				startActivity(new Intent(this, FriendRequestsActivity.class));
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
		this.cmInfo = (ExpandableListContextMenuInfo) menuInfo;

		int type = ExpandableListView.getPackedPositionType(this.cmInfo.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(this.cmInfo.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(this.cmInfo.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) // Show the pop-up menu for a friend
		{
			YahooUser user = (YahooUser) this.adapter.getChild(group, child);
			menu.setHeaderTitle(user.getId());

			//The stealth choices
			SubMenu subMenu = menu.addSubMenu(0, this.STEALTH_SETTINGS, 0, "Stealth settings...");
			subMenu.add(0, this.STEALTH_ONLINE, 0, "Appear online to " + user.getId()).setChecked(user.getStealth() == StealthStatus.NO_STEALTH);
			subMenu.add(0, this.STEALTH_OFFLINE, 0, "Appear permenantly offline to " + user.getId()).setChecked(user.getStealth() == StealthStatus.STEALTH_PERMENANT);
			subMenu.setGroupCheckable(0, true, true);

			menu.add(0, this.REMOVE_FIRNED, 0, "Remove from friends"); // the next choice is to remove this friend
			subMenu = menu.addSubMenu(0, this.MOVE_FIRNED, 0, "Move to...");
			subMenu.add(0, this.MOVE_NEW_GROUP, 0, "New group...");
			int i = this.MOVE_NEW_GROUP + 1;
			TreeMap<String, YahooGroup> friendsList = MessengerService.getYahooList().getFriendsList();

			for (String groupName : friendsList.keySet()) // generate the list of all groups
			{
				MenuItem item = subMenu.add(1, i, 0, groupName);
				for (String g : user.getGroupIds())
					if (g.equals(groupName))
					{
						item.setChecked(true); // check off the default group
						break;
					}
				i++;
			}
			subMenu.setGroupCheckable(1, true, true);
		}
		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) // Show the pop-up menu for a group
		{
			String groupName = ((YahooGroup) this.adapter.getGroup(group)).getName();
			menu.setHeaderTitle(groupName);
			menu.add(1, this.ADD_FIRNED, 0, "Add a friend");
			menu.add(1, this.DELETE_GROUP, 0, "Delete");
			menu.add(1, this.RENAME_GROUP, 0, "Rename");
		}
	};

	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
		if (item.hasSubMenu())
			return false;
		int type = ExpandableListView.getPackedPositionType(this.cmInfo.packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(this.cmInfo.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(this.cmInfo.packedPosition);

		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) //a child item is selected
		{
			final YahooUser user = (YahooUser) this.adapter.getChild(group, child);
			final String sourceGroup = user.getGroup();

			if (item.getItemId() == this.STEALTH_ONLINE) // Appear online
			{
				try
				{
					MessengerService.getYahooList().changeStealth(user, StealthStatus.NO_STEALTH);
					this.adapter.notifyDataSetChanged();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return true;
			}
			//TODO implement this for session stealth support
			else if (item.getItemId() == this.STEALTH_OFFLINE) //Appear permenantly offline
			{
				try
				{
					MessengerService.getYahooList().changeStealth(user, StealthStatus.STEALTH_PERMENANT);
					this.adapter.notifyDataSetChanged();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				return true;
			}
			else if (item.getItemId() == this.REMOVE_FIRNED) //Delete
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
							MessengerService.getYahooList().removeFriendFromGroup(user, sourceGroup);
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
			else if (item.getItemId() == this.MOVE_NEW_GROUP)
			{
				final EditText txtId = new EditText(this);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Move group").setMessage("Enter the name of the group you want to move this friend to:").setView(txtId).setPositiveButton("Move", new OnClickListener()
				{
					@Override
					public void onClick(final DialogInterface dialog, final int which)
					{
						String destinationGroup = txtId.getText().toString();
						if (destinationGroup == null || destinationGroup.equals(""))
							return;
						try
						{
							MessengerService.getYahooList().moveFriend(user, sourceGroup, destinationGroup);
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
				return true;
			}

			// Else : change the group of the current user
			String destinationGroup = item.getTitle().toString();

			try
			{
				MessengerService.getYahooList().moveFriend(user, sourceGroup, destinationGroup);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}

		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) //a group item is selected
		{

			final YahooGroup chosenGroup = (YahooGroup) this.adapter.getGroup(group);
			final String groupName = chosenGroup.getName();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			

			switch (item.getItemId())
			{
				case ADD_FIRNED://add friend
					final EditText txtId = new EditText(this);
					builder.setTitle("Add friend").setMessage("Enter the ID of the person you want to add:").setView(txtId).setPositiveButton("Add", new OnClickListener()
					{

						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							String friendId = txtId.getText().toString();
							if (friendId == null || friendId.equals(""))
								return;
							try
							{
								MessengerService.getYahooList().addFriend(friendId, groupName);
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
				case DELETE_GROUP: //delete this group and all its contents
					builder.setTitle("Confrim delete").setMessage("Are you sure you want to delete group \"" + groupName + "\"?").setPositiveButton("Yes", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							try
							{
								MessengerService.getYahooList().deleteGroup(groupName);
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
				case RENAME_GROUP: //rename this group
					final EditText txtGroupNewName = new EditText(this);
					builder.setTitle("Group rename").setMessage("Enter a new name for this group:").setView(txtGroupNewName).setPositiveButton("OK", new OnClickListener()
					{
						@Override
						public void onClick(final DialogInterface dialog, final int which)
						{
							String groupNewName = txtGroupNewName.getText().toString();
							if (groupNewName == null || groupNewName.equals(""))
								return;
							try
							{
								MessengerService.getYahooList().renameGroup(chosenGroup, groupNewName);
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
	}

	
	
	private void showProgressAndWaitLogin()
	{
		if (this.progressCheck == null || !this.progressCheck.isAlive())
		{
			this.progressCheck = new ProgressCheckThread(this.progressHandler);
			this.progressCheck.start();
		}

		this.pd.show();
	}

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
	
	OnCancelListener pdCancelListener = new OnCancelListener()
	{
		
		@Override
		public void onCancel(final DialogInterface dialog)
		{
			Log.w("M2X", "Sent a DESTROY intent from ContactsListActivity--pdCancelListener");
			sendBroadcast(new Intent(MessengerService.INTENT_DESTROY));
		}
	};
	
	Handler progressHandler = new Handler()
	{
		@Override
		public void handleMessage(final Message msg)
		{
			switch (msg.what)
			{
				case ProgressCheckThread.PROGRESS_UPDATE:
					switch (MessengerService.getSession().getSessionStatus())
					{
						case INITIALIZING:
							ContactsListActivity.this.pd.setMessage("Initializing...");
							break;
						case CONNECTING:
							ContactsListActivity.this.pd.setMessage("Sending credentials...");
							break;
						case STAGE1:
							ContactsListActivity.this.pd.setMessage("Authenticating...");
							break;
						case STAGE2:
							ContactsListActivity.this.pd.setMessage("Authenticating(2)...");
							break;
						case WAITING:
							ContactsListActivity.this.pd.setMessage("Waiting to reconnect...");
							break;
						case CONNECTED:
							ContactsListActivity.this.pd.setMessage("Loading list...");
							break;
						case LOGGED_ON:
							ContactsListActivity.this.pd.setMessage("Logged on!");
							break;
						case FAILED:
							ContactsListActivity.this.pd.setMessage("Failed!");
							break;
					}
					break;
				case ProgressCheckThread.PROGRESS_FAILED:
					ContactsListActivity.this.pd.dismiss();
					break;
				case ProgressCheckThread.PROGRESS_LOGGED_ON:
					ContactsListActivity.this.pd.dismiss();
					//TODO do something else in here...
					break;
			}
		}
	};
	
	

}