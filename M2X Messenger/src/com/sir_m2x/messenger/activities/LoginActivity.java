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
package com.sir_m2x.messenger.activities;

import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.ProgressCheckThread;
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
	private final String[] statusArray = { "Available", "Busy", "Away", "Invisible", "Custom" };

	private static String lastLoginStatus = "Initializing...";

	private ProgressDialog pd = null;
	private Status loginStatus = Status.AVAILABLE;
	private EditText txtUsername = null;
	private EditText txtPassword = null;
	private Spinner spnStatus = null;
	private EditText txtCustomMessage = null;
	private CheckBox chkBusy = null;
	private CheckBox chkRemember = null;
	private ProgressCheckThread progressCheck = null;
	private boolean isPaused = false;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		// get the device DPI
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		Utils.deviceDensity = dm.densityDpi;

		this.pd = new ProgressDialog(LoginActivity.this);
		this.pd.setIndeterminate(true);
		this.pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pd.setMessage(lastLoginStatus);
		this.pd.setCancelable(true);
		this.pd.setOnCancelListener(this.pdCancelListener);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView txtM2X = (TextView) findViewById(R.id.txtM2X);
		if (txtM2X != null)
		{
			Typeface lucida = Typeface.createFromAsset(getAssets(), "fonts/lucida.ttf");
			txtM2X.setTypeface(lucida, Typeface.BOLD);
		}

		((Button) findViewById(R.id.btnSignIn)).setOnClickListener(this.btnSingIn_Click);

		this.txtUsername = (EditText) findViewById(R.id.txtUsername);
		this.txtUsername.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
			{
				LoginActivity.this.txtPassword.setText("");
			}

			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
			{
			}

			@Override
			public void afterTextChanged(final Editable s)
			{
			}
		});
		this.txtPassword = (EditText) findViewById(R.id.txtPassword);
		this.spnStatus = (Spinner) findViewById(R.id.spnStatus);
		this.txtCustomMessage = (EditText) findViewById(R.id.txtCustomMessage);
		this.chkBusy = (CheckBox) findViewById(R.id.chkBusy);
		this.chkRemember = (CheckBox) findViewById(R.id.chkRemember);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.statusArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.spnStatus.setAdapter(adapter);
		this.spnStatus.setOnItemSelectedListener(this.spnItem_Selected);
		this.spnStatus.setSelection(0);

		readSavedPreferences();
	}

	OnCancelListener pdCancelListener = new OnCancelListener()
	{

		@Override
		public void onCancel(final DialogInterface dialog)
		{
			LoginActivity.this.progressCheck.isRunning = false;
			sendBroadcast(new Intent(MessengerService.INTENT_CANCEL_LOGIN));
		}
	};

	@Override
	protected void onResume()
	{
		this.isPaused = false;
		if (!Utils.isServiceRunning(getApplicationContext(), "com.sir_m2x.messenger.services.MessengerService"))
			startService(new Intent(this, MessengerService.class));
		else if (MessengerService.getSession() != null)
		{
			Session s = MessengerService.getSession();
			if (s.getSessionStatus() == SessionState.LOGGED_ON || MessengerService.hasloggedInYet)
			{
				overridePendingTransition(0, 0);
				startActivity(new Intent(getApplicationContext(), ContactsListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
				overridePendingTransition(0, 0);
				finish();
			}
			else if (s.getSessionStatus() != SessionState.UNSTARTED)
				showProgressAndWaitLogin();
		}

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
		this.chkRemember.setChecked(Preferences.remember);
	}

	private void savePreferences()
	{
		if (this.chkRemember.isChecked())
		{
			Preferences.username = this.txtUsername.getText().toString();
			Preferences.password = this.txtPassword.getText().toString();
			Preferences.status = this.spnStatus.getSelectedItemPosition();
			Preferences.customStatus = this.txtCustomMessage.getText().toString();
			Preferences.busy = this.chkBusy.isChecked();
		}
		else
		{
			Preferences.username = "";
			Preferences.password = "";
			Preferences.status = 0;
			Preferences.customStatus = "";
			Preferences.busy = false;
		}

		Preferences.remember = this.chkRemember.isChecked();
		Preferences.saveLoginPreferences(getApplicationContext());
	}

	OnItemSelectedListener spnItem_Selected = new OnItemSelectedListener()
	{

		@Override
		public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3)
		{
			((LinearLayout) findViewById(R.id.rowCustom)).setVisibility(View.GONE);

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
					((LinearLayout) findViewById(R.id.rowCustom)).setVisibility(View.VISIBLE);
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

			/*
			 * TODO if the user has previously canceled the ProgressDialog, the
			 * session state is FAILED and it will result in the ProgressDialog
			 * not being show again (ProgressCheckThread thinks that the session
			 * has failed recently!) Find a better solution than this "if"
			 * statement
			 */
			if (MessengerService.getSession() != null)
				MessengerService.getSession().setSessionStatus(SessionState.UNSTARTED);
			showProgressAndWaitLogin();

		}
	};

	private void showProgressAndWaitLogin()
	{
		if (this.progressCheck == null || !this.progressCheck.isAlive())
		{
			this.progressCheck = new ProgressCheckThread(this.progressHandler);
			this.progressCheck.start();
		}

		LoginActivity.this.pd.show();
	}

	@Override
	protected void onPause()
	{
		this.isPaused = true;
		super.onPause();

		if (this.pd != null)
			this.pd.dismiss();
		if (this.progressCheck != null)
			this.progressCheck.isRunning = false;
	};

	@Override
	public void onBackPressed()
	{
		if (!MessengerService.hasloggedInYet && MessengerService.getSession() != null && (MessengerService.getSession().getSessionStatus() != SessionState.LOGGED_ON))
		{
			if (this.progressCheck != null)
				this.progressCheck.isRunning = false;

			stopService(new Intent(getApplicationContext(), MessengerService.class));
		}
		
		super.onBackPressed();
	};

	Handler progressHandler = new Handler()
	{
		@Override
		public void handleMessage(final Message msg)
		{
			switch (msg.what)
			{
				case ProgressCheckThread.PROGRESS_UPDATE:
					LoginActivity.this.pd.setMessage((String) msg.obj);
					if (!LoginActivity.this.pd.isShowing())
						LoginActivity.this.pd.show();
					break;
				case ProgressCheckThread.PROGRESS_FAILED:
					LoginActivity.this.pd.dismiss();
					break;
				case ProgressCheckThread.PROGRESS_LOGGED_ON:
					LoginActivity.this.pd.dismiss();
					if (!LoginActivity.this.isPaused)
						startActivity(new Intent(getApplicationContext(), ContactsListActivity.class));
					savePreferences();
					break;
			}
		}
	};
}