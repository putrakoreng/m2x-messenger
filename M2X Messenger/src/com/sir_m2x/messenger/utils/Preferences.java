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
	 * Login preferences
	 */
	public static String username;
	public static String password;
	public static int status;
	public static String customStatus;
	public static boolean busy;
	
	
	/**
	 * Preference screen values
	 */
	public static String loadAvatars = AVATAR_LOAD_NEEDED;
	public static String vibration = VIBRATE_ON;
	public static boolean showToasts = true;
	public static String audibles = AUDIBLE_PLAY_ALL;
	public static boolean showOffline = true;
	public static int avatarRefreshInterval = 1;
	public static boolean saveLog = true;
	
	/**
	 * Reads the Shared Preferences for this application and loads them into their
	 * corresponding variables
	 * @param context
	 * 		The context which we want to read the preferences of
	 */
	public static void loadPreferences(final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		Preferences.username = preferences.getString("username", "");
		Preferences.password = preferences.getString("password", "");
		Preferences.status = preferences.getInt("status", 0);
		Preferences.customStatus = preferences.getString("customStatus", "");
		Preferences.busy = preferences.getBoolean("busy", false);
		
		Preferences.loadAvatars = preferences.getString("loadAvatars", "loadNeeded");
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.showToasts = preferences.getBoolean("showToasts", true);
		Preferences.audibles = preferences.getString("audibles", "playAll");		
		Preferences.showOffline = preferences.getBoolean("showOffline", true);
		Preferences.vibration = preferences.getString("vibration", "on");
		Preferences.avatarRefreshInterval = Integer.parseInt(preferences.getString("avatarPolicy", "1"));
		Preferences.saveLog = preferences.getBoolean("saveLog", true);
	}
	
	
	/**
	 * Saves login preferences to the SharedPreferences of the current application
	 * @param context
	 * 		The context which we want to save the preferences for
	 */
	public static void saveLoginPreferences(final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString("username", username);
		editor.putString("password", password);
		editor.putInt("status", status);
		editor.putString("customStatus", customStatus);
		editor.putBoolean("busy",busy);

		editor.commit();
	}
}
