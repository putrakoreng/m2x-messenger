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
package com.sir_m2x.messenger.datastructures;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.Html;
import android.text.Spanned;

/**
 * A class that represents a friend request.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class FriendRequest
{
	private String from;
	private String message;
	private Date timeStamp;

	public FriendRequest(final String from, final String message, final Date timeStamp)
	{
		this.setFrom(from);
		this.setMessage(message);
		this.setTimeStamp(timeStamp);
	}

	public String getFrom()
	{
		return this.from;
	}

	public void setFrom(final String from)
	{
		this.from = from;
	}

	public String getMessage()
	{
		return this.message;
	}

	public void setMessage(final String message)
	{
		this.message = message;
	}

	public Date getTimeStamp()
	{
		return this.timeStamp;
	}

	public void setTimeStamp(final Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public Spanned requestToHtml()
	{
		return Html.fromHtml("<b>" + this.from + "</b>" + (this.message != null ? ": " + this.message : ""));
	}

	public Spanned timeToHtml()
	{
		Date today = new Date(System.currentTimeMillis());
		Date timeStamp = this.timeStamp;
		SimpleDateFormat df;

		if (timeStamp.getYear() == today.getYear() && timeStamp.getMonth() == today.getMonth() && timeStamp.getDate() == today.getDate())
			df = new SimpleDateFormat("hh:mm a");
		else
			df = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

		String date = df.format(this.timeStamp);

		return Html.fromHtml("<small>" + date + "</small>");
	}

}