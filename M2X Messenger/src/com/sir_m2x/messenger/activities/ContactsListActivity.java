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
import java.util.TreeMap;

import org.openymsg.network.SessionState;
import org.openymsg.network.Status;
import org.openymsg.network.StealthStatus;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooUser;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.classes.ContactsListAdapter;
import com.sir_m2x.messenger.dialogs.ChangeStatusDialog;
import com.sir_m2x.messenger.dialogs.CustomDialog;
import com.sir_m2x.messenger.helpers.ToastHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.ProgressCheckThread;
import com.sir_m2x.messenger.utils.Utils;

/**
 * The buddy list of the messenger
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
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
	private ProgressCheckThread progressCheck = null;
	private ImageView imgTopLogo = null;
	private TextView txtProblem = null;
	private EditText txtSearch = null;
	private CheckBox chkSearch = null;
	private Button btnConversations = null;
	private Button btnSettings = null;
	private Button btnSignout = null;

	// ContextMenuInfo for keeping track of the selected item between submenus. Apparently Android has a bug...
	ExpandableListContextMenuInfo cmInfo = null;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.contacts_list);
		this.adapter = new ContactsListAdapter(this);
		setListAdapter(this.adapter);
		registerForContextMenu(getExpandableListView());

		((TextView) findViewById(R.id.txtLoginId)).setText(MessengerService.getMyId());

		this.imgTopLogo = (ImageView) findViewById(R.id.imgTopLogo);
		this.txtProblem = (TextView) findViewById(R.id.txtProblem);
		this.txtProblem.setVisibility(View.GONE);
		this.txtSearch = (EditText) findViewById(R.id.txtSearch);
		this.txtSearch.setVisibility(View.GONE);
		this.txtSearch.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
			{
				ContactsListActivity.this.adapter.setFilter(ContactsListActivity.this.txtSearch.getText().toString());
				getExpandableListView().expandGroup(0);
			}

			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
			{
			}

			@Override
			public void afterTextChanged(final Editable s)
			{
			}
		});

		determineTopLogo();

		this.chkSearch = (CheckBox) findViewById(R.id.chkSearch);
		this.chkSearch.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
			{
				if (isChecked)
				{
					ContactsListActivity.this.txtSearch.setVisibility(View.VISIBLE);
					ContactsListActivity.this.txtSearch.requestFocus();
					// show the virtual keyboard for txtSearch
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(ContactsListActivity.this.txtSearch, InputMethodManager.SHOW_IMPLICIT);
				}
				else
				{
					ContactsListActivity.this.txtSearch.setVisibility(View.GONE);
					ContactsListActivity.this.txtSearch.setText(""); // clear any filter
				}
			}
		});

		this.btnConversations = (Button) findViewById(R.id.btnConversations);
		this.btnConversations.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				if (MessengerService.getFriendsInChat().isEmpty())
					return;
				showActivity(new Intent(ContactsListActivity.this, ChatWindowPager.class));
			}
		});
		this.btnSettings = (Button) findViewById(R.id.btnSettings);
		this.btnSettings.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				ChangeStatusDialog dlg = new ChangeStatusDialog(ContactsListActivity.this);
				dlg.show();
			}
		});
		this.btnSignout = (Button) findViewById(R.id.btnSignout);
		this.btnSignout.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				confirmSignOut();
			}
		});
		
		((SlidingDrawer)findViewById(R.id.footerSlider)).open();
	}

	@Override
	protected void onResume()
	{
		if (MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON)
			if (this.progressCheck == null || this.progressCheck != null && !this.progressCheck.isAlive())
			{
				this.progressCheck = new ProgressCheckThread(this.progressHandler);
				this.progressCheck.start();
			}
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_IS_TYPING));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_STATUS_CHANGED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_LIST_CHANGED));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_CONNECTION_LOST));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_DESTROY));
		this.adapter.notifyDataSetChanged();
		super.onResume();
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
		Intent intent = new Intent(ContactsListActivity.this, ChatWindowPager.class);
		intent.putExtra(Utils.qualify("friendId"), this.adapter.getChild(groupPosition, childPosition).toString());
		showActivity(intent);

		return true;
	};

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
				confirmSignOut();
				break;

			case R.id.mnuShowConversations:
				if (MessengerService.getFriendsInChat().isEmpty())
					break;
				showActivity(new Intent(this, ChatWindowPager.class));
				break;

			case R.id.mnuLog:
				showActivity(new Intent(this, LogWindowActivity.class));
				break;

			case R.id.mnuNewIm:
				final EditText txtId = new EditText(this);
				txtId.setBackgroundResource(R.drawable.background_textbox);
				txtId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				final CustomDialog dlgGetId = new CustomDialog(this);
				dlgGetId.setTitle("New IM");
				// get the ID of the recipient 
				dlgGetId.setMessage("Enter the recipient's ID:").setView(txtId).setPositiveButton("OK", new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
					{
						String id = txtId.getText().toString();
						if (id == null || id.equals(""))
							return;

						Intent intent = new Intent(ContactsListActivity.this, ChatWindowPager.class);
						intent.putExtra(Utils.qualify("friendId"), id);
						showActivity(intent);
						dlgGetId.dismiss();
					}
				}).setNegativeButton("Cancel", new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
					{
						dlgGetId.cancel();
					}
				}).show();
				break;

			case R.id.mnuPreferences:
				showActivity(new Intent(this, PreferencesActivity.class));
				break;
			case R.id.mnuRequests:
				showActivity(new Intent(this, FriendRequestsActivity.class));
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
			if (!this.adapter.getFilter().equals("")) // don't show menu for "search result" group
				return;

			String groupName = ((YahooGroup) this.adapter.getGroup(group)).getName();
			menu.setHeaderTitle(groupName);
			menu.add(1, this.ADD_FIRNED, 0, "Add a friend...");
			menu.add(1, this.DELETE_GROUP, 0, "Delete");
			menu.add(1, this.RENAME_GROUP, 0, "Rename...");
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
				catch (IllegalAccessException e)
				{
					ToastHelper.showToast(ContactsListActivity.this, R.drawable.ic_stat_notify_busy, "Your are not logged in!", Toast.LENGTH_LONG);
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
				catch (IllegalAccessException e)
				{
					ToastHelper.showToast(ContactsListActivity.this, R.drawable.ic_stat_notify_busy, "Your are not logged in!", Toast.LENGTH_LONG);
					e.printStackTrace();
				}
				return true;
			}
			else if (item.getItemId() == this.REMOVE_FIRNED) //Delete
			{
				final CustomDialog dlg = new CustomDialog(this);
				dlg.setTitle("Remove friend");
				dlg.setMessage("Are you sure you want to remove \"" + user.getId() + "\" from your friends list?");
				dlg.setCancelable(true);
				dlg.setPositiveButton("Yes", new View.OnClickListener()
				{

					@Override
					public void onClick(final View v)
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
						catch (IllegalAccessException e)
						{
							ToastHelper.showToast(ContactsListActivity.this, R.drawable.ic_stat_notify_busy, "Your are not logged in!", Toast.LENGTH_LONG);
							e.printStackTrace();
						}
						dlg.dismiss();
					}
				});

				dlg.setNegativeButton("No", new View.OnClickListener()
				{

					@Override
					public void onClick(final View v)
					{
						dlg.cancel();
					}
				});

				dlg.show();
				return true;
			}
			else if (item.getItemId() == this.MOVE_NEW_GROUP)
			{
				final EditText txtId = new EditText(this);
				txtId.setBackgroundResource(R.drawable.background_textbox);
				txtId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				final CustomDialog dlg = new CustomDialog(this);
				dlg.setTitle("Move group");
				dlg.setMessage("Enter the name of the group you want to move this friend to:").setView(txtId).setPositiveButton("Move", new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
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

						dlg.dismiss();
					}
				}).setNegativeButton("Cancel", new View.OnClickListener()
				{

					@Override
					public void onClick(final View v)
					{
						dlg.cancel();
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
			catch (IllegalAccessException e)
			{
				ToastHelper.showToast(ContactsListActivity.this, R.drawable.ic_stat_notify_busy, "Your are not logged in!", Toast.LENGTH_LONG);
				e.printStackTrace();
			}

		}

		else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) //a group item is selected
		{

			final YahooGroup chosenGroup = (YahooGroup) this.adapter.getGroup(group);
			final String groupName = chosenGroup.getName();
			final CustomDialog dlg = new CustomDialog(this);

			switch (item.getItemId())
			{
				case ADD_FIRNED://add friend
					final EditText txtId = new EditText(this);
					txtId.setBackgroundResource(R.drawable.background_textbox);
					txtId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
					dlg.setTitle("Add friend");
					dlg.setMessage("Enter the ID of the person you want to add:").setView(txtId).setPositiveButton("Add", new View.OnClickListener()
					{

						@Override
						public void onClick(final View v)
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

							dlg.dismiss();
						}
					}).setNegativeButton("Cancel", new View.OnClickListener()
					{

						@Override
						public void onClick(final View v)
						{
							dlg.cancel();
						}
					}).show();
					break;
				case DELETE_GROUP: //delete this group and all its contents
					dlg.setTitle("Confrim delete");
					dlg.setMessage("Are you sure you want to delete group \"" + groupName + "\"?").setPositiveButton("Yes", new View.OnClickListener()
					{
						@Override
						public void onClick(final View v)
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

							dlg.dismiss();

						}
					}).setNegativeButton("No", new View.OnClickListener()
					{
						@Override
						public void onClick(final View v)
						{
							dlg.cancel();
						}
					}).show();

					break;
				case RENAME_GROUP: //rename this group
					final EditText txtGroupNewName = new EditText(this);
					txtGroupNewName.setBackgroundResource(R.drawable.background_textbox);
					txtGroupNewName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
					dlg.setTitle("Group rename");
					dlg.setMessage("Enter a new name for this group:").setView(txtGroupNewName).setPositiveButton("OK", new View.OnClickListener()
					{
						@Override
						public void onClick(final View v)
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

							dlg.dismiss();
						}
					}).setNegativeButton("Cancel", new View.OnClickListener()
					{
						@Override
						public void onClick(final View v)
						{
							dlg.cancel();
						}
					}).show();
					break;

			}
		}

		this.adapter.notifyDataSetChanged();
		return true;
	}

	BroadcastReceiver listener = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_NEW_IM) || intent.getAction().equals(MessengerService.INTENT_IS_TYPING)
					|| intent.getAction().equals(MessengerService.INTENT_BUZZ) || intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_ON)
					|| intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF) || intent.getAction().equals(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED))
				ContactsListActivity.this.adapter.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_LIST_CHANGED))
			{
				MessengerService.getYahooList().resortList(true);
				ContactsListActivity.this.adapter.notifyDataSetChanged();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_CONNECTION_LOST))
			{
				if (ContactsListActivity.this.progressCheck == null || ContactsListActivity.this.progressCheck != null && !ContactsListActivity.this.progressCheck.isAlive())
				{
					ContactsListActivity.this.progressCheck = new ProgressCheckThread(ContactsListActivity.this.progressHandler);
					ContactsListActivity.this.progressCheck.start();
				}
			}
			else if (intent.getAction().equals(MessengerService.INTENT_STATUS_CHANGED))
				determineTopLogo();
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
			getExpandableListView().setEnabled(false);
			switch (msg.what)
			{
				case ProgressCheckThread.PROGRESS_UPDATE:
					ContactsListActivity.this.txtProblem.setVisibility(View.VISIBLE);

					ContactsListActivity.this.txtProblem.setText((String) msg.obj);
					break;
				case ProgressCheckThread.PROGRESS_FAILED:
					ContactsListActivity.this.txtProblem.setVisibility(View.VISIBLE);
					ContactsListActivity.this.txtProblem.setText("Failed");
					break;
				case ProgressCheckThread.PROGRESS_LOGGED_ON:
					getExpandableListView().setEnabled(true);
					ContactsListActivity.this.txtProblem.setVisibility(View.GONE);
					break;
			}
		}
	};

	private void confirmSignOut()
	{
		final CustomDialog dlg = new CustomDialog(this);
		dlg.setTitle("Confirm sign out");
		dlg.setMessage("Are you sure you want to sign out?").setPositiveButton("Yes", new View.OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				stopService(new Intent(ContactsListActivity.this, MessengerService.class));
				finish();
				dlg.dismiss();
			}
		}).setNegativeButton("No", new View.OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				dlg.cancel();
			}
		}).show();
	}

	private void determineTopLogo()
	{
		Status currentStatus = MessengerService.getSession().getStatus();

		switch (currentStatus)
		{
			case AVAILABLE:
				this.imgTopLogo.setImageResource(R.drawable.top_logo_available);
				break;
			case BUSY:
				this.imgTopLogo.setImageResource(R.drawable.top_logo_busy);
				break;
			case IDLE:
				this.imgTopLogo.setImageResource(R.drawable.top_logo_away);
				break;
			case INVISIBLE:
				this.imgTopLogo.setImageResource(R.drawable.top_logo_invisible);
				break;
			case CUSTOM:
				if (MessengerService.getSession().isCustomBusy())
					this.imgTopLogo.setImageResource(R.drawable.top_logo_busy);
				else
					this.imgTopLogo.setImageResource(R.drawable.top_logo_available);

				break;

			default:
				break;
		}
	}

	@Override
	public boolean onSearchRequested()
	{
		this.chkSearch.setChecked(true);
		return true;
	};

	@Override
	public boolean onKeyLongPress(final int keyCode, final KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			onSearchRequested();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public void onBackPressed()
	{
		if (this.chkSearch.isChecked())
		{
			this.chkSearch.setChecked(false);
			return;
		}

		super.onBackPressed();
	}

	void showActivity(final Intent intent)
	{
		startActivity(intent);
		//overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
		//overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
	}

}