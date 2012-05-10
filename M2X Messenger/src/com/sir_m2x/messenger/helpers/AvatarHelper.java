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
package com.sir_m2x.messenger.helpers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Calendar;

import org.openymsg.network.YahooUser;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class AvatarHelper
{
	/**
	 * Requests the avatar of the specified Yahoo ID based on the preferences
	 * set by the user
	 * 
	 * @param id
	 *            The ID of the person to load the avatar of
	 * @return True if a request was sent, otherwise false.
	 */
	public static boolean requestAvatarIfNeeded(final String id)
	{
		if (Preferences.loadAvatars.equals(Preferences.AVATAR_DONT_LOAD))
			return false;

		File f = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", id + ".jpg");
		if (!f.exists())
			try
			{
				MessengerService.getSession().requestPicture(id);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		else if (Preferences.loadAvatars.equals(Preferences.AVATAR_LOAD_ALWAYS)) // we have to refresh because the user wants
			try
			{
				MessengerService.getSession().requestPicture(id);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		else if (Preferences.avatarRefreshInterval != -1) // check and see if we should refresh the avatar
		{
			Calendar lastModified = Calendar.getInstance();
			lastModified.setTimeInMillis(f.lastModified());
			int days = Utils.daysBetween(lastModified, Calendar.getInstance());
			if (days >= Preferences.avatarRefreshInterval)
				try
				{
					MessengerService.getSession().requestPicture(id);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
		}
		return true;
	}

	/**
	 * Looks for the avatar of the specified ID and if found, returns it as a
	 * Bitmap object.
	 * 
	 * @param id
	 *            The ID of the person
	 * @return A Bitmap containing the loaded avatar (null if no avatar was
	 *         found)
	 */
	public static Bitmap loadAvatarFromSD(final String id)
	{
		File f = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", id + ".jpg");
		if (f.exists())
		{
			Bitmap avatar = BitmapFactory.decodeFile(f.getAbsolutePath());
			if (id.equals(MessengerService.getMyId()))
				MessengerService.setMyAvatar(avatar);
			else
				MessengerService.getFriendAvatars().put(id, avatar); // for use in the future
			return avatar;
		}
		return null;
	}
	
	/**
	 * Saves the passed bitmap to SD-Card
	 * 
	 * @param friendId
	 * 		The ID of the owner of this avatar
	 * @param avatar
	 * 		The avatar image to save to SD-Card
	 * @return
	 * 		True if the avatar was successfully save to SD-Card, otherwise False
	 */
	public static boolean saveAvatarToSD(final String friendId, final Bitmap avatar)
	{
		if (avatar == null || friendId == null || friendId.equals(""))
			return false;
		
		File file = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", friendId + ".jpg");
		OutputStream fOut = null;
		try
		{
			fOut = new FileOutputStream(file);
			avatar.compress(CompressFormat.JPEG, 100, fOut);
			fOut.flush();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			if (fOut != null)
				try
				{
					fOut.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return false;
				}
		}
		
		return true;
	}

	/**
	 * Retrieves the Yahoo! avatar of the specified user.
	 * 
	 * @param userId
	 *            The ID of the user to get the avatar of.
	 * @return A bitmap containing the avatar which is received.
	 */
	public static Bitmap getYahooAvatar(final String userId)
	{
		try
		{
			return BitmapFactory.decodeStream(new URL("http://img.msg.yahoo.com/avatar.php?yids=" + userId).openStream());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}

	public static void loadAllAvatars()
	{
		try
		{
			MessengerService.getSession().requestPicture(MessengerService.getMyId());

			for (YahooUser r : MessengerService.getSession().getRoster())
				MessengerService.getSession().requestPicture(r.getId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
				Bitmap avatar = getYahooAvatar(myId);
				if (avatar != null)
				{
					MessengerService.setMyAvatar(avatar);

					OutputStream fOut = null;
					File file = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", myId + ".jpg");
					try
					{
						fOut = new FileOutputStream(file);

						avatar.compress(CompressFormat.JPEG, 100, fOut);
						fOut.flush();
						fOut.close();
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

				for (YahooUser r : MessengerService.getSession().getRoster())
				{
					String id = r.getId();

					avatar = getYahooAvatar(r.getId());
					if (avatar != null)
					{
						MessengerService.getFriendAvatars().put(id, avatar);
						OutputStream fOut = null;
						File file = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", id + ".jpg");
						try
						{
							fOut = new FileOutputStream(file);

							avatar.compress(CompressFormat.JPEG, 100, fOut);
							fOut.flush();
							fOut.close();
						}
						catch (FileNotFoundException e)
						{
							e.printStackTrace();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}

			}
		}).start();
	}

	public static void refreshAvatars()
	{
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				if (Preferences.loadAvatars.equals(Preferences.AVATAR_LOAD_ALWAYS))
					getAllAvatars();
				else
				{
					File[] files = new File(Utils.storagePath + "/M2X Messenger/avatar-cache").listFiles(new FileFilter()
					{

						@Override
						public boolean accept(final File pathname)
						{
							if (pathname.getName().endsWith("jpg"))
								return true;
							return false;
						}
					});

					for (File f : files)
					{
						String id = f.getName().replace(".jpg", "");
						Bitmap avatar = AvatarHelper.getYahooAvatar(id);

						if (avatar != null)
						{
							if (id.equals(MessengerService.getMyId()))
								MessengerService.setMyAvatar(avatar);

							MessengerService.getFriendAvatars().put(id, avatar);
							OutputStream fOut = null;
							File file = new File(Utils.storagePath + "/M2X Messenger/avatar-cache", id + ".jpg");
							try
							{
								fOut = new FileOutputStream(file);
								avatar.compress(CompressFormat.JPEG, 100, fOut);
								fOut.flush();
								fOut.close();
							}
							catch (FileNotFoundException e)
							{
								e.printStackTrace();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}

					}
				}
			}
		}).start();
	}
}