/*
 * OpenYMSG, an implementation of the Yahoo Instant Messaging and Chat protocol.
 * Copyright (C) 2007 G. der Kinderen, Nimbuzz.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA. 
 */
package org.openymsg.network;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.openymsg.addressBook.YahooAddressBookEntry;

/**
 * This class represents a single canonical representation of a user on Yahoo.
 * <p>
 * The UserId attribute is the primary attribute to distinguish between users,
 * and is the only field used in the equals() and hashCode() methods.
 * <p>
 * The groupCount integer is used to service the isFriend() method. When added
 * to a group it is incremented, when removed it is decremented. When zero, it
 * means this user is not part of the client's friends list.
 * <p>
 * Note: this API cannot guarantee the accuracy of details held on users who's
 * contact with you has expired. So... if you leave a chatroom in which 'fred'
 * was a member, 'fred's object will continue to be in the hashtable - BUT his
 * status will almost certainly not be updated any longer (unless, of course,
 * you have a link with him via some other route - like if he is on your friends
 * list too!)
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class YahooUser
{
	/**
	 * A comparator to compare YahooUsers against one another.
	 * @return
	 * 		The implemented comparator
	 */
	public static Comparator<YahooUser> getComparator()
	{
		return new Comparator<YahooUser>()
		{
			@Override
			public int compare(final YahooUser lhs,
					final YahooUser rhs)
			{						
				return lhs.getId().compareTo(rhs.getId());
			}
		};
	}
	public final String getGroup()
	{
		return this.groupIds.toArray()[0].toString();
	}
	
	public void changeGroup(final String from, final String to)
	{
		this.groupIds.remove(from);
		this.groupIds.add(to);
	}
	
	/**
	 * A flag to indicate that this user is pending and not currently on the friends list.
	 */
	private boolean isPending = false;
	
	/**
	 * A flag to indicate that this user has sent us a friend request.
	 */
	private boolean hasSentFriendRequest = false;
	
	/**
	 * A flag to indicate that this user is typing a message
	 */
	protected boolean isTyping = false;

	/**
	 * The ID by which this user is identified.
	 */
	protected final String userId;

	/**
	 * A set of IDs of the groups to which this user belongs.
	 */
	private final Set<String> groupIds;

	/**
	 * The presence status of this user (away, available, etc).
	 */
	protected Status status = Status.OFFLINE;

	protected StealthStatus stealth = StealthStatus.NO_STEALTH; // Stealth status

	protected boolean onChat = false;
	protected boolean onPager = false;

	/**
	 * Indicates that the user is on the ignore list (in other words: the local
	 * user does not want to receive messages from this user).
	 */
	protected boolean ignored = false;

	/**
	 * Flag that determines if this user is on our StealthBlocked list (in other
	 * words, if this user is allowed to see our details).
	 */
	protected boolean stealthBlocked = false;

	/**
	 * An free form status message.
	 */
	protected String customStatusMessage;

	/**
	 * A custom status. As yet I'm unsure if these are String identifiers, or
	 * numeric or even boolean values.
	 */
	// TODO: Find out what values this can have (boolean, numeric, string?)
	protected String customStatus;

	/**
	 * The amount of seconds that the user has been idle (or -1 if the idle time
	 * is unknown).
	 */
	private long idleTime = -1;

	/**
	 * Information from the address book. first name, last name, nick name
	 */
	private YahooAddressBookEntry addressBookEntry = YahooAddressBookEntry.EMPTY;

	/**
	 * Protocol of the user. Yahoo, Federated, OCS, msn.live, etc
	 */
	private YahooProtocol protocol;

	public YahooProtocol getProtocol()
	{
		return this.protocol;
	}

	/**
	 * Creates a new user in one particular group. Note that the fact that the
	 * user is in a group indicates that this user is on our roster.
	 * 
	 * @param userId
	 *            The ID of the user
	 * @param groupId
	 *            The ID of the group in which this user has been put.
	 */
	public YahooUser(final String userId, final String groupId,
			final YahooProtocol protocol)
	{
		this.userId = userId.toLowerCase();
		this.groupIds = new HashSet<String>();

		if (groupId != null && groupId.length() != 0)
			this.groupIds.add(groupId);

		this.protocol = protocol;
	}

	public YahooUser(final String userId, final String groupId,
			final YahooProtocol protocol, final YahooAddressBookEntry addressBookEntry)
	{
		this(userId, groupId, protocol);
		this.addressBookEntry = addressBookEntry;
	}

	public YahooUser(final String userId, final Set<String> groupIds,
			final YahooProtocol protocol)
	{
		this.userId = userId.toLowerCase();
		this.groupIds = groupIds;

		this.protocol = protocol;
	}

	public YahooUser(final String userId, final Set<String> groupIds,
			final YahooProtocol protocol, final YahooAddressBookEntry addressBookEntry)
	{
		this(userId, groupIds, protocol);
		this.addressBookEntry = addressBookEntry;
	}

	/**
	 * Creates a new anonymous user. This user is said to be anonymous, because
	 * it is not on our roster. Note that users that are on our roster MUST be
	 * in at least one group. Anonymous users most likely represent users from
	 * conferences.
	 * 
	 * @param userId
	 *            The ID of the user.
	 */
	public YahooUser(final String userId)
	{
		this(userId, (String) null, YahooProtocol.YAHOO);
	}

	public YahooUser(final String userId, final YahooAddressBookEntry addressBookEntry)
	{
		this(userId);
		this.addressBookEntry = addressBookEntry;
	}

	/**
	 * Returns the user ID of this instance.
	 * 
	 * @return the user ID of this instance.
	 */
	public String getId()
	{
		return this.userId;
	}

	/**
	 * Sets the 'ignored' flag for this user, which indicates if the user is on
	 * the ignore list (in other words: the local user does not want to receive
	 * messages from this user).
	 * <p>
	 * Note that setting this flag does not update the status of this User on
	 * the network. This method is used by internal library code, and should
	 * probably not be used by users of the OpenYMSG library directly.
	 * 
	 * @param ignored
	 *            <tt>true</tt> if this user is on the ignore list.
	 */
	void setIgnored(final boolean ignored)
	{
		this.ignored = ignored;
	}

	/**
	 * Checks if this user is on our ignore list, which indicates if the user is
	 * on the ignore list (in other words: the local user does not want to
	 * receive messages from this user).
	 * 
	 * @return <tt>true</tt> if this user is on the ignore list.
	 */
	public boolean isIgnored()
	{
		return this.ignored;
	}

	/**
	 * Sets the 'StealthBlocked' flag for this user, which indicates whether or
	 * not the user is allowed to see us.
	 * <p>
	 * Note that setting this flag does not update the status of this User on
	 * the network. This method is used by internal library code, and should
	 * probably not be used by users of the OpenYMSG library directly.
	 * 
	 * @param blocked
	 *            <tt>true</tt> if this user is on our StealthBlocked list.
	 */
	void setStealthBlocked(final boolean blocked)
	{
		this.stealthBlocked = blocked;
	}

	/**
	 * Checks if this user is on our stealth-blocked list, disallowing it to see
	 * us.
	 * 
	 * @return <tt>true</tt> if this user is on our StealthBlocked list.
	 */
	public boolean isStealthBlocked()
	{
		return this.stealthBlocked;
	}

	/**
	 * Sets a custom presence status and status message.
	 * <p>
	 * Note that setting these attributes does not update the custom status
	 * attributes of this User on the network. This method is used by internal
	 * library code, and should probably not be used by users of the OpenYMSG
	 * library directly.
	 * 
	 * @param message
	 *            A free form text, describing the new status.
	 * @param status
	 *            A custom status.
	 */
	public void setCustom(final String message, final String status)
	{
		this.customStatusMessage = message;
		this.customStatus = status;
	}

	/**
	 * Returns the custom status message, or <tt>null</tt> if no such message
	 * has been set.
	 * 
	 * @return The custom status message or <tt>null</tt>.
	 */
	public String getCustomStatusMessage()
	{
		return this.customStatusMessage;
	}

	/**
	 * Returns the custom status, or <tt>null</tt> if no such status has been
	 * set.
	 * 
	 * @return The custom status or <tt>null</tt>.
	 */
	public String getCustomStatus()
	{
		return this.customStatus;
	}

	/**
	 * Sets the amount of seconds that this user has been idle.
	 * <p>
	 * Note that this method is used by internal library code, and should
	 * probably not be used by users of the OpenYMSG library directly. *
	 * 
	 * @param seconds
	 *            The idle time of this user
	 */
	void setIdleTime(final long seconds)
	{
		this.idleTime = seconds;
	}

	/**
	 * Returns the amount of seconds that this user has been idle, or -1 if this
	 * is unknown.
	 * 
	 * @return the amount of seconds that this user has been idle, or -1.
	 */
	public long getIdleTime()
	{
		return this.idleTime;
	}

	/**
	 * Sets the stealth mode for this user.
	 * <p>
	 * Note that setting this flag does not update the status of this User on
	 * the network. This method is used by internal library code, and should
	 * probably not be used by users of the OpenYMSG library directly.
	 * 
	 * @param stealth
	 *            The stealth mode.
	 */
	// TODO: figure out what values are valid here.
	public void setStealth(final StealthStatus stealth)
	{
		this.stealth = stealth;
	}

	/**
	 * Gets the stealth modus of this user.
	 * 
	 * @return the stealth modus of this user.
	 */
	public StealthStatus getStealth()
	{
		return this.stealth;
	}

	/**
	 * Determines if this user is currently pending
	 * 
	 * @return the pending status of this user
	 */
	public boolean isPending()
	{
		return this.isPending;
	}
	
	/**
	 * Changes the pending state of this user
	 */
	public void setPending(final boolean isPending)
	{
		this.isPending = isPending;
	}
	
	
	/**
	 * Determine whether this user has sent us a friend request. 		
	 */
	public boolean hasSentFriendRequest()
	{
		return this.hasSentFriendRequest;
	}
	/**
	 * Change the friend request status of this user
	 * @param hasSentFriendRequest
	 * 		The new request status for this user.
	 */
	public void setHasSentFriendRequest(final boolean hasSentFriendRequest)
	{
		this.hasSentFriendRequest = hasSentFriendRequest;
	}
	/**
	 * Adds this user to an (additional) user.
	 * <p>
	 * Note that using this method does not update the status of this User on
	 * the network. This method is used by internal library code, and should
	 * probably not be used by users of the OpenYMSG library directly.
	 * 
	 * @param groupId
	 *            The ID of the group that this user is in.
	 */
	public void addGroupId(final String groupId)
	{
		this.groupIds.add(groupId);
	}
	
	public boolean removeGroupId(final String groupId)
	{
		if (this.groupIds.contains(groupId))
			this.groupIds.remove(groupId);
		
		return false;		
	}

	/**
	 * Returns an unmodifiable set of IDs of the groups that this user is in, or
	 * an empty set if this user is an anonymous user.
	 * 
	 * @return the IDs of the groups that this user is in, or an empty set
	 *         (never <tt>null</tt>)
	 */
	public Set<String> getGroupIds()
	{
		return this.groupIds;
	}

	/**
	 * Gets the presence status of this user.
	 * 
	 * @return The presence status of this user.
	 */
	public Status getStatus()
	{
		return this.status;
	}

	/**
	 * Gets the previous status of this user.
	 * 
	 * @return The presence status of this user.
	 */

	public boolean isOnChat()
	{
		return this.onChat;
	}

	public boolean isOnPager()
	{
		return this.onPager;
	}

//	public boolean isLoggedIn()
//	{
//		return (onChat || onPager);
//	}

	public boolean isTyping()
	{
		return this.isTyping;
	}

	public void setIsTyping(final boolean isTyping)
	{
		this.isTyping = isTyping;
	}

	/**
	 * Checks if this user is on our contact list (and this can be considered 'a
	 * friend'), or if this user is an anonymous user. This method is the exact
	 * opposite of {@link #isAnonymous()}.
	 * 
	 * @return <tt>true</tt> if this user is on our contact list, <tt>false</tt>
	 *         if this user is anonymous.
	 */
	public boolean isFriend()
	{
		return !this.groupIds.isEmpty();
	}

	/**
	 * Checks if this user is an anonymous user, or if he's a friend (if the
	 * user exists on our contact list the user is considered to be a friend).
	 * This method is the exact opposite of {@link #isFriend()}.
	 * 
	 * @return <tt>false</tt> if this user is on our contact list, <tt>true</tt>
	 *         if this user is anonymous.
	 */
	public boolean isAnonymous()
	{
		return !isFriend();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.userId;
		// return "YahooUser [ID=" + userId + ", status=" + status.name() +
		// ", ignored=" + ignored + ", stealthBlock="
		// + stealthBlocked + ", customStatusMessage=" + customStatusMessage +
		// ", isFriend=" + isFriend()
		// + ", protocol=" + protocol + ", ab=" + addressBookEntry + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return (this.userId == null) ? 1 : this.userId.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof YahooUser))
			return false;
		final YahooUser other = (YahooUser) obj;
		if (this.userId == null)
		{
			if (other.userId != null)
				return false;
		}
		else if (!this.userId.equals(other.userId))
			return false;
		return true;
	}

	/**
	 * Updates the YahooUser with the new values.
	 * 
	 * @param newStatus
	 *            replacement for current Status
	 * @param newVisibility
	 *            replacement for current onChat and onPager values
	 */
	public void update(final Status newStatus, final String newVisibility)
	{
		// This is the new version, where 13=combined pager/chat
		final int iVisibility = (newVisibility == null) ? 0 : Integer
				.parseInt(newVisibility);
		update(newStatus, (iVisibility & 2) > 0, (iVisibility & 1) > 0);
	}

	/**
	 * Updates the YahooUser with the new values.
	 * 
	 * @param newStatus
	 *            replacement for current Status value
	 * @param newOnChat
	 *            replacement for current onChat value
	 * @param newOnPager
	 *            replacement for current onPager value
	 */
	public void update(final Status newStatus, final boolean newOnChat, final boolean newOnPager)
	{
//		if ((this.onChat && newOnChat) || (this.onPager && newOnPager))
//			onlineStatusChanged = false;
//		else if ((!this.onChat && !newOnChat) || (!this.onPager && !newOnPager))
//			onlineStatusChanged = false;
//		else if ((!this.onChat && newOnChat) || (!this.onPager && newOnPager))
//			onlineStatusChanged = true;
//		else if ((this.onChat && !newOnChat) || (this.onPager && !newOnPager))
//			onlineStatusChanged = true;
		
		
		// This is the old version, when 13=pager and 17=chat
		update(newStatus);

		this.onChat = newOnChat;
		this.onPager = newOnPager;
	}

	/**
	 * Updates the YahooUser with the new values. This method should be called
	 * in cases when no chat or pager information was provided (presumed that
	 * these values don't change in this case)
	 * 
	 * @param newStatus
	 *            replacement for current Status value
	 */
	public void update(final Status newStatus)
	{
			
		// This is the old version, when 13=pager and 17=chat
		this.status = newStatus;

		if (this.status != Status.CUSTOM)
		{
			this.customStatusMessage = null;
			this.customStatus = null;
		}
	}

	public String getFirstName()
	{
		return this.addressBookEntry.getFirstName();
	}

	public String getLastName()
	{
		return this.addressBookEntry.getLastName();
	}

	public String getNickName()
	{
		return this.addressBookEntry.getNickName();
	}

	public YahooAddressBookEntry getAddressBookEntry()
	{
		return this.addressBookEntry;
	}
	
//	public void setAddressBookEntry(YahooAddressBookEntry addressBookEntry)
//	{
//		this.addressBookEntry = addressBookEntry;
//	}

	public void update(final YahooProtocol protocol)
	{
		this.protocol = protocol;
	}

}
