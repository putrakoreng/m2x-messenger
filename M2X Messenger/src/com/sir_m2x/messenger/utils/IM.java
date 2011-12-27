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

import android.text.Html;
import android.text.Spanned;

/**
 * A representaion of a single instant message used throughout various places in this project.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class IM
{
	public boolean isOfflineMessage()
	{
		return isOfflineMessage;
	}

	public void setOfflineMessage(boolean isOfflineMessage)
	{
		this.isOfflineMessage = isOfflineMessage;
	}

	public String getSender()
	{
		return sender;
	}

	public void setSender(String sender)
	{
		this.sender = sender;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Date getTimeStamp()
	{
		return timeStamp;
	}
	
	public Spanned getTime()
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

	public void setTimeStamp(Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public boolean isBuzz()
	{
		return isBuzz;
	}

	private String sender;
	private String message;
	private Date timeStamp = null;
	private boolean isOfflineMessage = false;
	private boolean isBuzz = false;
	
	public IM(String sender, String message, Date timeStamp)
	{
		this(sender, message, timeStamp, false, false);
	}
	
	public IM(String sender, String message, Date timeStamp, boolean isOfflineMessage, boolean isBuzz)
	{
		this.timeStamp = timeStamp;
		this.message = message;
		this.sender = sender;
		this.isOfflineMessage = isOfflineMessage;
		this.isBuzz = isBuzz;
	}
	
	
	@Override
	public String toString()
	{
		return sender + ": " + message;
	}
	
	public Spanned toHtml()
	{
		if (isBuzz)
			return Html.fromHtml("<html><body><b>"+ sender +":<br/><font color=\"red\">BUZZ!!!</font></b></body></html>");
		return Html.fromHtml("<html><body><b>" + sender +"</b>: " + message + "</body></html>");
	}
}