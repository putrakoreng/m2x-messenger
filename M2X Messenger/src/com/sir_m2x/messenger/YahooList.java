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
package com.sir_m2x.messenger;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openymsg.network.ContactListType;
import org.openymsg.network.FireEvent;
import org.openymsg.network.ServiceType;
import org.openymsg.network.Session;
import org.openymsg.network.SessionState;
import org.openymsg.network.Status;
import org.openymsg.network.StealthStatus;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooProtocol;
import org.openymsg.network.YahooUser;
import org.openymsg.network.event.SessionAuthorizationEvent;
import org.openymsg.network.event.SessionChatEvent;
import org.openymsg.network.event.SessionConferenceDeclineInviteEvent;
import org.openymsg.network.event.SessionConferenceInviteEvent;
import org.openymsg.network.event.SessionConferenceLogoffEvent;
import org.openymsg.network.event.SessionConferenceLogonEvent;
import org.openymsg.network.event.SessionConferenceMessageEvent;
import org.openymsg.network.event.SessionErrorEvent;
import org.openymsg.network.event.SessionEvent;
import org.openymsg.network.event.SessionExceptionEvent;
import org.openymsg.network.event.SessionFileTransferEvent;
import org.openymsg.network.event.SessionFriendAcceptedEvent;
import org.openymsg.network.event.SessionFriendEvent;
import org.openymsg.network.event.SessionFriendFailureEvent;
import org.openymsg.network.event.SessionFriendRejectedEvent;
import org.openymsg.network.event.SessionGroupEvent;
import org.openymsg.network.event.SessionListEvent;
import org.openymsg.network.event.SessionListener;
import org.openymsg.network.event.SessionNewMailEvent;
import org.openymsg.network.event.SessionNotifyEvent;
import org.openymsg.network.event.SessionPictureEvent;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.sir_m2x.messenger.activities.ContactsListActivity;
import com.sir_m2x.messenger.activities.FriendRequestsActivity;
import com.sir_m2x.messenger.classes.FriendRequest;
import com.sir_m2x.messenger.helpers.AvatarHelper;
import com.sir_m2x.messenger.helpers.NotificationHelper;
import com.sir_m2x.messenger.services.MessengerService;
import com.sir_m2x.messenger.utils.Preferences;

/**
 * This class hold the Yahoo! list and is an alternative to OpenYMSG's Roster
 * class. Roster is cumbersome to use and still lacks some important features
 * like storing and pendings list. So it makes sense to implement another class
 * which is manageable and seamless.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class YahooList implements SessionListener
{
	// The infamous friends list
	private TreeMap<String, YahooGroup> friendsList;
	// A list to manage arrived friend requests
	private TreeMap<String, FriendRequest> friendRequests = new TreeMap<String, FriendRequest>();
	// The session which is the owner of this instance
	private Session parentSession = null;
	// Android's context
	private MessengerService context = null;

	private MySessionAdapter mySessionAdapter = null;
	/**
	 * A flag for the users of this class. If this flag is false, then it means
	 * that the list is not ready yet and accessing it may result in unknown
	 * behavior.
	 */
	private Boolean listReady = false;

	public Boolean initFinished = false;

	public YahooList(final Session parentSession, final MessengerService context)
	{
		this.parentSession = parentSession;
		this.context = context;
		Log.d("M2X", "List not ready!");
		this.mySessionAdapter = new MySessionAdapter(this.context);
	}

	// for when we want to reconnect
	public void refreshSession(final Session session)
	{
		this.initFinished = false;
		this.parentSession = session;
	}

	/**
	 * Returns the friend list maintained by this instance.
	 * 
	 * @return The friends list.
	 */
	public TreeMap<String, YahooGroup> getFriendsList()
	{
		return this.friendsList;
	}

	public void setFriendsList(final TreeMap<String, YahooGroup> friendsList)
	{
		this.friendsList = friendsList;
	}

	public TreeMap<String, FriendRequest> getFriendRequests()
	{
		return this.friendRequests;
	}

	/**
	 * Check the "listReady" flag.
	 * 
	 * @return True - if this instance is ready to be used. False - if this
	 *         instance is not ready yet.
	 */
	public Boolean isListReady()
	{
		synchronized (this.listReady)
		{
			return this.listReady;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openymsg.network.event.SessionListener#dispatch(org.openymsg.network
	 * .FireEvent)
	 */
	@Override
	public void dispatch(final FireEvent event)
	{
		final SessionEvent sEvent = event.getEvent();
		final ServiceType sType = event.getType();
		if (!(sEvent instanceof SessionFriendEvent) && sType != ServiceType.LIST && sType != ServiceType.Y7_AUTHORIZATION && !(sEvent instanceof SessionNotifyEvent))
		{
			// handling non-list events the "SessionAdapter" way!
			switch (sType)
			{
				case LOGOFF: //	if it is not about us, then it means that a contact has gone offline
					if (sEvent instanceof SessionFriendEvent)
						this.mySessionAdapter.friendSignedOff((SessionFriendEvent) sEvent);
					break;
				case Y6_STATUS_UPDATE: // a contact has changed status message
					this.mySessionAdapter.friendsUpdateReceived((SessionFriendEvent) sEvent);
					break;
				case STATUS_15: //a contact has logged on
					this.mySessionAdapter.friendSignedOn((SessionFriendEvent) sEvent);
					break;
				case MESSAGE:
					this.mySessionAdapter.messageReceived(sEvent);
					break;
				case X_OFFLINE:
					this.mySessionAdapter.offlineMessageReceived(sEvent);
					break;
				case NEWMAIL:
					this.mySessionAdapter.newMailReceived((SessionNewMailEvent) sEvent);
					break;
				case CONTACTNEW:
					contactRequestReceived((SessionAuthorizationEvent) sEvent);
					break;
				case CONFDECLINE:
					this.mySessionAdapter.conferenceInviteDeclinedReceived((SessionConferenceDeclineInviteEvent) sEvent);
					break;
				case CONFINVITE:
					this.mySessionAdapter.conferenceInviteReceived((SessionConferenceInviteEvent) sEvent);
					break;
				case CONFLOGON:
					this.mySessionAdapter.conferenceLogonReceived((SessionConferenceLogonEvent) sEvent);
					break;
				case CONFLOGOFF:
					this.mySessionAdapter.conferenceLogoffReceived((SessionConferenceLogoffEvent) sEvent);
					break;
				case CONFMSG:
					this.mySessionAdapter.conferenceMessageReceived((SessionConferenceMessageEvent) sEvent);
					break;
				case FILETRANSFER:
					this.mySessionAdapter.fileTransferReceived((SessionFileTransferEvent) sEvent);
					break;
				case NOTIFY:
					if (sEvent instanceof SessionNotifyEvent)
						this.mySessionAdapter.notifyReceived((SessionNotifyEvent) sEvent);
					else
						// this is a SessionPictureEvent
						this.mySessionAdapter.pictureReceived((SessionPictureEvent) sEvent);
					break;
				case LIST:
					this.mySessionAdapter.listReceived((SessionListEvent) sEvent);
					break;
				case FRIENDADD:
					SessionFriendEvent friendAddEvent = (SessionFriendEvent) sEvent;
					if (friendAddEvent.isFailure())
						this.mySessionAdapter.friendsUpdateFailureReceived((SessionFriendFailureEvent) sEvent);
					else
						this.mySessionAdapter.friendAddedReceived((SessionFriendEvent) sEvent);
					break;
				case FRIENDREMOVE:
					this.mySessionAdapter.friendRemovedReceived((SessionFriendEvent) sEvent);
					break;
				case GOTGROUPRENAME:
					this.mySessionAdapter.groupRenameReceived((SessionGroupEvent) sEvent);
					break;
				case GROUPRENAME:
					this.mySessionAdapter.groupRenameReceived((SessionGroupEvent) sEvent);
					break;
				case CONTACTREJECT:
					contactRejectionReceived((SessionFriendRejectedEvent) sEvent);
					break;
				case CHATJOIN:
					this.mySessionAdapter.chatJoinReceived((SessionChatEvent) sEvent);
					break;
				case CHATEXIT:
					this.mySessionAdapter.chatExitReceived((SessionChatEvent) sEvent);
					break;
				case CHATDISCONNECT:
					this.mySessionAdapter.chatConnectionClosed(sEvent);
					break;
				case CHATMSG:
					this.mySessionAdapter.chatMessageReceived((SessionChatEvent) sEvent);
					break;
				case X_CHATUPDATE:
					this.mySessionAdapter.chatUserUpdateReceived((SessionChatEvent) sEvent);
					break;
				case X_ERROR:
					this.mySessionAdapter.errorPacketReceived((SessionErrorEvent) sEvent);
					break;
				case X_EXCEPTION:
					this.mySessionAdapter.inputExceptionThrown((SessionExceptionEvent) sEvent);
					break;
				case X_BUZZ:
					this.mySessionAdapter.buzzReceived(sEvent);
					break;
				case LOGON:
					//this.mySessionAdapter.logonReceived(sEvent);
					break;
				case X_CHATCAPTCHA:
					this.mySessionAdapter.chatCaptchaReceived((SessionChatEvent) sEvent);
					break;
				case PICTURE:
					this.mySessionAdapter.pictureReceived((SessionPictureEvent) sEvent);
					break;
				case Y7_AUTHORIZATION:
					if (sEvent instanceof SessionAuthorizationEvent)
						contactRequestReceived((SessionAuthorizationEvent) sEvent);
					else if (sEvent instanceof SessionFriendRejectedEvent)
						contactRejectionReceived((SessionFriendRejectedEvent) sEvent);
					else if (sEvent instanceof SessionFriendAcceptedEvent)
						contactAcceptedReceived((SessionFriendAcceptedEvent) sEvent);
					else
						throw new IllegalArgumentException("Don't know how to handle '" + event.getType() + "' event: " + event);
					break;
				default:
					throw new IllegalArgumentException("Don't know how to handle service type '" + event.getType() + "'");
			}
			return;
		}

		try
		{
			if (sType == ServiceType.LIST)
			{
				final SessionListEvent lEvent = (SessionListEvent) sEvent;
				if (lEvent.getType() == ContactListType.Pending)
				{
					final Set<YahooUser> contacts = lEvent.getContacts();
					for (final YahooUser contact : contacts)
					{
						contact.setPending(true);
						updateUser(contact);
					}

					return;
				}
				else if (lEvent.getType() == ContactListType.StealthBlocked)
				{
					final Set<YahooUser> contacts = lEvent.getContacts();
					for (final YahooUser contact : contacts)
					{
						contact.setStealth(StealthStatus.STEALTH_PERMENANT);
						updateUser(contact);
					}

					return;
				}
				else if (lEvent.getType() != ContactListType.Friends)
					// Ignoring non-Friends list
					return;
				// Session just received the initial user list
				this.setFriendsList(new TreeMap<String, YahooGroup>());
				final Set<YahooUser> contacts = lEvent.getContacts();
				for (final YahooUser contact : contacts)
					addUser(contact);

				// the list can be used now!
				synchronized (this.isListReady())
				{
					this.listReady = true;
				}

				// TODO: hacky! prevent initial 10000 notifications when we have just logged on!
				// make me better!
				new Timer().schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						YahooList.this.initFinished = true;
					}
				}, 3000);

				//YahooList.this.parentSession.addSessionListener(YahooList.this.mySessionAdapter);
				AvatarHelper.requestAvatarIfNeeded(MessengerService.getMyId());

				return;
			}

			//TODO clean me up!
			if (sEvent instanceof SessionNotifyEvent)
			{
				if (event.getType() == ServiceType.NOTIFY)
				{
					notifyReceived((SessionNotifyEvent) event.getEvent());
					this.mySessionAdapter.notifyReceived((SessionNotifyEvent) sEvent);
				}
			}
			else
			{
				YahooUser user = null;
				if (sEvent instanceof SessionFriendEvent)
				{
					final SessionFriendEvent fEvent = (SessionFriendEvent) sEvent;
					user = fEvent.getUser();

					if (fEvent.isFailure())
						return;
				}
				switch (event.getType())
				{
					case FRIENDADD:
						// Adding user to friends list
						addUser(user);
						break;

					//case CONTACTREJECT:
					case FRIENDREMOVE:
						// Removing user from list
						removeUser(user);
						break;

					case Y6_STATUS_UPDATE:
						// Updating user on list
						this.mySessionAdapter.friendsUpdateReceivedV2((SessionFriendEvent) sEvent, updateUser(user));
						break;

					case STATUS_15:
						// Updating user on list
						if (updateUser(user)) // prevent duplicated notifications
							this.mySessionAdapter.friendSignedOn((SessionFriendEvent) sEvent);
						break;

					case LOGOFF:
						// Updating user on list
						updateUser(user);
						if (sEvent instanceof SessionFriendEvent)
							this.mySessionAdapter.friendSignedOff((SessionFriendEvent) sEvent);
						break;

					case Y7_AUTHORIZATION:
						if (event.getEvent() instanceof SessionAuthorizationEvent)
							contactRequestReceived((SessionAuthorizationEvent) event.getEvent());
						else if (event.getEvent() instanceof SessionFriendRejectedEvent)
							contactRejectionReceived((SessionFriendRejectedEvent) event.getEvent());
						else if (event.getEvent() instanceof SessionFriendAcceptedEvent)
							contactAcceptedReceived((SessionFriendAcceptedEvent) event.getEvent());
						// TODO Figure what what needs to be done here!
						//				if (fEvent instanceof SessionFriendAcceptedEvent)
						//				{
						//					log.debug("Adding user to roster, as triggered by " + "SessionFriendAcceptedEvent: " + event);
						//					syncedAdd(user);
						//				}
						//				else if (fEvent instanceof SessionFriendRejectedEvent)
						//				{
						//					log.debug("Removing user from roster as triggered by " + "SessionFriendRejectedEvent: " + event);
						//					syncedRemove(user.getId());
						//				}
						//				else;
						// Ignoring SessionFriendEvent that contains an event that we do not know how to process
						break;
					default:
						break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Fired when a person has sent us a new friend request.
	 * 
	 * @param event
	 *            The event which is dispatched to us by Session
	 */
	private void contactRequestReceived(final SessionAuthorizationEvent event)
	{
		String from = event.getFrom();
		FriendRequest fr = new FriendRequest(from, event.getMessage(), event.getTimestamp());

		this.friendRequests.put(from, fr);
		Log.d("M2X", "Request received!");
		NotificationHelper nHelper = new NotificationHelper(this.context, (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE));
		nHelper.updateNotification("You have new requests!", "M2X Messenger", from + " has sent you a friend request. Click to see more!",
				NotificationHelper.NOTIFICATION_CONTACT_REQUEST, R.drawable.ic_stat_notify_event, new Intent(this.context, FriendRequestsActivity.class), 0,
				Notification.FLAG_AUTO_CANCEL, true);
		MessengerService.getEventLog().log("M2X Messenger", "New Request from " + from, System.currentTimeMillis());

		if (!Preferences.loadAvatars.equals(Preferences.AVATAR_DONT_LOAD))
		{
			Bitmap avatar = AvatarHelper.getYahooAvatar(from);
			if (avatar != null)
			{
				MessengerService.getFriendAvatars().put(from, avatar);
				AvatarHelper.saveAvatarToSD(from, avatar);
			}
		}
	}

	/**
	 * Fired when a friend has rejected our add request.
	 * 
	 * @param event
	 *            The event which is dispatched to us by Session
	 */
	private void contactRejectionReceived(final SessionFriendRejectedEvent event)
	{
		YahooUser user = event.getUser();
		for (String groupName : user.getGroupIds())
			if (this.friendsList.containsKey(groupName))
				this.friendsList.get(groupName).getUsers().remove(user);

		this.mySessionAdapter.notifyListChanged();

		// fire a notification and alert the user
		NotificationHelper nHelper = new NotificationHelper(this.context, (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE));
		nHelper.updateNotification(user.getId() + " has rejected your request!", "M2X Messenger", user.getId() + " has rejected your friend request",
				NotificationHelper.NOTIFICATION_CONTACT_REJECTED, R.drawable.ic_stat_notify_event, new Intent(this.context, ContactsListActivity.class), 0,
				Notification.FLAG_AUTO_CANCEL, true);
		MessengerService.getEventLog().log("M2X Messenger", "Request rejected by " + user.getId(), System.currentTimeMillis());
	}

	/**
	 * Fired when a friend has accepted our add request.
	 * 
	 * @param event
	 *            The event which is dispatched to us by Session
	 */
	private void contactAcceptedReceived(final SessionFriendAcceptedEvent event)
	{
		YahooUser user = event.getUser();
		for (String groupName : user.getGroupIds())
			if (this.friendsList.containsKey(groupName))
			{
				this.friendsList.get(groupName).getUsers().remove(user);
				this.friendsList.get(groupName).getUsers().add(user);
			}
			else
			{
				YahooGroup group = new YahooGroup(groupName);
				group.addUser(user);
				this.friendsList.put(groupName, group);
			}

		this.mySessionAdapter.notifyListChanged();

		// fire a notification and alert the user
		NotificationHelper nHelper = new NotificationHelper(this.context, (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE));
		nHelper.updateNotification(user.getId() + " has accepted your request!", "M2X Messenger", user.getId() + " has accepted your friend request",
				NotificationHelper.NOTIFICATION_CONTACT_ACCEPTED, R.drawable.ic_stat_notify_event, new Intent(this.context, ContactsListActivity.class), 0,
				Notification.FLAG_AUTO_CANCEL, true);
		MessengerService.getEventLog().log("M2X Messenger", "Request accepted by " + user.getId(), System.currentTimeMillis());
	}

	/**
	 * Adds a new user to the friends list. Note that this does not affect this
	 * user on the Yahoo! network.
	 * 
	 * @param user
	 *            The user to be added to the current list.
	 */
	private void addUser(final YahooUser user) throws IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		synchronized (this.getFriendsList())
		{
			for (String group : user.getGroupIds())
			{
				YahooGroup g = null;
				if (!this.getFriendsList().containsKey(group))
				{
					g = new YahooGroup(group);
					this.getFriendsList().put(group, g);
				}
				else
					g = this.getFriendsList().get(group);

				g.addUser(user);
			}
		}
	}

	/**
	 * Updates an existing user in the friends list. Note that this does not
	 * affect this user on the Yahoo! network.
	 * 
	 * @param user
	 *            The user to be updated in the current list.
	 * @throws IllegalAccessException
	 */
	private boolean updateUser(final YahooUser user) throws IllegalAccessException
	{
		boolean presenceChanged = true; // a flag to prevent duplicated status changes (A user is already online yet we get a notification that he/she is now online!

		//TODO do something about custom status messages

		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		synchronized (this.getFriendsList())
		{
			for (YahooGroup g : this.getFriendsList().values())
				for (YahooUser x : g.getUsers())
					if (x.getId().equals(user.getId()))
					{
						if (x.getStatus() != Status.OFFLINE && user.getStatus() != Status.OFFLINE)
							presenceChanged = false;
						x.update(user.getStatus(), user.isOnChat(), user.isOnPager());
						x.setCustom(user.getCustomStatusMessage(), user.isCustomStatusBusy());
						break;
					}
		}
		
		resortList(false);
		
		return presenceChanged;
	}
	
	/**
	 * Resorts the list based on the users preference (IE. online users should be fist or not)
	 * @param preferenceFlipped
	 * 		A flag indicating that the preference screen has changed so the list has to be updated to reflect changes
	 */
	public void resortList(final boolean preferenceFlipped)
	{
		if (preferenceFlipped || Preferences.onlinesFirst)	// recreate the set to show the online contacts first
			for (YahooGroup g : this.friendsList.values())
			{
				Set<YahooUser> users = g.getUsers();
				TreeSet<YahooUser> newSet = new TreeSet<YahooUser>(YahooUser.getComparator());
				newSet.addAll(users);
				g.setUsers(newSet);
			}
	}

	private void removeUser(final YahooUser user)
	{
		//TODO ???!
	}

	public void notifyReceived(final SessionNotifyEvent event)
	{
		if (this.friendsList.size() == 0)
			return;

		String userId = event.getFrom();
		if (event.isTyping())
			for (YahooGroup group : this.friendsList.values())
				for (YahooUser user : group.getUsers())
					if (user.getId().equals(userId))
					{
						user.setIsTyping(event.isOn());
						return;
					}
	}

	/**
	 * Changes out stealth status for this user.
	 * 
	 * @param user
	 *            The user who we would like to change out presence state for
	 *            (!)
	 * @param newStealth
	 *            The new stealth setting
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	public void changeStealth(final YahooUser user, final StealthStatus newStealth) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		this.parentSession.changeStealth(user.getId(), newStealth);
		user.setStealth(newStealth);
	}

	/**
	 * Removes the specified friend from the specified group.
	 * 
	 * @param user
	 *            Firend's ID
	 * @param groupId
	 *            The name of the group to remove the friend from.
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	public void removeFriendFromGroup(final YahooUser user, final String groupId) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		synchronized (this.friendsList)
		{
			this.parentSession.removeFriendFromGroup(user.getId(), groupId);
			user.removeGroupId(groupId);
			if (user.getGroupIds().size() == 0) // effectively remove this friend from our friends list
				this.friendsList.get(groupId).getUsers().remove(user);
		}

	}

	/**
	 * Moves the specified friend to a new group.
	 * 
	 * @param user
	 *            The ID of the user who we would like to move.
	 * @param fromGroup
	 *            The initial group of this user.
	 * @param toGroup
	 *            The destination group.
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	public void moveFriend(final YahooUser user, final String fromGroup, final String toGroup) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		synchronized (this.friendsList)
		{
			this.parentSession.moveFriend(user.getId(), fromGroup, toGroup);

			this.friendsList.get(fromGroup).getUsers().remove(user);// The user must now be indexed under a different group
			user.changeGroup(fromGroup, toGroup);
			if (this.friendsList.containsKey(toGroup))
				this.friendsList.get(toGroup).getUsers().add(user);
			else
			{
				YahooGroup group = new YahooGroup(toGroup);
				group.addUser(user);
				this.friendsList.put(toGroup, group);
			}
		}
	}

	/**
	 * Renames a group.
	 * 
	 * @param group
	 *            The group which we would like to change its name
	 * @param newName
	 *            The new name for this group
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	public void renameGroup(final YahooGroup group, final String newName) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		synchronized (this.friendsList)
		{
			if (group.getName().equals(newName))
				return;

			this.parentSession.renameGroup(group.getName(), newName);

			for (YahooUser user : group.getUsers())
			{
				user.removeGroupId(group.getName());
				user.addGroupId(newName);
			}
			// Change the key as well
			this.friendsList.remove(group.getName());
			group.setName(newName);
			this.friendsList.put(newName, group);
		}
	}

	public void addFriend(final String userId, final String groupId) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		YahooUser newUser = null;

		for (YahooGroup group : this.friendsList.values())
			for (YahooUser user : group.getUsers())
				if (user.getId().equals(userId))
				{
					if (user.isPending())
						return;

					newUser = user;
					break;
				}

		this.parentSession.sendNewFriendRequest(userId, groupId, YahooProtocol.YAHOO);

		if (newUser == null)
		{
			newUser = new YahooUser(userId, groupId, YahooProtocol.YAHOO);
			newUser.setPending(true);
			if (!this.friendsList.containsKey(groupId))
				this.friendsList.put(groupId, new YahooGroup(groupId));

			this.friendsList.get(groupId).addUser(newUser);
		}
	}

	public void deleteGroup(final String groupId) throws IOException, IllegalAccessException
	{
		if (this.parentSession.getSessionStatus() != SessionState.LOGGED_ON)
			throw new IllegalAccessException("SessionStatus must be LOGGED_ON");

		// we just have to remove all the users from this group and 
		// remove the group from our local list 
		// as yahoo does not provide a mechanism for deleting a group

		YahooGroup group = this.friendsList.get(groupId);
		for (YahooUser user : group.getUsers())
			user.removeGroupId(groupId);

		this.friendsList.remove(groupId);
	}
}
