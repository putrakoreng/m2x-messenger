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

import org.openymsg.network.SessionState;

import android.os.Handler;
import android.os.Message;

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
	public boolean isRunning = true;
	
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
		
		Message msg;
		String progressMessage = "";

		while ( this.isRunning && /*TODO not good for login screen : MessengerService.getSession().getSessionStatus() != SessionState.FAILED && */MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON && MessengerService.getSession().getSessionStatus() != SessionState.UNSTARTED)
		{
			switch(MessengerService.getSession().getSessionStatus())
			{
				case INITIALIZING:
					progressMessage = "Initializing...";
					break;
				case CONNECTING:
					progressMessage ="Sending credentials...";
					break;
				case STAGE1:
					progressMessage ="Authenticating...";
					break;
				case STAGE2:
					progressMessage ="Authenticating(2)...";
					break;
				case WAITING:
					progressMessage ="Waiting to reconnect (" + MessengerService.countDownSec + ")";
					break;
				case CONNECTED:
					progressMessage ="Loading list...";
					break;
				case LOGGED_ON:
					progressMessage ="Logged on!";
					break;
				case FAILED:
					progressMessage ="Failed!";
					break;
			}
			msg = new Message();
			msg.obj = progressMessage;
			msg.what = PROGRESS_UPDATE;
			this.handler.sendMessage(msg);
			
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
		else if (MessengerService.getSession().getSessionStatus() == SessionState.LOGGED_ON)
			this.handler.sendEmptyMessage(PROGRESS_LOGGED_ON);
	}
}
