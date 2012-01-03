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
package com.sir_m2x.messenger.activities;

import java.util.Date;

import org.openymsg.network.FireEvent;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;
import org.openymsg.network.event.SessionExceptionEvent;
import org.openymsg.network.event.SessionListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.Toast;

import com.sir_m2x.messenger.FriendsList;
import com.sir_m2x.messenger.MySessionAdapter;
import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.CustomExceptionHandler;
import com.sir_m2x.messenger.utils.Utils;

/**
 * This is the initial screen that the user is going to see.
 * It handles the login process and is responsible for taking the username and password of the user.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class LoginActivity extends Activity
{
	private ProgressDialog pd = null;

	private final String[] statusArray = {"Available", "Busy", "Away", "Invisible", "Custom"};
	private Status loginStatus = Status.AVAILABLE;

	private EditText txtUsername = null;
	private EditText txtPassword = null;
	private Spinner spnStatus = null;
	private EditText txtCustomMessage = null;
	private CheckBox chkBusy= null;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		InitializePrerequisites();
		
		if (isServiceRunning("com.sir_m2x.messenger.services.MessengerService"))
			startActivity(new Intent(LoginActivity.this, ContactsListActivity.class));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.pd = new ProgressDialog(LoginActivity.this);
		this.pd.setIndeterminate(true);
		this.pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pd.setMessage("Signing in...");
		this.pd.setCancelable(false);

		((Button) findViewById(R.id.btnSignIn)).setOnClickListener(this.btnSingIn_Click);

		this.txtUsername = (EditText)findViewById(R.id.txtUsername);
		this.txtPassword = (EditText)findViewById(R.id.txtPassword);
		this.spnStatus = (Spinner)findViewById(R.id.spnStatus);
		this.txtCustomMessage = (EditText)findViewById(R.id.txtCustomMessage);
		this.chkBusy = (CheckBox)findViewById(R.id.chkBusy);


		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.statusArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.spnStatus.setAdapter(adapter);
		this.spnStatus.setOnItemSelectedListener(this.spnItem_Selected);
		this.spnStatus.setSelection(0);

		readSavedPreferences();
	}

	/**
	 * Initialize several required stuff before starting the whole application
	 */
	private void InitializePrerequisites()
	{
		System.setProperty("http.keepAlive", "false");	// for compatibility with Android 2.1+
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler("/sdcard/m2x-messenger", "http://sirm2x.heliohost.org/m2x-messenger/upload.php"));
	}

	private void readSavedPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(Utils.qualify("LOGIN_PREFERENCES"), 0);

		this.txtUsername.setText(preferences.getString("username", ""));
		this.txtPassword.setText(preferences.getString("password", ""));
		this.spnStatus.setSelection(preferences.getInt("status", 0));
		this.txtCustomMessage.setText(preferences.getString("customStatus", ""));
		this.chkBusy.setChecked(preferences.getBoolean("busy", false));
	}

	private void savePreferences()
	{
		SharedPreferences preferences = getSharedPreferences(Utils.qualify("LOGIN_PREFERENCES"), 0);
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString("username", this.txtUsername.getText().toString());
		editor.putString("password", this.txtPassword.getText().toString());
		editor.putInt("status", this.spnStatus.getSelectedItemPosition());
		editor.putString("customStatus", this.txtCustomMessage.getText().toString());
		editor.putBoolean("busy", this.chkBusy.isChecked());

		editor.commit();

	}

	OnItemSelectedListener spnItem_Selected = new OnItemSelectedListener()
	{

		@Override
		public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2,
				final long arg3)
		{
			((TableRow)findViewById(R.id.rowCustom)).setVisibility(View.GONE);

			switch(arg2)
			{
				case 0:
					LoginActivity.this.loginStatus = Status.AVAILABLE;
					break;
				case 1:
					LoginActivity.this.loginStatus = Status.BUSY;
					break;
				case 2:
					LoginActivity.this.loginStatus = Status.NOTATDESK;
					break;
				case 3:
					LoginActivity.this.loginStatus = Status.INVISIBLE;
					break;
				case 4:
					LoginActivity.this.loginStatus = Status.CUSTOM;
					((TableRow)findViewById(R.id.rowCustom)).setVisibility(View.VISIBLE);
					break;
			}
		}

		@Override
		public void onNothingSelected(final AdapterView<?> arg0)
		{
			LoginActivity.this.loginStatus = Status.AVAILABLE;
		}
	};

	View.OnClickListener btnSingIn_Click = new OnClickListener()
	{
		@Override
		public void onClick(final View arg0)
		{
			String username = LoginActivity.this.txtUsername.getText().toString();
			String password = LoginActivity.this.txtPassword.getText().toString();
			((Button) arg0).requestFocus();

			LoginActivity.this.pd.show();
			AsyncLogin al = new AsyncLogin();
			al.execute(new String[] { username, password });
		}
	};

	private class AsyncLogin extends
	AsyncTask<String, Void, org.openymsg.network.Session>
	{
		private Exception getEx()
		{
			return this.ex;
		}

		private void setEx(final Exception ex)
		{
			this.ex = ex;
		}

		private Exception ex = null;
		@Override
		protected Session doInBackground(final String... arg0)
		{
			org.openymsg.network.Session ys = new org.openymsg.network.Session();
			ys.addSessionListener(new MySessionAdapter(getApplicationContext()));
			ys.addSessionListener(LoginActivity.this.preConnectionSessionListener);
			try
			{
				if (LoginActivity.this.loginStatus != org.openymsg.network.Status.CUSTOM)
					ys.setStatus(LoginActivity.this.loginStatus);
				ys.login(arg0[0], arg0[1]);
			}
			catch (Exception ex)
			{
				setEx(ex);
				ys = null;
			}
			return ys;
		}

		@Override
		protected void onPostExecute(final org.openymsg.network.Session result)
		{
			LoginActivity.this.pd.dismiss();
			if (getEx() != null)
			{
				String message = "";
				if (getEx().toString().toLowerCase().contains("host"))
					message = "Could not connect to server, try again!";
				Toast.makeText(LoginActivity.this, message.isEmpty() ? this.ex.toString() : message, Toast.LENGTH_LONG).show();
				return;
			}

			try
			{
				if (LoginActivity.this.loginStatus != org.openymsg.network.Status.CUSTOM)
					result.setStatus(LoginActivity.this.loginStatus);	//in case the user has selected a status other than custom
				else
					result.setStatus(LoginActivity.this.txtCustomMessage.getText().toString(), LoginActivity.this.chkBusy.isChecked());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			MessengerService.setSession(result);
			if (result != null && result.getSessionStatus() == SessionState.LOGGED_ON)
			{
				MessengerService.setMyId(MessengerService.getSession().getLoginID().getId());
				MessengerService.getEventLog().log(MessengerService.getMyId(), "logged in", new Date(System.currentTimeMillis()));
				//Hacky! sleep for 2 seconds so that the session is fully ready
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
				}
				result.removeSessionListener(LoginActivity.this.preConnectionSessionListener);
				while(!result.getRoster().isRosterReady());		//wait for the roster to become fully ready.
				Utils.getAllAvatars();
				FriendsList.fillFriends(result.getRoster().getGroups());
				result.addSessionListener(new FriendsList());
				savePreferences();
				startService(new Intent(LoginActivity.this, com.sir_m2x.messenger.services.MessengerService.class));
				startActivity(new Intent(LoginActivity.this, ContactsListActivity.class));
			}
		}

	}

	public boolean isServiceRunning(final String className)
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if (service.service.getClassName().equals(className))
				return true;

		return false;
	}
	
	SessionListener preConnectionSessionListener = new SessionListener()
	{
		
		@Override
		public void dispatch(final FireEvent event)
		{
			if (event.getEvent() instanceof SessionExceptionEvent)
				Toast.makeText(getApplicationContext(), event.toString(), Toast.LENGTH_LONG).show();
		}
	};
}