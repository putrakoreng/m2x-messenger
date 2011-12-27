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

import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;

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
	public void onCreate(Bundle savedInstanceState)
	{
		if (isServiceRunning("com.sir_m2x.messenger.services.MessengerService"))
			startActivity(new Intent(LoginActivity.this, ContactsListActivity.class));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		pd = new ProgressDialog(LoginActivity.this);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setMessage("Signing in...");
		pd.setCancelable(false);

		((Button) findViewById(R.id.btnSignIn)).setOnClickListener(btnSingIn_Click);
		
		txtUsername = (EditText)findViewById(R.id.txtUsername);
		txtPassword = (EditText)findViewById(R.id.txtPassword);
		spnStatus = (Spinner)findViewById(R.id.spnStatus);
		txtCustomMessage = (EditText)findViewById(R.id.txtCustomMessage);
		chkBusy = (CheckBox)findViewById(R.id.chkBusy);
		
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, statusArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnStatus.setAdapter(adapter);
		spnStatus.setOnItemSelectedListener(spnItem_Selected);
		spnStatus.setSelection(0);
		
		readSavedPreferences();
	}
	
	private void readSavedPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(Utils.qualify("LOGIN_PREFERENCES"), 0);
		
		txtUsername.setText(preferences.getString("username", ""));
		txtPassword.setText(preferences.getString("password", ""));
		spnStatus.setSelection(preferences.getInt("status", 0));
		txtCustomMessage.setText(preferences.getString("customStatus", ""));
		chkBusy.setChecked(preferences.getBoolean("busy", false));
	}
	
	private void savePreferences()
	{
		SharedPreferences preferences = getSharedPreferences(Utils.qualify("LOGIN_PREFERENCES"), 0);
		SharedPreferences.Editor editor = preferences.edit();
		
		editor.putString("username", txtUsername.getText().toString());
		editor.putString("password", txtPassword.getText().toString());
		editor.putInt("status", spnStatus.getSelectedItemPosition());
		editor.putString("customStatus", txtCustomMessage.getText().toString());
		editor.putBoolean("busy", chkBusy.isChecked());
		
		editor.commit();
		
	}

	OnItemSelectedListener spnItem_Selected = new OnItemSelectedListener()
	{

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3)
		{
			((TableRow)findViewById(R.id.rowCustom)).setVisibility(View.GONE);
			
			switch(arg2)
			{
				case 0:
					loginStatus = Status.AVAILABLE;
					break;
				case 1:
					loginStatus = Status.BUSY;
					break;
				case 2:
					loginStatus = Status.NOTATDESK;
					break;
				case 3:
					loginStatus = Status.INVISIBLE;
					break;
				case 4:
					loginStatus = Status.CUSTOM;
					((TableRow)findViewById(R.id.rowCustom)).setVisibility(View.VISIBLE);
					break;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0)
		{
			loginStatus = Status.AVAILABLE;			
		}
	};

	View.OnClickListener btnSingIn_Click = new OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			String username = txtUsername.getText().toString();
			String password = txtPassword.getText().toString();
			((Button) arg0).requestFocus();

			pd.show();
			AsyncLogin al = new AsyncLogin();
			al.execute(new String[] { username, password });

		}
	};

	private class AsyncLogin extends
			AsyncTask<String, Void, org.openymsg.network.Session>
	{
		private Exception getEx()
		{
			return ex;
		}

		private void setEx(Exception ex)
		{
			this.ex = ex;
		}

		private Exception ex = null;
		@Override
		protected Session doInBackground(String... arg0)
		{
			org.openymsg.network.Session ys = new org.openymsg.network.Session();
			ys.addSessionListener(new MySessionAdapter(getApplicationContext()));
			try
			{
				if (loginStatus != org.openymsg.network.Status.CUSTOM)
					ys.setStatus(loginStatus);
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
		protected void onPostExecute(org.openymsg.network.Session result)
		{
			pd.dismiss();
			if (getEx() != null)
			{
				String message = "";
				if (getEx().toString().toLowerCase().contains("host"))
					message = "Could not connect to server, try again!";
				Toast.makeText(LoginActivity.this, message.isEmpty() ? ex.toString() : message, Toast.LENGTH_LONG).show();
				return;
			}
			
			try
			{
				if (loginStatus != org.openymsg.network.Status.CUSTOM)
					result.setStatus(loginStatus);	//in case the user has selected a status other than invisible
				else
				{
					result.setStatus(txtCustomMessage.getText().toString(), chkBusy.isChecked());
				}
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
				Utils.getAllAvatars();
				FriendsList.fillFriends(result.getRoster().getGroups());
				result.addSessionListener(new FriendsList());
				savePreferences();
				startService(new Intent(LoginActivity.this, com.sir_m2x.messenger.services.MessengerService.class));
				startActivity(new Intent(LoginActivity.this, ContactsListActivity.class));
			}
		}

	}	
	
	public boolean isServiceRunning(String className)
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if (service.service.getClassName().equals(className))
				return true;
		
		return false;
	}
}