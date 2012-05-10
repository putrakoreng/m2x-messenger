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
package com.sir_m2x.messenger.helpers.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A derivation of SQLiteOpenHelper.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class SQLiteHelper extends SQLiteOpenHelper
{
	
	public static final String TABLE_CONVERSATIONS = "conversations";
	public static final String TABLE_CONVERSATION_CONTENTS = "conversation_contents";
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_LOGIN_ID = "login_id";
	public static final String COLUMN_BUDDY_ID = "buddy_id";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_FKEY = "f_key";
	public static final String COLUMN_MESSAGE = "message";
	public static final String COLUMN_SENDER = "sender";
	public static final String COLUMN_IS_OFFLINE = "isOffline";
		
	private static final String DATABASE_NAME = "M2XDb.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String CREATE_CONVERSATIONS = 
			"CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATIONS +
				"(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				COLUMN_BUDDY_ID + " TEXT," + 
				COLUMN_LOGIN_ID + " TEXT);";
	
	private static final String CREATE_CONVERSATION_CONTENTS =
			"CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATION_CONTENTS +
			"(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			COLUMN_TIMESTAMP + " REAL," +
			COLUMN_FKEY + " INTEGER," +
			COLUMN_SENDER + " TEXT," +
			COLUMN_MESSAGE + " TEXT," +
			COLUMN_IS_OFFLINE + " INTEGER," +
			"FOREIGN KEY ( " + COLUMN_FKEY + ") REFERENCES " + TABLE_CONVERSATIONS + "(" + COLUMN_ID + ")" + "ON DELETE CASCADE ON UPDATE CASCADE);";

	public SQLiteHelper(final Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onOpen(final SQLiteDatabase db)
	{
		super.onOpen(db);
		db.execSQL("PRAGMA foreign_keys=ON;");	// to make "ON DELETE CASCADE" work
	}

	@Override
	public void onCreate(final SQLiteDatabase db)
	{
		db.execSQL(CREATE_CONVERSATIONS);
		db.execSQL(CREATE_CONVERSATION_CONTENTS);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
	{
		// TODO Auto-generated method stub
	}

}
