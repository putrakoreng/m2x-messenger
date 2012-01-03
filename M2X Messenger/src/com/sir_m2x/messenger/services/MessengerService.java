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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.openymsg.network.FireEvent;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.event.SessionExceptionEvent;
import org.openymsg.network.event.SessionListener;
import org.openymsg.network.event.SessionLogoutEvent;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.sir_m2x.messenger.FriendsList;
import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.activities.ChatWindowTabActivity;
import com.sir_m2x.messenger.activities.LoginActivity;
import com.sir_m2x.messenger.helpers.NotificationHelper;
import com.sir_m2x.messenger.helpers.ToastHelper;
import com.sir_m2x.messenger.utils.EventLogger;
import com.sir_m2x.messenger.utils.IM;
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

	public static EventLogger getEventLog()
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

	public Handler getToastHandler()
	{
		return this.toastHandler;
	}

	public void setToastHandler(final Handler toastHandler)
	{
		this.toastHandler = toastHandler;
	}

	//	public Thread getShowToasts()
	//	{
	//		return showToasts;
	//	}
	//
	//	public void setShowToasts(Thread showToasts)
	//	{
	//		this.showToasts = showToasts;
	//	}

	public static NotificationHelper getNotificationHelper()
	{
		return notificationHelper;
	}

	private Handler toastHandler = new Handler()
	{
		@Override
		public void handleMessage(final android.os.Message msg)
		{
			//			synchronized (getAlertMessages())
			//			{
			//				for (String alertMessage : getAlertMessages())
			//				{
			//					Toast.makeText(getApplicationContext(), alertMessage,
			//							Toast.LENGTH_LONG).show();
			//					try
			//					{
			//						Thread.sleep(1000);
			//					}
			//					catch (InterruptedException e)
			//					{
			//						e.printStackTrace();
			//					}
			//				}
			//				getAlertMessages().clear();
			//			}
		};
	};

	//	private Thread showToasts = new Thread()
	//	{
	//		@Override
	//		public void run()
	//		{
	//			while (isMessageThreadRun())
	//			{
	//				if (!getAlertMessages().isEmpty())
	//				{
	//					Message msg = new Message();
	//					msg.setTarget(toastHandler);
	//					toastHandler.sendMessage(msg);
	//				}
	//				try
	//				{
	//					Thread.sleep(1000);
	//				}
	//				catch (Exception ex)
	//				{
	//					ex.printStackTrace();
	//				}
	//			}
	//		};
	//	};

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId)
	{
		session.addSessionListener(this.sessionListener);
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_BUZZ));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_ON));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_UPDATE_RECEIVED));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_SIGNED_OFF));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_FRIEND_EVENT));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_DESTROY));
		registerReceiver(this.serviceBroadcastReceiver, new IntentFilter(MessengerService.INTENT_STATUS_CHANGED));

		MessengerService.notificationHelper = new NotificationHelper(getApplicationContext(), (NotificationManager) getSystemService(NOTIFICATION_SERVICE));
		MessengerService.notificationHelper.showDefaultNotification(true, false);

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
		eventLog.getEventLog().clear();
		FriendsList.getMasterList().clear();

		//unregister broadcast receivers
		unregisterReceiver(this.serviceBroadcastReceiver);
		//Toast.makeText(getApplicationContext(), "Logged out", Toast.LENGTH_LONG).show();
		sendBroadcast(new Intent(INTENT_DESTROY));
		startActivity(new Intent(getApplicationContext(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		super.onDestroy();
	}

	private final BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_NEW_IM))
			{
				String sender = intent.getExtras().getString(Utils.qualify("from"));
				if (intent.getExtras().getString(Utils.qualify("message")) == null)
					return;

				String message = Html.fromHtml(intent.getExtras().getString(Utils.qualify("message"))).toString(); //in order to strip the HTML tags! 
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(250);

				PlayAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.message), AudioManager.STREAM_NOTIFICATION);
				if (ChatWindowTabActivity.isActive)
					return;

				// raise a notification
				Intent intent2 = new Intent(getApplicationContext(), ChatWindowTabActivity.class);
				intent2.putExtra(Utils.qualify("friendId"), sender);

				notificationHelper.updateNotification(sender + ": " + message, "New message from " + sender + ": ", message, NotificationHelper.NOTIFICATION_SIGNED_IN,
						R.drawable.ic_stat_notify, intent2);
			}
			else if (intent.getAction().equals(MessengerService.INTENT_BUZZ))
			{
				// TODO don't fire notification if the ChatWindowTabActivity is open
				//String sender = intent.getExtras().getString(Utils.qualify("from"));
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(500);

				PlayAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.buzz), AudioManager.STREAM_NOTIFICATION);

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
				PlayAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.online), AudioManager.STREAM_NOTIFICATION);

				ToastHelper.showToast(getApplicationContext(), R.drawable.ic_stat_notify, friendId + " has signed on", Toast.LENGTH_LONG, Gravity.TOP|Gravity.RIGHT);
				//Toast.makeText(getApplicationContext(), friendId + " has signed on", Toast.LENGTH_LONG).show();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_FRIEND_SIGNED_OFF))
			{
				String friendId = intent.getExtras().getString(Utils.qualify("who"));
				PlayAudio(Uri.parse("android.resource://com.sir_m2x.messenger/" + R.raw.offline), AudioManager.STREAM_NOTIFICATION);

				ToastHelper.showToast(getApplicationContext(), R.drawable.ic_stat_notify_invisible, friendId + " has signed off", Toast.LENGTH_LONG, Gravity.TOP|Gravity.RIGHT);
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
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
			{
				String reason = intent.getExtras().getString(Utils.qualify("reason"));
				if (reason != null)
					Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_LONG).show();
				stopSelf();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_STATUS_CHANGED))
				notificationHelper.showDefaultNotification(false, true);

		}
	};

	private void PlayAudio(final Uri uri, final int streamType)
	{
		MediaPlayer mp = new MediaPlayer();
		try
		{
			mp.setDataSource(getApplicationContext(), uri);
			mp.setAudioStreamType(streamType);
			mp.prepare();
			mp.start();
		}
		catch (Exception e)
		{
			Log.w("M2X-Messenger", "Unable to play " + uri);
		}

	}

	private final SessionListener sessionListener = new SessionListener()
	{

		@Override
		public void dispatch(final FireEvent event)
		{
			//TODO IMPLEMENT LISTENER TO RESPONSE TO VARIOUS SITUATIONS
			if (event.getEvent() instanceof SessionLogoutEvent)
				sendBroadcast(new Intent(INTENT_DESTROY).putExtra(Utils.qualify("reason"), "You are now logged in with this ID somewhere else!"));
			else if (event.getEvent() instanceof SessionExceptionEvent)
				sendBroadcast(new Intent(INTENT_DESTROY).putExtra(Utils.qualify("reason"), "Oops! Crashed or lost connection..."));
		}
	};

	@Override
	public IBinder onBind(final Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}
}