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
package com.sir_m2x.messenger.utils;

import java.net.URL;

import org.openymsg.network.YahooUser;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.sir_m2x.messenger.services.MessengerService;

/**
 * Several utility functions used throughout this projects.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class Utils
{
	/**
	 * Retrieves the Yahoo! avatar of the specified user.
	 * @param userId
	 * 		The ID of the user to get the avatar of.
	 * @return
	 * 		A bitmap containing the avatar which is received. 
	 */
	public static Bitmap getYahooAvatar(String userId)
	{
		try
		{
			return BitmapFactory.decodeStream(new URL(
					"http://img.msg.yahoo.com/avatar.php?yids=" + userId)
					.openStream());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Retrieves all the avatars of the people currently on our friends list.
	 */
	public static void getAllAvatars()
	{
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				//Load my own avatar first
				String myId = MessengerService.getSession().getLoginID().getId();
				//synchronized (MessengerService.myAvatar)
				{
					Bitmap avatar = getYahooAvatar(myId);
					if (avatar != null)
						MessengerService.setMyAvatar(avatar);
				}
				
				for (YahooUser r : MessengerService.getSession().getRoster())
				{
					String id = r.getId();
					if (!MessengerService.getFriendAvatars().containsKey(id))
					{
						//synchronized (MessengerService.friendAvatars)
						{
							Bitmap avatar = getYahooAvatar(r.getId());
							if (avatar != null)
							{
								MessengerService.getFriendAvatars().put(id, avatar);
								Log.d("M2X", "Avatar loaded for " + id);
							}
						}

					}
				}

			}
		}).start();
	}
	
	/**
	 * Qualifies a string with the complete package name.
	 * Note that this method is used primarily for sending and receiving intents and prevents 
	 * accidental naming collisions.
	 * 
	 * @param string
	 * 		The string to be qualified.
	 * @return
	 *		The qualified string.
	 */
	public static String qualify(String string)
	{
		return "com.sir_m2x.messenger." + string;
	}
}
