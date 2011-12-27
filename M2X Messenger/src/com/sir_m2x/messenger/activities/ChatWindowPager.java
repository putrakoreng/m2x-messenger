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

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.services.MessengerService;

/**
 * INCOMPLETE!
 * Literally useless for now ;-]
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public class ChatWindowPager extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_window_pager);
		
	}
	
	PagerAdapter p = new PagerAdapter()
	{

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void finishUpdate(View arg0)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getCount()
		{
			return MessengerService.getFriendsInChat().size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1)
		{
			return null;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public Parcelable saveState()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void startUpdate(View arg0)
		{
			// TODO Auto-generated method stub
			
		}
		
	};
}
