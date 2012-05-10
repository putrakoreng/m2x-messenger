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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.TreeMap;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.os.Environment;

import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.sir_m2x.messenger.helpers.sqlite.ConversationContents;
import com.sir_m2x.messenger.helpers.sqlite.Conversations;
import com.sir_m2x.messenger.services.MessengerService;

/**
 * Several utility functions used throughout this projects.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class Utils
{
	public static TreeMap<String, String> smileySymbolTable = null;
	public static String storagePath = "";
	public static float deviceDensity = 0;
	
	/**
	 * Initialize several required stuff before starting the whole application
	 */
	public static void initializeEnvironment(final Context context)
	{
		System.setProperty("http.keepAlive", "false"); // for compatibility with Android 2.1+
		storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(context, storagePath + "/M2X Messenger/crash-log", "http://sirm2x.heliohost.org/m2x-messenger/upload.php"));
		initializeFolders();
	}

	private static void initializeFolders()
	{
		// create folders
		File f = new File(storagePath + "/M2X Messenger");
		if (!f.exists())
			f.mkdir();
		f = new File(storagePath + "/M2X Messenger/avatar-cache");
		if (!f.exists())
			f.mkdir();
		f = new File(storagePath + "/M2X Messenger/crash-log");
		if (!f.exists())
			f.mkdir();
		f = new File(storagePath + "/M2X Messenger/history");
		if (!f.exists())
			f.mkdir();
	}

	/**
	 * Reads event log from SD-Card and loads it in the supplied EventLogger
	 * object
	 * 
	 * @param log
	 *            The EventLogger object to load with the saved log
	 */
	public static void loadEventLog(final EventLogger log)
	{
		File f = new File(storagePath + "/M2X Messenger", "event.log");
		if (!f.exists())
			return;

		StringBuffer result = new StringBuffer();

		try
		{
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String input;
			while ((input = bf.readLine()) != null)
				result.append(input);
			bf.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		log.deserialize(result.toString());
	}

	public static void saveEventLog(final EventLogger log)
	{
		try
		{
			File f = new File(storagePath + "/M2X Messenger", "event.log");
			if (!f.exists())
				f.createNewFile();
			String logs = log.serialize().toString();

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			bw.write(logs);
			bw.flush();
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Qualifies a string with the complete package name. Note that this method
	 * is used primarily for sending and receiving intents and prevents
	 * accidental naming collisions.
	 * 
	 * @param string
	 *            The string to be qualified.
	 * @return The qualified string.
	 */
	public static String qualify(final String string)
	{
		return "com.sir_m2x.messenger." + string;
	}

	/**
	 * A convenience method to convert a string to its italic equivalent in HTML
	 * 
	 * @param string
	 *            String to convert
	 * @return The string enclosed in proper italic HTML tags.
	 */
	public static String toItalic(final String string)
	{
		return "<i>" + string + "</i>";
	}

	/**
	 * A convenience method to convert a string to its bold equivalent in HTML
	 * 
	 * @param string
	 *            String to convert
	 * @return The string enclosed in proper bold HTML tags.
	 */
	public static String toBold(final String string)
	{
		return "<b>" + string + "</b>";
	}

	public static int daysBetween(final Calendar startDate, final Calendar endDate)
	{
		Calendar date = (Calendar) startDate.clone();
		int daysBetween = 0;
		while (date.before(endDate))
		{
			date.add(Calendar.DAY_OF_MONTH, 1);
			daysBetween++;
		}
		return daysBetween;
	}
	
	/**
	 * Determines if a particular service is running
	 * @param context
	 * 		Android's context
	 * @param className
	 * 		The class name of the service
	 * @return
	 * 		True if the service is running
	 */
	public static boolean isServiceRunning(final Context context, final String className)
	{
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if (service.service.getClassName().equals(className))
				return true;

		return false;
	}
	
	/**
	 * Parses the Emoticons.plist file to load the smiley symbols into memory
	 * @param context
	 * 		Android Context
	 * @return
	 * 		The list of all smileys and their equivalents
	 */
	public static Dict parseSmileys(final Context context)
	{
		Dict result = null;
		PListXMLParser parser = new PListXMLParser();
		parser.setHandler(new PListXMLHandler());
		
		InputStream is = null;
		try
		{
			is = context.getResources().getAssets().open("smiley/Emoticons.plist");	
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			parser.parse(is);
			PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
			result = (Dict)((Dict)actualPList.getRootElement()).getConfigMap().get("Emoticons");
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		TreeMap<String, String> symbolTable = new TreeMap<String, String>();
		Set<String> allSmileyNames = result.getConfigMap().keySet();
		
		for(String smileyName : allSmileyNames)
		{
			Array configurationArray = ((Dict)result.getConfigMap().get(smileyName)).getConfigurationArray("Equivalents");
			
			for (int i = 0; i < configurationArray.size() ; i++)
				symbolTable.put(((com.longevitysoft.android.xml.plist.domain.String)configurationArray.get(i)).getValue(), smileyName);
		}
		
		Utils.smileySymbolTable = symbolTable;
		return result;
	}
	
	/**
	 * Analyzes a string and replaces the smiley symbols with their picture equivalents
	 * @param input
	 * 		The input string to be analyzed
	 * @return
	 * 		The string containing the smiley images
	 */
	public static String parseTextForSmileys(String input)
	{
		String result = "";
		ArrayList<String> matches = new ArrayList<String>();	// array containing possible matches to smiley symbols
		ArrayList<String> toBeRemoved = new ArrayList<String>();	// array containing shorter matches which are not desired
		
		for(String smiley : smileySymbolTable.keySet())		// find all smiley matches
			if (input.contains(smiley))
				matches.add(smiley);
		
		for(int i = 0; i < matches.size(); i++)	// find possible short-length matches and mark them to be removed
			for(int j = 0; j < matches.size(); j++)
				if (matches.get(i).contains(matches.get(j)) && !matches.get(i).equals(matches.get(j)))
					toBeRemoved.add(matches.get(j));
		
		matches.removeAll(toBeRemoved);
		
		for(String item : matches)	// replace smiley symbols with their equivalent file names
			input = input.replace(item, "<img src=\"" + smileySymbolTable.get(item) + "\"/>");
		
		result = input;
		
		return result;
	}
	
	/**
	 * Strips a string of its Yahoo formatting characters
	 * @param input
	 * 		The input string
	 * @return
	 * 		The string stripped of all formatting characters
	 */
	public static String stripYahooFormatting(String input)
	{
		if (input.contains("")) // we have some formatting characters -- how stupid is Yahoo?
		{
			String[] splitted = input.split("\\[(\\d|\\D)+?m", 2);	//note: non-greedy version is desired thus the '?' mark
			input = splitted[0] + splitted[1];
		}
		
		return input;
	}
	
	/**
	 * Reads a text from the supplied InputStream and returns the read text as a String 
	 * @param is
	 * 		The InputStream to read from
	 * @return
	 * 		A string containing the read text
	 * @throws IOException
	 */
	public static String readText(final InputStream is) throws IOException
	{
		StringBuilder result = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		char[] buffer = new char[1024];
        int numRead=0;
        
        while((numRead=br.read(buffer)) != -1)
        {
            String readData = String.valueOf(buffer, 0, numRead);
            result.append(readData);
            buffer = new char[1024];
        }
		
		br.close();
		
		return result.toString();
	}
	
	public static void saveConversationHistory(final Context context, final String id)
	{
		/*
		LinkedList<IM> ims = MessengerService.getFriendsInChat().get(id);	// TODO change this to getFriendIMs??
		
		if (ims.size() == 0)
			return;
		
		StringBuilder out = new StringBuilder();
		
		for (IM im : ims)
			out.append(im.serialize() + IM.OUTER_SEPARATOR);
		
		try
		{
			File f = new File(storagePath + "/M2X Messenger/history", MessengerService.getMyId() + "-" + id + ".htr");
			if (!f.exists())
				f.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			bw.write(out.toString());
			bw.flush();
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		*/
		
//		LinkedList<IM> ims = MessengerService.getFriendsInChat().get(id);
//		long foreignKey = Conversations.getConversationId(context, MessengerService.getMyId(), id);
//		
//		for(IM im : ims)
//			ConversationContents.insert(context, MessengerService.getMyId(), im, foreignKey);
	}
	
	public static void loadConversationHistory(final Context context, final String id)
	{
		// delete old history based on user's preference
		if (Preferences.historyKeep != Preferences.HISTORY_ALWAYS_KEEP)
		{
			int days = Preferences.historyKeep * 7;
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -days);
			
			ConversationContents.delete(context, c.getTimeInMillis());
		}
		
		MessengerService.getFriendsInChat().put(id,ConversationContents.select(context, MessengerService.getMyId(), id));
	}
	
	public static void clearHistory(final Context context, final String id)
	{
		Conversations.delete(context, MessengerService.getMyId(), id);
		MessengerService.getFriendsInChat().get(id).clear();
	}
}