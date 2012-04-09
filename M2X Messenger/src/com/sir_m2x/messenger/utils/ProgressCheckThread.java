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

import org.openymsg.network.SessionState;

import android.os.Handler;

import com.sir_m2x.messenger.services.MessengerService;

/**
 * A thread that checks the status of a Session object and returns a login status message based on it
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class ProgressCheckThread extends Thread
{
	public static final int PROGRESS_UPDATE = 0;
	public static final int PROGRESS_FAILED = 1;
	public static final int PROGRESS_LOGGED_ON = 2;
	
	private Handler handler;
	
	public ProgressCheckThread(final Handler handler)
	{
		this.handler = handler;
	}
	
	@Override
	public void run()
	{
		while (MessengerService.getSession() == null)
			;
		while (MessengerService.getSession().getSessionStatus() == SessionState.UNSTARTED)
			;

		while (MessengerService.getSession().getSessionStatus() != SessionState.FAILED && MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON && MessengerService.getSession().getSessionStatus() != SessionState.UNSTARTED)
		{
			this.handler.sendEmptyMessage(PROGRESS_UPDATE);
			try
			{
				Thread.sleep(250);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		if (MessengerService.getSession().getSessionStatus() == SessionState.FAILED || MessengerService.getSession().getSessionStatus() == SessionState.UNSTARTED)
			this.handler.sendEmptyMessage(PROGRESS_FAILED);
		else
			this.handler.sendEmptyMessage(PROGRESS_LOGGED_ON);
	}
}
