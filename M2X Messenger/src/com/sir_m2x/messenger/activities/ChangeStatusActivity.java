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

import org.openymsg.network.Status;

import android.app.Activity;
import android.content.Intent;
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

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;

/**
 * This activity acts as a dialog. The user can change his/her status via this
 * activity
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ChangeStatusActivity extends Activity
{
	private final String[] statusArray = { "Available", "Busy", "Away", "Invisible", "Custom" };
	private Status newStatus = Status.AVAILABLE;

	private Button btnOk = null;
	private Button btnCancel = null;
	private Spinner spnStatus = null;
	private EditText txtCustomMessage = null;
	private CheckBox chkBusy = null;


	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.change_status_dialog);
		this.btnOk = (Button)findViewById(R.id.btnOk);
		this.btnCancel = (Button)findViewById(R.id.btnCancel);
		this.spnStatus = (Spinner) findViewById(R.id.spnStatus);
		this.txtCustomMessage = (EditText) findViewById(R.id.txtCustomMessage);
		this.chkBusy = (CheckBox) findViewById(R.id.chkBusy);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.statusArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.spnStatus.setAdapter(adapter);
		this.spnStatus.setOnItemSelectedListener(this.spnItem_Selected);
		int currentSelection = 0;

		switch (MessengerService.getSession().getStatus())
		{
			case AVAILABLE:
				currentSelection = 0;
				break;
			case BUSY:
				currentSelection = 1;
				break;
			case IDLE:
				currentSelection = 2;
				break;
			case INVISIBLE:
				currentSelection = 3;
				break;
			case CUSTOM:
				currentSelection = 4;
				this.txtCustomMessage.setText(MessengerService.getSession().getCustomStatusMessage());
				this.chkBusy.setChecked(MessengerService.getSession().isCustomBusy());
				break;

			default:
				break;
		}
		this.spnStatus.setSelection(currentSelection);
		this.btnOk.setOnClickListener(this.btnOk_Listener);
		this.btnCancel.setOnClickListener(this.btnCancel_Listener);
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
					ChangeStatusActivity.this.newStatus = Status.AVAILABLE;
					break;
				case 1:
					ChangeStatusActivity.this.newStatus = Status.BUSY;
					break;
				case 2:
					ChangeStatusActivity.this.newStatus = Status.IDLE;
					break;
				case 3:
					ChangeStatusActivity.this.newStatus = Status.INVISIBLE;
					break;
				case 4:
					ChangeStatusActivity.this.newStatus = Status.CUSTOM;
					((TableRow) findViewById(R.id.rowCustom)).setVisibility(View.VISIBLE);
					break;
			}
		}

		@Override
		public void onNothingSelected(final AdapterView<?> arg0)
		{
			ChangeStatusActivity.this.newStatus = MessengerService.getSession().getStatus();
		}
	};

	OnClickListener btnOk_Listener = new OnClickListener()
	{

		@Override
		public void onClick(final View v)
		{
			try
			{
				if (ChangeStatusActivity.this.newStatus != Status.CUSTOM)
					MessengerService.getSession().setStatus(ChangeStatusActivity.this.newStatus);
				else
					MessengerService.getSession().setStatus(ChangeStatusActivity.this.txtCustomMessage.getText().toString(), ChangeStatusActivity.this.chkBusy.isChecked());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			// update status bar notification
			sendBroadcast(new Intent(MessengerService.INTENT_STATUS_CHANGED));
			
			finish();
		}
	};

	OnClickListener btnCancel_Listener = new OnClickListener()
	{

		@Override
		public void onClick(final View v)
		{
			finish();
		}
	};
}
