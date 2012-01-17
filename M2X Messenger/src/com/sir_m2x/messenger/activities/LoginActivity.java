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

import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;

/**
 * This is the initial screen that the user is going to see. It handles the
 * login process and is responsible for taking the username and password of the
 * user.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class LoginActivity extends Activity
{
	private ProgressDialog pd = null;

	private final String[] statusArray = { "Available", "Busy", "Away", "Invisible", "Custom" };
	private Status loginStatus = Status.AVAILABLE;
	private static String lastLoginStatus = "Initializing...";

	private EditText txtUsername = null;
	private EditText txtPassword = null;
	private Spinner spnStatus = null;
	private EditText txtCustomMessage = null;
	private CheckBox chkBusy = null;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		//TODO
		//		if (isServiceRunning("com.sir_m2x.messenger.services.MessengerService"))
		//			startActivity(new Intent(LoginActivity.this, ContactsListActivity.class));

		this.pd = new ProgressDialog(LoginActivity.this);
		this.pd.setIndeterminate(true);
		this.pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pd.setMessage(lastLoginStatus);
		this.pd.setCancelable(false);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		((Button) findViewById(R.id.btnSignIn)).setOnClickListener(this.btnSingIn_Click);

		this.txtUsername = (EditText) findViewById(R.id.txtUsername);
		this.txtPassword = (EditText) findViewById(R.id.txtPassword);
		this.spnStatus = (Spinner) findViewById(R.id.spnStatus);
		this.txtCustomMessage = (EditText) findViewById(R.id.txtCustomMessage);
		this.chkBusy = (CheckBox) findViewById(R.id.chkBusy);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.statusArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.spnStatus.setAdapter(adapter);
		this.spnStatus.setOnItemSelectedListener(this.spnItem_Selected);
		this.spnStatus.setSelection(0);

		readSavedPreferences();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume()
	{
		if (!Utils.isServiceRunning(getApplicationContext(), "com.sir_m2x.messenger.services.MessengerService"))
			startService(new Intent(this, MessengerService.class));
		else if (MessengerService.getSession() != null)
		{
			Session s = MessengerService.getSession();
			if (s.getSessionStatus() == SessionState.LOGGED_ON)
			{
				startActivity(new Intent(getApplicationContext(), ContactsListActivity.class));
				finish();
			}
			else if (s.getSessionStatus() != SessionState.UNSTARTED)
				showProgressAndWaitLogin();

		}
		
		registerReceiver(LoginActivity.this.progressListener, new IntentFilter(MessengerService.INTENT_LOGIN_PROGRESS));
		super.onResume();
	}

	private void readSavedPreferences()
	{
		Preferences.loadPreferences(getApplicationContext());

		this.txtUsername.setText(Preferences.username);
		this.txtPassword.setText(Preferences.password);
		this.spnStatus.setSelection(Preferences.status);
		this.txtCustomMessage.setText(Preferences.customStatus);
		this.chkBusy.setChecked(Preferences.busy);
	}

	private void savePreferences()
	{
		Preferences.username = this.txtUsername.getText().toString();

		Preferences.password = this.txtPassword.getText().toString();
		Preferences.status = this.spnStatus.getSelectedItemPosition();
		Preferences.customStatus = this.txtCustomMessage.getText().toString();
		Preferences.busy = this.chkBusy.isChecked();

		Preferences.saveLoginPreferences(getApplicationContext());
	}

	OnItemSelectedListener spnItem_Selected = new OnItemSelectedListener()
	{

		@Override
		public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3)
		{
			((TableRow) findViewById(R.id.rowCustom)).setVisibility(View.GONE);

			switch (arg2)
			{
				case 0:
					LoginActivity.this.loginStatus = Status.AVAILABLE;
					break;
				case 1:
					LoginActivity.this.loginStatus = Status.BUSY;
					break;
				case 2:
					LoginActivity.this.loginStatus = Status.IDLE;
					break;
				case 3:
					LoginActivity.this.loginStatus = Status.INVISIBLE;
					break;
				case 4:
					LoginActivity.this.loginStatus = Status.CUSTOM;
					((TableRow) findViewById(R.id.rowCustom)).setVisibility(View.VISIBLE);
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
			String customMessage = LoginActivity.this.txtCustomMessage.getText().toString();
			boolean isCustomBusy = LoginActivity.this.chkBusy.isChecked();
			((Button) arg0).requestFocus();

			Intent intent = new Intent(MessengerService.INTENT_LOGIN);
			intent.putExtra(Utils.qualify("username"), username);
			intent.putExtra(Utils.qualify("password"), password);
			intent.putExtra(Utils.qualify("status"), LoginActivity.this.loginStatus.getValue());
			intent.putExtra(Utils.qualify("customMessage"), customMessage);
			intent.putExtra(Utils.qualify("isCustomBusy"), isCustomBusy);

			sendBroadcast(intent);
			showProgressAndWaitLogin();

		}
	};

	private void showProgressAndWaitLogin()
	{
		LoginActivity.this.pd.show();
	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop()
	{
		if (MessengerService.getSession().getSessionStatus() == SessionState.UNSTARTED || MessengerService.getSession().getSessionStatus() == SessionState.FAILED)
			stopService(new Intent(getApplicationContext(), MessengerService.class));
		super.onStop();
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		this.unregisterReceiver(this.progressListener);
		if (this.pd != null)
			this.pd.dismiss();
	};

	BroadcastReceiver progressListener = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_LOGIN_PROGRESS))
				try
				{
				String message = intent.getExtras().getString(Utils.qualify("message"));
				if (message.equals("Logged on!") || message.equals("Failed!"))
				{
					LoginActivity.this.pd.dismiss();
					savePreferences();
				}
				else
				{
					lastLoginStatus = message;
					LoginActivity.this.pd.setMessage(message);
				}
				}
				catch(Exception e)
				{
					Log.d("M2X", "I'm getting fraustrated by this...");
				}
		}
	};

}