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
package com.sir_m2x.messenger.utils;

import org.openymsg.network.YahooUser;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A class which represents user preferences.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class Preferences
{
	/**
	 * Constant declarations
	 */
	public static final String AUDIBLE_PLAY_ALL = "playAll";
	public static final String AUDIBLE_PLAY_IM = "playIm";
	public static final String AUDIBLE_PLAY_STATUS = "playStatus";
	public static final String AUDIBLE_DONT_PLAY = "noPlay";

	public static final String VIBRATE_ON = "on";
	public static final String VIBRATE_OFF = "off";
	public static final String VIBRATE_SYSTEM = "system";

	public static final String AVATAR_LOAD_NEEDED = "loadNeeded";
	public static final String AVATAR_LOAD_ALWAYS = "loadAlways";
	public static final String AVATAR_DONT_LOAD = "noLoad";
	
	public static final int HISTORY_ALWAYS_KEEP = -1;

	/**
	 * Login preferences
	 */
	public static String username;
	public static String password;
	public static int status;
	public static String customStatus;
	public static boolean busy;
	public static boolean remember;

	/**
	 * Preference screen values
	 */
	public static String loadAvatars = AVATAR_LOAD_NEEDED;
	public static String vibration = VIBRATE_ON;
	public static boolean showToasts = true;
	public static int toastGravity = 0;
	public static int xOffset = 0;
	public static int yOffset = 0;
	public static String audibles = AUDIBLE_PLAY_ALL;
	public static boolean showOffline = true;
	public static boolean onlinesFirst = false;
	public static int avatarRefreshInterval = 1;
	public static boolean saveLog = true;
	public static int audibleStream = 5;
	public static boolean logCrash = true;
	public static boolean timeStamp = true;
	public static boolean history = true;
	public static int historyKeep = HISTORY_ALWAYS_KEEP;

	/**
	 * Reads the Shared Preferences for this application and loads them into
	 * their corresponding variables
	 * 
	 * @param context
	 *            The context which we want to read the preferences of
	 */
	public static void loadPreferences(final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		Preferences.username = preferences.getString("username", "");
		Preferences.password = preferences.getString("password", "");
		Preferences.status = preferences.getInt("status", 0);
		Preferences.customStatus = preferences.getString("customStatus", "");
		Preferences.busy = preferences.getBoolean("busy", false);
		Preferences.remember = preferences.getBoolean("remember", false);

		Preferences.loadAvatars = preferences.getString("loadAvatars", "loadNeeded");
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.showToasts = preferences.getBoolean("showToasts", true);
		Preferences.toastGravity = Integer.parseInt(preferences.getString("toastGravity", "-1"));
		try
		{
			Preferences.xOffset = Integer.parseInt(preferences.getString("xOffset", "0"));
			Preferences.yOffset = Integer.parseInt(preferences.getString("yOffset", "0"));
		}
		catch (Exception e)
		{
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("xOffset", "0");
			editor.putString("yOffset", "0");
			editor.commit();
		}
		Preferences.audibles = preferences.getString("audibles", "playAll");
		Preferences.audibleStream = Integer.parseInt(preferences.getString("stream", "1"));
		Preferences.showOffline = preferences.getBoolean("showOffline", true);
		Preferences.onlinesFirst = preferences.getBoolean("onlinesFirst", false);
		YahooUser.onlinesFirst = onlinesFirst;
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.avatarRefreshInterval = Integer.parseInt(preferences.getString("avatarPolicy", "1"));
		Preferences.saveLog = preferences.getBoolean("saveLog", true);
		Preferences.logCrash = preferences.getBoolean("logCrash", true);
		Preferences.timeStamp = preferences.getBoolean("timeStamp", true);
		Preferences.history = preferences.getBoolean("history", true);
		Preferences.historyKeep = Integer.parseInt(preferences.getString("historyKeep", String.valueOf(HISTORY_ALWAYS_KEEP)));
	}

	/**
	 * Saves login preferences to the SharedPreferences of the current
	 * application
	 * 
	 * @param context
	 *            The context which we want to save the preferences for
	 */
	public static void saveLoginPreferences(final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString("username", username);
		editor.putString("password", password);
		editor.putInt("status", status);
		editor.putString("customStatus", customStatus);
		editor.putBoolean("busy", busy);
		editor.putBoolean("remember", remember);

		editor.commit();
	}
}
