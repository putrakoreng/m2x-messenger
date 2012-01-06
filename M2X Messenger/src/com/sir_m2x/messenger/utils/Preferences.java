/**
 * 
 */
package com.sir_m2x.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A class which represents user preferences.
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
	
	
	
	/**
	 * Preference values
	 */
	public static String loadAvatars = AVATAR_LOAD_NEEDED;
	public static String vibration = VIBRATE_ON;
	public static boolean showToasts = true;
	public static String audibles = AUDIBLE_PLAY_ALL;
	public static boolean showOffline = true;
	public static int avatarRefreshInterval = 1;
	
	public static void loadPreferences(final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Preferences.loadAvatars = preferences.getString("loadAvatars", "loadNeeded");
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.showToasts = preferences.getBoolean("showToasts", true);
		Preferences.audibles = preferences.getString("audibles", "playAll");		
		Preferences.showOffline= preferences.getBoolean("showOffline", true);
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.avatarRefreshInterval = Integer.parseInt(preferences.getString("avatarPolicy", "1"));
	}
	
}
