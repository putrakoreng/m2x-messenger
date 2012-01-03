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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 * A class to log any uncaught exception.
 * The class's code is derived from:
 * 	<a href="http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application"/>
 * So the thanks goes to the original author.
 *  
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class CustomExceptionHandler implements UncaughtExceptionHandler
{

	private UncaughtExceptionHandler defaultUEH;

	private String localPath;

	private String url;

	/*
	 * if any of the parameters is null, the respective functionality will not
	 * be used
	 */
	public CustomExceptionHandler(final String localPath, final String url)
	{
		this.localPath = localPath;
		this.url = url;
		this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(final Thread t, final Throwable e)
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHMMSS");
		String timestamp = df.format(new Date(System.currentTimeMillis()));
		
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String stacktrace = result.toString();
		printWriter.close();
		String filename = timestamp + ".stacktrace";

		if (this.url != null)
			sendToServer(stacktrace, filename);
		if (this.localPath != null)
			writeToFile(stacktrace, filename);

		this.defaultUEH.uncaughtException(t, e);
	}

	private void writeToFile(final String stacktrace, final String filename)
	{
		try
		{
			File f = new File(this.localPath + "/");
			if (!f.exists())
				f.mkdir();
			BufferedWriter bos = new BufferedWriter(new FileWriter(this.localPath + "/" + filename));
			bos.write(stacktrace);
			bos.flush();
			bos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void sendToServer(final String stacktrace, final String filename)
	{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(this.url);
		List<org.apache.http.NameValuePair> nvps = new ArrayList<org.apache.http.NameValuePair>();
		nvps.add(new BasicNameValuePair("filename", filename));
		nvps.add(new BasicNameValuePair("stacktrace", stacktrace));
		try
		{
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			httpClient.execute(httpPost);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
