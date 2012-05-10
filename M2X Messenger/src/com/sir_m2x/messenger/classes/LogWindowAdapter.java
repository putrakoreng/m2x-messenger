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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.EventLogger;

/**
 * An instance of this class is used as an adapter for the ListView in LogWindowActivity
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class LogWindowAdapter extends BaseAdapter
{
	Context context;
	
	public LogWindowAdapter(final Context context)
	{
		this.context = context;
	}
	
	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent)
	{
		View v;
		ViewHolder viewHolder;
		
		if (convertView == null)
		{
			v = ((LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_window_row_friend, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.txtMessage = (TextView) v.findViewById(R.id.friendMessageTextView);
			viewHolder.txtTimeStamp = (TextView) v.findViewById(R.id.timeStampTextView);
			viewHolder.img = (ImageView) v.findViewById(R.id.imgFriendAvatarChat);
		}
		else
		{
			v = convertView;
			viewHolder = (ViewHolder) v.getTag();
		}

		EventLogger.LogFormat log = (EventLogger.LogFormat) getItem(position);
		String id = log.getWho();

		viewHolder.txtMessage.setText(log.eventToHtml());
		viewHolder.txtTimeStamp.setText(log.timeToHtml());

		if (id.equals(MessengerService.getMyId()) && MessengerService.getMyAvatar() != null)
			viewHolder.img.setImageBitmap(MessengerService.getMyAvatar());
		else if (MessengerService.getFriendAvatars().containsKey(id))
			viewHolder.img.setImageBitmap(MessengerService.getFriendAvatars().get(id));
		else if (id.contains("M2X Messenger"))
			viewHolder.img.setImageResource(R.drawable.ic_launcher_noborder);
		else
			viewHolder.img.setImageResource(R.drawable.yahoo_no_avatar);

		v.setTag(viewHolder);
		return v;
	}

	@Override
	public long getItemId(final int arg0)
	{
		return arg0;
	}

	@Override
	public Object getItem(final int arg0)
	{
		return MessengerService.getEventLog().getEventLog().get(arg0);
	}

	@Override
	public int getCount()
	{
		return MessengerService.getEventLog().getEventLog().size();
	}
	
	static class ViewHolder
	{
		TextView txtMessage;
		TextView txtTimeStamp;
		ImageView img;
	}
}
