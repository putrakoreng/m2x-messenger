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
package com.sir_m2x.messenger.services;

import java.io.IOException;

import org.openymsg.network.AccountLockedException;
import org.openymsg.network.FailedLoginException;
import org.openymsg.network.LoginRefusedException;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;

import android.os.AsyncTask;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class AsyncLogin extends AsyncTask<String, Void, Session>
{

	private MessengerService parentService;
	private Session session;
	private String username;
	private String password;
	private org.openymsg.network.Status loginStatus;

	private Exception loginException = null;
	private boolean reconnect = false;

	public Exception getLoginException()
	{
		return this.loginException;
	}

	public AsyncLogin(final MessengerService parentService, final Session session, final org.openymsg.network.Status loginStatus, final boolean reconnect)
	{
		this.parentService = parentService;
		this.session = session;
		this.loginStatus = loginStatus;
		this.reconnect = reconnect;
	}

	@Override
	protected Session doInBackground(final String... arg0)
	{
		this.username = arg0[0];
		this.password = arg0[1];
		this.loginException = null;

		try
		{
			doLogin(this.username, this.password);
		}
		catch (Exception e)
		{
			this.loginException = e;
		}

		if (this.session.getSessionStatus() != SessionState.LOGGED_ON || isCancelled())
			return null;

		this.loginException = null;		
		return this.session;
	}

	@Override
	protected void onPostExecute(final org.openymsg.network.Session result)
	{
		this.parentService.asyncFinished(result, this.reconnect);	// callback to notify the service that the login task has finished
	}

	@Override
	protected void onCancelled()
	{
		this.session.removeAllSessionListeners();
//		try
//		{
//			this.session.forceCloseSession();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		super.onCancelled();
	}
	
	/**
	 * Cancels OpenYMSG login process
	 */
	public void cancelLogin()
	{
		this.session.setCancelLogin(true);
	}
	
	private void doLogin(final String username, final String password) throws AccountLockedException, IllegalStateException, LoginRefusedException, FailedLoginException,
			IOException
	{
		if (this.loginStatus != org.openymsg.network.Status.CUSTOM)
			this.session.setStatus(this.loginStatus);
		this.session.login(username, password);
	}

}
