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

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.activities.ContactsListActivity;
import com.sir_m2x.messenger.activities.LoginActivity;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.helpers.ToastHelper;
import com.sir_m2x.messenger.utils.Utils;

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

	public AsyncLogin(final Service parentService, final Session session, final org.openymsg.network.Status loginStatus, final String customMessage, final boolean isCustomBusy,
			final boolean reconnect)
	{
		this.parentService = parentService;
		this.session = session;
		this.loginStatus = loginStatus;
		this.customMessage = customMessage;
		this.isCustomBusy = isCustomBusy;
		this.reconnect = reconnect;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	 */
	@Override
	protected void onProgressUpdate(final Void... values)
	{
		super.onProgressUpdate(values);
		Intent intent = new Intent(MessengerService.INTENT_LOGIN_PROGRESS);
		switch (this.session.getSessionStatus())
		{
			case INITIALIZING:
				intent.putExtra(Utils.qualify("message"), "Initializing...");
				break;
			case CONNECTING:
				intent.putExtra(Utils.qualify("message"), "Sending credentials...");
				break;
			case STAGE1:
				intent.putExtra(Utils.qualify("message"), "Authenticating...");
				break;
			case STAGE2:
				intent.putExtra(Utils.qualify("message"), "Authenticating(2)...");
				break;
			case WAITING:
				intent.putExtra(Utils.qualify("message"), "Waiting to reconnect...");
				break;
			case CONNECTED:
				intent.putExtra(Utils.qualify("message"), "Loading list...");
				break;
			case LOGGED_ON:
				intent.putExtra(Utils.qualify("message"), "Logged on!");
				break;
			case FAILED:
				intent.putExtra(Utils.qualify("message"), "Failed!");
				break;
		}

		this.parentService.sendBroadcast(intent);

	}

	@Override
	protected Session doInBackground(final String... arg0)
	{
		this.username = arg0[0];
		this.password = arg0[1];
		publishProgress((Void)null);

		//TODO
		//this.ys.addSessionListener(LoginActivity.this.preConnectionSessionListener);

		/**
		 * A thread to inform the user about the current status of the login
		 * progress
		 */
		Thread progressReportThread = new Thread()
		{
			@Override
			public void run()
			{
				SessionState initialStatus = AsyncLogin.this.session.getSessionStatus();
				while (true)
				{
					if (AsyncLogin.this.session.getSessionStatus() != initialStatus)
					{
						initialStatus = AsyncLogin.this.session.getSessionStatus();
						publishProgress((Void) null);
					}
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
					}

					if (initialStatus == SessionState.LOGGED_ON || initialStatus == SessionState.FAILED)
						break;
				}
			}
		};
		
		progressReportThread.start();

		while (++this.retryCount <= MAX_RETRIES)
			try
			{
				doLogin(this.username, this.password);
				break;
			}
			catch (Exception e)
			{
				this.loginException = e;
				Log.d("M2X", "Trying to reconect..." + this.retryCount);
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
		
		MessengerService.setMyId(MessengerService.getSession().getLoginID().getId());
		MessengerService.getEventLog().log(MessengerService.getMyId(), "logged in", new Date(System.currentTimeMillis()));

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
		MessengerService.setSession(result);
		if (result.getSessionStatus() == SessionState.LOGGED_ON)
		{			
			// load my own avatar
			if (!AvatarHelper.requestAvatarIfNeeded(this.username))
				MessengerService.setMyAvatar(AvatarHelper.loadAvatarFromSD(this.username));

			//TODO
			//savePreferences();
			if (!this.reconnect)
			{
				this.parentService.startActivity(new Intent(this.parentService, ContactsListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				MessengerService.getNotificationHelper().showDefaultNotification(true, false);
			}
		}
	}

	private void doLogin(final String username, final String password) throws AccountLockedException, IllegalStateException, LoginRefusedException, FailedLoginException,
			IOException
	{
		if (this.loginStatus != org.openymsg.network.Status.CUSTOM)
			this.session.setStatus(this.loginStatus);
		this.session.login(username, password);
	}

}
