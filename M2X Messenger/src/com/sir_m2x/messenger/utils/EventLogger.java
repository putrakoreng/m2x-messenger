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

import java.util.Date;
import java.util.LinkedList;

import android.text.Html;
import android.text.Spanned;
/**
 * A simple event logger that logs various events in the system.
 * Currently it only logs status changes of the people on our friends list.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class EventLogger
{
	private final LinkedList<LogFormat> eventLog; 
	
	public LinkedList<LogFormat> getEventLog()
	{
		return eventLog;
	}
	
	public EventLogger()
	{
		this.eventLog = new LinkedList<LogFormat>();		
	}
	
	public void log(String who, String event, Date timeStamp)
	{
		this.log(new LogFormat(who, event, timeStamp));
	}
	
	public void log(LogFormat logObject)
	{
		eventLog.add(logObject);
	}
		
	public static class LogFormat
	{
		private String who;
		private String event;
		private Date timeStamp;
		
		public String getWho()
		{
			return who;
		}

		public void setWho(String who)
		{
			this.who = who;
		}

		public String getEvent()
		{
			return event;
		}
		
		public void setEvent(String event)
		{
			this.event = event;
		}
		
		public Date getTimeStamp()
		{
			return timeStamp;
		}
		
		public void setTimeStamp(Date timeStamp)
		{
			this.timeStamp = timeStamp;
		}
		
		public LogFormat(String who, String event, Date timeStamp)
		{
			this.setWho(who);
			this.event = event;
			this.timeStamp = timeStamp;
		}
		
		@Override
		public String toString()
		{
			Date today = new Date(System.currentTimeMillis());
			String time = String.format("%02d:%02d " + (timeStamp.getHours() > 12 ? "PM" : "AM"), timeStamp.getHours() > 12 ? timeStamp.getHours() - 12 : timeStamp.getHours(),timeStamp.getMinutes());
			String date = String.format("%d-%d-%d", timeStamp.getYear() + 1900, timeStamp.getMonth() + 1, timeStamp.getDate()); 
			if (timeStamp.getYear() == today.getYear() && 
				timeStamp.getMonth() == today.getMonth() &&
				timeStamp.getDate() == today.getDate())
				return time + ": " + who + ": " + event;
			
			return date + "  " + time + ": " + who + ": " + event;
		}
		
		public Spanned eventToHtml()
		{
			return Html.fromHtml("<b>" + who +"</b>: " + event);
		}
		
		public Spanned timeToHtml()
		{
			Date today = new Date(System.currentTimeMillis());
			String time = String.format("%02d:%02d " + (timeStamp.getHours() > 12 ? "PM" : "AM"), timeStamp.getHours() > 12 ? timeStamp.getHours() - 12 : timeStamp.getHours(),timeStamp.getMinutes());
			String date = String.format("%d-%d-%d", timeStamp.getYear() + 1900, timeStamp.getMonth() + 1, timeStamp.getDate()); 
			if (timeStamp.getYear() == today.getYear() && 
				timeStamp.getMonth() == today.getMonth() &&
				timeStamp.getDate() == today.getDate())
				return Html.fromHtml("<small>" + time + "</small>");
			return Html.fromHtml("<small>" + date + "  " + time + "</small>");
		}
		
	}
}
