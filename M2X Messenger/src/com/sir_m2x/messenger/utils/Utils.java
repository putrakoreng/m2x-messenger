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

import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Array;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;

/**
 * Several utility functions used throughout this projects.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class Utils
{
	public static TreeMap<String, String> smileySymbolTable = null;
	
	/**
	 * Initialize several required stuff before starting the whole application
	 */
	public static void initializeEnvironment(final Context context)
	{
		System.setProperty("http.keepAlive", "false"); // for compatibility with Android 2.1+
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(context, "/sdcard/M2X Messenger/crash-log", "http://sirm2x.heliohost.org/m2x-messenger/upload.php"));
		initializeFolders();
	}

	private static void initializeFolders()
	{
		// create folders
		File f = new File("/sdcard/M2X Messenger");
		if (!f.exists())
			f.mkdir();
		f = new File("/sdcard/M2X Messenger/avatar-cache");
		if (!f.exists())
			f.mkdir();
		f = new File("/sdcard/M2X Messenger/crash-log");
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
		File f = new File("/sdcard/M2X Messenger", "event.log");
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
			File f = new File("/sdcard/M2X Messenger", "event.log");
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
	
	public static String parseTextForSmileys(String input)
	{
		String result = "";
		ArrayList<String> matches = new ArrayList<String>();
		ArrayList<String> toBeRemoved = new ArrayList<String>();
		
		for(String smiley : smileySymbolTable.keySet())
			if (input.contains(smiley))
				matches.add(smiley);
		
		for(int i = 0; i < matches.size(); i++)
			for(int j = 0; j < matches.size(); j++)
				if (matches.get(i).contains(matches.get(j)) && !matches.get(i).equals(matches.get(j)))
					toBeRemoved.add(matches.get(j));
		
		matches.removeAll(toBeRemoved);
		
		for(String item : matches)
			input = input.replace(item, "<img src=\"" + smileySymbolTable.get(item)) + "\"/>";
		
		result = input;
		
		return result;
	}

}