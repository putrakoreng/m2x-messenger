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
package com.sir_m2x.messenger.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sir_m2x.messenger.R;

/**
 * A subclass of Dialog which is themed like the rest of the application.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class CustomDialog extends Dialog
{
	TextView txtTitle = null;
	TextView txtMessage = null;
	LinearLayout content = null;
	Button btnPositive = null;
	Button btnNegative = null;
	

	public CustomDialog(final Context context)
	{
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.custom_dialog_layout);
		
		this.txtTitle = (TextView) findViewById(R.id.txtTitle);
		this.txtMessage = (TextView) findViewById(R.id.txtMessage);
		this.content = (LinearLayout) findViewById(R.id.content);
		this.btnPositive = (Button) findViewById(R.id.btnPositive);
		this.btnNegative = (Button) findViewById(R.id.btnNegative);
		
		this.txtTitle.setVisibility(View.GONE);
		this.txtMessage.setVisibility(View.GONE);
		this.content.setVisibility(View.GONE);
		this.btnPositive.setVisibility(View.GONE);
		this.btnNegative.setVisibility(View.GONE);
	}

	@Override
	public void setTitle(final CharSequence title)
	{
		this.txtTitle.setText(title);
		this.txtTitle.setVisibility(View.VISIBLE);
	}

	@Override
	public void setTitle(final int titleId)
	{
		this.txtTitle.setText(titleId);
		this.txtTitle.setVisibility(View.VISIBLE);
	}
	
	public CustomDialog setMessage(final CharSequence message)
	{
		this.txtMessage.setText(message);
		this.txtMessage.setVisibility(View.VISIBLE);
		return this;
	}
	
	public CustomDialog setMessage(final int messageId)
	{
		this.txtMessage.setText(messageId);
		this.txtMessage.setVisibility(View.VISIBLE);
		return this;
	}

	public CustomDialog setView(final View v)
	{
		this.content.addView(v);
		this.content.setVisibility(View.VISIBLE);

		return this;
	}

	public CustomDialog setPositiveButton(final CharSequence text, final android.view.View.OnClickListener listener)
	{
		
		this.btnPositive.setText(text);
		this.btnPositive.setOnClickListener(listener);
		this.btnPositive.setVisibility(View.VISIBLE);
		
		return this;
	}
	
	public CustomDialog setNegativeButton(final CharSequence text, final android.view.View.OnClickListener listener)
	{
		
		this.btnNegative.setText(text);
		this.btnNegative.setOnClickListener(listener);
		this.btnNegative.setVisibility(View.VISIBLE);
		
		return this;
	}
}