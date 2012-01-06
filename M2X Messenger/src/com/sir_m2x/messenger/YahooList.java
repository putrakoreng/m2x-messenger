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
package com.sir_m2x.messenger;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;

import org.openymsg.network.ContactListType;
import org.openymsg.network.FireEvent;
import org.openymsg.network.ServiceType;
import org.openymsg.network.Session;
import org.openymsg.network.StealthStatus;
import org.openymsg.network.YahooGroup;
import org.openymsg.network.YahooProtocol;
import org.openymsg.network.YahooUser;
import org.openymsg.network.event.SessionEvent;
import org.openymsg.network.event.SessionFriendEvent;
import org.openymsg.network.event.SessionListEvent;
import org.openymsg.network.event.SessionListener;
import org.openymsg.network.event.SessionNotifyEvent;

import android.content.Context;
import android.util.Log;

import com.sir_m2x.messenger.services.MessengerService;

/**
 * This class hold the Yahoo! list and is an alternative to OpenYMSG's Roster
 * class. Roster is cumbersome to use and still lacks some important features
 * like storing pendings list. So it makes sense to implement another class
 * which is manageable and seamless.
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 * 
 */
public class YahooList implements SessionListener
{
	// The infamous friends list
	private TreeMap<String, YahooGroup> friendsList;
	// The session which is the owner of this instance
	private Session parentSession = null;
	// Android's context
	private Context context = null;
	/**
	 * A flag for the users of this class. If this flag is false, then it means
	 * that the list is not ready yet and accessing it may result in unknown
	 * behavior.
	 */
	private Boolean listReady = false;

	public YahooList(final Session parentSession, final Context context)
	{
		this.parentSession = parentSession;
		this.context = context;
		Log.d("M2X", "List not ready!");
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
		if (!(sEvent instanceof SessionFriendEvent) && sType != ServiceType.LIST && !(sEvent instanceof SessionNotifyEvent))
			// Ignoring non-list
			return;

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
			}
			else if (lEvent.getType() == ContactListType.StealthBlocked)
			{
				final Set<YahooUser> contacts = lEvent.getContacts();
				for (final YahooUser contact : contacts)
				{
					contact.setStealth(StealthStatus.STEALTH_PERMENANT);
					updateUser(contact);
				}
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

			this.parentSession.addSessionListener(new MySessionAdapter(this.context));

			return;
		}

		//TODO clean me up!
		if (sEvent instanceof SessionNotifyEvent)
		{
			if (event.getType() == ServiceType.NOTIFY)
					notifyReceived((SessionNotifyEvent) event.getEvent());				
		}
		else
		{
			final SessionFriendEvent fEvent = (SessionFriendEvent) sEvent;
			final YahooUser user = fEvent.getUser();

			if (fEvent.isFailure())
				return;
			switch (event.getType())
			{
				case FRIENDADD:
					// Adding user to friends list
					addUser(user);
					break;

				case CONTACTREJECT:
				case FRIENDREMOVE:
					// Removing user from list
					removeUser(user);
					break;

				case Y6_STATUS_UPDATE:
					// Updating user on list
					updateUser(user);
					break;

				case STATUS_15:
					// Updating user on list
					updateUser(user);
					break;

				case LOGOFF:
					// Updating user on list
					updateUser(user);
					break;

				case Y7_AUTHORIZATION:
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

	/**
	 * Adds a new user to the friends list. Note that this does not affect this
	 * user on the Yahoo! network.
	 * 
	 * @param user
	 *            The user to be added to the current list.
	 */
	private void addUser(final YahooUser user)
	{
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
	 */
	private void updateUser(final YahooUser user)
	{
		synchronized (this.getFriendsList())
		{
			for (YahooGroup g : this.getFriendsList().values())
				for (YahooUser x : g.getUsers())
					if (x.getId().equals(user.getId()))
					{
						x.update(user.getStatus(), user.isOnChat(), user.isOnPager());
						x.setCustom(user.getCustomStatusMessage(), user.isCustomStatusBusy());
						break;
					}
		}
	}

	private void removeUser(final YahooUser user)
	{

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
	 */
	public void changeStealth(final YahooUser user, final StealthStatus newStealth) throws IOException
	{
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
	 */
	public void removeFriendFromGroup(final YahooUser user, final String groupId) throws IOException
	{
		synchronized (this.friendsList)
		{
			//TODO! TEMP FOR TEST ONLY
			//MessengerService.getSession().removeFriendFromGroup(user.getId(), groupId);
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
	 */
	public void moveFriend(final YahooUser user, final String fromGroup, final String toGroup) throws IOException
	{
		synchronized (this.friendsList)
		{
			//TODO! TEMP FOR TEST ONLY
			//MessengerService.getSession().moveFriend(user.getId(), fromGroup, toGroup);

			this.friendsList.get(fromGroup).getUsers().remove(user);// The user must now be indexed under a different group
			user.changeGroup(fromGroup, toGroup);
			this.friendsList.get(toGroup).getUsers().add(user);
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
	 */
	public void renameGroup(final YahooGroup group, final String newName) throws IllegalStateException, IOException
	{
		synchronized (this.friendsList)
		{
			if (group.getName().equals(newName))
				return;

			MessengerService.getSession().renameGroup(group.getName(), newName);

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

	public void addFriend(final String userId, final String groupId) throws IOException
	{
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
			this.friendsList.get("groupId").addUser(newUser);
		}
	}

	public void deleteGroup(final String groupId) throws IOException
	{
		// we just have to remove all the users from this group and 
		// remove the group from our local list 
		// as yahoo does not provide a mechanism for deleting a group

		YahooGroup group = this.friendsList.get(groupId);
		for (YahooUser user : group.getUsers())
			//removeFriendFromGroup(user, groupId);
			user.removeGroupId(groupId);

		this.friendsList.remove(groupId);
	}

	//TODO contact request received
	//and the rest of the FRIEND management stuff
}
