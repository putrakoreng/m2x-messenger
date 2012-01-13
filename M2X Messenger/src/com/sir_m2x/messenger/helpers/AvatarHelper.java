/**
 * 
 */
package com.sir_m2x.messenger.helpers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import org.openymsg.network.YahooUser;

import android.graphics.Bitmap;
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
	 * Requests the avatar of the specified Yahoo ID
	 * 
	 * @param id
	 *            The ID of the person to load the avatar of
	 */
	public static void requestAvatar(final String id)
	{
		//TODO implement me!
		//for example when we manually want to download a person's avatar
	}

	/**
	 * Requests the avatar of the specified Yahoo ID based on the preferences
	 * set by the user
	 * 
	 * @param id
	 *      The ID of the person to load the avatar of
	 * @return
	 * 		True if a request was sent, otherwise false.
	 */
	public static boolean requestAvatarIfNeeded(final String id)
	{
		if (!Preferences.loadAvatars.equals(Preferences.AVATAR_LOAD_NEEDED))
			return false;
		
		
			File f = new File("/sdcard/M2X Messenger/avatar-cache", id + ".jpg");
			if (!f.exists())
				try
				{
					MessengerService.getSession().requestPicture(id);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			else if (Preferences.avatarRefreshInterval != -1)
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
		File f = new File("/sdcard/M2X Messenger/avatar-cache", id + ".jpg");
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
					//synchronized (MessengerService.friendAvatars)
					{
						Bitmap avatar = getYahooAvatar(r.getId());
						if (avatar != null)
							MessengerService.getFriendAvatars().put(id, avatar);
					}
				}

			}
		}).start();
	}
}