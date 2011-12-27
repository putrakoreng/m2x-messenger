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
package com.sir_m2x.messenger;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.openymsg.network.YahooUser;
import org.openymsg.network.event.SessionAdapter;
import org.openymsg.network.event.SessionChatEvent;
import org.openymsg.network.event.SessionErrorEvent;
import org.openymsg.network.event.SessionEvent;
import org.openymsg.network.event.SessionExceptionEvent;
import org.openymsg.network.event.SessionFriendEvent;
import org.openymsg.network.event.SessionNotifyEvent;
import org.openymsg.network.event.SessionPictureEvent;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.IM;
import com.sir_m2x.messenger.utils.Utils;
/**
 * An extension of SessionAdapter.
 * This class is primarily used to process various events that are sent to us by the Session class.
 * (ex: When a user is online, etc.)
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class MySessionAdapter extends SessionAdapter
{
	public static Context context = null;
	

	
	@Override
	public void inputExceptionThrown(SessionExceptionEvent event)
	{
		Log.wtf("WTF", "inputExceptionThrown");
		event.getException().printStackTrace();
	}
	@Override
	public void errorPacketReceived(SessionErrorEvent event)
	{
		Log.wtf("WTF", "Error packet received");
		super.errorPacketReceived(event);
	}
	public MySessionAdapter(Context context)
	{
		MySessionAdapter.context = context;
	}
	
	@Override
	public void friendAddedReceived(SessionFriendEvent event)
	{
		super.friendAddedReceived(event);
	}

	@Override
	public void chatMessageReceived(SessionChatEvent event)
	{

	}

	@Override
	public void buzzReceived(SessionEvent event)
	{
		String from = event.getFrom();
		IM im = new IM(from, "BUZZ!!!", event.getTimestamp(), false, true);

		synchronized (MessengerService.getFriendsInChat())
		{

			if (!MessengerService.getFriendsInChat().keySet().contains(from))
			{
				LinkedList<IM> ims = new LinkedList<IM>();
				ims.add(im);
				MessengerService.getFriendsInChat().put(from, ims);
				Intent intent = new Intent(MessengerService.INTENT_NEW_IM_ADDED);
				intent.putExtra(Utils.qualify("from"), im.getSender());

				ContextWrapper wrapper = new ContextWrapper(
						MySessionAdapter.context);
				wrapper.sendBroadcast(intent);
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			else
				MessengerService.getFriendsInChat().get(from).add(im);

			/**
			 * for later use in the contacts list activity we want to show which
			 * friends are typing a message
			 */
			synchronized (MessengerService.getUnreadIMs())
			{
				HashMap<String, Integer> unreadIMs = MessengerService
						.getUnreadIMs();
				if (unreadIMs.containsKey(from))
				{
					int count = unreadIMs.get(from).intValue();
					unreadIMs.remove(from);
					unreadIMs.put(from, ++count);
				}
				else
				{
					unreadIMs.put(from, new Integer(1));
				}
			}

			ContextWrapper wrapper = new ContextWrapper(
					MySessionAdapter.context);
			Intent intent = new Intent();
			intent.setAction(MessengerService.INTENT_BUZZ);
			intent.putExtra(Utils.qualify("from"), from);
			wrapper.sendBroadcast(intent);
		}
	}
	
	@Override
	public void offlineMessageReceived(SessionEvent event)
	{
		
		try
		{
			// wait for everything to load up perfectly then send the broadcast
			Thread.sleep(5000);
		}
		catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
		
		String from = event.getFrom();
		String message = event.getMessage();
		if (message.contains("<ding>"))
		{
			buzzReceived(event);
			return;
		}
		
		IM im = new IM(from, message, event.getTimestamp(), true, false);

		synchronized (MessengerService.getFriendsInChat())
		{

			if (!MessengerService.getFriendsInChat().keySet().contains(from))
			{
				LinkedList<IM> ims = new LinkedList<IM>();
				ims.add(im);
				MessengerService.getFriendsInChat().put(from, ims);
				Intent intent = new Intent(MessengerService.INTENT_NEW_IM_ADDED);
				intent.putExtra(Utils.qualify("message"), im.getMessage());
				intent.putExtra(Utils.qualify("from"), im.getSender());

				ContextWrapper wrapper = new ContextWrapper(
						MySessionAdapter.context);
				wrapper.sendBroadcast(intent);
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			else
				MessengerService.getFriendsInChat().get(from).add(im);

			/**
			 * for later use in the contacts list activity we want to show which
			 * friends are typing a message
			 */
			synchronized (MessengerService.getUnreadIMs())
			{
				HashMap<String, Integer> unreadIMs = MessengerService
						.getUnreadIMs();
				if (unreadIMs.containsKey(from))
				{
					int count = unreadIMs.get(from).intValue();
					unreadIMs.remove(from);
					unreadIMs.put(from, ++count);
				}
				else
				{
					unreadIMs.put(from, new Integer(1));
				}
			}

			ContextWrapper wrapper = new ContextWrapper(
					MySessionAdapter.context);
			Intent intent = new Intent();
			intent.setAction(MessengerService.INTENT_NEW_IM);
			intent.putExtra(Utils.qualify("from"), from);
			intent.putExtra(Utils.qualify("message"), im.getMessage());
			wrapper.sendBroadcast(intent);
		}
	}

	@Override
	public void messageReceived(SessionEvent event)
	{
		String from = event.getFrom();
		IM im = new IM(from, event.getMessage(), event.getTimestamp());

		synchronized (MessengerService.getFriendsInChat())
		{

			if (!MessengerService.getFriendsInChat().keySet().contains(from))
			{
				LinkedList<IM> ims = new LinkedList<IM>();
				ims.add(im);
				MessengerService.getFriendsInChat().put(from, ims);
				Intent intent = new Intent(MessengerService.INTENT_NEW_IM_ADDED);
				intent.putExtra(Utils.qualify("message"), im.getMessage());
				intent.putExtra(Utils.qualify("from"), im.getSender());

				ContextWrapper wrapper = new ContextWrapper(
						MySessionAdapter.context);
				wrapper.sendBroadcast(intent);
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			else
				MessengerService.getFriendsInChat().get(from).add(im);

			/**
			 * for later use in the contacts list activity we want to show which
			 * friends are typing a message
			 */
			synchronized (MessengerService.getUnreadIMs())
			{
				HashMap<String, Integer> unreadIMs = MessengerService
						.getUnreadIMs();
				if (unreadIMs.containsKey(from))
				{
					int count = unreadIMs.get(from).intValue();
					unreadIMs.remove(from);
					unreadIMs.put(from, ++count);
				}
				else
				{
					unreadIMs.put(from, new Integer(1));
				}
			}

			ContextWrapper wrapper = new ContextWrapper(
					MySessionAdapter.context);
			Intent intent = new Intent();
			intent.setAction(MessengerService.INTENT_NEW_IM);
			intent.putExtra(Utils.qualify("from"), from);
			intent.putExtra(Utils.qualify("message"), im.getMessage());
			wrapper.sendBroadcast(intent);
		}
	}

	@Override
	public void notifyReceived(SessionNotifyEvent event)
	{
		if (event.isTyping())
		{
			MessengerService.getSession().getRoster().getUser(event.getFrom())
					.setIsTyping(event.getMode());

			if (!MessengerService.getFriendsInChat().containsKey(
					event.getFrom()))
			{
				String message = event.getFrom() + " is preparing a message...";

				ContextWrapper wrapper = new ContextWrapper(
						MySessionAdapter.context);
				Intent intent = new Intent();
				intent.setAction(MessengerService.INTENT_FRIEND_EVENT);
				intent.putExtra(Utils.qualify("event"), message);
				wrapper.sendBroadcast(intent);
			}

			ContextWrapper wrapper = new ContextWrapper(
					MySessionAdapter.context);
			Intent intent = new Intent();
			intent.setAction(MessengerService.INTENT_IS_TYPING);
			intent.putExtra(Utils.qualify("from"), event.getFrom());
			intent.putExtra(Utils.qualify("isTyping"), event.getMode());
			wrapper.sendBroadcast(intent);
		}

		return;
	}

	@Override
	public void chatExitReceived(SessionChatEvent event)
	{
		Log.d("M2X", event.getChatUser().toString());
		// TODO Auto-generated method stub
	}

	@Override
	public void friendsUpdateReceived(SessionFriendEvent event)
	{
		String id = event.getUser().getId().toString();
		String statusMessage = event.getUser().getCustomStatusMessage();
		
		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED);
		intent.putExtra(Utils.qualify("who"), id);
		intent.putExtra(Utils.qualify("what"), statusMessage);
		
		wrapper.sendBroadcast(intent);
		
		if (statusMessage != null && !statusMessage.isEmpty())
		MessengerService.getEventLog().log(id, statusMessage, new Date(System.currentTimeMillis()));
	}
	
	@Override
	public void friendSignedOn(SessionFriendEvent event)
	{
		YahooUser u = event.getUser();
		String id = u.getId().toString();
		
		ContextWrapper wrapper = new ContextWrapper(MySessionAdapter.context);
		Intent intent = new Intent();
		intent.setAction(MessengerService.INTENT_FRIEND_SIGNED_ON);
		intent.putExtra(Utils.qualify("who"), id);
		
		wrapper.sendBroadcast(intent);
		
		MessengerService.getEventLog().log(id, "signed on", new Date(System.currentTimeMillis()));

	}
	
	@Override
	public void friendSignedOff(SessionFriendEvent event)
	{
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
	public void pictureReceived(SessionPictureEvent ev)
	{
		byte[] pictureData = ev.getPictureData();
		
		// TODO Auto-generated method stu
		MessengerService.getFriendAvatars().put(ev.getFrom(), BitmapFactory.decodeByteArray(pictureData, 0,pictureData.length));
	}

}