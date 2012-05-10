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
package com.sir_m2x.messenger.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * A HorizontalScrollView that is not scrollable!!!
 * This widget is to be used in ListViews, the items of which are
 * clickable and need not be scrollable.
 * 
 * WARNING: DO NOT USE IF YOU WANT SCROLLABLE BEHAVIOR!
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class ClickableHorizontalScrollView extends HorizontalScrollView
{
	
	public ClickableHorizontalScrollView(final Context context)
	{
		super(context);
	}
	
	public ClickableHorizontalScrollView(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ClickableHorizontalScrollView(final Context context, final AttributeSet attrs, final int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onTouchEvent(final MotionEvent ev)
	{
		// do not let this scrollview scroll!
		// we want to use this scrollview in ListView and it should be clickable
		// not scrollable
		return false;
	}
	

}
