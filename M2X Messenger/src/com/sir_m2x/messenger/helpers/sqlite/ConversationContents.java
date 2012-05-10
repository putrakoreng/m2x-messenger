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

import java.util.Date;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sir_m2x.messenger.classes.IM;

/**
 * DAO for conversation_contents table
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class ConversationContents
{
	/**
	 * Selects the stored conversation history between us and a friend.
	 * @param context
	 * 		Android context
	 * @param loginId
	 * 		Out id
	 * @param buddyId
	 * 		Buddy's ID
	 * @return
	 * 		A LinkedList containing the history of our conversation
	 */
	public static LinkedList<IM> select(final Context context, final String loginId, final String buddyId)
	{
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		String query = String.format("SELECT * FROM %s, %s WHERE %s = %s AND %s = '%s' AND %s = '%s' ", SQLiteHelper.TABLE_CONVERSATIONS, SQLiteHelper.TABLE_CONVERSATION_CONTENTS,
				SQLiteHelper.TABLE_CONVERSATIONS + "." + SQLiteHelper.COLUMN_ID, SQLiteHelper.TABLE_CONVERSATION_CONTENTS + "." + SQLiteHelper.COLUMN_FKEY, SQLiteHelper.COLUMN_LOGIN_ID, loginId, SQLiteHelper.COLUMN_BUDDY_ID, buddyId);

		Cursor result = db.rawQuery(query, null);
		
		if (!result.moveToFirst())		// no history is present
		{
			result.close();
			db.close();
			dbHelper.close();
			
			return new LinkedList<IM>();
		}

		LinkedList<IM> ims = new LinkedList<IM>();

		while (!result.isAfterLast())
		{
			String sender = result.getString(result.getColumnIndex(SQLiteHelper.COLUMN_SENDER));
			String message = result.getString(result.getColumnIndex(SQLiteHelper.COLUMN_MESSAGE));
			long timeStamp = result.getLong(result.getColumnIndex(SQLiteHelper.COLUMN_TIMESTAMP));
			boolean isOffline = result.getInt(result.getColumnIndex(SQLiteHelper.COLUMN_IS_OFFLINE)) == 1 ? true : false;

			IM im = new IM(sender, message, new Date(timeStamp), isOffline, true);
			ims.add(im);
			result.moveToNext();
		}
		
		result.close();
		db.close();
		dbHelper.close();
		
		return ims;
	}

	/**
	 * Inserts a new IM in the history tables in the database.
	 * @param context
	 * 		Android context
	 * @param loginId
	 * 		Our id
	 * @param im
	 * 		The IM to store in the database
	 * @param fKey
	 * 		The foreign key needed to set the relationship
	 * @return
	 * 		The id of the inserted item.
	 */
	public static long insert(final Context context, final String loginId, final IM im, final long fKey)
	{
		if (im.isOld())
			return -1;
		
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_SENDER, im.getSender());
		values.put(SQLiteHelper.COLUMN_MESSAGE, im.getMessage());
		values.put(SQLiteHelper.COLUMN_TIMESTAMP, im.getTimeStamp().getTime());
		values.put(SQLiteHelper.COLUMN_IS_OFFLINE, im.isOfflineMessage() ? 1 : 0);
		values.put(SQLiteHelper.COLUMN_FKEY, fKey);

		long id = db.insert(SQLiteHelper.TABLE_CONVERSATION_CONTENTS, null, values);
		db.close();
		dbHelper.close();

		return id;
	}

	/**
	 * Deletes those history rows which are older than the timestamp specified.
	 * @param context
	 * 		Android context
	 * @param maxTimeStamp
	 * 		Rows inserted after this timestamp are not affected
	 * @return
	 * 		Count of affected rows
	 */
	public static int delete(final Context context, final long maxTimeStamp)
	{
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		String where = String.format("%s < %d", SQLiteHelper.COLUMN_TIMESTAMP, maxTimeStamp);

		int affectedRows = db.delete(SQLiteHelper.TABLE_CONVERSATION_CONTENTS, where, null);

		db.close();
		dbHelper.close();
		return affectedRows;
	}
	
	/**
	 * Deletes rows from history based on their foreign key value.
	 * @param context
	 * 		Android context
	 * @param fKey
	 * 		The value of the foreign key
	 * @return
	 * 		Count of accected rows
	 */
	public static int deleteByForeignKey(final Context context, final long fKey)
	{
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		String where = String.format("%s = %d", SQLiteHelper.COLUMN_FKEY, fKey);

		int affectedRows = db.delete(SQLiteHelper.TABLE_CONVERSATION_CONTENTS, where, null);

		db.close();
		dbHelper.close();
		return affectedRows;
	}
}