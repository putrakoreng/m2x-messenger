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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;

/**
 * An instance of this class is used as an adapter for the ListView in
 * ChanWindowFragment
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class ChatWindowAdapter extends BaseAdapter
{
	//FIXME Some references are really memory haungry!
	Context context;
	String friendId;

	public ChatWindowAdapter(final Context context, final String friendId)
	{
		this.context = context;
		this.friendId = friendId;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent)
	{
		synchronized (MessengerService.getFriendsInChat())
		{
			IM im = (IM) getItem(position);
			
			String sender = "";
			boolean isOfflineMessage = false;

			try
			//TODO fix?
			{
				sender = im.getSender();
				isOfflineMessage = im.isOfflineMessage();

			}
			catch (Exception e)
			{
				e.toString();
			}
			boolean isSenderSelf = sender.equals(MessengerService.getMyId());

			LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v;
			ViewHolder viewHolder;

			if (convertView == null) // we have to inflate a new layout
			{
				v = inflater.inflate(R.layout.chat_window_row, parent, false);

				viewHolder = new ViewHolder();
				viewHolder.l = (FrameLayout) v.findViewById(R.id.layoutHolder);
				viewHolder.txtTimestamp = (TextView) v.findViewById(R.id.txtTimestamp);
				viewHolder.txtMessageFriend = (TextView) v.findViewById(R.id.txtMessageFriend);
				viewHolder.imgAvatarFriend = (ImageView) v.findViewById(R.id.imgAvatarFriend);
				viewHolder.txtMessageSelf = (TextView) v.findViewById(R.id.txtMessageSelf);
				viewHolder.imgAvatarSelf = (ImageView) v.findViewById(R.id.imgAvatarSelf);
			}
			else
			{
				v = convertView;
				viewHolder = (ViewHolder) convertView.getTag();
			}
			
			viewHolder.txtTimestamp.setText(im.getTime());

			if (isSenderSelf)
			{
				viewHolder.txtMessageFriend.setVisibility(View.GONE);
				viewHolder.imgAvatarFriend.setVisibility(View.GONE);
				viewHolder.txtMessageSelf.setVisibility(View.VISIBLE);
				viewHolder.imgAvatarSelf.setVisibility(View.VISIBLE);
				
				viewHolder.txtMessageSelf.setText(im.toHtml(this.context));
				
				if (MessengerService.getMyAvatar() != null)
					viewHolder.imgAvatarSelf.setImageBitmap(MessengerService.getMyAvatar());
				else
					viewHolder.imgAvatarSelf.setImageResource(R.drawable.yahoo_no_avatar);
				
				if (im.isBuzz())
					viewHolder.txtMessageSelf.setBackgroundResource(R.drawable.bubble_buzz_self);
				else
					viewHolder.txtMessageSelf.setBackgroundResource(R.drawable.bubble_self);
			}
			else
			{
				viewHolder.txtMessageFriend.setVisibility(View.VISIBLE);
				viewHolder.imgAvatarFriend.setVisibility(View.VISIBLE);
				viewHolder.txtMessageSelf.setVisibility(View.GONE);
				viewHolder.imgAvatarSelf.setVisibility(View.GONE);
				
				viewHolder.txtMessageFriend.setText(im.toHtml(this.context));
				
				if (isOfflineMessage)
					viewHolder.txtMessageFriend.setBackgroundResource(R.drawable.bubble_offline);
				else if (im.isBuzz())
					viewHolder.txtMessageFriend.setBackgroundResource(R.drawable.bubble_buzz_friend);
				else
					viewHolder.txtMessageFriend.setBackgroundResource(R.drawable.bubble_friend);
				
				Bitmap friendAvatar = MessengerService.getFriendAvatars().get(this.friendId);
				if (friendAvatar != null)
					viewHolder.imgAvatarFriend.setImageBitmap(friendAvatar);
				else
					viewHolder.imgAvatarFriend.setImageResource(R.drawable.yahoo_no_avatar);
			}
			
			viewHolder.txtTimestamp.setVisibility(Preferences.timeStamp ? View.VISIBLE : View.GONE);
			if (im.isOld())
				v.setBackgroundColor(Color.parseColor("#AAFFFFF0"));
			else
				v.setBackgroundColor(Color.parseColor("#00000000"));

			v.setTag(viewHolder);

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
			return MessengerService.getFriendIMs(this.context, this.friendId).get(position);
		}
	}

	@Override
	public int getCount()
	{
		synchronized (MessengerService.getFriendsInChat())
		{
			if (MessengerService.getFriendsInChat().get(this.friendId) != null)
				return MessengerService.getFriendIMs(this.context, this.friendId).size();
			return 0;
		}
	}

	static class ViewHolder
	{
		FrameLayout l;
		TextView txtTimestamp;
		TextView txtMessageFriend;
		ImageView imgAvatarFriend;
		TextView txtMessageSelf;
		ImageView imgAvatarSelf;
	}
}