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
package com.sir_m2x.messenger.classes;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import com.sir_m2x.messenger.utils.Utils;

/**
 * A representation of a single instant message used throughout various places
 * in this project.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class IM
{
	private String sender;
	private String message;
	private Date timeStamp = null;
	private boolean isOfflineMessage = false;
	private boolean isOld = false;

	public boolean isOfflineMessage()
	{
		return this.isOfflineMessage;
	}

	public void setOfflineMessage(final boolean isOfflineMessage)
	{
		this.isOfflineMessage = isOfflineMessage;
	}

	public String getSender()
	{
		return this.sender;
	}

	public void setSender(final String sender)
	{
		this.sender = sender;
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

	public Spanned getTime()
	{
		Date today = new Date(System.currentTimeMillis());
		SimpleDateFormat df;

		if (this.timeStamp.getYear() == today.getYear() && this.timeStamp.getMonth() == today.getMonth() && this.timeStamp.getDate() == today.getDate())
			df = new SimpleDateFormat("hh:mm a");
		else
			df = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

		String date = df.format(this.timeStamp);

		return Html.fromHtml("<small>" + date + "</small>");
	}

	public void setTimeStamp(final Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public boolean isBuzz()
	{
		return this.message.equals("<ding>");
	}
	
	public void setOld(final boolean isOld)
	{
		this.isOld = isOld;
	}
	
	public boolean isOld()
	{
		return this.isOld;
	}

	public IM()
	{
		this(null, null, null, false);
	}

	public IM(final String sender, final String message, final Date timeStamp)
	{
		this(sender, message, timeStamp, false);
	}

	public IM(final String sender, final String message, final Date timeStamp, final boolean isOfflineMessage)
	{
		this.timeStamp = timeStamp;
		this.message = message;
		this.sender = sender;
		this.isOfflineMessage = isOfflineMessage;
	}
	
	public IM(final String sender, final String message, final Date timeStamp, final boolean isOfflineMessage, final boolean isOld)
	{
		this.timeStamp = timeStamp;
		this.message = message;
		this.sender = sender;
		this.isOfflineMessage = isOfflineMessage;
		this.isOld = isOld;
	}

	@Override
	public String toString()
	{
		return this.sender + ": " + this.message;
	}

	public Spanned toHtml(final Context context)
	{
		if (isBuzz())
			return Html.fromHtml("<html><body><b><font color=\"red\">BUZZ!!!</font></b></body></html>");

		return Html.fromHtml("<html><body>" + Utils.parseTextForSmileys(this.message) + "</body></html>", new ImageGetter(context), null);
	}

	class ImageGetter implements Html.ImageGetter
	{
		Context context;

		public ImageGetter(final Context context)
		{
			this.context = context;
		}

		@Override
		public Drawable getDrawable(final String source)
		{
			InputStream is;
			Drawable d = null;
			try
			{
				is = this.context.getResources().getAssets().open("smiley/" + source);
				d = Drawable.createFromStream(is, source);
				d.setBounds(0, 0, (int) (51 * (Utils.deviceDensity / 240)), (int) (38 * (Utils.deviceDensity / 240)));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			return d;
		}
	}
}