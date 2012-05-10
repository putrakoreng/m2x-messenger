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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * DAO for conversations table
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class Conversations
{
	/**
	 * Utility method to determine the value of the foreign key field in the conversation_contents table
	 * @param context
	 * 		Android context
	 * @param loginId
	 * 		Our ID
	 * @param buddyId
	 * 		Buddy's ID
	 * @return
	 * 		The primary key in the 'conversations' table
	 */
	public static long getConversationId(final Context context, final String loginId, final String buddyId)
	{
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		String query = String.format("SELECT %s FROM %s WHERE %s = '%s' AND %s = '%s'", SQLiteHelper.COLUMN_ID, SQLiteHelper.TABLE_CONVERSATIONS,
				SQLiteHelper.COLUMN_LOGIN_ID, loginId, SQLiteHelper.COLUMN_BUDDY_ID, buddyId);

		Cursor result = db.rawQuery(query, null);
		
		long fKey;

		if (!result.moveToFirst())	// if we did not have this conversation record before, create it
		{
			result.close();
			db.close();
			dbHelper.close();
			
			fKey = Conversations.insert(context, loginId, buddyId);
		}
		else
		{
			fKey = result.getLong(result.getColumnIndex(SQLiteHelper.COLUMN_ID));
			result.close();
			db.close();
			dbHelper.close();
		}
		
		return fKey;		
	}
	
	/**
	 * Inserts a new row in the conversations table.
	 * @param context
	 * 		Android context
	 * @param loginId
	 * 		Our ID
	 * @param buddyId
	 * 		Buddy's ID
	 * @return
	 * 		The ID of the inserted row
	 */
	public static long insert(final Context context, final String loginId, final String buddyId)
	{
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_LOGIN_ID, loginId);		
		values.put(SQLiteHelper.COLUMN_BUDDY_ID, buddyId);
		long id = db.insert(SQLiteHelper.TABLE_CONVERSATIONS, "", values);
		db.close();
		dbHelper.close();
		
		return id;
	}
	
	/**
	 * Deletes a row in the 'conversations' table 
	 * @param context
	 * 		Android context
	 * @param loginId
	 * 		Our ID
	 * @param buddyId
	 * 		Buddy's ID
	 * @return
	 * 		Count of affected rows
	 */
	public static int delete(final Context context, final String loginId, final String buddyId)
	{
		long convId = getConversationId(context, loginId, buddyId);
		
		SQLiteHelper dbHelper = new SQLiteHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		String where = String.format("%s = '%s' AND %s = '%s'", SQLiteHelper.COLUMN_LOGIN_ID, loginId, SQLiteHelper.COLUMN_BUDDY_ID, buddyId);
		
		int affectedRows = db.delete(SQLiteHelper.TABLE_CONVERSATIONS, where, null);
		db.close();
		dbHelper.close();
		
		ConversationContents.deleteByForeignKey(context, convId);	// for compatibility with Android v2.1 (Foreign key constraints are not supported)
				
		return affectedRows;
	}
}