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
package com.sir_m2x.messenger.services;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.openymsg.network.FireEvent;
import org.openymsg.network.ServiceType;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;
import org.openymsg.network.event.SessionExceptionEvent;
import org.openymsg.network.event.SessionListener;
import org.openymsg.network.event.SessionLogoutEvent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.YahooList;
import com.sir_m2x.messenger.activities.ChatWindowTabActivity;
import com.sir_m2x.messenger.datastructures.IM;
import com.sir_m2x.messenger.helpers.NotificationHelper;
import com.sir_m2x.messenger.helpers.ToastHelper;
import com.sir_m2x.messenger.utils.EventLogger;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;

/**
 * The messenger service which runs in the background and is responsible for
 * holding session-specific variables and show various notifications to the
 * user. This service is terminated upon sign out.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class MessengerService extends Service
{
	/*
	 * Intent constants
	 */
	public static final String INTENT_LOGIN = "com.sir_m2x.messenger.LOGIN";
	public static final String INTENT_LOGIN_PROGRESS = "com.sir_m2x.messenger.LOGIN_PROGRESS";
	public static final String INTENT_NEW_IM = "com.sir_m2x.messenger.NEW_IM";
	public static final String INTENT_IS_TYPING = "com.sir_m2x.messenger.IS_TYPING";
	public static final String INTENT_NEW_IM_ADDED = "com.sir_m2x.messenger.NEW_IM_ADDED";
	public static final String INTENT_CHAT_WINDOW_CREATED = "com.sir_m2x.messenger.CHAT_WINDOW_CREATED";
	public static final String INTENT_BUZZ = "com.sir_m2x.messenger.BUZZ";
	public static final String INTENT_DESTROY = "com.sir_m2x.messenger.DESTROY";
	public static final String INTENT_FOCUS_CONVERSATION = "com.sir_m2x.messenger.FOCUS_CONVERSATION";
	public static final String INTENT_FRIEND_UPDATE_RECEIVED = "com.sir_m2x.messenger.FRIEND_UPDATE_RECEIVED";
	public static final String INTENT_FRIEND_SIGNED_ON = "com.sir_m2x.messenger.FRIEND_SIGNED_ON";
	public static final String INTENT_FRIEND_SIGNED_OFF = "com.sir_m2x.messenger.FRIEND_SIGNED_OFF";
	public static final String INTENT_FRIEND_EVENT = "com.sir_m2x.messenger.FRIEND_EVENT";
	public static final String INTENT_LIST_CHANGED = "com.sir_m2x.messenger.LIST_CHANGED";
	public static final String INTENT_STATUS_CHANGED = "com.sir_m2x.messenger.STATUS_CHANGED";

	private static Session session;
	private static java.util.LinkedHashMap<String, LinkedList<IM>> friendsInChat = new LinkedHashMap<String, LinkedList<IM>>();
	private static Bitmap myAvatar = null;
	private static HashMap<String, Bitmap> friendAvatars = new HashMap<String, Bitmap>();
	private static String myId;
	private static HashMap<String, Integer> unreadIMs = new HashMap<String, Integer>();
	private static EventLogger eventLog = new EventLogger();
	private static NotificationHelper notificationHelper = null;
	private static Status currentStatus = Status.AVAILABLE;
	
	private WifiLock lock = null;

	private static YahooList yahooList = null;

	public static synchronized EventLogger getEventLog()
	{
		return MessengerService.eventLog;
	}

	public static HashMap<String, Integer> getUnreadIMs()
	{
		return unreadIMs;
	}

	public static Session getSession()
	{
		return session;
	}

	public static void setSession(final Session session)
	{
		MessengerService.session = session;
	}

	public static java.util.LinkedHashMap<String, LinkedList<IM>> getFriendsInChat()
	{
		return friendsInChat;
	}

	public static void setFriendsInChat(final java.util.LinkedHashMap<String, LinkedList<IM>> friendsInChat)
	{
		MessengerService.friendsInChat = friendsInChat;
	}

	public static Bitmap getMyAvatar()
	{
		return myAvatar;
	}

	public static void setMyAvatar(final Bitmap myAvatar)
	{
		MessengerService.myAvatar = myAvatar;
	}

	public static HashMap<String, Bitmap> getFriendAvatars()
	{
		return friendAvatars;
	}

	public static void setFriendAvatars(final HashMap<String, Bitmap> friendAvatars)
	{
		MessengerService.friendAvatars = friendAvatars;
	}

	public static String getMyId()
	{
		return myId;
	}

	public static void setMyId(final String myId)
	{
		MessengerService.myId = myId;
	}

	public static NotificationHelper getNotificationHelper()
	{
		return notificationHelper;
	}

	public static YahooList getYahooList()
	{
		return yahooList;
	}

	public static void setYahooList(final YahooList yahooList)
	{
		MessengerService.yahooList = yahooList;
	}

	public void Login(final String username, final String password, final Status status, final String customMessage, final boolean isCustomBusy)
	{
		yahooList = new YahooList(session, getApplicationContext());
		session.addSessionListener(yahooList);

		AsyncLogin asyncLogin = new AsyncLogin(this, session, status, customMessage, isCustomBusy, false);
		currentStatus = status;
		asyncLogin.execute(new String[] { username, password });
		MessengerService.notificationHelper.showDefaultNotification(true, false);
	}
	
	private void reconnect()
	{
		Log.d("M2X", "Reconnecting...");
		session = new Session();
		yahooList.refreshSession(session);
		session.addSessionListener(yahooList);
		session.addSessionListener(this.sessionListener);
	
		String username = Preferences.username;
		String password = Preferences.password;
		Status status = currentStatus;
		String customMessage = Preferences.customStatus;
		boolean isCustomBusy = Preferences.busy;
		
		AsyncLogin asyncLogin = new AsyncLogin(this, session, status, customMessage, isCustomBusy, true);
		
		asyncLogin.execute(new String[] { username, password });
		MessengerService.notificationHelper.showDefaultNotification(true, false);
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId)
	{
		Utils.initializeEnvironment();
		WifiManager wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		this.lock = wfm.createWifiLock("M2X");
		this.lock.acquire();
		
		session = new Session();
		session.addSessionListener(this.sessionListener);
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_LOGIN));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_EVENT));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_DESTROY));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_STATUS_CHANGED));

		MessengerService.notificationHelper = new NotificationHelper(getApplicationContext(), (NotificationManager) getSystemService(NOTIFICATION_SERVICE));
		if (Preferences.saveLog)
			Utils.loadEventLog(MessengerService.eventLog);
				
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy()
	{
		if (getSession().getSessionStatus() == SessionState.LOGGED_ON)
			try
			{
				getSession().logout();
			}
			catch (IllegalStateException e)
			{
			}
			catch (IOException e)
			{
			}

		//cancel all notifications and destroy all data in memory
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
		getFriendsInChat().clear();
		getFriendAvatars().clear();
		setMyAvatar(null);
		getUnreadIMs().clear();
		setMyId("");
		if (Preferences.saveLog)
			Utils.saveEventLog(eventLog);
		eventLog.getEventLog().clear();
		if (yahooList != null)
			yahooList.getFriendsList().clear();

		//unregister broadcast receivers
		unregisterReceiver(this.serviceBroadcastReceiver);
		//Toast.makeText(getApplicationContext(), "Logged out", Toast.LENGTH_LONG).show();
		sendBroadcast(new Intent(INTENT_DESTROY));
		//TODO when should this happen?
		//startActivity(new Intent(getApplicationContext(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		this.lock.release();
		super.onDestroy();
	}

	private final BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_LOGIN))
			{
				String username = intent.getExtras().getString(Utils.qualify("username"));
				String password = intent.getExtras().getString(Utils.qualify("password"));
				Status status = Status.getStatus(intent.getExtras().getLong(Utils.qualify("status"), Status.AVAILABLE.getValue()));
				String customMessage = intent.getExtras().getString(Utils.qualify("customMessage"));
				boolean isCustomBusy = intent.getExtras().getBoolean(Utils.qualify("isCustomBusy"));

				Login(username, password, status, customMessage, isCustomBusy);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_NEW_IM))
			{
				String sender = intent.getExtras().getString(Utils.qualify("from"));
				if (intent.getExtras().getString(Utils.qualify("message")) == null)
					return;

				String message = Html.fromHtml(intent.getExtras().getString(Utils.qualify("message"))).toString(); //in order to strip the HTML tags! 
				notificationHelper.vibrate(250);

				notificationHelper.playAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.message), Preferences.audibleStream, true);
				if (ChatWindowTabActivity.isActive)
					return;

				// raise a notification
				Intent intent2 = new Intent(getApplicationContext(), ChatWindowTabActivity.class);
				intent2.putExtra(Utils.qualify("friendId"), sender);

				// count the total number of unread IMs
				int unreadCount = 0;
				for (String key : unreadIMs.keySet())
					unreadCount += unreadIMs.get(key);

				notificationHelper.updateNotification(sender + ": " + message, "New message (" + unreadCount + ")", sender + ": " + message,
						NotificationHelper.NOTIFICATION_SIGNED_IN, R.drawable.ic_stat_notify, intent2, unreadCount, Notification.FLAG_ONGOING_EVENT, false);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_BUZZ))
			{
				// TODO don't fire notification if the ChatWindowTabActivity is open
				//String sender = intent.getExtras().getString(Utils.qualify("from"));
				notificationHelper.vibrate(500);

				notificationHelper.playAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.buzz), Preferences.audibleStream, true);

				//TODO fix notification
				//				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				//				Notification notification = new Notification(android.R.drawable.stat_notify_more, sender +": BUZZ!!!", System.currentTimeMillis());
				//				Intent intent2 = new Intent(getApplicationContext(), ChatWindowTabActivity.class);
				//				intent2.putExtra(Utils.qualify("friendId"), sender);
				//				PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent2, 0);
				//				notification.setLatestEventInfo(getApplicationContext(),"New message from " + sender + ": BUZZ!!!", pending);
				//				nm.notify(MessengerService.NOTIFICATION_SIGNED_IN, notification);				
			}
			else if (intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_ON))
			{
				String friendId = intent.getExtras().getString(Utils.qualify("who"));
				notificationHelper.playAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.online), Preferences.audibleStream, false);

				ToastHelper.showToast(getApplicationContext(), R.drawable.ic_stat_notify, friendId + " has signed on", Toast.LENGTH_LONG, Gravity.TOP | Gravity.RIGHT);
				//Toast.makeText(getApplicationContext(), friendId + " has signed on", Toast.LENGTH_LONG).show();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF))
			{
				String friendId = intent.getExtras().getString(Utils.qualify("who"));
				notificationHelper.playAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.offline), Preferences.audibleStream, false);

				ToastHelper.showToast(getApplicationContext(), R.drawable.ic_stat_notify_invisible, friendId + " has signed off", Toast.LENGTH_LONG, Gravity.TOP | Gravity.RIGHT);
				//Toast.makeText(getApplicationContext(), friendId + " has signed off", Toast.LENGTH_LONG).show();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED))
			{
				//				String friendId = intent.getExtras().getString(Utils.qualify("who"));
				//				String statusMessage = intent.getExtras().getString(Utils.qualify("what"));

				//TODO handle a status update message if needed.
			}
			else if (intent.getAction().equals(MessengerService.INTENT_FRIEND_EVENT))
			{
				String message = intent.getExtras().getString(Utils.qualify("event"));
				ToastHelper.showToast(getApplicationContext(), R.drawable.ic_stat_notify_away, message, Toast.LENGTH_LONG, Gravity.TOP | Gravity.RIGHT);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
			{
				String reason = intent.getExtras().getString(Utils.qualify("reason"));
				if (reason != null)
					Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_LONG).show();
				stopSelf();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_STATUS_CHANGED))
			{
				currentStatus = session.getStatus();						
				notificationHelper.showDefaultNotification(false, true);
			}

		}
	};

	

	private final SessionListener sessionListener = new SessionListener()
	{

		@Override
		public void dispatch(final FireEvent event)
		{
			//TODO IMPLEMENT LISTENER TO RESPONSE TO VARIOUS SITUATIONS
			if (event.getEvent() instanceof SessionLogoutEvent)
			{
				eventLog.log("M2X Messenger", "You are now logged in with this ID somewhere else!", System.currentTimeMillis());
				sendBroadcast(new Intent(INTENT_DESTROY).putExtra(Utils.qualify("reason"), "You are now logged in with this ID somewhere else!"));
			}
			else if (event.getEvent() == null && event.getType() == ServiceType.LOGOFF)	// probably an unknown hose exception!
			{
//				if (eventLog!=null)
//					eventLog.log("M2X Messenger", "Connection has encountered a problem", System.currentTimeMillis());
			}
			else if (event.getEvent() instanceof SessionExceptionEvent)
			{
				Log.d("M2X", "Received a sessionexception with: " + event.getEvent().getMessage());
				String message = event.getEvent().getMessage();
				if (message.toLowerCase().contains("lost"))
				{
					eventLog.log("M2X Messenger", "Sadly we got disconnected from Yahoo! Will try to reconnect...", new Date(System.currentTimeMillis()));
					MessengerService.this.reconnect();
				}
			}
//			else if (event.getEvent() instanceof SessionExceptionEvent && session.getSessionStatus() != SessionState.UNSTARTED)
////			 if (event.getType() == ServiceType.LOGOFF)	// if the connection is lost
//				{
//					eventLog.log("M2X Messenger", "Sadly we got disconnected from Yahoo!", new Date(System.currentTimeMillis()));
//					MessengerService.this.reconnect();
//				}
////				else
////					sendBroadcast(new Intent(INTENT_DESTROY).putExtra(Utils.qualify("reason"), "Oops! Crashed or lost connection..."));
//			
////			else if (event.getEvent() == null && event.getType() == ServiceType.LOGOFF)
////				sendBroadcast(new Intent(INTENT_DESTROY).putExtra(Utils.qualify("reason"), "Disconnected from Yahoo!"));
		}
	};

	@Override
	public IBinder onBind(final Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	
}