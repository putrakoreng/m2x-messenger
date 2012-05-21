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
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openymsg.network.NetworkConstants;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooUser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.longevitysoft.android.xml.plist.domain.Dict;
import com.sir_m2x.messenger.R;
import com.sir_m2x.messenger.classes.IM;
import com.sir_m2x.messenger.dialogs.CustomDialog;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;
import com.sir_m2x.messenger.utils.Utils;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.ViewProvider;

/**
 * INCOMPLETE! Literally useless for now ;-]
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class ChatWindowPager extends FragmentActivity implements SensorEventListener
{
	private ViewPager mViewPager = null;
	public TabPageIndicator mTabPageIndicator = null;
	private ChatFragmentAdapter mAdapter = null;
	private SlidingDrawer drawer = null;
	private Button btnClose = null;
	private Button btnSmiley = null;
	public static String currentFriendId = "";
	public static int currentItem = 0;
	public static boolean isActive = false;

	private RelativeLayout rootLayout = null;
	private Animation buzzAnimation = null;

	// a flag to prevent sending consecutive BUZZes!
	// this flag should be reset after a specific timeout 
	private static final AtomicBoolean buzzWaitElapsed = new AtomicBoolean(true);
	private SensorManager sensorMgr = null;
	
	private CountDownTimer buzzAllower = new CountDownTimer(NetworkConstants.BUZZ_SEND_DELAY, 1000)
	{
		@Override
		public void onTick(final long millisUntilFinished)
		{			
		}
		
		@Override
		public void onFinish()
		{
			buzzWaitElapsed.set(true);
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_window_pager);

		this.rootLayout = (RelativeLayout) findViewById(R.id.chatPagerParent);
		this.buzzAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);

		this.mViewPager = (ViewPager) findViewById(R.id.chatPager);
		this.mAdapter = new ChatFragmentAdapter(getSupportFragmentManager());
		this.mViewPager.setAdapter(this.mAdapter);

		this.drawer = (SlidingDrawer) findViewById(R.id.drawer);
		this.btnClose = (Button) findViewById(R.id.btnClose);
		this.btnSmiley = (Button) findViewById(R.id.btnSmiley);
		GridView gv = (GridView) this.drawer.findViewById(R.id.content);
		gv.setAdapter(new SmileyAdapter());
		gv.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3)
			{
				String smiley = arg1.getTag().toString();
				Intent intent = new Intent();
				intent.setAction(MessengerService.INTENT_INSERT_SMILEY).putExtra(Utils.qualify("from"), ChatWindowPager.currentFriendId).putExtra(Utils.qualify("symbol"), smiley);

				sendBroadcast(intent);
				ChatWindowPager.this.drawer.animateClose();
			}
		});

		if (getIntent().hasExtra(Utils.qualify("friendId")))
		{
			currentFriendId = getIntent().getExtras().getString(Utils.qualify("friendId"));
			if (!MessengerService.getFriendsInChat().keySet().contains(currentFriendId))
				MessengerService.getFriendIMs(this, currentFriendId);
		}
		else if (currentFriendId == "" || !MessengerService.getFriendsInChat().keySet().contains(currentFriendId))
			currentFriendId = MessengerService.getFriendsInChat().keySet().toArray()[0].toString();

		for (int i = 0; i < MessengerService.getFriendsInChat().size(); i++)
			if (MessengerService.getFriendsInChat().keySet().toArray()[i].toString().equals(currentFriendId))
			{
				currentItem = i;
				break;
			}

		this.mTabPageIndicator = (TabPageIndicator) findViewById(R.id.chatIndicator);
		this.mTabPageIndicator.setViewPager(this.mViewPager, currentItem);
		this.mTabPageIndicator.setOnPageChangeListener(new OnPageChangeListener()
		{

			@Override
			public void onPageScrollStateChanged(final int arg0)
			{
			}

			@Override
			public void onPageScrolled(final int arg0, final float arg1, final int arg2)
			{
			}

			@Override
			public void onPageSelected(final int arg0)
			{
				buzzWaitElapsed.set(true);
				ChatWindowPager.this.buzzAllower.cancel();
				ChatWindowPager.currentFriendId = ChatWindowPager.this.mAdapter.getId(arg0);
				currentItem = arg0;

				synchronized (MessengerService.getUnreadIMs())
				{
					MessengerService.getUnreadIMs().remove(ChatWindowPager.this.mAdapter.getId(arg0));
					ChatWindowPager.this.mTabPageIndicator.notifyDataSetChanged();
				}
			}
		});

		this.btnClose.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				Utils.saveConversationHistory(ChatWindowPager.this, currentFriendId);
				MessengerService.getFriendsInChat().remove(currentFriendId);
				currentItem--;
				if (currentItem < 0)
					if (MessengerService.getFriendsInChat().size() == 0)
					{
						finish();
						return;
					}
					else
						currentItem++;

				currentFriendId = MessengerService.getFriendsInChat().keySet().toArray()[currentItem].toString();

				Intent intent = getIntent();
				overridePendingTransition(0, 0);
				intent.putExtra(Utils.qualify("friendId"), currentFriendId);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				finish();
				overridePendingTransition(0, 0);
				startActivity(intent);
			}
		});
		this.btnSmiley.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(final View v)
			{
				ChatWindowPager.this.drawer.animateOpen();
			}
		});

		MessengerService.getUnreadIMs().remove(currentFriendId);
		this.mViewPager.setCurrentItem(currentItem);
		this.mTabPageIndicator.notifyDataSetChanged();

		// reshow the default notification
		MessengerService.getNotificationHelper().showDefaultNotification(false, false);
	}

	@Override
	protected void onResume()
	{
		isActive = true;
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_IS_TYPING));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_NEW_IM));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_DESTROY));
		registerReceiver(this.listener, new IntentFilter(MessengerService.INTENT_BUZZ));

		this.sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		this.mAccel = 0.00f;
		this.mAccelCurrent = SensorManager.GRAVITY_EARTH;
		this.mAccelLast = SensorManager.GRAVITY_EARTH;

		if (Preferences.shake2Buzz)
			this.sensorMgr.registerListener(this, this.sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

		super.onResume();
	}

	@Override
	protected void onPause()
	{
		MessengerService.getNotificationHelper().showDefaultNotification(false, false);
		isActive = false;
		unregisterReceiver(this.listener);
		this.sensorMgr.unregisterListener(this);
		super.onPause();
	}

	class ChatFragmentAdapter extends FragmentPagerAdapter implements ViewProvider
	{
		public ChatFragmentAdapter(final FragmentManager fm)
		{
			super(fm);
		}

		public String getId(final int position)
		{
			return MessengerService.getFriendsInChat().keySet().toArray()[position].toString();
		}

		@Override
		public Fragment getItem(final int arg0)
		{
			String friendId = MessengerService.getFriendsInChat().keySet().toArray()[arg0].toString();
			ChatWindowFragment f = ChatWindowFragment.newInstance(friendId);
			return f;
		}

		@Override
		public int getCount()
		{
			return MessengerService.getFriendsInChat().size();
		}

		@Override
		public View getView(final int position)
		{
			View v = getLayoutInflater().inflate(R.layout.vpi__tab_holder, null);
			TextView tv = (TextView) v.findViewById(R.id.txtId);
			ImageView imgBulb = (ImageView) v.findViewById(R.id.imgBulb);
			ImageView imgIsTyping = (ImageView) v.findViewById(R.id.imgIsTyping);
			ImageView imgUnreadIm = (ImageView) v.findViewById(R.id.imgUnreadIm);

			String friendId = MessengerService.getFriendsInChat().keySet().toArray()[position].toString();
			YahooUser user = null;

			for (YahooGroup g : MessengerService.getYahooList().getFriendsList().values())
			{
				for (YahooUser u : g.getUsers())
					if (u.getId().equals(friendId))
					{
						user = u;
						break;
					}
				if (user != null)
					break;
			}

			if (user == null) // then it means we are having a conversation with a non-list person
			{
				imgBulb.setImageResource(R.drawable.presence_invisible);
				imgIsTyping.setVisibility(View.GONE);
			}
			else
			{
				switch (user.getStatus())
				{
					case AVAILABLE:
						imgBulb.setImageResource(R.drawable.presence_online);
						break;
					case BUSY:
						imgBulb.setImageResource(R.drawable.presence_busy);
						break;
					case IDLE:
						imgBulb.setImageResource(R.drawable.presence_away);
						break;
					case CUSTOM:
						if (user.isCustomStatusBusy())
							imgBulb.setImageResource(R.drawable.presence_busy);
						else
							imgBulb.setImageResource(R.drawable.presence_online);
						break;
					case OFFLINE:
						imgBulb.setImageResource(R.drawable.presence_offline);
						break;
					default:
						imgBulb.setImageResource(R.drawable.presence_busy);
				}

				imgIsTyping.setVisibility(user.isTyping() ? View.VISIBLE : View.GONE);
			}

			synchronized (MessengerService.getUnreadIMs())
			{
				HashMap<String, Integer> unreadIMs = MessengerService.getUnreadIMs();
				imgUnreadIm.setVisibility(unreadIMs.containsKey(friendId) ? View.VISIBLE : View.GONE);
			}

			tv.setText(friendId);
			return v;
		}
	}

	BroadcastReceiver listener = new BroadcastReceiver()
	{

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			if (intent.getAction().equals(MessengerService.INTENT_IS_TYPING))
				ChatWindowPager.this.mTabPageIndicator.notifyDataSetChanged();
			else if (intent.getAction().equals(MessengerService.INTENT_NEW_IM))
			{
				synchronized (MessengerService.getUnreadIMs())
				{
					MessengerService.getUnreadIMs().remove(ChatWindowPager.this.mAdapter.getId(ChatWindowPager.this.mViewPager.getCurrentItem()));
				}
				ChatWindowPager.this.mTabPageIndicator.notifyDataSetChanged();
			}
			else if (intent.getAction().equals(MessengerService.INTENT_BUZZ))
				startBuzzAnimation();
			else if (intent.getAction().equals(MessengerService.INTENT_DESTROY))
				finish();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_chat_window, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		if (item.getItemId() == R.id.mnuClose)
		{
			//TODO find a better solution for this than restarting the activity

			//MessengerService.getFriendsInChat().get(currentFriendId).clear();
			Utils.saveConversationHistory(this, currentFriendId);
			MessengerService.getFriendsInChat().remove(currentFriendId);
			currentItem--;
			if (currentItem < 0)
				if (MessengerService.getFriendsInChat().size() == 0)
				{
					finish();
					return true;
				}
				else
					currentItem++;

			currentFriendId = MessengerService.getFriendsInChat().keySet().toArray()[currentItem].toString();

			Intent intent = getIntent();
			overridePendingTransition(0, 0);
			intent.putExtra(Utils.qualify("friendId"), currentFriendId);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intent);
			/*
			 * Not working properly: removing the fragment and selecting another
			 * fragment will not destroy the already instantiated one and
			 * therefore will cause some UI bugs.
			 * 
			 * this.mViewPager.setAdapter(null); this.mAdapter = new
			 * ChatFragmentAdapter(getSupportFragmentManager());
			 * this.mViewPager.setAdapter(this.mAdapter);
			 * 
			 * this.mTabPageIndicator.notifyDataSetChanged();
			 * this.mAdapter.notifyDataSetChanged();
			 * this.mTabPageIndicator.setCurrentItem(currentItem);
			 * this.mViewPager.setCurrentItem(currentItem, true);
			 */

		}
		else if (item.getItemId() == R.id.mnuBuzz)
			doBuzz();
		else if (item.getItemId() == R.id.mnuClear)
		{
			final CustomDialog dlg = new CustomDialog(this);
			dlg.setTitle("Clear history");
			dlg.setMessage("Are you sure you want to clear the history for this contact (all messages will be cleared)?").setPositiveButton("Yes", new View.OnClickListener()
			{

				@Override
				public void onClick(final View v)
				{
					Utils.clearHistory(ChatWindowPager.this, currentFriendId);
					Intent intent = new Intent(MessengerService.INTENT_LIST_CHANGED);
					sendBroadcast(intent);
					dlg.dismiss();
				}
			}).setNegativeButton("No", new View.OnClickListener()
			{

				@Override
				public void onClick(final View v)
				{
					dlg.dismiss();
				}
			}).show();
		}

		return true;
	}

	@Override
	public void onBackPressed()
	{
		if (this.drawer.isOpened())
		{
			this.drawer.animateClose();
			return;
		}

		Intent intent = new Intent(this, ContactsListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		finish();
		startActivity(intent);
		overridePendingTransition(R.anim.reverse_zoom_exit, R.anim.reverse_zoom_enter);
	};

	@Override
	protected void onNewIntent(final Intent intent)
	{
		if (intent.hasExtra(Utils.qualify("friendId")))
		{
			currentFriendId = intent.getExtras().getString(Utils.qualify("friendId"));
			if (!MessengerService.getFriendsInChat().keySet().contains(currentFriendId))
				MessengerService.getFriendIMs(this, currentFriendId);
		}
		else if (currentFriendId == "" || !MessengerService.getFriendsInChat().keySet().contains(currentFriendId))
			currentFriendId = MessengerService.getFriendsInChat().keySet().toArray()[0].toString();

		for (int i = 0; i < MessengerService.getFriendsInChat().size(); i++)
			if (MessengerService.getFriendsInChat().keySet().toArray()[i].toString().equals(currentFriendId))
			{
				currentItem = i;
				break;
			}

		MessengerService.getUnreadIMs().remove(currentFriendId);
		this.mTabPageIndicator.setCurrentItem(currentItem);
		this.mViewPager.setCurrentItem(currentItem);
		this.mTabPageIndicator.notifyDataSetChanged();

		// reshow the default notification
		MessengerService.getNotificationHelper().showDefaultNotification(false, false);

		super.onNewIntent(intent);
	}

	void startBuzzAnimation()
	{
		this.rootLayout.startAnimation(this.buzzAnimation);
	}

	class SmileyAdapter extends BaseAdapter
	{
		private final int COUNT = 94; // count of smileys

		@Override
		public int getCount()
		{
			return this.COUNT;
		}

		@Override
		public Object getItem(final int arg0)
		{
			return null;
		}

		@Override
		public long getItemId(final int position)
		{
			return position;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent)
		{
			ImageView iv;
			if (convertView == null)
				iv = new ImageView(ChatWindowPager.this);
			else
				iv = (ImageView) convertView;

			iv.setLayoutParams(new GridView.LayoutParams((int) (51 * (Utils.deviceDensity / 240)), (int) (38 * (Utils.deviceDensity / 240))));
			String smileyName = "smiley" + (position <= 78 ? position + 1 : position + 21) + ".png";
			InputStream is = null;

			try
			{
				is = getResources().getAssets().open("smiley/" + smileyName);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			iv.setImageBitmap(BitmapFactory.decodeStream(is));
			String equivalent = ((com.longevitysoft.android.xml.plist.domain.String) ((Dict) MessengerService.emoticonsMap.getConfigMap().get(smileyName)).getConfigurationArray(
					"Equivalents").get(0)).getValue();
			iv.setTag(equivalent);

			return iv;
		}

	}

	private float mAccel; // acceleration apart from gravity
	private float mAccelCurrent; // current acceleration including gravity
	private float mAccelLast; // last acceleration including gravity

	@Override
	public void onSensorChanged(final SensorEvent event)
	{
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		this.mAccelLast = this.mAccelCurrent;
		this.mAccelCurrent = (float) Math.sqrt((x * x + y * y + z * z));
		float delta = this.mAccelCurrent - this.mAccelLast;
		this.mAccel = this.mAccel * 0.9f + delta; // perform low-cut filter

		if (this.mAccel > Preferences.shakeSensitivity)
			if (doBuzz())
			{
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(new long[] { 0, 100, 200, 100, 200, 100 }, -1);
			}

	}

	boolean doBuzz()
	{
		synchronized (buzzWaitElapsed)
		{
			Log.w("M2X", "" + buzzWaitElapsed.get());
			if (!buzzWaitElapsed.get())
				return false;

			buzzWaitElapsed.set(false);
			try
			{
				this.buzzAllower.start();

				IM im = new IM(MessengerService.getMyId(), NetworkConstants.BUZZ, new Date(System.currentTimeMillis()), false);
				MessengerService.getSession().sendBuzz(currentFriendId);
				MessengerService.addIm(this, currentFriendId, im);
				Intent intent = new Intent();
				intent.setAction(MessengerService.INTENT_BUZZ);
				intent.putExtra(Utils.qualify("from"), MessengerService.getMyId());
				sendBroadcast(intent);

				intent = new Intent();
				intent.setAction(MessengerService.INTENT_NEW_IM);
				intent.putExtra(Utils.qualify("from"), currentFriendId);
				sendBroadcast(intent);

				this.mTabPageIndicator.notifyDataSetChanged();
				startBuzzAnimation();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy)
	{
	}
}