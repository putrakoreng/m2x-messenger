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

import java.io.IOException;
import java.io.InputStream;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.webkit.WebView;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.dialogs.CustomDialog;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.helpers.NotificationHelper;
import com.sir_m2x.messenger.helpers.ToastHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class PreferencesActivity extends PreferenceActivity
{
	private Preference refreshPref;
	private Preference homepage;
	private Preference changelog;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		String data = "";
		if (getIntent().getData() != null)
			data = getIntent().getData().toString();
		if (data.equals("preferences://advanced_notification"))
			addPreferencesFromResource(R.xml.advance_notification_preferences);
		else if (data.equals("preferences://about"))
		{
			addPreferencesFromResource(R.xml.about_preferences);
			this.homepage = findPreference("homepage");
			this.changelog = findPreference("changelog");
		}
		else
		{
			addPreferencesFromResource(R.xml.preferences);
			this.refreshPref = findPreference("refresh");
			this.homepage = findPreference("homepage");
			this.changelog = findPreference("changelog");
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		Preferences.loadPreferences(getApplicationContext());
		sendBroadcast(new Intent(MessengerService.INTENT_LIST_CHANGED));
		
		if (!Preferences.showNotification)
			MessengerService.getNotificationHelper().cancelNotification(NotificationHelper.NOTIFICATION_SIGNED_IN);
	}

	@Override
	public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference)
	{
		if (preference == this.refreshPref)
		{
			ToastHelper.showToast(this, R.drawable.ic_stat_notify, "Avatars will be refreshed shortly...", Toast.LENGTH_LONG);
			AvatarHelper.refreshAvatars();
		}
		else if (preference == this.homepage)
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://code.google.com/p/m2x-messenger")));
		else if (preference == this.changelog)
		{
			InputStream is = null;

			try
			{
				is = getAssets().open("changelog.txt");
				String changelog = Utils.readText(is);
				CustomDialog dlg = new CustomDialog(this);
				dlg.setTitle("Changelog");
				WebView v = (WebView) getLayoutInflater().inflate(R.layout.changelog_view, null);
				v.loadData(changelog, "text/html", "utf8");
				//				((TextView)v.findViewById(R.id.txtChangelog)).setText(Html.fromHtml());
				dlg.setCancelable(true);
				dlg.setView(v).show();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (is != null)
					try
					{
						is.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
			}

		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}