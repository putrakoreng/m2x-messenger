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
package com.sir_m2x.messenger.classes;

import java.util.ArrayList;
import java.util.TreeMap;

import org.openymsg.network.Status;
import org.openymsg.network.StealthStatus;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooUser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;

/**
 * An instance of this class is used as an adapter for the ListView in
 * ContactsListActivity
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class ContactsListAdapter extends BaseExpandableListAdapter
{
	private Context context; // android context
	private String filter = "";
	private ArrayList<YahooUser> filteredUsers = null;

	public ContactsListAdapter(final Context context)
	{
		this.context = context;
	}

	public String getFilter()
	{
		return this.filter;
	}

	public void setFilter(final String filter)
	{
		this.filter = filter;
		this.filteredUsers = new ArrayList<YahooUser>();

		for (YahooGroup g : MessengerService.getYahooList().getFriendsList().values())
			for (YahooUser u : g.getUsers())
				if (u.getId().contains(filter))
					this.filteredUsers.add(u);

		notifyDataSetChanged();
	}

	@Override
	public Object getChild(final int groupPosition, final int childPosition)
	{
		if (!this.filter.equals(""))
			return this.filteredUsers.get(childPosition);

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
		LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v;
		ChildViewHolder viewHolder;

		if (convertView == null)
		{
			v = inflater.inflate(R.layout.contacts_list_child_view, parent, false);
			viewHolder = new ChildViewHolder();
			viewHolder.txtId = (TextView) v.findViewById(R.id.txtId);
			viewHolder.txtStatus = (TextView) v.findViewById(R.id.txtStatus);
			viewHolder.txtUnreadCount = (TextView) v.findViewById(R.id.txtNewImCount);
			viewHolder.imgIsTyping = (ImageView) v.findViewById(R.id.imgIsTyping);
			viewHolder.imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			viewHolder.imgAvatar = (ImageView) v.findViewById(R.id.imgAvatar);
			viewHolder.imgBulb = (ImageView) v.findViewById(R.id.imgBulb);
		}
		else
		{
			v = convertView;
			viewHolder = (ChildViewHolder) v.getTag();
		}

		boolean isOnline = false;

		if (user.getStatus() != Status.OFFLINE)
			isOnline = true;

		int unread = 0;

		if (MessengerService.getFriendAvatars().containsKey(friendId))
			viewHolder.imgAvatar.setImageBitmap(MessengerService.getFriendAvatars().get(friendId));
		else
		{
			Bitmap b = AvatarHelper.loadAvatarFromSD(friendId);
			if (b != null)
				viewHolder.imgAvatar.setImageBitmap(b);
			else
				viewHolder.imgAvatar.setImageResource(R.drawable.yahoo_no_avatar);
		}

		if (MessengerService.getUnreadIMs().containsKey(friendId))
		{
			unread = MessengerService.getUnreadIMs().get(friendId).intValue();
			viewHolder.txtUnreadCount.setText(String.valueOf(unread));
		}

		String friendIdText = isOnline ? Utils.toBold(friendId) : friendId;
		String friendStatus = "";

		if (user.getStealth() == StealthStatus.STEALTH_PERMENANT)
			friendIdText = Utils.toItalic(friendId);

		if (user.getCustomStatusMessage() != null)
			friendStatus += user.getCustomStatusMessage();
		if (user.isPending())
			friendStatus += "<i>[Add request pending]</i>";

		viewHolder.txtId.setText(Html.fromHtml(friendIdText));

		if (friendStatus.equals("") || friendStatus == null)
			viewHolder.txtStatus.setVisibility(View.GONE);
		else
		{
			viewHolder.txtStatus.setVisibility(View.VISIBLE);
			viewHolder.txtStatus.setText(Html.fromHtml(friendStatus));
		}

		if (!isOnline)
		{
			viewHolder.txtId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			viewHolder.txtId.setTextColor(viewHolder.txtId.getTextColors().withAlpha(200));
			viewHolder.imgAvatar.setAlpha(130);
		}
		else
		{
			viewHolder.txtId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
			viewHolder.txtId.setTextColor(viewHolder.txtId.getTextColors().withAlpha(255));
			viewHolder.imgAvatar.setAlpha(255);
		}

		switch (user.getStatus())
		{
			case AVAILABLE:
				viewHolder.imgBulb.setImageResource(R.drawable.presence_online);
				break;
			case BUSY:
				viewHolder.imgBulb.setImageResource(R.drawable.presence_busy);
				break;
			case IDLE:
				viewHolder.imgBulb.setImageResource(R.drawable.presence_away);
				break;
			case CUSTOM:
				if (user.isCustomStatusBusy())
					viewHolder.imgBulb.setImageResource(R.drawable.presence_busy);
				else
					viewHolder.imgBulb.setImageResource(R.drawable.presence_online);
				break;
			case OFFLINE:
				viewHolder.imgBulb.setImageResource(R.drawable.presence_offline);
				break;
			default:
				viewHolder.imgBulb.setImageResource(R.drawable.presence_invisible);
		}

		viewHolder.txtUnreadCount.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
		viewHolder.imgEnvelope.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);

		viewHolder.imgIsTyping.setVisibility(user.isTyping() == true ? View.VISIBLE : View.GONE);
		viewHolder.friendId = friendId;
		v.setTag(viewHolder);

		return v;
	}

	@Override
	public int getChildrenCount(final int groupPosition)
	{
		if (!this.filter.equals(""))
			return this.filteredUsers.size();

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
		if (!this.filter.equals(""))
			return "Search results:";

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
		if (!this.filter.equals(""))
			return 1;

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
		LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v;
		GroupViewHolder viewHolder;

		if (convertView == null)
		{
			v = inflater.inflate(R.layout.contacts_list_group_view, parent, false);
			viewHolder = new GroupViewHolder();
			viewHolder.txtGroupName = (TextView) v.findViewById(R.id.txtGroupName);
			viewHolder.txtGroupCount = (TextView) v.findViewById(R.id.txtGroupCount);
			viewHolder.imgEnvelope = (ImageView) v.findViewById(R.id.imgEnvelope);
			viewHolder.imgIndicator = (ImageView) v.findViewById(R.id.imgIndicator);
		}
		else
		{
			v = convertView;
			viewHolder = (GroupViewHolder) convertView.getTag();
		}

		int unreadCount = 0;

		if (!this.filter.equals(""))
		{
			viewHolder.txtGroupCount.setText(String.valueOf(getChildrenCount(groupPosition)));
			viewHolder.imgEnvelope.setVisibility(View.GONE);
			viewHolder.txtGroupName.setText("Search results");
			if (isExpanded)
				viewHolder.imgIndicator.setImageResource(R.drawable.list_arrow_down);
			else
				viewHolder.imgIndicator.setImageResource(R.drawable.list_arrow_right);
			v.setTag(viewHolder);
			return v;
		}

		YahooGroup group = (YahooGroup) getGroup(groupPosition);
		viewHolder.txtGroupName.setText(group.getName());

		Integer totalCount = getTotalChildrenCount(groupPosition);
		Integer onlineCount = 0;

		for (YahooUser user : group.getUsers())
		{
			if (user.getStatus() != Status.OFFLINE)
				onlineCount++;
			if (MessengerService.getUnreadIMs().keySet().contains(user.getId()))
				unreadCount += MessengerService.getUnreadIMs().get(user.getId());
		}

		viewHolder.txtGroupCount.setText(onlineCount + "/" + totalCount);
		viewHolder.imgEnvelope.setVisibility(unreadCount == 0 ? View.GONE : View.VISIBLE);
		viewHolder.txtGroupName.setTypeface(unreadCount == 0 ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

		if (isExpanded)
			viewHolder.imgIndicator.setImageResource(R.drawable.list_arrow_down);
		else
			viewHolder.imgIndicator.setImageResource(R.drawable.list_arrow_right);

		v.setTag(viewHolder);
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

	static class GroupViewHolder
	{
		TextView txtGroupName;
		TextView txtGroupCount;
		ImageView imgEnvelope;
		ImageView imgIndicator;
	}

	/**
	 * Holder of all controls used to construct a ChildView for this adapter
	 */
	static class ChildViewHolder
	{
		String friendId;
		TextView txtId;
		TextView txtStatus;
		TextView txtUnreadCount;
		ImageView imgIsTyping;
		ImageView imgEnvelope;
		ImageView imgAvatar;
		ImageView imgBulb;

		@Override
		public String toString()
		{
			return this.friendId;
		}
	}
}
