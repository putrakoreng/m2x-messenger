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
package com.sir_m2x.messenger;

import java.util.Date;

import org.openymsg.network.NetworkConstants;
import org.openymsg.network.YahooUser;
import org.openymsg.network.event.SessionAdapter;
import org.openymsg.network.event.SessionEvent;
import org.openymsg.network.event.SessionFileTransferEvent;
import org.openymsg.network.event.SessionFriendEvent;
import org.openymsg.network.event.SessionNotifyEvent;
import org.openymsg.network.event.SessionPictureEvent;

import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.Log;

import com.sir_m2x.messenger.classes.IM;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Utils;

/**
 * An extension of SessionAdapter. This class is primarily used to process
 * various events that are sent to us by the Session class. (ex: When a user is
 * online, etc.)
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class MySessionAdapter extends SessionAdapter
{
	public static MessengerService context = null;

	public MySessionAdapter(final MessengerService context)
	{
		MySessionAdapter.context = context;
	}

	@Override
	public void buzzReceived(final SessionEvent event)
	{
		String from = event.getFrom();
		IM im = new IM(from, NetworkConstants.BUZZ, event.getTimestamp(), false);

		Message msg = new Message();
		msg.obj = im;
		// we ask the handler to put the new message in its appropriate data
		// structure to prevent getting IllegalStateExceptions from ListView
		context.newImHandler.sendMessage(msg);

	}

	@Override
	public void offlineMessageReceived(final SessionEvent event)
	{
		String from = event.getFrom();
		String message = event.getMessage();
		
		message = Utils.stripYahooFormatting(message);
		
		if (message.contains("<ding>"))
		{
			buzzReceived(event);
			return;
		}

		IM im = new IM(from, message, event.getTimestamp(), true);

		Message msg = new Message();
		msg.obj = im;

		context.newImHandler.sendMessage(msg);
	}

	@Override
	public void messageReceived(final SessionEvent event)
	{
		String from = event.getFrom();
		String message = event.getMessage();
		

		message = Utils.stripYahooFormatting(message);
		
		IM im = new IM(from, message, event.getTimestamp());

		Message msg = new Message();
		msg.obj = im;

		context.newImHandler.sendMessage(msg);
	}

	@Override
	public void notifyReceived(final SessionNotifyEvent event)
	{
		if (event.isTyping())
		{
			YahooUser user = MessengerService.getSession().getRoster().getUser(event.getFrom());
			if (user == null)
				return;

			user.setIsTyping(event.getMode());

			if (!MessengerService.getFriendsInChat().containsKey(event.getFrom()))
			{
				String message = event.getFrom() + " is preparing a message...";

				ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
				Intent intent = new Intent();
				intent.setAction(MessengerService.INTENT_FRIEND_EVENT);
				intent.putExtra(Utils.qualify("event"), message);
				wrapper.sendBroadcast(intent);
			}

			ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
			Intent intent = new Intent();
			intent.setAction(MessengerService.INTENT_IS_TYPING);
			intent.putExtra(Utils.qualify("from"), event.getFrom());
			intent.putExtra(Utils.qualify("isTyping"), event.getMode());
			wrapper.sendBroadcast(intent);
		}

		return;
	}

	@Override
	public void friendsUpdateReceived(final SessionFriendEvent event)
	{
		String id = event.getUser().getId().toString();
		String statusMessage = event.getUser().getCustomStatusMessage();

		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED);
		intent.putExtra(Utils.qualify("who"), id);
		intent.putExtra(Utils.qualify("what"), statusMessage);

		wrapper.sendBroadcast(intent);

		if (statusMessage != null && !statusMessage.equals(""))
			MessengerService.getEventLog().log(id, statusMessage, new Date(System.currentTimeMillis()));
	}
	
	/*
	 * If the custom message of the friend is not changed, there is no need to log that in the event log
	 */
	public void friendsUpdateReceivedV2(final SessionFriendEvent event, final boolean messageChanged)
	{
		String id = event.getUser().getId().toString();
		String statusMessage = event.getUser().getCustomStatusMessage();

		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED);
		intent.putExtra(Utils.qualify("who"), id);
		intent.putExtra(Utils.qualify("what"), statusMessage);

		wrapper.sendBroadcast(intent);

		if (statusMessage != null && !statusMessage.equals("") && messageChanged)
			MessengerService.getEventLog().log(id, statusMessage, new Date(System.currentTimeMillis()));
	}

	@Override
	public void friendSignedOn(final SessionFriendEvent event)
	{
		YahooUser u = event.getUser();
		String id = u.getId().toString();

		if (!MessengerService.getYahooList().initFinished)
		{
			AvatarHelper.requestAvatarIfNeeded(id);
			return;
		}

		//FIXME A contact is online and I still receive a notification! (Pisht!)
		//TODO FIXED by YahooList... Check and see if it is really resolved

		//		for(String g : u.getGroupIds())
		//			for (YahooUser user : MessengerService.getYahooList().getFriendsList().get(g).getUsers())
		//				if(user.getId().equals(u.getId()) && user.getStatus() == u.getStatus())
		//					return;

		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_SIGNED_ON);
		intent.putExtra(Utils.qualify("who"), id);

		wrapper.sendBroadcast(intent);

		// request this contact's picture
		AvatarHelper.requestAvatarIfNeeded(id);

		MessengerService.getEventLog().log(id, "signed on", new Date(System.currentTimeMillis()));

	}

	@Override
	public void friendSignedOff(final SessionFriendEvent event)
	{
		if (!MessengerService.getYahooList().initFinished)
			return;

		if (event == null) //when we sign out, this event is fired with event == null
			return;

		String id = event.getUser().getId().toString();

		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_SIGNED_OFF);
		intent.putExtra(Utils.qualify("who"), id);

		wrapper.sendBroadcast(intent);

		MessengerService.getEventLog().log(id, "signed off", new Date(System.currentTimeMillis()));
	}

	@Override
	public void pictureReceived(final SessionPictureEvent ev)
	{
		String id = ev.getFrom();
		byte[] pictureData = ev.getPictureData();
		Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);

		// save this picture to sdcard
		//		OutputStream fOut = null;
		//		File file = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", id + ".jpg");

		//		try
		//		{
		//			fOut = new FileOutputStream(file);
		//
		//			bitmap.compress(CompressFormat.JPEG, 100, fOut);
		//			fOut.flush();
		//		}
		//		catch (FileNotFoundException e)
		//		{
		//			e.printStackTrace();
		//		}
		//		catch (IOException e)
		//		{
		//			e.printStackTrace();
		//		}
		//		finally
		//		{
		//			if (fOut != null)
		//				try
		//				{
		//					fOut.close();
		//				}
		//				catch (IOException e)
		//				{
		//					e.printStackTrace();
		//				}
		//		}

		if (!AvatarHelper.saveAvatarToSD(id, bitmap))
			return;

		if (id.equals(MessengerService.getMyId()))
			MessengerService.setMyAvatar(bitmap);
		else
			MessengerService.getFriendAvatars().put(id, bitmap);
		context.sendBroadcast(new Intent(MessengerService.INTENT_LIST_CHANGED));
	}

	public void notifyListChanged()
	{
		context.sendBroadcast(new Intent(MessengerService.INTENT_LIST_CHANGED));
	}

	@Override
	public void fileTransferReceived(final SessionFileTransferEvent event)
	{
		Log.d("M2X", "A file??!");
	}
}