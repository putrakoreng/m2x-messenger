/**
 * 
 */
package com.sir_m2x.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;

/**
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class PreferencesActivity extends PreferenceActivity
{
	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
	
	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onStop()
	 */
	@Override
	protected void onStop()
	{
		super.onStop();
		
		Preferences.loadPreferences(getApplicationContext());		
		sendBroadcast(new Intent(MessengerService.INTENT_LIST_CHANGED));
	}
	
	
	
	

}
