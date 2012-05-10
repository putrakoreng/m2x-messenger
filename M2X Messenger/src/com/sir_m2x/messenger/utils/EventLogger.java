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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.text.Html;
import android.text.Spanned;

/**
 * A simple event logger that logs various events in the system. Currently it
 * only logs status changes of the people on our friends list.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class EventLogger
{
	private final List<LogFormat> eventLog;

	public List<LogFormat> getEventLog()
	{
		return this.eventLog;
	}

	public EventLogger()
	{
		this.eventLog = Collections.synchronizedList(new LinkedList<LogFormat>());
	}

	public synchronized void log(final String who, final String event, final Date timeStamp)
	{
		this.log(new LogFormat(who, event, timeStamp));
	}
	
	public synchronized void log(final String who, final String event, final long timeStamp)
	{
		this.log(new LogFormat(who, event, timeStamp));
	}

	public synchronized void log(final LogFormat logObject)
	{
		this.eventLog.add(logObject);
	}

	/**
	 * Serializes the information in the current log.
	 * Suitable for when we want to save to log as a file
	 * @return
	 * 		The serialized logs
	 */
	public StringBuffer serialize()
	{
		StringBuffer out = new StringBuffer();
		
		synchronized (this.eventLog)
		{
			for (LogFormat log : this.eventLog)
				out.append(log.who + " " + log.event + " " + log.timeStamp + "  ");
		}
		
		return out;
	}
	
	public void deserialize(final String input)
	{
		String[] logs = input.split("  ");
		if (logs.length == 0)
			return;
		
		for (String log : logs)
		{
			String[] split = log.split(" ");
			if (split.length < 3)	// The log is empty!
				return;
			LogFormat lf = new LogFormat(split[0], split[1], Long.parseLong(split[2]));
			this.eventLog.add(lf);
		}
	}

	public static class LogFormat
	{
		private String who;
		private String event;
		private long timeStamp;

		public String getWho()
		{
			return this.who;
		}

		public void setWho(final String who)
		{
			this.who = who;
		}

		public String getEvent()
		{
			return this.event;
		}

		public void setEvent(final String event)
		{
			this.event = event;
		}

		public Date getTimeStamp()
		{
			return new Date(this.timeStamp);
		}

		public void setTimeStamp(final Date timeStamp)
		{
			this.timeStamp = timeStamp.getTime();
		}

		public LogFormat(final String who, final String event, final Date timeStamp)
		{
			this.setWho(who);
			this.event = event;
			this.timeStamp = timeStamp.getTime();
		}
		
		public LogFormat(final String who, final String event, final long timeStamp)
		{
			this.setWho(who);
			this.event = event;
			this.timeStamp = timeStamp;
		}

		public Spanned eventToHtml()
		{
			return Html.fromHtml("<b>" + this.who + "</b>: " + this.event);
		}

		public Spanned timeToHtml()
		{
			Date today = new Date(System.currentTimeMillis());
			Date timeStamp = this.getTimeStamp();
			SimpleDateFormat df;

			if (timeStamp.getYear() == today.getYear() && timeStamp.getMonth() == today.getMonth() && timeStamp.getDate() == today.getDate())
				df = new SimpleDateFormat("hh:mm a");
			else
				df = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

			String date = df.format(this.timeStamp);

			return Html.fromHtml("<small>" + date + "</small>");
		}

	}
}
