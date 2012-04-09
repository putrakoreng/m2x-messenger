/**
 * 
 */
package com.sir_m2x.messenger.services;

import java.io.IOException;
import java.util.Date;

import org.openymsg.network.AccountLockedException;
import org.openymsg.network.FailedLoginException;
import org.openymsg.network.LoginRefusedException;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.event.SessionListener;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.activities.LoginActivity;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.helpers.ToastHelper;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class AsyncLogin extends AsyncTask<String, Void, Session>
{

	private static final int MAX_RETRIES = 5;

	private Service parentService;
	private Session session;
	private String username;
	private String password;
	private org.openymsg.network.Status loginStatus;
	private String customMessage;
	private boolean isCustomBusy;
	private Exception loginException = null;
	private boolean reconnect = false;
	private int retryCount = 0;
	private SessionListener sessionListener = null;

	public AsyncLogin(final MessengerService parentService, final Session session, final org.openymsg.network.Status loginStatus, final String customMessage, final boolean isCustomBusy,
			final boolean reconnect, final SessionListener sessionListener)
	{
		this.parentService = parentService;
		this.session = session;
		this.loginStatus = loginStatus;
		this.customMessage = customMessage;
		this.isCustomBusy = isCustomBusy;
		this.reconnect = reconnect;
		this.sessionListener = sessionListener;
	}

	@Override
	protected Session doInBackground(final String... arg0)
	{
		this.username = arg0[0];
		this.password = arg0[1];

		while (++this.retryCount <= MAX_RETRIES)
		 try
			{
				doLogin(this.username, this.password);
				break;
			}
			catch (AccountLockedException e)
			{
				this.loginException = e;
				e.printStackTrace();
				break;
			}
			catch (IllegalStateException e)
			{
				this.loginException = e;
				e.printStackTrace();
				break;
			}
			catch (LoginRefusedException e)
			{
				this.loginException = e;
				e.printStackTrace();
				break;
			}
			catch (FailedLoginException e)
			{
				this.loginException = e;
				e.printStackTrace();
				break;
			}
			catch (IOException e)
			{
				this.loginException = e;
				e.printStackTrace();
				try
				{
					this.session.setWaiting();
					publishProgress((Void) null);
					Thread.sleep(10000);
					this.session.cancelWaiting();
				}
				catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
		
		if(this.session.getSessionStatus() != SessionState.LOGGED_ON)
			return null;
		
		this.loginException = null;
		MessengerService.setMyId(MessengerService.getSession().getLoginID().getId());
		MessengerService.getEventLog().log(MessengerService.getMyId(), "logged in", new Date(System.currentTimeMillis()));
		return this.session;
	}

	@Override
	protected void onPostExecute(final org.openymsg.network.Session result)
	{
		String faultMessage = "";

		if (this.loginException instanceof AccountLockedException)
			faultMessage = "This account has been blocked!";
		else if (this.loginException instanceof IllegalArgumentException)
			faultMessage = "Invalid input provided";
		else if (this.loginException instanceof IllegalStateException)
			faultMessage = "Session should be unstarted!";
		else if (this.loginException instanceof LoginRefusedException)
			faultMessage = "Invalid username or password!";
		else if (this.loginException instanceof FailedLoginException)
			faultMessage = "Login failed: Unknow reason";
		else if (this.loginException instanceof IOException)
			faultMessage = "Could not connect to Yahoo!";

		if (!faultMessage.equals(""))
			ToastHelper.showToast(this.parentService, R.drawable.ic_stat_notify_busy, faultMessage, Toast.LENGTH_LONG, -1);

		if (result == null)
		{
			if (this.reconnect)
				this.parentService.startActivity(new Intent(this.parentService, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			return;
		}
		
		Log.d("M2X", "Login successful!");
		
		while (true)
		{
			synchronized (MessengerService.getYahooList().isListReady())
			{
				if (MessengerService.getYahooList().isListReady())
					break;
			}

			Log.d("M2X", "Waiting for the list to get ready!");
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}

		try
		{
			if (this.loginStatus != org.openymsg.network.Status.CUSTOM)
				result.setStatus(this.loginStatus); //in case the user has selected a status other than custom
			else
				result.setStatus(this.customMessage, this.isCustomBusy);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		result.addSessionListener(this.sessionListener);
		MessengerService.setSession(result);
		
		if (result.getSessionStatus() == SessionState.LOGGED_ON)
		{			
			// load my own avatar
			if (!AvatarHelper.requestAvatarIfNeeded(this.username))
				MessengerService.setMyAvatar(AvatarHelper.loadAvatarFromSD(this.username));

			//TODO
			//savePreferences();
			if (!this.reconnect)
				MessengerService.getNotificationHelper().showDefaultNotification(true, false);
//				this.parentService.startActivity(new Intent(this.parentService, ContactsListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//				MessengerService.getNotificationHelper().showDefaultNotification(true, false);
			if (this.reconnect)
				MessengerService.getNotificationHelper().showDefaultNotification(true, false);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onCancelled()
	 */
	@Override
	protected void onCancelled()
	{
		this.session.removeAllSessionListeners();
		try
		{
			this.session.forceCloseSession();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		super.onCancelled();
	}

	private void doLogin(final String username, final String password) throws AccountLockedException, IllegalStateException, LoginRefusedException, FailedLoginException,
			IOException
	{
		if (this.loginStatus != org.openymsg.network.Status.CUSTOM)
			this.session.setStatus(this.loginStatus);
		this.session.login(username, password);
	}

}
