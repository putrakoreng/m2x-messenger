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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import org.openymsg.addressBook.BuddyListImport;
import org.openymsg.network.challenge.ChallengeResponseV10;
import org.openymsg.network.challenge.ChallengeResponseV16;
import org.openymsg.network.challenge.ChallengeResponseV9;
import org.openymsg.network.chatroom.ChatroomManager;
import org.openymsg.network.chatroom.YahooChatLobby;
import org.openymsg.network.chatroom.YahooChatUser;
import org.openymsg.network.event.SessionAuthorizationEvent;
import org.openymsg.network.event.SessionChatEvent;
import org.openymsg.network.event.SessionConferenceDeclineInviteEvent;
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
import org.openymsg.network.event.SessionLogoutEvent;
import org.openymsg.network.event.SessionNewMailEvent;
import org.openymsg.network.event.SessionNotifyEvent;
import org.openymsg.network.event.SessionPictureEvent;
import org.openymsg.network.event.SessionPictureHandler;
import org.openymsg.roster.Roster;


/**
 * Written by FISH, Feb 2003 , Copyright FISH 2003 - 2007
 * 
 * This class represents the main entry point into the YMSG9 API. A Session represents one IM connection.
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public class Session implements StatusConstants, FriendManager {
	
	/**
	 * The number of time that we have faced a timeout when transmitting keep alive
	 * Used to log out the current session
	 */
	private int timeoutCount = 0;
	/**
	 * A timer used for resending messages in case the other party
	 * did not acknowledge the last sent message.
	 */
	private Timer messageResendTimer = new Timer();

	/**
	 * A queue to store sent messages.
	 * Note that a message is stored in this queue when it is sent over the network
	 * and is removed from it, when the recipient has acknowledged the message.
	 */
	private ConcurrentLinkedQueue<IM> imQueue = new ConcurrentLinkedQueue<IM>();

	private static final String LOGIN_YAHOO_COM = "login.yahoo.com";

	/** Primary Yahoo ID: the real account id. */
	private YahooIdentity primaryID;

	/** Login Yahoo ID: we logged in under this. */
	private YahooIdentity loginID;

	/** Map of alternative identities that can be used by this user. */
	private Map<String, YahooIdentity> identities = new HashMap<String, YahooIdentity>();

	/** Yahoo user password. */
	private String password;

	private String cookieY, cookieT, cookieC;

	/** IMvironment (decor, etc.) */
	private String imvironment;

	/** Yahoo status (presence) */
	protected Status status = Status.AVAILABLE; // set the initial presence to available

	/** Message for custom status. */
	private String customStatusMessage;

	/** Available/Back=f, away=t */
	private boolean customStatusBusy;

	private Roster roster = new Roster(this);

	/** Status of session (see StatusConstants) */
	private volatile SessionState sessionStatus;

	/** Holds Yahoo's session id */
	volatile long sessionId = 0;

	public volatile ConnectionHandler network;

	private Timer scheduledPingerService;

	private TimerTask pingerTask;

	protected InputThread ipThread;

	protected EventDispatcher eventDispatchQueue;

	private YahooException loginException = null;

	// private boolean receivedListFired = false;

	/** For split packets in multiple parts */
	private YMSG9Packet cachePacket;

	private ChatroomManager chatroomManager;

	/** Current conferences, hashed on room */
	private Hashtable<String, YahooConference> conferences = new Hashtable<String, YahooConference>();

	private SessionState chatSessionStatus;

	private volatile YahooChatLobby currentLobby = null;

	private YahooIdentity chatID;

	private Set<SessionListener> sessionListeners = new HashSet<SessionListener>();

	private SessionPictureHandler pictureHandler = null;

	/** Message number to be included in sending a message */
	private int messageNumber;

	private long pingTimestamp;

	private ArrayList<YMSG9Packet> queueOfList15 = new ArrayList<YMSG9Packet>();

	private String yahooLoginHost = LOGIN_YAHOO_COM;


	// used to simulate failures
	//     static private int triesBeforeFailure = 0;

	private static final Log log = LogFactory.getLog(Session.class);

	private static final int LOGIN_HTTP_TIMEOUT = 10000;

	public YahooGroup[] groups; // Yahoo user's groups

	/**
	 * Creates a new Session based on a ConnectionHandler as configured in the current System properties.
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 * @throws MalformedURLException
	 * @throws NumberFormatException
	 */
	public Session() throws NumberFormatException {
		this(null);
	}

	/**
	 * Creates a new Session based on the provided ConnectionHandler. If the attribute is 'null', a ConnectionHandler as
	 * configured in the current System properties will be used.
	 * 
	 * @param connectionHandler
	 *            The ConnectionHandler that backs the connection to the Yahoo Network.
	 * @throws IOException
	 * @throws JDOMException
	 * @throws MalformedURLException
	 * @throws NumberFormatException
	 */
	public Session(final ConnectionHandler connectionHandler) throws NumberFormatException {
		this(connectionHandler, LOGIN_YAHOO_COM);
	}

	public Session(final ConnectionHandler connectionHandler, final String yahooLoginHost) throws NumberFormatException {
		this.yahooLoginHost = yahooLoginHost;
		if (connectionHandler != null)
			this.network = connectionHandler;
		else {
			Properties p = System.getProperties();
			if (p.containsKey(NetworkConstants.SOCKS_HOST))
				this.network = new SOCKSConnectionHandler();
			else if (p.containsKey(NetworkConstants.PROXY_HOST) || p.containsKey(NetworkConstants.PROXY_HOST_OLD))
				this.network = new HTTPConnectionHandler();
			else
				this.network = new DirectConnectionHandler();
		}

		this.status = Status.WEBLOGIN;
		this.sessionId = 0;
		this.sessionStatus = SessionState.UNSTARTED;

		this.network.install(this);

	}

	/**
	 * Adds a session listener to the collection of listeners to which events are dispatched.
	 * 
	 * @param sessionListener
	 *            SessionListener to be added.
	 */
	public void addSessionListener(final SessionListener sessionListener) {
		if (sessionListener == null)
			throw new IllegalArgumentException("Argument 'sessionListener' cannot be null.");
		this.sessionListeners.add(sessionListener);
	}

	public Set<SessionListener> getSessionListeners() {
		return this.sessionListeners;
	}

	/**
	 * Removes the listener from the collection of listeners to which events are dispatched.
	 * 
	 * @param sessionListener
	 *            The SessionListener to be removed
	 */
	public void removeSessionListener(final SessionListener sessionListener) {
		if (sessionListener == null)
			throw new IllegalArgumentException("Argument 'sessionListener' cannot be null.");
		if (!this.sessionListeners.remove(sessionListener))
			log.warn("SessionListener not found to be removed");
	}
	
	/**
	 * Removes all listeners from the collection of listeners to which events are dispatched.
	 */
	public void removeAllSessionListeners()
	{
		this.sessionListeners.clear();
	}

	/**
	 * Returns the handler used to send/receive messages from the network
	 */
	public ConnectionHandler getConnectionHandler() {
		return this.network;
	}
	
	/**
	 * Call this to connect to the Yahoo server and do all the initial handshaking and accepting of data Session will do
	 * it's own thread for pings and keepAlives
	 * 
	 * @param username
	 *            Yahoo id
	 * @param password
	 *            password
	 */
	public void login(final String username, final String password) throws IllegalStateException, IOException,
	AccountLockedException, LoginRefusedException, FailedLoginException {
		this.login(username, password, true, false);
	}

	/**
	 * Call this to connect to the Yahoo server and do all the initial handshaking and accepting of data
	 * 
	 * @param username
	 *            Yahoo id
	 * @param password
	 *            password
	 * @param createPingerTask
	 *            Session will do it's own thread for pings and keepAlives
	 */
	public void login(String username, final String password, final boolean createPingerTask, final boolean searchForAddress)
			throws IllegalStateException,
			IOException, AccountLockedException, LoginRefusedException, FailedLoginException {
		this.identities = new HashMap<String, YahooIdentity>();
		this.conferences = new Hashtable<String, YahooConference>();
		this.chatroomManager = new ChatroomManager(null, null);
		if (this.eventDispatchQueue == null) {
			this.eventDispatchQueue = new EventDispatcher(username, this);
			this.eventDispatchQueue.start();
		}
		if (username == null || username.length() == 0) {
			this.sessionStatus = SessionState.FAILED;
			throw new IllegalArgumentException("Argument 'username' cannot be null or an empty String.");
		}

		if (password == null || password.length() == 0) {
			this.sessionStatus = SessionState.FAILED;
			throw new IllegalArgumentException("Argument 'password' cannot be null or an empty String.");
		}

		// Check the session status first
		synchronized (this) {
			if (this.sessionStatus != SessionState.UNSTARTED)
				throw new IllegalStateException("Session should be unstarted");
			this.sessionStatus = SessionState.INITIALIZING;
		}

		// Yahoo ID's are apparently always lower case
		username = username.toLowerCase();

		// Reset session and init some variables
		resetData();

		this.roster = new Roster(this);
		this.addSessionListener(this.roster);
		this.loginID = new YahooIdentity(username);
		this.primaryID = null;
		this.password = password;
		this.sessionId = 0;
		this.imvironment = "0";
		try {
			// Create the socket and threads (ipThread, sessionPingRunnable and
			// maybe eventDispatchQueue)
			log.trace("Opening session...");
			openSession(createPingerTask, searchForAddress);

			// Begin login process
			log.trace("Transmitting auth...");
			this.sessionStatus = SessionState.CONNECTING;
			transmitAuth();

			// Wait until connection or timeout
			long timeout = System.currentTimeMillis() + Util.loginTimeout(NetworkConstants.LOGIN_TIMEOUT);
			while (this.sessionStatus != SessionState.LOGGED_ON && this.sessionStatus != SessionState.FAILED && !past(timeout)
					&& this.loginException == null)
				try {
					Thread.sleep(10);
				}
			catch (InterruptedException e) {
				// ignore
			}
			log.trace("finished waiting for connection: " + this.sessionStatus + "/" + past(timeout) + "/" + this.loginException);

			if (past(timeout)) {
				this.sessionStatus = SessionState.FAILED;
				throw new InterruptedIOException("Login timed out");
			}

			if (this.sessionStatus == SessionState.FAILED) {
				if (this.loginException instanceof FailedLoginException)
					throw (FailedLoginException) this.loginException;
				throw (LoginRefusedException) this.loginException;
			}
		}
		finally {
			// Logon failure? When network input finishes, all supporting
			// threads are stopped.
			if (this.sessionStatus != SessionState.LOGGED_ON) {
				log.error("Never logged in, sessionStatus is: " + this.sessionStatus);
				closeSession(false);
			}
		}
	}

	/**
	 * Logs off the current session.
	 * 
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public synchronized void logout() throws IllegalStateException, IOException {
		log.trace("logout");
		try {
			if (this.sessionStatus == SessionState.CONNECTING || this.sessionStatus == SessionState.CONNECTED)
				throw new IllegalStateException("Not logged in or connecting");
			transmitLogoff();
		}
		finally {
			closeSession(false);
		}
	}

	public void forceCloseSession() throws IOException {
		log.trace("force close session");
		this.sessionStatus = SessionState.UNSTARTED;
		this.cachePacket = null;
		try {
			this.network.close();
		}
		catch (IOException e) {
		}
		finally {
			closeSession(true);
		}
	}

	/**
	 * Reset a failed session, so the session object can be used again (for all those who like to minimise the number of
	 * discarded objects for the GC to clean up ;-)
	 */
	public synchronized void reset() throws IllegalStateException {
		if (this.sessionStatus != SessionState.FAILED && this.sessionStatus != SessionState.UNSTARTED)
			throw new IllegalStateException("Session currently active");
		this.sessionStatus = SessionState.UNSTARTED;
		this.chatSessionStatus = SessionState.UNSTARTED;
		resetData(); // Just to be on the safe side
	}

	/**
	 * Send keepAlive. Should not call if the session is doing this itself. This allows calling clients to manage the
	 * threads.
	 * 
	 * @return true if a ping was sent
	 */
	public boolean sendKeepAliveAndPing() {
		boolean pingSent = false;
		try {
			long now = System.currentTimeMillis();
			if (now - this.pingTimestamp > NetworkConstants.PING_TIMEOUT_IN_SECS * 1000) {
				this.transmitPings();
				this.pingTimestamp = now;
				pingSent = true;
			}
			/////////////////////////////////////////
			URL url = new URL("http://www.yahoo.com");
			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
		    urlc.setRequestProperty("User-Agent", "Android Application:OpenYMSG");
		    urlc.setRequestProperty("Connection", "close");
		    urlc.setConnectTimeout(1000 * 30); // mTimeout is in seconds
		    urlc.connect();
		    if (urlc.getResponseCode() == 200)
		    {
		    	this.timeoutCount = 0;
		    	urlc.disconnect();
				this.transmitKeepAlive();
		    }
			else
			{
				urlc.disconnect();
				this.timeoutCount++;
				if (this.timeoutCount <= NetworkConstants.TIMEOUT_RETRY_COUNT)
					this.transmitKeepAlive();
				else
				{
					this.timeoutCount = 0;
					throw new SocketException();
				}
			}
		    
		}
		catch (IOException ex) {
			if (ex instanceof SocketException) {
				log.warn("Logging out due to socket exception: " + this.getSessionID() + "/" + this.getLoginID());
				try {
					this.forceCloseSession();
				}
				catch (Exception e) {
					log.error("Failed to close session: " + this.getSessionID() + "/" + this.getLoginID(), e);
				}
			}
			else
				log.error("Could not send keep-alive to: " + this.getSessionID() + "/" + this.getLoginID(), ex);
		}
		return pingSent;

	}

	/**
	 * Send a chat message.
	 * 
	 * @param to
	 *            Yahoo ID of the user to transmit the message.
	 * @param message
	 *            The message to transmit.
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public String sendMessage(final String to, final String message) throws IllegalStateException, IOException {
		return sendMessage(to, message, this.loginID);
	}

	/**
	 * Send a chat message
	 * 
	 * @param to
	 *            Yahoo ID of the user to transmit the message.
	 * @param message
	 *            The message to transmit.
	 * @param yid
	 *            Yahoo Identity used to transmit the message from.
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws IllegalIdentityException
	 */
	public String sendMessage(final String to, final String message, final YahooIdentity yid) throws IllegalStateException, IOException,
	IllegalIdentityException {
		checkStatus();

		if (!this.identities.containsKey(yid.getId()))
			throw new IllegalIdentityException("The YahooIdentity '" + yid.getId()
					+ "'is not a valid identity for this session.");
		return transmitMessage(to, yid, message);
	}

	/**
	 * Send a buzz message
	 * 
	 * @param to
	 *            Recipient of the buzz.
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void sendBuzz(final String to) throws IllegalStateException, IOException {
		sendMessage(to, NetworkConstants.BUZZ);
	}

	/**
	 * Send a buzz message
	 * 
	 * @param to
	 *            Recipient of the buzz.
	 * @param yid
	 *            Yahoo Identity used to send the buzz.
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws IllegalIdentityException
	 */
	public void sendBuzz(final String to, final YahooIdentity yid) throws IllegalStateException, IOException,
	IllegalIdentityException {
		sendMessage(to, NetworkConstants.BUZZ, yid);
	}

	/**
	 * Get the status of the session, ie: unstarted, authenticating, etc. Check this after login() to find out if you've
	 * connected to Yahoo okay.
	 * 
	 * @return Current Status of this session object.
	 */
	public SessionState getSessionStatus() {
		return this.sessionStatus;
	}

	/**
	 * Get the Yahoo status, ie: available, invisible, busy, not at desk, etc.
	 * 
	 * @return Current presence status of the user.
	 */
	public synchronized Status getStatus() {
		return this.status;
	}

	/**
	 * Sets the Yahoo status, ie: available, invisible, busy, not at desk, etc. If you want to login as invisible, set
	 * this to Status.INVISIBLE before you call login().
	 * 
	 * Note: this setter is overloaded. The second version is intended for use when setting custom status messages.
	 * 
	 * @param status
	 *            The new Status to be set for this user.
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public synchronized void setStatus(final Status status) throws IllegalArgumentException, IOException {
		log.debug("setting status: " + status);
		if (status == Status.CUSTOM)
			throw new IllegalArgumentException("Cannot set custom state without message");

		boolean wasInvisible = this.status == Status.INVISIBLE;
		this.status = status;
		this.customStatusMessage = null;

		if (this.sessionStatus != SessionState.UNSTARTED)
			if (status == Status.INVISIBLE)
			{
				if (wasInvisible)
					return;
				transmitVisibleToggle(false);
				return;
			}
			else
			{
				if (wasInvisible)
					transmitVisibleToggle(true);
				transmitNewStatus();
			}
	}

	/**
	 * Sets the Yahoo status, ie: available, invisible, busy, not at desk, etc. Legit values are in the StatusConstants
	 * interface. If you want to login as invisible, set this to Status.INVISIBLE before you call login() Note: setter
	 * is overloaded, the second version is intended for use when setting custom status messages. The boolean is true if
	 * available and false if away.
	 * 
	 * @param message
	 * @param showBusyIcon
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public synchronized void setStatus(final String message, final boolean showBusyIcon) throws IllegalArgumentException,
	IOException {
		if (this.sessionStatus == SessionState.UNSTARTED)
			throw new IllegalArgumentException("Unstarted sessions can be available or invisible only");

		if (message == null)
			throw new IllegalArgumentException("Cannot set custom state with null message");

		boolean wasInvisible = this.status == Status.INVISIBLE; 
		this.status = Status.CUSTOM;
		this.customStatusMessage = message;
		this.customStatusBusy = showBusyIcon;

		transmitNewCustomStatus(wasInvisible);
	}
	
	/**
	 * This is used to set our on/offline status for the current session only on a user-by-user basis.
	 * 
	 * @param userId
	 * 		The id of the user whom we would like to change our stealth status for
	 * @param newStealth
	 * 		New stealth status
	 * @throws IOException
	 */
	public synchronized void changeStealth(final String target, final StealthStatus newStealth) throws IOException
	{
		YahooUser user = this.roster.getUser(target);
		if (user == null)
			throw new IllegalArgumentException("Parameter \"userId\" cannot be null or empty string");
		
		transmitStealthSession(this.loginID.getId(), target, newStealth);
		user.setStealth(newStealth);
	}

	public String getCustomStatusMessage() {
		return this.customStatusMessage;
	}

	public boolean isCustomBusy() {
		return this.customStatusBusy;
	}

	/**
	 * Ask server to return refreshed stats for this session. Server will send back a USERSTAT and truncated NEWMAIL
	 * packet.
	 */
	public void refreshStats() throws IllegalStateException, IOException {
		checkStatus();
		transmitUserStat();
	}

	/**
	 * Checks if the provided ID matches one of the identities of the user of this session.
	 * 
	 * @param yahooIdentity
	 *            The identity to check for.
	 * @return ''true'' if the ID matches one of the IDs belonging to this user.
	 */
	public boolean isValidYahooID(final String yahooIdentity) {
		return this.identities.containsKey(yahooIdentity);
	}

	/**
	 * Return the primary Yahoo Identity of this session.
	 * 
	 * @return This users primary identity.
	 */
	public YahooIdentity getPrimaryIdentity() {
		return this.primaryID;
	}

	/**
	 * Returns the Yahoo Identity that was used to login to the network.
	 * 
	 * @return YahooIdentity used to login with.
	 */
	public YahooIdentity getLoginIdentity() {
		return this.loginID;
	}

	/**
	 * Activate or deactivate a particular Yahoo Identity.
	 * 
	 * @param yid
	 * @param activate
	 * @throws IllegalStateException
	 * @throws IllegalIdentityException
	 * @throws IOException
	 */
	public void activateIdentity(final YahooIdentity yid, final boolean activate) throws IllegalStateException,
	IllegalIdentityException, IOException {
		checkStatus();

		if (!this.identities.containsKey(yid.getId()))
			throw new IllegalIdentityException("The YahooIdentity '" + yid.getId()
					+ "'is not a valid identity for this session.");

		// Make an exception of the primary identity
		if (yid.equals(this.primaryID))
			throw new IllegalIdentityException("Primary identity cannot be de/activated");
		// Send message
		if (activate)
			transmitIdActivate(yid.getId());
		else
			transmitIdDeactivate(yid.getId());
	}

	/**
	 * General accessors
	 */
	public String getImvironment() {
		return this.imvironment;
	}

	public String[] getCookies() {
		String[] arr = new String[3];
		arr[NetworkConstants.COOKIE_Y] = this.cookieY;
		arr[NetworkConstants.COOKIE_T] = this.cookieT;
		arr[NetworkConstants.COOKIE_C] = this.cookieC;
		return arr;
	}

	/**
	 * Conference code
	 */
	public YahooConference createConference(final String[] users, final String msg) throws IllegalStateException, IOException,
	IllegalIdentityException {
		for (int i = 0; i < users.length; i++)
			if (this.primaryID.getId().equals(users[i]) || this.loginID.getId().equals(users[i])
					|| this.identities.containsKey(users[i]))
				throw new IllegalIdentityException(users[i] + " is an identity of this session and cannot be used here");
		return createConference(users, msg, this.loginID);
	}

	public YahooConference createConference(final String[] users, final String msg, final YahooIdentity yid)
			throws IllegalStateException, IOException, IllegalIdentityException {
		checkStatus();

		if (!this.identities.containsKey(yid.getId()))
			throw new IllegalIdentityException("The YahooIdentity '" + yid.getId()
					+ "'is not a valid identity for this session.");

		for (int i = 0; i < users.length; i++)
			if (this.primaryID.getId().equals(users[i]) || this.loginID.getId().equals(users[i])
					|| this.identities.containsKey(users[i]))
				throw new IllegalIdentityException(users[i] + " is an identity of this session and cannot be used here");

		String conferenceName = getConferenceName(yid.getId());
		transmitConfInvite(users, yid.getId(), conferenceName, msg);
		return getConference(conferenceName);
	}

	public void acceptConferenceInvite(final YahooConference room) throws IllegalStateException, IOException,
	NoSuchConferenceException {
		checkStatus();
		transmitConfLogon(room.getName(), room.getIdentity().getId());
	}

	public void declineConferenceInvite(final YahooConference room, final String msg) throws IllegalStateException,
	IOException, NoSuchConferenceException {
		checkStatus();
		transmitConfDecline(room.getName(), room.getIdentity().getId(), msg);
	}

	public void extendConference(final YahooConference room, final String username, final String msg) throws IllegalStateException,
	IOException, NoSuchConferenceException, IllegalIdentityException {
		checkStatus();

		final String id = username;
		if (this.primaryID.getId().equals(id) || this.loginID.getId().equals(id) || this.identities.containsKey(id))
			throw new IllegalIdentityException(id + " is an identity of this session and cannot be used here");
		transmitConfAddInvite(username, room.getName(), room.getIdentity().getId(), msg);
	}

	public void sendConferenceMessage(final YahooConference room, final String msg) throws IllegalStateException, IOException,
	NoSuchConferenceException {
		checkStatus();
		transmitConfMsg(room.getName(), room.getIdentity().getId(), msg);
	}

	public void leaveConference(final YahooConference room) throws IllegalStateException, IOException,
	NoSuchConferenceException {
		checkStatus();
		transmitConfLogoff(room.getName(), room.getIdentity().getId());
	}

	// Friend Management code.

	/**
	 * Sends a new request to become friends to the user. This is a subscription request, to which to other user should
	 * reply to. Responses will arrive asynchronously.
	 * Note that if the friend already exists on the roster of the current user, the user will be added to the specified
	 * group.
	 * 
	 * @param userId
	 *            Yahoo id of user to add as a new friend.
	 * @param groupId
	 *            Name of the group to add the new friend to.
	 * @throws IllegalArgumentException
	 *             if one of the arguments is null or an empty String.
	 * @throws IllegalStateException
	 *             if this session is not logged onto the Yahoo network correctly.
	 * @throws IOException
	 *             if any problem occured related to creating or sending the request to the Yahoo network.
	 */
	@Override
	public void sendNewFriendRequest(final String userId, final String groupId, final YahooProtocol yahooProtocol)
			throws IOException {
		log.trace("Adding new user: " + userId + ", group: " + groupId + ", protocol: " + yahooProtocol);
		// TODO: perhaps we should check the roster to make sure that this
		// friend does not already exist.
		transmitFriendAdd(userId, groupId, yahooProtocol);
	}

	/**
	 * Instructs the Yahoo network to remove this friend from the particular group on the roster of the current user. If
	 * this is the last group that the user is removed from, the user is effectively removed from the roster.
	 * 
	 * @param friendId
	 *            Yahoo IDof the contact to remove from a group.
	 * @param groupId
	 *            Group to remove the contact from.
	 * @throws IllegalArgumentException
	 *             if one of the arguments is null or an empty String.
	 * @throws IllegalStateException
	 *             if this session is not logged onto the Yahoo network correctly.
	 * @throws IOException
	 *             on any problem while trying to create or send the packet.
	 */
	@Override
	public void removeFriendFromGroup(final String friendId, final String groupId) throws IOException {
		// TODO: we should check the roster to find out of this friend actually
		// exists on the roster, in that particular group.
		transmitFriendRemove(friendId, groupId);
	}

	public void renameGroup(final String oldName, final String newName) throws IllegalStateException, IOException {
		checkStatus();
		transmitGroupRename(oldName, newName);
	}

	public void rejectContact(final SessionEvent se, final String msg) throws IllegalArgumentException, IllegalStateException,
	IOException {
		if (se.getFrom() == null || se.getTo() == null)
			throw new IllegalArgumentException("Missing to or from field in event object.");
		checkStatus();
		transmitContactReject(se.getFrom(), se.getTo(), msg);
	}

	public void ignoreContact(final String friend, final boolean ignore) throws IllegalStateException, IOException {
		checkStatus();
		transmitContactIgnore(friend, ignore);
	}

	public void refreshFriends() throws IllegalStateException, IOException {
		checkStatus();
		transmitList();
	}

	public void moveFriend(final String friendId, final String fromGroup, final String toGroup) throws IOException
	{
		checkStatus();
		transmitChangeGroup(friendId, this.loginID, fromGroup, toGroup);
	}

	public void rejectFriendAuthorization(final String friend, final String msg)
			throws IllegalStateException, IOException {
		checkStatus();
		transmitRejectBuddy(friend, msg);
	}

	public void acceptFriendAuthorization(final String friend, final YahooProtocol procotol) throws IllegalStateException,
	IOException {
		checkStatus();
		transmitAcceptBuddy(friend, procotol.getStringValue());
	}

	/**
	 * File transfer 'save as' saves to a particular directory and filename, 'save to' uses the file's own name and
	 * saves it to a particular directory. Note: the 'to' method gets its filename from different sources. Initially the
	 * URL filename (minus the path), however the header Content-Disposition will override this. The 'as' method always
	 * uses its own specified filename. If both _path_ and _filename_ are not null then the saveFT() method assumes
	 * 'to'... but if _path_ is null, saveFT() assumes 'as' and _filename_ is the entire path+filename.
	 */
	public void sendFileTransfer(final String user, final File file, final String msg) throws IllegalStateException,
	FileTransferFailedException, IOException {
		checkStatus();
		transmitFileTransfer(user, msg, file);
	}

	public void saveFileTransferAs(final SessionFileTransferEvent ev, final String filename) throws FileTransferFailedException,
	IOException {
		saveFT(ev, null, filename);
	}

	public void saveFileTransferTo(final SessionFileTransferEvent ev, String dir) throws FileTransferFailedException,
	IOException {
		// Yahoo encodes the filename into the URL, but allow for
		// Content-Disposition header override.
		if (!dir.endsWith(File.separator)) dir = dir + File.separator;
		saveFT(ev, dir, ev.getFilename());
	}

	private void saveFT(final SessionFileTransferEvent ev, final String path, String filename) throws FileTransferFailedException,
	IOException {
		int len;
		byte[] buff = new byte[4096];

		// HTTP request
		HttpURLConnection uConn = (HttpURLConnection) (ev.getLocation().openConnection());
		Util.initURLConnection(uConn);
		uConn.setRequestProperty("User-Agent", NetworkConstants.USER_AGENT);
		// uConn.setRequestProperty("Host",ftHost);
		uConn.setRequestProperty("Cookie", this.cookieY + "; " + this.cookieT);
		uConn.connect();

		// Response header
		if (uConn.getResponseCode() != 200)
			throw new FileTransferFailedException("Server HTTP error code: " + uConn.getResponseCode());
		String rp = uConn.getHeaderField("Content-Disposition");
		if (path != null && rp != null) {
			int i = rp.indexOf("filename=");
			if (i >= 0) filename = rp.substring(i + 9);
			// Strip quotes if necessary
			if (filename.charAt(0) == '\"') filename = filename.substring(1, filename.length() - 1);
		}

		// Response body
		if (path != null) filename = path + filename;
		InputStream is = uConn.getInputStream();
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename));
		try {
			do {
				len = is.read(buff);
				if (len > 0) bos.write(buff, 0, len);
			} while (len >= 0);
			bos.flush();
		}
		finally {
			bos.close();
			is.close();
		}
		uConn.disconnect();
	}

	/**
	 * Logs the current default identity of this session into the provided lobby.
	 * 
	 * @param lobby
	 *            Lobby to login to.
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws LoginRefusedException
	 */
	public void chatLogin(final YahooChatLobby lobby) throws IllegalStateException, IOException, LoginRefusedException {
		chatLogin(lobby, this.loginID);
	}

	/**
	 * Logs the provided yahoo identity into the provided lobby.
	 * 
	 * @param lobby
	 *            Lobby to login to.
	 * @param yahooId
	 *            Yahoo Identity that should login to the lobby.
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws LoginRefusedException
	 * @throws IllegalIdentityException
	 */
	public void chatLogin(final YahooChatLobby lobby, final YahooIdentity yahooId) throws IllegalStateException, IOException,
	LoginRefusedException, IllegalIdentityException {
		checkStatus();

		if (!this.identities.containsKey(yahooId.getId()))
			throw new IllegalIdentityException("The YahooIdentity '" + yahooId.getId()
					+ "'is not a valid identity for this session.");

		// TODO synchronized blocks don't really work (java optimization problem). Need a separate synchronized method
		// for this
		synchronized (this) {
			if (this.chatSessionStatus != SessionState.UNSTARTED && this.chatSessionStatus != SessionState.LOGGED_ON)
				throw new IllegalStateException(
						"Chat session should be unstarted or messaging. You can't login to two chatrooms at the same time. Wait for one login to complete before connecting to the next one.");
			this.chatSessionStatus = SessionState.CONNECTING;
		}

		final long timeout = System.currentTimeMillis() + Util.loginTimeout(NetworkConstants.LOGIN_TIMEOUT);
		this.chatID = yahooId;

		try {
			transmitChatConnect(this.chatID.getId());
			while (this.chatSessionStatus != SessionState.CONNECTED && !past(timeout))
				try {
					Thread.sleep(10);
				}
			catch (InterruptedException e) {
				// ignore
			}
			if (past(timeout))
				throw new InterruptedIOException("Chat connect timed out");

			// Transmit 'login' packet and wait for acknowledgement
			transmitChatJoin(lobby.getNetworkName(), lobby.getParentRoomId());
			while (this.chatSessionStatus == SessionState.CONNECTED && !past(timeout))
				try {
					Thread.sleep(10);
				}
			catch (InterruptedException e) {
				// ignore
			}

			if (past(timeout))
				throw new InterruptedIOException("Chat login timed out");

			if (this.chatSessionStatus == SessionState.FAILED)
				throw (LoginRefusedException) this.loginException;

			// Successful?
			if (this.chatSessionStatus == SessionState.LOGGED_ON)
				this.currentLobby = lobby;
			else
				this.currentLobby = null;
		}
		finally {
			if (this.chatSessionStatus != SessionState.LOGGED_ON) {
				this.chatSessionStatus = SessionState.FAILED;
				this.chatID = null;
			}
		}
	}

	public synchronized void chatLogout() throws IllegalStateException, IOException {
		checkStatus();
		checkChatStatus();
		transmitChatDisconnect(this.currentLobby.getNetworkName());
		this.currentLobby = null;
	}

	public synchronized void extendChat(final String to, final String msg) throws IllegalStateException, IOException {
		checkStatus();
		checkChatStatus();
		final String netName = this.currentLobby.getNetworkName();
		final long roomId = this.currentLobby.getParentRoomId();
		transmitChatInvite(netName, roomId, to, msg);
	}

	public void sendChatMessage(final String msg) throws IllegalStateException, IOException {
		checkStatus();
		checkChatStatus();
		transmitChatMsg(this.currentLobby.getNetworkName(), msg, false);
	}

	public void sendChatEmote(final String emote) throws IllegalStateException, IOException {
		checkStatus();
		checkChatStatus();
		transmitChatMsg(this.currentLobby.getNetworkName(), emote, true);
	}

	public YahooChatLobby getCurrentChatLobby() {
		return this.currentLobby;
	}

	public SessionState getChatSessionStatus() {
		return this.chatSessionStatus;
	}

	public void resetChat() throws IllegalStateException {
		if (this.chatSessionStatus != SessionState.FAILED && this.chatSessionStatus != SessionState.UNSTARTED)
			throw new IllegalStateException("Chat session currently active");
		this.chatSessionStatus = SessionState.UNSTARTED;
	}

	/**
	 * Transmit an AUTH packet, as a way of introduction to the server. As we do not know our primary ID yet, both 0 and
	 * 1 use loginID .
	 */
	protected void transmitAuth() throws IOException {
		if (this.sessionStatus != SessionState.CONNECTING)
			throw new IllegalStateException(
					"Cannot transmit an AUTH packet if you're not completely unconnected to the Yahoo Network. Current state: "
							+ this.sessionStatus);

		final PacketBodyBuffer body = new PacketBodyBuffer();
		// FIX: only req. for HTTPConnectionHandler ?
		// body.addElement("0", loginID.getId());
		body.addElement("1", this.loginID.getId());
		sendPacket(body, ServiceType.AUTH);
	}

	/**
	 * Transmit an VERIFY packet, as a way of introduction to the server. As we do not know our primary ID yet, both 0 and
	 * 1 use loginID .
	 */
	protected void transmitVerify() throws IOException {
		if (this.sessionStatus != SessionState.CONNECTING)
			throw new IllegalStateException(
					"Cannot transmit an VERIFY packet if you're not completely unconnected to the Yahoo Network. Current state: "
							+ this.sessionStatus);

		final PacketBodyBuffer body = new PacketBodyBuffer();
		sendPacket(body, ServiceType.VERIFY);
	}


	/**
	 * Transmit an AUTHRESP packet, the second part of our login process. As we do not know our primary ID yet, both 0
	 * and 1 use loginID . Note: message also contains our initial status. plp - plain response (not MD5Crypt'd) crp -
	 * crypted response (MD5Crypt'd)
	 */
	private void transmitAuthResp(final String plp, final String crp, final String base64) throws IOException {

		if (this.sessionStatus != SessionState.CONNECTED)
			throw new IllegalStateException(
					"Cannot transmit an AUTHRESP packet if you're not completely connected to the Yahoo Network. Current state: "
							+ this.sessionStatus);

		if (base64 == null) {
			final PacketBodyBuffer body = new PacketBodyBuffer();
			body.addElement("0", this.loginID.getId());
			body.addElement("6", plp);
			body.addElement("96", crp);
			body.addElement("2", this.loginID.getId());
			body.addElement("2", "1");
			// body.addElement("135", "6,0,0,1710"); // Needed for v12(?)
			body.addElement("244", "2097087"); // Needed for v15(?)
			body.addElement("148", "180"); // Needed for v15(?)
			body.addElement("135", NetworkConstants.CLIENT_VERSION); // Needed for v15(?)
			body.addElement("1", this.loginID.getId());

			// add our picture checksum, if it's available
			if (this.pictureHandler != null && this.pictureHandler.getPictureChecksum() != null)
				body.addElement("192", this.pictureHandler.getPictureChecksum());

			sendPacket(body, ServiceType.AUTHRESP, this.status); // 0x54
		}
		else {
			PacketBodyBuffer body = new PacketBodyBuffer();
			body.addElement("1", this.loginID.getId());
			body.addElement("0", this.loginID.getId());
			body.addElement("277", plp);// 277 Needed for v16(?)
			body.addElement("278", crp);// 278 Needed for v16(?)
			body.addElement("307", base64);// 307 Needed for v16(?)
			body.addElement("244", NetworkConstants.CLIENT_VERSION_ID); // Needed for v15(?)
			body.addElement("2", this.loginID.getId());
			body.addElement("2", "1");
			body.addElement("135", NetworkConstants.CLIENT_VERSION); // Needed for v15(?)

			if (this.pictureHandler != null && this.pictureHandler.getPictureChecksum() != null)
				body.addElement("192", this.pictureHandler.getPictureChecksum());

			sendPacket(body, ServiceType.AUTHRESP, this.status); // 0x54
		}
	}

	/**
	 * Transmit a CHATCONNECT packet. We send one of these as the first packet to the chat server - by way of
	 * introduction. The server responds with its own 0x96 packet back at us, and then we can logon.
	 */
	protected void transmitChatConnect(final String yid) throws IOException {
		this.chatSessionStatus = SessionState.CONNECTING; // Set status
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("109", this.primaryID.getId());
		body.addElement("1", yid);
		body.addElement("6", "abcde"); // FIX: what is this?
		sendPacket(body, ServiceType.CHATCONNECT); // 0x96
	}

	/**
	 * Sends a packet requesting chat room creation
	 */
	protected void transmitChatCreate(final long catId, final String rname, final String topic, final boolean pub) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.chatID.getId());
		body.addElement("104", rname); // Room name
		body.addElement("105", topic);
		body.addElement("126", "0"); // Fix: what is this?
		body.addElement("128", catId + "");
		body.addElement("129", catId + "");
		body.addElement("62", "2"); // Fix: what is this?
		sendPacket(body, ServiceType.getServiceType(0xa9));
	}

	/**
	 * Transmit a CHATDISCONNECT packet.
	 */
	protected void transmitChatDisconnect(final String room) throws IOException {
		this.chatSessionStatus = SessionState.UNSTARTED;
		this.currentLobby = null;
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("104", room);
		body.addElement("109", this.chatID.getId());
		sendPacket(body, ServiceType.CHATDISCONNECT); // 0xa0
	}

	/**
	 * Transmit a CHATADDINVITE packet. We send one of these to invite a fellow Yahoo user to a chat room.
	 */
	protected void transmitChatInvite(final String netname, final long id, final String to, final String msg) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.chatID.getId());
		body.addElement("104", netname); // Room name
		body.addElement("117", msg); // Invite text
		body.addElement("118", to); // Target
		body.addElement("129", id + ""); // Room id
		sendPacket(body, ServiceType.getServiceType(0x9d)); // 0x9d
	}

	/**
	 * Transmit a CHATJOINpacket. We send one of these after the CHATCONNECT packet, as the second phase of chat login.
	 * Note: netname uses network name of "room:lobby".
	 */
	protected void transmitChatJoin(final String netname, final long roomId) throws IOException {
		if (this.sessionStatus != SessionState.CONNECTED)
			throw new IllegalStateException(
					"Logging on is only possible right after successfully connecting to the network.");

		if (netname == null || netname.length() < 3 || !netname.contains(":"))
			throw new IllegalStateException(
					"Argument 'netname' cannot be null and must include network name, containing a room and lobby name seperated by a colon (':') character.");

		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.chatID.getId());
		body.addElement("104", netname);
		body.addElement("129", Long.toString(roomId));
		body.addElement("62", "2"); // FIX: what is this?
		sendPacket(body, ServiceType.CHATJOIN); // 0x98
	}

	/**
	 * Transmit a CHATMSG packet. The contents of this message will be forwarded to other users of the chatroom, BUT NOT
	 * TO US! Note: 'netname' is the network name of "room:lobby".
	 */
	protected void transmitChatMsg(final String netname, final String msg, final boolean emote) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.chatID.getId());
		body.addElement("104", netname);
		body.addElement("117", msg);
		if (emote)
			body.addElement("124", "2"); // 1=Regular, 2=Emote
		else
			body.addElement("124", "1");
		if (Util.isUtf8(msg)) body.addElement("97", "1");
		sendPacket(body, ServiceType.CHATMSG); // 0xa8
	}

	/**
	 * Transmit a CHATPM packet. Person message packets. FIX: DOES THIS WORK ???
	 */
	protected void transmitChatPM(final String to, final String msg) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("5", to);
		body.addElement("14", msg);
		sendPacket(body, ServiceType.CHATPM); // 0x020
	}

	/**
	 * Transmit a CHATPING packet.
	 */
	protected void transmitChatPing() throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		sendPacket(body, ServiceType.CHATPING); // 0xa1
	}

	/**
	 * Transmit an CONFADDINVITE packet. We send one of these when we wish to invite more users to our conference.
	 */
	protected void transmitConfAddInvite(final String username, final String room, final String yid, final String msg) throws IOException,
	NoSuchConferenceException {
		// Check this conference actually exists (throws exception if not)
		getConference(room);
		// Send new invite packet to Yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		body.addElement("51", username);
		body.addElement("57", room);

		final Set<YahooUser> users = getConference(room).getUsers();
		for (YahooUser u : users)
			body.addElement("53", u.getId());

		body.addElement("58", msg);
		body.addElement("13", "0"); // FIX : what's this for?
		sendPacket(body, ServiceType.CONFADDINVITE); // 0x1c
	}

	/**
	 * Transmit an CONFDECLINE packet. We send one of these when we decline an offer to join a conference.
	 */
	protected void transmitConfDecline(final String room, final String yid, final String msg) throws IOException,
	NoSuchConferenceException {
		// Flag this conference as now dead
		YahooConference yc = getConference(room);
		yc.closeConference();
		final Set<YahooUser> users = yc.getUsers();
		// Send decline packet to Yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		for (YahooUser user : users)
			body.addElement("3", user.getId());

		body.addElement("57", room);
		body.addElement("14", msg);
		sendPacket(body, ServiceType.CONFDECLINE); // 0x1a
	}

	/**
	 * Transmit an CONFINVITE packet. This is sent when we want to create a new conference, with the specified users and
	 * with a given welcome message.
	 */
	protected void transmitConfInvite(final String[] users, final String yid, final String room, final String msg) throws IOException {
		// Create a new conference object
		this.conferences.put(room, new YahooConference(this.identities.get(yid.toLowerCase()), room, msg, this, false));
		// Send request to Yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		body.addElement("57", room);
		for (int i = 0; i < users.length; i++)
			body.addElement("52", users[i]);
		body.addElement("58", msg);
		body.addElement("97", "1");
		body.addElement("13", "0"); // 0 for not voice.  voice is 256
		sendPacket(body, ServiceType.CONFINVITE); // 0x18
	}

	/**
	 * Transmit an CONFLOGOFF packet. We send one of these when we wish to leave a conference.
	 */
	protected void transmitConfLogoff(final String room, final String yid) throws IOException, NoSuchConferenceException {
		// Flag this conference as now dead
		YahooConference yc = getConference(room);
		yc.closeConference();
		final Set<YahooUser> users = yc.getUsers();
		// Send decline packet to Yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		for (YahooUser user : users)
			body.addElement("3", user.getId());
		body.addElement("57", room);
		sendPacket(body, ServiceType.CONFLOGOFF); // 0x1b
	}

	/**
	 * Transmit an CONFLOGON packet. Send this when we want to accept an offer to join a conference.
	 */
	protected void transmitConfLogon(final String room, final String yid) throws IOException, NoSuchConferenceException {
		// Get a list of users for this conference
		final Set<YahooUser> users = getConference(room).getUsers();
		// Send accept packet to Yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		for (YahooUser user : users)
			body.addElement("3", user.getId());
		body.addElement("57", room);
		sendPacket(body, ServiceType.CONFLOGON); // 0x19
	}

	/**
	 * Transmit an CONFMSG packet. We send one of these when we wish to send a message to a conference.
	 */
	protected void transmitConfMsg(final String room, final String yid, final String msg) throws IOException, NoSuchConferenceException {
		// Get a list of users for this conference
		final Set<YahooUser> users = getConference(room).getUsers();
		// Send message packet to yahoo
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		for (YahooUser user : users)
			body.addElement("53", user.getId());
		body.addElement("57", room);
		body.addElement("14", msg);
		if (Util.isUtf8(msg)) body.addElement("97", "1");
		sendPacket(body, ServiceType.CONFMSG); // 0x1d
	}

	/**
	 * Transmit an CONTACTIGNORE packet. We would do this in response to an ADDFRIEND packet arriving. (???) FIX: when
	 * does this get sent?
	 */
	protected void transmitContactIgnore(final String friend, final boolean ignore) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId()); // FIX: effective id?
		body.addElement("7", friend);
		if (ignore)
			body.addElement("13", "1"); // Bug: 1/2 not 0/1 ???
		else
			body.addElement("13", "2");
		sendPacket(body, ServiceType.CONTACTIGNORE); // 0x85
	}

	/**
	 * Transmit a CONTACTREJECT packet. We would do this when we wish to overrule an attempt to add us as a friend (when
	 * we get a ADDFRIEND packet!)
	 */
	protected void transmitContactReject(final String friend, final String yid, final String msg) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid);
		body.addElement("7", friend);
		body.addElement("14", msg);
		sendPacket(body, ServiceType.CONTACTREJECT); // 0x86
	}

	protected void transmitRejectBuddy(final String friend, final String msg) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.loginID.getId());	// My ID
		body.addElement("5", friend);	//Friend's ID
		body.addElement("13", "2");// Reject Authorization

		if (msg != null) body.addElement("14", msg);

		sendPacket(body, ServiceType.Y7_AUTHORIZATION, Status.AVAILABLE); // 0xd6
	}

	protected void transmitAcceptBuddy(final String friend, final String type) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId());
		body.addElement("5", friend);
		body.addElement("241", type);
		body.addElement("13", "1");// Accept Authorization
		// body.addElement("334", ""); not therein v16
		sendPacket(body, ServiceType.Y7_AUTHORIZATION, Status.AVAILABLE); // 0xd6,
	}

	/**
	 * Transmit a FILETRANSFER packet, to send a binary file to a friend.
	 */
	protected void transmitFileTransfer(final String to, final String message, final File file) throws FileTransferFailedException,
	IOException {
		if (file == null)
			throw new IllegalArgumentException("Argument 'file' cannot be null.");

		if (!file.isFile())
			throw new IllegalArgumentException(
					"The provided file object does not denote a normal file (but possibly a directory).");

		if (file.length() == 0L)
			throw new FileTransferFailedException("File transfer: empty file");

		final String cookie = this.cookieY + "; " + this.cookieT;

		final byte[] marker = { '2', '9', (byte) 0xc0, (byte) 0x80 };

		// Create a Yahoo packet into 'packet'
		final PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("0", this.primaryID.getId());
		body.addElement("5", to);
		body.addElement("28", Long.toString(file.length()));
		body.addElement("27", file.getName());
		body.addElement("14", message);
		byte[] packet = body.getBuffer();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.write(NetworkConstants.MAGIC, 0, 4);
		dos.write(NetworkConstants.VERSION, 0, 4);
		dos.writeShort((packet.length + 4) & 0xFFFF);
		dos.writeShort(ServiceType.FILETRANSFER.getValue() & 0xFFFF);
		dos.writeInt((int) (this.status.getValue() & 0xFFFFFFFF));
		dos.writeInt((int) (this.sessionId & 0xFFFFFFFF));
		dos.write(packet, 0, packet.length);
		dos.write(marker, 0, 4); // Extra 4 bytes : marker before file data
		// (?)

		packet = baos.toByteArray();

		// Send to Yahoo using POST
		String ftHost = Util.fileTransferHost();
		String ftURL = "http://" + ftHost + NetworkConstants.FILE_TF_PORTPATH;
		HttpURLConnection uConn = (HttpURLConnection) (new URL(ftURL).openConnection());
		uConn.setRequestMethod("POST");
		uConn.setDoOutput(true); // POST, not GET
		Util.initURLConnection(uConn);
		uConn.setRequestProperty("Content-Length", Long.toString(file.length() + packet.length));
		uConn.setRequestProperty("User-Agent", NetworkConstants.USER_AGENT);
		// uConn.setRequestProperty("Host",ftHost);
		uConn.setRequestProperty("Cookie", cookie);
		uConn.connect();

		final BufferedOutputStream bos = new BufferedOutputStream(uConn.getOutputStream());
		try {
			bos.write(packet);
			bos.write(Util.getBytesFromFile(file));
			bos.flush();
		}
		finally {
			bos.close();
		}

		final int ret = uConn.getResponseCode();
		uConn.disconnect();

		if (ret != 200)
			throw new FileTransferFailedException("Server rejected upload");
	}

	/**
	 * Transmit a FRIENDADD packet. If all goes well we'll get a FRIENDADD packet back with the details of the friend to
	 * confirm the transaction (usually preceded by a CONTACTNEW packet with well detailed info).
	 * 
	 * @param userId
	 *            Yahoo id of friend to add group
	 * @param groupId
	 *            Group to add the new friend in
	 * @throws IllegalArgumentException
	 *             if one of the arguments is null or an empty String.
	 * @throws IllegalStateException
	 *             if this session is not logged onto the Yahoo network correctly.
	 * @throws IOException
	 *             on any problem while trying to create or send the packet.
	 */
	protected void transmitFriendAdd(final String userId, final String groupId, final YahooProtocol yahooProtocol)
			throws IOException {
		if (userId == null || userId.length() == 0)
			throw new IllegalArgumentException("Argument 'userId' cannot be null or an empty String.");

		if (groupId == null || groupId.length() == 0)
			throw new IllegalArgumentException("Argument 'groupId' cannot be null or an empty String.");

		// check if the current Session state allows us to send anything.
		checkStatus();

		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId()); // ???: effective id?
		body.addElement("302", "319");
		body.addElement("300", "319");
		body.addElement("7", userId);
		body.addElement("241", "" + yahooProtocol.getValue()); // type
		body.addElement("301", "319");
		body.addElement("303", "319");
		body.addElement("65", groupId);
		body.addElement("14", "");
		body.addElement("216", "");
		body.addElement("254", "");
		body.addElement("97", "1");
		// body.addElement("334", "0"); not used in 16

		sendPacket(body, ServiceType.FRIENDADD); // 0x83
	}

	/**
	 * Transmit a FRIENDREMOVE packet. We should get a FRIENDREMOVE packet back (usually preceded by a CONTACTNEW
	 * packet).
	 * <p>
	 * Note that removing a user from all groups that it is in, equals removing the user from the contact list
	 * completely.
	 * 
	 * @param friendId
	 *            Yahoo IDof the contact to remove from a group.
	 * @param groupId
	 *            Group to remove the contact from.
	 * @throws IllegalArgumentException
	 *             if one of the arguments is null or an empty String.
	 * @throws IllegalStateException
	 *             if this session is not logged onto the Yahoo network correctly.
	 * @throws IOException
	 *             on any problem while trying to create or send the packet.
	 */
	protected void transmitFriendRemove(final String friendId, final String groupId) throws IOException {
		if (friendId == null || friendId.length() == 0)
			throw new IllegalArgumentException("Argument 'friendId' cannot be null or an empty String.");

		if (groupId == null || groupId.length() == 0)
			throw new IllegalArgumentException("Argument 'groupId' cannot be null or an empty String.");

		// check if the current Session state allows us to send anything.
		checkStatus();

		final PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId()); // ???: effective id?
		body.addElement("7", friendId);
		body.addElement("241", "0"); // for ack
		body.addElement("65", groupId);
		sendPacket(body, ServiceType.FRIENDREMOVE); // 0x84
	}

	/**
	 * Transmit a GOTGROUPRENAME packet, to change the name of one of our friends groups.
	 */
	/*
	 * TODO: Currently, this behavior is as it was in jYMSG. Protocol specification would suggest that not 0x13
	 * (GOTGROUPRENAME) but 0x89 (GROUPRENAME) should be used for this operation. Find out and make sure.
	 */
	protected void transmitGroupRename(final String oldName, final String newName) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId()); // ???: effective id?
		body.addElement("65", oldName);
		body.addElement("67", newName);
		sendPacket(body, ServiceType.GROUPRENAME); // 0x13
	}

	/**
	 * Transmit a IDACT packet.
	 */
	protected void transmitIdActivate(final String id) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("3", id);
		sendPacket(body, ServiceType.IDACT); // 0x07
	}

	/**
	 * Transmit a IDDEACT packet.
	 */
	protected void transmitIdDeactivate(final String id) throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("3", id);
		sendPacket(body, ServiceType.IDDEACT); // 0x08
	}

	/**
	 * Transmit an IDLE packet. Used by the HTTP proxy connection to provide a mechanism were by incoming packets can be
	 * delivered. An idle packet is sent every thirty seconds (as part of a HTTP POST) and the server responds with all
	 * the packets accumulated since last contact.
	 */
	protected void transmitIdle() throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.loginID.getId()); // FIX: Should this be primary?
		body.addElement("0", this.primaryID.getId());
		sendPacket(body, ServiceType.IDLE); // 0x05
	}

	/**
	 * Transmit the current status to the Yahoo network.
	 * 
	 * @throws IOException
	 */
	protected void transmitNewStatus() throws IOException {
		final PacketBodyBuffer body = new PacketBodyBuffer();
        body.addElement("10", String.valueOf(this.status.getValue()));
        body.addElement("19", "");
        body.addElement("97", "1"); // required in the new version of Yahoo!
        							// no status will be changed otherwise
        sendPacket(body, ServiceType.Y6_STATUS_UPDATE);
	}
	
	
	/**
	 * 	Transmit the desired visibility to Yahoo network. Basically a visible/invisible toggle
	 * 
	 * @param toggleOn
	 * 		Set to true if you want to be present on the Yahoo network, otherwise set to false
	 * @throws IOException
	 */
	protected void transmitVisibleToggle(final boolean toggleOn) throws IOException
	{
		PacketBodyBuffer body = new PacketBodyBuffer();
		String flag = toggleOn ? "1" : "2";
		
		body.addElement("13", flag);
		sendPacket(body, ServiceType.Y6_VISIBLE_TOGGLE);
	}

	/**
	 * Transmit the current custom status to the Yahoo network.
	 * 
	 * @throws IOException
	 */
	protected void transmitNewCustomStatus(final boolean wasInvisible) throws IOException {
		if (wasInvisible)
			transmitVisibleToggle(true); // toggle on in case we were invisible
		
		final PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("10", "99");
		body.addElement("19", this.customStatusMessage);
		body.addElement("97", "1");

		if (this.customStatusBusy)
			body.addElement("47", "1");
		// body.addElement("187", "0");
		sendPacket(body, ServiceType.Y6_STATUS_UPDATE, Status.AVAILABLE);
	}
	
	/**
	 * Transmit a LIST packet.
	 */
	protected void transmitList() throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.primaryID.getId());
		sendPacket(body, ServiceType.LIST); // 0x55
	}

	/**
	 * Transmit a LOGOFF packet, which should exit us from Yahoo IM.
	 */
	protected void transmitLogoff() throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("0", this.loginID.getId()); // Is this only in for HTTP?
		sendPacket(body, ServiceType.LOGOFF); // 0x02
		this.ipThread.stopMe();
		this.network.close();
	}

	/**
	 * Transmit a MESSAGE packet.
	 * 
	 * @param to
	 *            he Yahoo ID of the user to send the message to
	 * @param yid
	 *            Yahoo identity used to send the message from
	 * @param msg
	 *            the text of the message
	 */
	protected String transmitMessage(final String to, final YahooIdentity yid, final String msg) throws IOException {
		if (to == null || to.length() == 0)
			throw new IllegalArgumentException("Argument 'to' cannot be null or an empty String.");

		if (yid == null)
			throw new IllegalArgumentException("Argument 'yid' cannot be null.");

		String type = this.getType(to);

		String messageNumberString = buildMessageNumber();

		IM im = new IM(messageNumberString, this.loginID.getId(), to, msg);
		synchronized(this.imQueue)
		{
			this.imQueue.add(im);
		}

		// Send packet
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid.getId()); // From (effective ID)
		body.addElement("5", to); // To
		body.addElement("241", type);
		body.addElement("14", msg); // Message
		// Extension for YMSG9
		if (Util.isUtf8(msg)) body.addElement("97", "1");
		body.addElement("63", ";" + this.imvironment); // Not supported here!
		body.addElement("64", "0");
		body.addElement("206", "0");
		body.addElement("429", messageNumberString);
		body.addElement("450", "0");
		sendPacket(body, ServiceType.MESSAGE, Status.OFFLINE); // 0x06
		this.messageResendTimer.schedule(new TimerTask()
		{

			@Override
			public void run()
			{
				IM im;
				synchronized (Session.this.imQueue)
				{
					if (Session.this.imQueue.isEmpty())
						return;

					im = Session.this.imQueue.remove();
				}
				log.info("Retransmitting message: " + im.toString());
				String from = im.getFrom();
				String to = im.getTo();
				String msg = im.getMessage();
				String messageNumberString = im.getMessageNumberString();

				try
				{
					// Send packet
					PacketBodyBuffer body = new PacketBodyBuffer();
					body.addElement("1", from); // From (effective ID)
					body.addElement("5", to); // To
					body.addElement("241", getType(to));
					body.addElement("14", msg); // Message
					// Extension for YMSG9
					if (Util.isUtf8(msg)) body.addElement("97", "1");
					body.addElement("63", ";" + Session.this.imvironment); // Not supported here!
					body.addElement("64", "0");
					body.addElement("206", "0");
					body.addElement("429", messageNumberString);
					body.addElement("450", "0");
					sendPacket(body, ServiceType.MESSAGE, Status.OFFLINE); // 0x06
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}, NetworkConstants.RESEND_TIMEOUT * 1000);
		return messageNumberString;
	}

	/**
	 * Transmit a CHANGEGROUP packet.
	 * 
	 * @param friendId
	 *            The Yahoo ID of the user to move to another group
	 * @param yid
	 *            Yahoo identity used to perform this action
	 * @param fromGroup
	 *            The group to move this friend from
	 * @param toGroup
	 * 			  The destination group
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	protected void transmitChangeGroup(final String friendId, final YahooIdentity yid, final String fromGroup, final String toGroup) throws IOException
	{
		if (friendId == null || fromGroup == null || toGroup==null)
			throw new IllegalArgumentException("Arguments cannot be null or an empty String.");

		if (yid == null)
			throw new IllegalArgumentException("Argument 'yid' cannot be null.");

		// Send packet
		PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", yid.getId()); // From (effective ID)
		body.addElement("302", "240");
		body.addElement("300", "240");
		body.addElement("7", friendId);
		body.addElement("224", fromGroup);
		body.addElement("264", toGroup);
		body.addElement("301", "240");
		body.addElement("303", "240");

		sendPacket(body, ServiceType.Y7_CHANGE_GROUP, Status.OFFLINE);
	}

	protected String getType(final String to) {
		YahooUser user = this.roster.getUser(to);
		if (user == null || user.getProtocol() == null)
			return YahooProtocol.YAHOO.getStringValue();
		return user.getProtocol().getStringValue();
	}

	protected String buildMessageNumber() {
		String blankMessageNumber = "0000000000000000";
		String messageNumber = "" + this.messageNumber++;
		messageNumber = blankMessageNumber.substring(0, blankMessageNumber.length() - messageNumber.length())
				+ messageNumber;
		return messageNumber;
	}

	/**
	 * notify to friend the typing start or end action message parameters Version: 16 Service: Notify (75) Status:
	 * Notify (22)
	 * 
	 * 49: TYPING 1: userId 14: <empty> 13: 1 or 0 5: sendingToId
	 * 
	 * @param friend
	 *            user whose sending message
	 * @param isTyping
	 *            true if start typing, false if typing end up
	 * @throws IOException
	 */
	public void sendTypingNotification(final String friend, final boolean isTyping) throws IOException {
		String type = this.getType(friend);
		transmitNotify(friend, this.primaryID.getId(), isTyping, " ", NOTIFY_TYPING, type);
	}

	/**
	 * Transmit a NOTIFY packet. Could be used for all sorts of purposes, but mainly games and typing notifications.
	 * Only typing is supported by this API. The mode determines the type of notification, "TYPING" or "GAME"; msg holds
	 * the game name (or a single space if typing). *
	 * 
	 * @param friend
	 * @param yid
	 *            id
	 * @param on
	 *            true start typing, false stop typing
	 * @param msg
	 * @param mode
	 * @throws IOException
	 */
	protected void transmitNotify(final String friend, final String yid, final boolean on, final String msg, final String mode, final String type)
			throws IOException {
		final PacketBodyBuffer body = new PacketBodyBuffer();
		// Added 1 for is typing, not sure we need the "4"
		body.addElement("49", mode);
		body.addElement("1", yid);
		// body.addElement("4", yid);
		body.addElement("14", msg);
		if (on)
			body.addElement("13", "1");
		else
			body.addElement("13", "0");
		body.addElement("5", friend);
		// added for is typing
		body.addElement("241", type);
		sendPacket(body, ServiceType.NOTIFY, Status.TYPING); // 0x4b
	}

	/**
	 * Transmits a Keep-Alive packet to the Yahoo network.
	 * 
	 * At the time of implementation, this packet gets sent by the official Yahoo client once every 60 seconds, and
	 * seems to replace the older PING service type (although this service type is still received by the client just
	 * after authentication).
	 * 
	 * The keep-alive does not appear to be sent to a Yahoo Chatrooms.
	 * 
	 * @throws IOException
	 */
	protected void transmitKeepAlive() throws IOException {
		final PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("0", this.primaryID.getId());
		sendPacket(body, ServiceType.KEEPALIVE);
	}

	/**
	 * Transmit a PING packet always and a CHATPING packet, if the user is logged into a lobby. Needed every hour to
	 * keep from getting knocked off by LOGGOFF 52
	 */
	protected void transmitPings() throws IOException {
		PacketBodyBuffer body = new PacketBodyBuffer();
		sendPacket(body, ServiceType.PING); // 0x12

		// Not sure we need to chatPing
		// if (currentLobby != null) {
		// transmitChatPing();
		// }
	}

	/**
	 * Transmit a USERSTAT packet. Purpose? Unknown :-) It would seem that transmiting this packet results in a USERSTAT
	 * (0x0a) packet followed by a NEWMAIL (0x0b) packet with just the 9 field (new mail count) set being sent by the
	 * server.
	 */
	protected void transmitUserStat() throws IOException // 0x0a
	{
		PacketBodyBuffer body = new PacketBodyBuffer();
		sendPacket(body, ServiceType.USERSTAT);
	}

	/**
	 * Transmit a STEALTHSESSION packet. This is used to set our on/offline status for the current session only on a
	 * user-by-user basis
	 */
	protected void transmitStealthSession(final String yid, final String target, final StealthStatus newStealth) throws IOException {
		String flag = newStealth == StealthStatus.STEALTH_PERMENANT? "2" : "1";
		
		PacketBodyBuffer body = new PacketBodyBuffer();
		//TODO implement these for session stealth support!
//		body.addElement("1", yid); // My id
//		body.addElement("31", flag); // Stealth status
//		body.addElement("13", "1"); // What is this for?
//		body.addElement("302", "319");
//		body.addElement("300", "319");
//		body.addElement("7", target); // Friend who is target
//		body.addElement("301", "319");
//		body.addElement("303", "319");
//		sendPacket(body, ServiceType.STEALTH_SESSION);
		
		flag = newStealth == StealthStatus.STEALTH_PERMENANT? "1" : "2";
		body = new PacketBodyBuffer();
		body.addElement("1", yid); // My id
		body.addElement("31", flag); // Stealth status
		body.addElement("13", "2"); // What is this for?
		body.addElement("302", "319");
		body.addElement("300", "319");
		body.addElement("7", target); // Friend who is target
		body.addElement("301", "319");
		body.addElement("303", "319");
		sendPacket(body, ServiceType.STEALTH_PERM);
		
		//PERM : 2 ==> ONLINE
		//		 1 ==> OFFLINE
	}
	
	/**
	 * Process an incoming ADDIGNORE packet. We get one of these when we ignore/unignore someone, although their purpose
	 * is unknown as Yahoo follows up with a CONTACTIGNORE packet. The only disting- uising feature is the latter is
	 * always sent, wereas this packet is only sent if there's an actual change in ignore status. The packet's payload
	 * appears to always be empty.
	 */
	protected void receiveAddIgnore(final YMSG9Packet pkt) // 0x11
	{
		// Not implementation (yet!)
	}

	/**
	 * Process an incoming AUTH packet (in response to the AUTH packet we transmitted to the server).
	 * <p>
	 * Format: <tt>"1" <loginID> "94" <challenge string * (24 chars)></tt>
	 * <p>
	 * Note: for YMSG10 and later, Yahoo sneakily changed the challenge/response method dependent upon a switch in field
	 * '13'. If this field is absent or 0, use v9, if 1 or 2, then use v10.
	 */
	protected void receiveAuth(final YMSG9Packet pkt) // 0x57
			throws IOException, YahooException {
		if (this.sessionStatus != SessionState.CONNECTING)
			throw new IllegalStateException("Received a response to an AUTH packet, outside the normal"
					+ " login flow. Current state: " + this.sessionStatus);
		log.trace("Received AUTH from server. Going to parse challenge...");
		// Value for key 13: '0'=v9, '1'=v10, '2'=v16
		String v10 = pkt.getValue("13");
		final String[] s;
		String seed = pkt.getValue("94");
		try {
			if (v10 != null && v10.equals("0")) {
				log.trace("Parsing V9 challenge...");
				s = ChallengeResponseV9.getStrings(this.loginID.getId(), this.password, pkt.getValue("94"));
			}
			else if (v10 != null && v10.equals("1")) {
				log.trace("Parsing V10 challenge...");
				s = ChallengeResponseV10.getStrings(this.loginID.getId(), this.password, pkt.getValue("94"));
			}
			else
				s = yahooAuth16Stage1(seed);
		}
		catch (NoSuchAlgorithmException e) {
			throw new YMSG9BadFormatException("auth", pkt, e);
		}
		catch (IOException e) {
			this.loginException = new FailedLoginException("User " + this.loginID + ": Login failed unexpectedly.", e);
			throw this.loginException;
		}
		catch (LoginRefusedException e) {
			this.loginException = e;
			throw e;
		}
		catch (Exception e) {
			this.loginException = new FailedLoginException("User " + this.loginID + ": Login failed unexpectedly.", e);
			throw this.loginException;
		}
		this.sessionStatus = SessionState.CONNECTED;
		log.trace("Going to transmit Auth response, " + "containing the challenge...");
		if (s.length > 2)
			transmitAuthResp(s[0], s[1], s[2]);
		else
			transmitAuthResp(s[0], s[1], null);
		loadAddressBook(s);
	}

	private void loadAddressBook(final String[] sessionCookies) {
		log.trace("loadAddressBook");
		BuddyListImport buddyListImport = new BuddyListImport(this.loginID.getId(), this.roster, sessionCookies);
		try {
			buddyListImport.process(this.loginID.getId(), this.password);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String[] yahooAuth16Stage1(final String seed) throws LoginRefusedException, IOException, NoSuchAlgorithmException {
		String authLink = "https://" + this.yahooLoginHost + "/config/pwtoken_get?src=ymsgr&ts=&login=" + this.loginID.getId()
				+ "&passwd=" + URLEncoder.encode(this.password, "UTF-8") + "&chal=" + URLEncoder.encode(seed, "UTF-8");

		URL u = new URL(authLink);
		URLConnection uc = u.openConnection();
		uc.setConnectTimeout(LOGIN_HTTP_TIMEOUT);

		if (uc instanceof HttpsURLConnection) {
			HttpsURLConnection httpUc = (HttpsURLConnection) uc;
			// used to simulate failures
			//             if  (triesBeforeFailure++ % 3 == 0) {
			//                 throw new SocketException("Test failure");
			//             }
			if (!this.yahooLoginHost.equalsIgnoreCase(LOGIN_YAHOO_COM))
				httpUc.setHostnameVerifier(new HostnameVerifier() {

					@Override
					public boolean verify(final String hostname, final SSLSession session) {
						Principal principal = null;
						try {
							principal = session.getPeerPrincipal();
						}
						catch (SSLPeerUnverifiedException e) {
						}
						String localName = "no set";
						if (principal != null)
							localName  = principal.getName();
						log.debug("Hostname verify: " + hostname + "localName: " + localName);
						return true;
					}
				});

			int responseCode = httpUc.getResponseCode();
			this.sessionStatus = SessionState.STAGE1;
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = uc.getInputStream();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int read = -1;
				byte[] buff = new byte[256];
				while ((read = in.read(buff)) != -1)
					out.write(buff, 0, read);
				in.close();

				StringTokenizer toks = new StringTokenizer(out.toString(), "\r\n");
				if (toks.countTokens() <= 0)
					// errrorrrr
					throw new LoginRefusedException("Login Failed, wrong response in stage 1:"
							+ httpUc.getResponseMessage());

				int responseNo = -1;
				try {
					responseNo = Integer.valueOf(toks.nextToken());
				}
				catch (NumberFormatException e) {
					throw new LoginRefusedException("Login Failed, wrong response in stage 1:"
							+ httpUc.getResponseMessage());
				}

				if (responseNo != 0 || !toks.hasMoreTokens())
					switch (responseNo) {
						case 1235:
							throw new LoginRefusedException("Login Failed, Invalid username",
									AuthenticationState.BADUSERNAME);
						case 1212:
							throw new LoginRefusedException("Login Failed, Wrong password", AuthenticationState.BAD);
						case 1213:
							throw new LoginRefusedException("Login locked: Too many failed login attempts",
									AuthenticationState.LOCKED);
						case 1236:
							throw new LoginRefusedException("Login locked", AuthenticationState.LOCKED);
						case 100:
							throw new LoginRefusedException("Username or password missing", AuthenticationState.BAD);
						default:
							throw new LoginRefusedException("Login Failed, Unkown error", AuthenticationState.BAD);
					}

				String ymsgr = toks.nextToken();

				if (ymsgr.indexOf("ymsgr=") == -1 && toks.hasMoreTokens())
					ymsgr = toks.nextToken();

				ymsgr = ymsgr.replaceAll("ymsgr=", "");

				return yahooAuth16Stage2(ymsgr, seed);
			}
			else {
				log.error("Failed opening login url: " + authLink + " return code: " + responseCode);
				throw new LoginRefusedException("Login Failed, Login url: " + authLink + " return code: "
						+ responseCode);
			}
		}
		else {
			Class<? extends URLConnection> ucType = null;
			if (uc != null)
				ucType = uc.getClass();
			log.error("Failed opening login url: " + authLink + " returns: " + ucType);
			throw new LoginRefusedException("Login Failed, Unable to submit login url");
		}

		//throw new LoginRefusedException("Login Failed, unable to retrieve stage 1 url");
	}

	private String[] yahooAuth16Stage2(final String token, final String seed) throws LoginRefusedException, IOException,
	NoSuchAlgorithmException {
		String loginLink = "https://" + this.yahooLoginHost + "/config/pwtoken_login?src=ymsgr&ts=&token=" + token;

		URL u = new URL(loginLink);
		URLConnection uc = u.openConnection();
		uc.setConnectTimeout(LOGIN_HTTP_TIMEOUT);

		if (uc instanceof HttpsURLConnection) {
			trustEveryone();
			HttpsURLConnection httpUc = (HttpsURLConnection) uc;

			if (!this.yahooLoginHost.equalsIgnoreCase(LOGIN_YAHOO_COM))
				httpUc.setHostnameVerifier(new HostnameVerifier() {

					@Override
					public boolean verify(final String hostname, final SSLSession session) {
						return true;
					}
				});

			int responseCode = httpUc.getResponseCode();
			this.sessionStatus = SessionState.STAGE2;
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = uc.getInputStream();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int read = -1;
				byte[] buff = new byte[256];
				while ((read = in.read(buff)) != -1)
					out.write(buff, 0, read);

				int responseNo = -1;
				String crumb = null;
				String cookieY = null;
				String cookieT = null;

				StringTokenizer toks = new StringTokenizer(out.toString(), "\r\n");
				if (toks.countTokens() <= 0)
					// errrorrrr
					throw new LoginRefusedException("Login Failed, wrong response in stage 2:"
							+ httpUc.getResponseMessage());

				try {
					responseNo = Integer.valueOf(toks.nextToken());
				}
				catch (NumberFormatException e) {
					throw new LoginRefusedException("Login Failed, wrong response in stage 2:"
							+ httpUc.getResponseMessage());
				}

				if (responseNo != 0 || !toks.hasMoreTokens())
					throw new LoginRefusedException("Login Failed, Unkown error", AuthenticationState.BAD);

				while (toks.hasMoreTokens()) {
					String t = toks.nextToken();
					if (t.startsWith("crumb="))
						crumb = t.replaceAll("crumb=", "");
					else if (t.startsWith("Y="))
						cookieY = t.replaceAll("Y=", "");
					else if (t.startsWith("T="))
						cookieT = t.replaceAll("T=", "");
				}

				if (crumb == null || cookieT == null || cookieY == null)
					throw new LoginRefusedException("Login Failed, Unkown error", AuthenticationState.BAD);

				// Iterator<String> iter =
				// ((HttpURLConnection) uc).getHeaderFields().get("Set-Cookie").iterator();
				// while (iter.hasNext())
				// {
				// String string = iter.next();
				// System.out.println("\t" + string);
				// }
				this.cookieY = cookieY;
				this.cookieT = cookieT;
				return yahooAuth16Stage3(crumb + seed, cookieY, cookieT);
			}
		}

		throw new LoginRefusedException("Login Failed, unable to retrieve stage 2 url");
	}

	private String[] yahooAuth16Stage3(final String crypt, final String cookieY, final String cookieT) throws NoSuchAlgorithmException {
		return ChallengeResponseV16.getStrings(cookieY, cookieT, crypt);
	}

	/**
	 * Process an incoming AUTHRESP packet. If we get one of these it means the logon process has failed. Set the
	 * session state to be failed, and flag the end of login. Note: we don't throw exceptions on the input thread, but
	 * instead we pass them to the thread which called login()
	 */
	protected void receiveAuthResp(final YMSG9Packet pkt) // 0x54
	{
		log.trace("Received AUTHRESP packet.");
		SessionLogoutEvent sessionEvent = null;
		try {
			if (pkt.exists("66")) {
				final long l = Long.parseLong(pkt.getValue("66"));
				switch (AuthenticationState.getStatus(l)) {
					// Account locked out?
					case LOCKED:
						URL u;
						try {
							u = new URL(pkt.getValue("20"));
						}
						catch (Exception e) {
							u = null;
						}
						this.loginException = new AccountLockedException("User " + this.loginID + " has been locked out", u);
						log.info("AUTHRESP says: authentication failed! " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.LOCKED);
						break;

						// Bad login (password?)
					case BAD2:
					case BAD:
						this.loginException = new LoginRefusedException("User " + this.loginID + " refused login" + this.loginID,
								AuthenticationState.BAD);
						log.info("AUTHRESP says: authentication failed! " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.BAD);
						break;

						// unknown account?
					case BADUSERNAME:
						this.loginException = new LoginRefusedException("User " + this.loginID + " unknown" + this.loginID,
								AuthenticationState.BADUSERNAME);
						log.info("AUTHRESP says: authentication failed! " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.BADUSERNAME);
						break;
					case INVALID_CREDENTIALS:
						this.loginException = new LoginRefusedException("User " + this.loginID + " invalid Credentials" + this.loginID,
								AuthenticationState.BADUSERNAME);
						log.info("AUTHRESP says: authentication failed! " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.INVALID_CREDENTIALS);
						break;
						// You have been logged out of the yahoo service, possibly due to a duplicate login.
					case DUPLICATE_LOGIN:
						this.loginException = new LoginRefusedException("User " + this.loginID + " unknown" + this.loginID,
								AuthenticationState.DUPLICATE_LOGIN);
						log.info("AUTHRESP says: authentication failed! " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.DUPLICATE_LOGIN);
						break;
					case UNKNOWN_52:
						this.loginException = new LoginRefusedException("User " + this.loginID + " was forced off" + this.loginID,
								AuthenticationState.UNKNOWN_52);
						log.info("AUTHRESP says: authentication failed with unknown: " + AuthenticationState.UNKNOWN_52
								+ " " + this.loginID, this.loginException);
						sessionEvent = new SessionLogoutEvent(AuthenticationState.UNKNOWN_52);
				}
			} else {
				this.loginException = new LoginRefusedException("User " + this.loginID + " was forced off" + this.loginID,
						AuthenticationState.NO_REASON);
				log.info("AUTHRESP says: authentication failed without a reason"
						+ " " + this.loginID, this.loginException);
				sessionEvent = new SessionLogoutEvent(AuthenticationState.NO_REASON);
			}
		}
		catch (IllegalArgumentException ex) {
			this.loginException = new LoginRefusedException("User " + this.loginID
					+ ": Login refused. The reason why is unrecognized.", ex);
		}
		finally {
			// When this method exits, the ipThread loop calling it will
			// terminate.
			this.ipThread.stopMe();
			// Notify login() calling thread of failure
			this.sessionStatus = SessionState.FAILED;
			this.eventDispatchQueue.append(sessionEvent, ServiceType.LOGOFF);
		}
	}

	/**
	 * Process an incoming CHATCONNECT packet. We get one of these in reply to sending a CHATCONNECT packet to Yahoo. It
	 * marks the end of the first stage of the chat login handshake. chatLogin() waits for this packet before proceeding
	 * to the next stage.
	 */
	protected void receiveChatConnect(final YMSG9Packet pkt) // 0x96
	{
		if (this.chatSessionStatus != SessionState.CONNECTING)
			throw new IllegalStateException(
					"Received a 'CHATCONNECT' packet outside of the logon process. Current state: " + this.chatSessionStatus);
		this.chatSessionStatus = SessionState.CONNECTED;
	}

	/**
	 * Process an incoming CHATDISCONNECT packet. We get one of these to confirm we have logged off.
	 */
	protected void receiveChatDisconnect(final YMSG9Packet pkt) // 0xa0
	{
		// This should already have been set by transmitChatDisconnect(),
		// if it isn't set then we have been booted out without actually
		// asking to leave. This usually happens when Yahoo times us out
		// of a room after a long period of inactivity.
		if (this.chatSessionStatus != SessionState.UNSTARTED)
			this.eventDispatchQueue.append(new SessionEvent(this), ServiceType.CHATDISCONNECT);
		this.chatSessionStatus = SessionState.UNSTARTED;
	}

	/**
	 * Process an incoming CHATEXIT packet. We get one of these when someone leaves the chatroom - including ourselves,
	 * received just prior to our CHATDISCONNECT confirmation packet (see above). Note: on rare occassions this packet
	 * has been received for a user we previously had not been informed about.
	 */
	protected void receiveChatExit(final YMSG9Packet pkt) // 0x9b
	{
		try {
			String netname = pkt.getValue("104"); // room:lobby
			String id = pkt.getValue("109"); // Yahoo id
			YahooChatLobby ycl = this.chatroomManager.getLobby(netname);
			if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");
			// Remove user from room (very very occassionally the user does
			// not exist as a known member of this lobby, so ycu==null!!)
			YahooChatUser ycu = ycl.getUser(id);
			if (ycu != null)
				ycl.removeUser(ycu);
			else
				ycu = createChatUser(pkt, 0); // Create for benefit of event!
			// Create and fire event FIX: should cope with multiple users!
			SessionChatEvent se = new SessionChatEvent(this, 1, ycl);
			se.setChatUser(0, ycu);
			this.eventDispatchQueue.append(se, ServiceType.CHATEXIT);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("chat logoff", pkt, e);
		}
	}

	/**
	 * Process an incoming CHATJOIN packet. We get one of these: (a) as a way of finishing the login handshaking
	 * process, containing room details (we already know) and a list of current members. (b) when the login process
	 * fails (room full?), containing only a 114 field (set to '-35'?) - see error handling code elsewhere (c) as a
	 * stripped down version when a new user enters the room (including ourselves!) (d) as a stripped down version when
	 * an existing member updates their details. (e) when we need to deal with a chat captcha. Sometimes a login packet
	 * is broken over several packets. The clue here appears to be that the first (and subsequent?) packets have a
	 * status of 5, while the final packet has a status of 1.
	 */
	protected void receiveChatJoin(YMSG9Packet pkt) // 0x98
	{
		boolean joining = false;
		try {
			// Is this an error packet, sent to us via processError() ?
			if (pkt.exists("114")) {
				this.loginException = new LoginRefusedException("User " + this.chatID + " refused chat login");
				joining = true;
				this.chatSessionStatus = SessionState.FAILED;
				return; // ...to finally block
			}

			// Returns null if more packets to come
			pkt = compoundChatLoginPacket(pkt);
			if (pkt == null) return;

			// As we need to load a room to get at its lobby data so we
			// can login, the next line *should* never fail... however :-)
			String netname = pkt.getValue("104"); // room:lobby
			YahooChatLobby ycl = this.chatroomManager.getLobby(netname);
			if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");

			// -----Process a captcha packet
			if (pkt.exists("105"))
				try {
					String captchaMsg = pkt.getValue("105");
					String captchaURL = null;
					int idx = (captchaMsg != null) ? captchaMsg.indexOf("http://captcha.") : -1;
					if (idx >= 0) captchaURL = captchaMsg.substring(idx);

					SessionChatEvent se = new SessionChatEvent(this, captchaMsg, captchaURL, ycl);
					this.eventDispatchQueue.append(se, ServiceType.X_CHATCAPTCHA);
					return; // ...to finally block
				}
			catch (Exception e) {
				throw new YMSG9BadFormatException("chat captcha", pkt, e);
			}

			// Note: Yahoo sometimes lies about the '108' count of users!
			// Reduce count until to see how many users there *actually* are!
			int cnt = Integer.parseInt(pkt.getValue("108"));
			while (cnt > 0 && pkt.getNthValue("109", cnt - 1) == null)
				cnt--;

			// Is this an update packet, for an existing member?
			YahooChatUser ycu = ycl.getUser(pkt.getValue("109"));
			if (cnt == 1 && ycu != null) {
				// Count is one and user exists - UPDATE
				final int attributes = Integer.parseInt(pkt.getValue("113"));
				final String alias = pkt.getValue("141"); // optional
				final int age = Integer.parseInt(pkt.getValue("110"));
				final String location = pkt.getValue("142"); // optional

				ycu.setAttributes(attributes);
				ycu.setAlias(alias);
				ycu.setAge(age);
				ycu.setLocation(location);

				SessionChatEvent se = new SessionChatEvent(this, 1, ycl);
				se.setChatUser(0, ycu);
				this.eventDispatchQueue.append(se, ServiceType.X_CHATUPDATE);
				return; // ...to finally block
			}
			// Full sized packet, when joining room?
			joining = pkt.exists("61");
			// If we are joining, clear the array of users (just to be sure!)
			if (joining) ycl.clearUsers();

			// When sent in muliple parts the login packet usually
			// contains a high degree of duplicates. Remove using hash.
			Hashtable<String, YahooChatUser> ht = new Hashtable<String, YahooChatUser>();
			for (int i = 0; i < cnt; i++) {
				// Note: automatically creates YahooUser if necessary
				ycu = createChatUser(pkt, i);
				ht.put(ycu.getId(), ycu);
			}
			// Create event, add users
			SessionChatEvent se = new SessionChatEvent(this, cnt, ycl);
			int i = 0;
			for (Enumeration<YahooChatUser> en = ht.elements(); en.hasMoreElements();) {
				ycu = en.nextElement();
				// Does this user exist already? (This should always be
				// no, as update packets should always have only one member
				// who already exists - thus caught by the 'if' block above!
				if (!ycl.exists(ycu)) ycl.addUser(ycu); // Add to lobby
				se.setChatUser(i++, ycu); // Add to event
			}

			// We don't send an event if we get the larger 'logging in'
			// type packet as the chat user list is brand new. We only send
			// events when someone joins and we need to signal an update.
			if (!joining) {
				// Did we actually accrue any *new* users in previous loop?
				if (se.getChatUsers().length > 0) this.eventDispatchQueue.append(se, ServiceType.CHATJOIN);
			}
			else
				this.chatSessionStatus = SessionState.LOGGED_ON;
			return; // ...to finally block
		}
		catch (RuntimeException e) {
			log.error("error on receveing Chat join ", e);
			throw new YMSG9BadFormatException("chat login", pkt, e);
		}
		finally {
			// FIX: Not thread safe if multiple chatroom supported!
			if (joining && this.chatSessionStatus != SessionState.FAILED) this.chatSessionStatus = SessionState.LOGGED_ON;
		}
	}

	/**
	 * Process an incoming CHATMSG packet. We get one of these when a chatroom user sends a message to the room. (Note:
	 * very very occassionally we *might* get a message from a user we have not been told about yet! I've not seen any
	 * chat message packets like this, but it *does* happen with other chat packets, so I have to assume it can happen
	 * to chat message packets too!) Note: status checked because we may have timed out on chat login
	 */
	protected void receiveChatMsg(final YMSG9Packet pkt) // 0xa8
	{
		if (this.chatSessionStatus != SessionState.LOGGED_ON)
			throw new IllegalStateException("Time out on chat login.");

		try {
			String netname = pkt.getValue("104"); // room:lobby
			YahooChatLobby ycl = this.chatroomManager.getLobby(netname);
			if (ycl == null) throw new NoSuchChatroomException("Chatroom/lobby " + netname + " not found.");
			// Create and fire event
			YahooChatUser ycu = ycl.getUser(pkt.getValue("109"));
			if (ycu == null) ycu = createChatUser(pkt, 0);
			SessionChatEvent se = new SessionChatEvent(this, ycu, // from
					pkt.getValue("117"), // message
					pkt.getValue("124"), // 1=Regular, 2=Emote
					ycl // room:lobby
					);
			this.eventDispatchQueue.append(se, ServiceType.CHATMSG);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("chat message", pkt, e);
		}
	}

	/**
	 * Process an incoming CHATPM packet. We get one of these when someone 'personal messages' us from within a
	 * chatroom. Why this packet is used, not a regular MESSAGE packet is unknown. It seems that the web-based chat
	 * clients, at least, prefer this packet type. Note: status checked because we may have timed out on chat login
	 */
	protected void receiveChatPM(final YMSG9Packet pkt) // 0x20
	{
		if (this.chatSessionStatus != SessionState.LOGGED_ON)
			throw new IllegalStateException("Time out on chat login.");

		try {
			SessionEvent se = new SessionEvent(this, pkt.getValue("5"), // to
					pkt.getValue("4"), // from
					pkt.getValue("14") // message
					);
			this.eventDispatchQueue.append(se, ServiceType.MESSAGE);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("chat PM", pkt, e);
		}
	}

	/**
	 * Process an incoming CONFDECLINE packet. We get one of these when someone we previously invited to a conference,
	 * declines our invite.
	 */
	protected void receiveConfDecline(final YMSG9Packet pkt) // 0x1a
	{
		try {
			YahooConference yc = getOrCreateConference(pkt);
			String to = pkt.getValue("1");
			String from = pkt.getValue("54");
			String message = pkt.getValue("14");
			// Create event
			SessionConferenceDeclineInviteEvent se =
					new SessionConferenceDeclineInviteEvent(this, to, from, message, yc);
			// Remove the user
			yc.removeUser(se.getFrom());
			// Fire invite event
			if (!yc.isClosed()) // Should never be closed!
				this.eventDispatchQueue.append(se, ServiceType.CONFDECLINE);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("conference decline", pkt, e);
		}
	}

	/**
	 * Process an incoming CONFLOGOFF packet. We get one of these when someone leaves the conference. Note: in *very*
	 * extreme circum- stances, this may arrive before the invite packet.
	 */
	protected void receiveConfLogoff(final YMSG9Packet pkt) // 0x1b
	{
		// If we have not received an invite yet, buffer packets
		YahooConference yc = getOrCreateConference(pkt);
		String to = pkt.getValue("1");
		String from = pkt.getValue("56");
		synchronized (yc) {
			if (!yc.isInvited()) {
				yc.addPacket(pkt);
				return;
			}
		}
		// Otherwise, handle the packet
		try {
			SessionConferenceLogoffEvent se = new SessionConferenceLogoffEvent(this, to, from, yc);
			// Remove the user
			yc.removeUser(se.getFrom());
			// Fire invite event
			if (!yc.isClosed()) // Should never be closed fir invite!
				this.eventDispatchQueue.append(se, ServiceType.CONFLOGOFF);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("conference logoff", pkt, e);
		}
	}

	/**
	 * Process an incoming CONFLOGON packet. We get one of these when someone joins a conference we have been invited to
	 * (even if we ourselves have yet to accept/decline). Note: in extreme circum- stances, this may arrive before the
	 * invite packet.
	 */
	protected void receiveConfLogon(final YMSG9Packet pkt) // 0x19
	{
		// If we have not received an invite yet, buffer packets
		YahooConference yc = getOrCreateConference(pkt);
		String to = pkt.getValue("1");
		String from = pkt.getValue("53");
		synchronized (yc) {
			if (!yc.isInvited()) {
				yc.addPacket(pkt);
				return;
			}
		}
		// Otherwise, handle the packet
		try {
			SessionConferenceLogonEvent se = new SessionConferenceLogonEvent(this, to, from, yc);
			// Add user (probably already on list, but just to be sure!)
			yc.addUser(se.getFrom());
			// Fire event
			if (!yc.isClosed()) this.eventDispatchQueue.append(se, ServiceType.CONFLOGON);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("conference logon", pkt, e);
		}
	}

	/**
	 * Process an incoming CONFMSG packet. We get one of these when someone in a conference we are part of sends a
	 * message. Note: in extreme circumstances this may arrive before the invite packet.
	 */
	protected void receiveConfMsg(final YMSG9Packet pkt) // 0x1d
	{
		// If we have not received an invite yet, buffer packets
		YahooConference yc = getOrCreateConference(pkt);
		String to = pkt.getValue("1");
		String from = pkt.getValue("3");
		String message = pkt.getValue("14");
		synchronized (yc) {
			if (!yc.isInvited()) {
				yc.addPacket(pkt);
				return;
			}
		}
		// Otherwise, handle the packet
		try {
			SessionConferenceMessageEvent se = new SessionConferenceMessageEvent(this, to,
					from, message, yc);
			// Fire event
			if (!yc.isClosed()) this.eventDispatchQueue.append(se, ServiceType.CONFMSG);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("conference mesg", pkt, e);
		}
	}

	/**
	 * Process an incoming CONTACTIGNORE packet. We get one of these to confirm an outgoing CONTACTIGNORE - an ADDIGNORE
	 * packet may precede this, but only if the ignore status has genuinely changed state.
	 */
	protected void receiveContactIgnore(final YMSG9Packet pkt) // 0x85
	{
		try {
			String userId = pkt.getValue("0");
			boolean ignored = pkt.getValue("13").charAt(0) == '1';
			int st = Integer.parseInt(pkt.getValue("66"));
			if (st == 0) {
				// Update ignore status, and fire friend changed event
				final YahooUser user = this.roster.getUser(userId);
				user.setIgnored(ignored);
				// Fire event
				SessionFriendEvent se = new SessionFriendEvent(this, user);
				this.eventDispatchQueue.append(se, ServiceType.Y6_STATUS_UPDATE);
			}
			else {
				// Error
				String m = "Contact ignore error: ";
				switch (st) {
					case 2:
						m = m + "Already on ignore list";
						break;
					case 3:
						m = m + "Not currently ignored";
						break;
					case 12:
						m = m + "Cannot ignore friend";
						break;
					default:
						m = m + "Unknown error";
						break;
				}
				errorMessage(pkt, m);
			}
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("contact ignore", pkt, e);
		}
	}

	protected void receiveContactRejected(final YMSG9Packet pkt) {
		String from = pkt.getValue("1");// from
		log.debug(from + " has rejected to been added like friend");

		// removeFriend(from);
	}

	/**
	 * Process an incoming CONTACTNEW packet. We get one of these:
	 * <ol>
	 * <li>when someone has added us to their friends list, giving us the chance to refuse them;</li>
	 * <li>when we add or remove a friend (see FRIENDADD/REMOVE outgoing) as confirmation prior to the FRIENDADD/REMOVE
	 * packet being echoed back to us - if the friend is online status info may be included (supposedly for multiple
	 * friends, although given the circumstances probably always contains only one!);</li>
	 * <li>when someone refuses a contact request (add friend) from us.</li>
	 * </ol>
	 */
	protected void receiveContactNew(final YMSG9Packet pkt) // 0x0f
	{
		// Empty CONTACTNEW, response to FRIENDADD/REMOVE.
		if (pkt.length <= 0) {
			log.trace("Received an empty CONTACTNEW packet, which is "
					+ "probably sent back to us after we transmitted " + "a FRIENDADD/REMOVE. Just ignore it.");
			return;
		}

		try {
			// Ditto, except friend is online
			if (pkt.exists("7")) {
				log.trace("Received a CONTACTNEW packet, which is probably "
						+ "sent back to us after we transmitted a "
						+ "FRIENDADD/REMOVE. Pass it to updateFriendStatus.");
				updateFriendsStatus(pkt);
				return;
			}

			final String userId = pkt.getValue("3");
			final String message = pkt.getValue("14");

			// Contact refused our subscription request.
			if (pkt.status == 0x07) {
				log.trace("A friend refused our subscription request: " + userId);
				final YahooUser user = this.roster.getUser(userId);
				final SessionFriendRejectedEvent se = new SessionFriendRejectedEvent(this, user, message);
				this.eventDispatchQueue.append(se, ServiceType.CONTACTREJECT);
				return;
			}

			// Someone is sending us a subscription request.
			log.trace("Someone is sending us a subscription request: " + userId);
			final String to = pkt.getValue("1");
			// final String timestamp = pkt.getValue("15");
			String protocolString = pkt.getValue("241"); // not there, it is yahoo
			YahooProtocol protocol = YahooProtocol.getProtocol(protocolString);

			final SessionEvent se;
			// if (timestamp == null || timestamp.length() == 0) {
			se = new SessionAuthorizationEvent(this, to, message, protocol);
			// } else {
			// final long timestampInMillis = 1000 * Long.parseLong(timestamp);
			// se = new SessionEvent(this, to, userId, message,
			// timestampInMillis);
			// }
			se.setStatus(pkt.status); // status!=0 means offline message
			this.eventDispatchQueue.append(se, ServiceType.CONTACTNEW);
		}
		catch (RuntimeException e) {
			throw new YMSG9BadFormatException("contact request", pkt, e);
		}
	}

	/**
	 * pkt.status: 1 - Authorization Accepted 2 - Authorization Denied 3 - Authorization Request
	 */
	protected void receiveAuthorization(final YMSG9Packet pkt) { // 0xd6
		try {
			if (pkt.length <= 0)
				return;

			YahooProtocol protocol;
			String who = pkt.getValue("4");
			String msg = pkt.getValue("14");
			String fname = pkt.getValue("216");
			String lname = pkt.getValue("254");
			String id = pkt.getValue("5");
			String authStatus = pkt.getValue("13");
			String protocolString = pkt.getValue("241");
			protocol = getUserProtocol(protocolString, who);

			if (pkt.status == 1) {
				if (authStatus.equals("1")) {
					log.trace("A friend accepted our authorization request: " + who);
					YahooUser user = this.roster.getUser(who);
					SessionFriendAcceptedEvent ser = new SessionFriendAcceptedEvent(this, user, msg, protocol);
					int eventStatus = 1;
					ser.setStatus(eventStatus);
					this.eventDispatchQueue.append(ser, ServiceType.Y7_AUTHORIZATION);
				}
				else if (authStatus.equals("2")) {
					log.trace("A friend refused our subscription request: " + who);
					log.debug("roster: " + this.roster);
					log.debug("who: " + who);
					YahooUser user = this.roster.getUser(who);
					log.debug("user: " + user);
					SessionFriendRejectedEvent ser = new SessionFriendRejectedEvent(this, user, msg);
					log.debug("ser: " + ser);
					int eventStatus = 2;
					log.debug("eventStatus: " + eventStatus);
					ser.setStatus(eventStatus);
					this.eventDispatchQueue.append(ser, ServiceType.Y7_AUTHORIZATION);
				}
				else
					log.info("Unexpected authorization packet. Do not know how to handle: " + pkt);
			}
			else if (pkt.status == 3) {
				SessionAuthorizationEvent se = new SessionAuthorizationEvent(this, id, who, fname, lname, msg, protocol);
				se.setStatus(pkt.status);

				log.trace("Someone is sending us a subscription request: " + who);
				this.eventDispatchQueue.append(se, ServiceType.Y7_AUTHORIZATION);
			}
			else
				log.info("Unexpected authorization packet. Do not know how to handle: " + pkt);

		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("contact request", pkt, e);
		}
	}

	private YahooProtocol getUserProtocol(final String protocolString, final String who) {
		try {
			return YahooProtocol.getProtocol(protocolString);
		}
		catch (IllegalArgumentException e) {
			log.error("Failed finding protocol: " + protocolString + " for user: " + who);
			return YahooProtocol.YAHOO;
		}
	}

	/**
	 * Process an incoming FILETRANSFER packet. This packet can be received under two circumstances: after a successful
	 * FT upload, in which case it contains a text message with the download URL, or because someone has sent us a file.
	 * Note: TF packets do not contain the file data itself, but rather a link to a tmp area on Yahoo's webservers which
	 * holds the file.
	 */
	protected void receiveFileTransfer(final YMSG9Packet pkt) // 0x46
	{
		try {
			final String to = pkt.getValue("5");
			final String from = pkt.getValue("4");
			final String message = pkt.getValue("14");

			if (!pkt.exists("38")) {
				// Acknowledge upload
				final SessionEvent se = new SessionEvent(this, to, from, message);
				this.eventDispatchQueue.append(se, ServiceType.MESSAGE);
			}
			else {
				// Receive file transfer
				final String expires = pkt.getValue("38");
				final String url = pkt.getValue("20");

				final SessionFileTransferEvent se = new SessionFileTransferEvent(this, to, from, message, Long
						.valueOf(expires), url);
				this.eventDispatchQueue.append(se, ServiceType.FILETRANSFER);
			}
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("file transfer", pkt, e);
		}
	}

	/**
	 * Process an incoming FRIENDADD packet. We get one of these after we've sent a FRIENDADD packet, as confirmation.
	 * It contains basic details of our new friend, although it seems a bit redundant as Yahoo sents a CONTACTNEW with
	 * these details before this packet.
	 */
	protected void receiveFriendAdd(final YMSG9Packet pkt) // 0x83
	{
		try {
			String userId = null;
			String groupName = null;
			YahooUser user = null;
			String friendAddStatus = pkt.getValue("66");
			String myName = pkt.getValue("1");
			if (!"0".equals(friendAddStatus))
				log.warn("Me: " + myName + " Friend add status is not 0: " + friendAddStatus);

			userId = pkt.getValue("7");
			groupName = pkt.getValue("65");

			if (pkt.status == 1) { // status 1 is an ack
				String pending = pkt.getValue("223");
				String protocol = pkt.getValue("241");
				log.trace("Me: " + myName + " Friend add is an ack: " + pkt.status + "/" + userId + ", error code: "
						+ friendAddStatus + ", pending: " + pending + ", protocol: " + protocol);
				if (friendAddStatus.equals("0")) { // successful add
					log.info("Me: " + myName + " friend added: " + userId + ", pending: " + pending + ", protocol: "
							+ protocol);
					// Sometimes, a status update arrives before the FRIENDADD
					// confirmation. If that's the case, we'll already have this contact
					// on our roster.
					user = this.roster.getUser(userId);
					if (user == null) {
						YahooProtocol yahooProtocol = getUserProtocol(protocol, userId);
						user = new YahooUser(userId, groupName, yahooProtocol);
					}
					// Fire event : 7=friend, 66=status, 65=group name
					final SessionFriendEvent se = new SessionFriendEvent(this, user, groupName);
					this.eventDispatchQueue.append(se, ServiceType.FRIENDADD);
				}
				else {
					if (friendAddStatus.equals("2"))
						log.warn("Me: " + myName + " friend already in list: " + userId + ", pending: " + pending
								+ ", protocol: " + protocol);
					else if (friendAddStatus.equals("3"))
						log.warn("Me: " + myName + " friend does not exist in yahoo: " + userId + ", pending: "
								+ pending + ", protocol: " + protocol);
					else
						log.warn("Me: " + myName + " problem adding friend: " + userId + ", pending: " + pending
								+ ", protocol: " + protocol + ", error code: " + friendAddStatus);
					if (userId != null) {
						user = this.roster.getUser(userId);
						if (user == null) {
							YahooProtocol yahooProtocol = getUserProtocol(protocol, userId);
							user = new YahooUser(userId, groupName, yahooProtocol);
						}
						else if (!friendAddStatus.equals("2"))
							log.warn("Adding friend failed and friend is in our roster: " + userId);
					}
					else
						user = null;
					final SessionFriendEvent se = new SessionFriendFailureEvent(this, user, groupName, friendAddStatus);
					this.eventDispatchQueue.append(se, ServiceType.FRIENDADD);
				}

				// userId = pkt.getValue("7");
				// groupName = pkt.getValue("65");
				// user = new YahooUser(userId, groupName);
			}
			else
				log.warn("Me: " + myName + " Add buddy attempt: " + this.primaryID + ", " + userId + " problem: "
						+ friendAddStatus + " not an ack: " + pkt.status);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("friend added", pkt, e);
		}
	}

	/**
	 * Process an incoming FRIENDREMOVE packet. We get one of these after we've sent a FRIENDREMOVE packet, as
	 * confirmation. It contains basic details of the friend we've deleted.
	 */
	protected void receiveFriendRemove(final YMSG9Packet pkt) // 0x84
	{
		String userId = null;
		String groupName = null;
		YahooUser user = null;
		try {
			if (pkt.status == 1) { // status 1 is an ack
				userId = pkt.getValue("7");
				groupName = pkt.getValue("65");
				user = this.roster.getUser(userId);
				user = new YahooUser(userId);
			}
			else {
				userId = pkt.getValue("7");
				// TODO: if this is a request to remove a user from one particular
				// group, and that same user exists in another group, this might go
				// terribly wrong...
				groupName = pkt.getValue("65");

				user = this.roster.getUser(userId);

				if (user == null) {
					log.info("Unable to remove a user that's not on the roster: " + userId);
					return;
				}
			}
			// Fire event : 7=friend, 66=status, 65=group name
			SessionFriendEvent se = new SessionFriendEvent(this, user, groupName);
			this.eventDispatchQueue.append(se, ServiceType.FRIENDREMOVE);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("friend removed", pkt, e);
		}
	}

	/**
	 * Process and incoming GOTGROUPRENAME packet.
	 */
	protected void receiveGroupRename(final YMSG9Packet pkt) // 0x13
	{
		try {
			final String oldName = pkt.getValue("67");
			final String newName = pkt.getValue("65");
			if (oldName == null || newName == null)
				return;

			log.trace("Received a GROUPRENAME packet for group '" + oldName + "' which should be renamed to '"
					+ newName + "'");

			final SessionGroupEvent se = new SessionGroupEvent(this, oldName, newName);
			this.eventDispatchQueue.append(se, ServiceType.GROUPRENAME);
			// TODO: use this event to update the group.
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("group rename", pkt, ex);
		}
	}

	/**
	 * Process an incoming IDACT packet.
	 */
	protected void receiveIdAct(final YMSG9Packet pkt) // 0x07
	{
		// FIX: do something here!
	}

	/**
	 * Process an incoming IDDEACT packet.
	 */
	protected void receiveIdDeact(final YMSG9Packet pkt) // 0x08
	{
		// Fix: do something here!
	}

	/**
	 * Process an incoming V6_STATUS_UPDATE packet. This most likely replaces {@link #receiveIsAway(YMSG9Packet)} and
	 * {@link #receiveIsBack(YMSG9Packet)}.
	 * 
	 * @param pkt
	 *            The V6_STATUS_UPDATE packet.
	 */
	protected void receiveStatusUpdate(final YMSG9Packet pkt) // 0xC6
	{
		updateFriendsStatus(pkt);
	}

	/**
	 * Process an incoming ISAWAY packet. See ISBACK below.
	 */
	protected void receiveIsAway(final YMSG9Packet pkt) // 0x03
	{
		// If this an update to a friend?
		if (pkt.exists("7"))
			updateFriendsStatus(pkt);
	}

	/**
	 * Process an incoming ISBACK packet. God alone knows what I'm supposed to do with this when the payload is empty!!
	 */
	protected void receiveIsBack(final YMSG9Packet pkt) // 0x04
	{
		if (pkt.exists("7"))
			updateFriendsStatus(pkt);
	}

	protected void receiveStatus15(final YMSG9Packet pkt) {
		updateFriendsStatus(pkt);
	}

	/**
	 * Process and incoming LIST packet. We'll typically get one of these when our logon is sucessful. (It should arrive
	 * before the LOGON packet.) Note: this packet can arrive in several parts. Taking a look at other third-party YMSG
	 * implemenations it would seem that the absence of cookies is the trait marking an incomplete packet.
	 */
	protected void receiveList(final YMSG9Packet pkt) // 0x55
	{
		// These fields will be concatenated, others will be appended
		String[] concatFields = { "87", "88", "89" };
		// Either cache or merge with cached packet
		if (this.cachePacket == null)
			this.cachePacket = pkt;
		else
			this.cachePacket.merge(pkt, concatFields);

		// Complete: this is the final packet
		if (pkt.exists("59"))
			_receiveList(this.cachePacket);
	}

	/**
	 * Handles a completely constructed LIST packet. LIST packets can come in several parts. These are being merged into
	 * one by {@link #receiveList(YMSG9Packet)}. The final, merged packet is then used to call this method.
	 * 
	 * @param pkt
	 *            A 'complete' LIST packet - the result of merging all fragmented LIST packets sent by the Yahoo
	 *            Network.
	 */
	private void _receiveList(final YMSG9Packet pkt) // 0x55
	{
		/*
		 * Friends list, each group is encoded as the group name (ie: "Friends") followed by a colon, followed by a
		 * comma separated list of friend ids, followed by a single \n (0x0a).
		 */
		try {

			final String grps = pkt.getValue("87"); // Value for key "87"
			if (grps != null) {
				final Map<String, YahooUser> usersOnList = new Hashtable<String, YahooUser>();
				final StringTokenizer st1 = new StringTokenizer(grps, "\n");
				int i = 0;
				this.groups = new YahooGroup[st1.countTokens()];
				// Extract each group.
				while (st1.hasMoreTokens()) {
					final String s1 = st1.nextToken();
					this.groups[i] = new YahooGroup(s1.substring(0, s1.indexOf(":")));
					// Store group name and decoded friends list
					final String groupId = s1.substring(0, s1.indexOf(":"));
					System.out.println(groupId);
					final StringTokenizer st2 = new StringTokenizer(s1.substring(s1.indexOf(":") + 1), ",");

					ArrayList<YahooUser> mUser = new ArrayList<YahooUser>();	//SirM2X: list of current users in the group

					// extract each user.
					while (st2.hasMoreTokens()) {
						final String userId = st2.nextToken();

						// see if we previously added this user (as part of
						// another group).
						YahooUser user = usersOnList.get(userId);
						if (user == null)
							// user wasn't created yet. Do it now.
							user = new YahooUser(userId);

						// add the user to the current group.
						user.addGroupId(groupId);

						usersOnList.put(userId, user);
						mUser.add(user);
					}

					i++;
					log.debug("add new group from list " + groupId);
				}

				if (!usersOnList.isEmpty()) {
					// create a Set of YahooUsers from the map.
					final Set<YahooUser> users = new HashSet<YahooUser>(usersOnList.values());

					// trigger listeners
					this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.Friends, users),
							ServiceType.LIST);
				}
			}
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("friends list in list", pkt, ex);
		}

		// Ignored list (people we don't want to hear from!)
		try {
			final String string = pkt.getValue("88"); // Value for key "88"
			if (string != null) {
				final Set<YahooUser> usersOnList = new HashSet<YahooUser>();

				// Comma separated list (?)
				final StringTokenizer st = new StringTokenizer(string, ",");
				while (st.hasMoreTokens()) {
					final String userId = st.nextToken();
					final YahooUser yu = new YahooUser(userId);
					yu.setIgnored(true);
					usersOnList.add(yu);
				}

				if (!usersOnList.isEmpty())
					// trigger listeners
					this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.Ignored, usersOnList),
							ServiceType.LIST);
			}
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("ignored list in list", pkt, ex);
		}

		// Identities list (alternative yahoo ids we can use!)
		try {
			String s = pkt.getValue("89"); // Value for key "89"
			if (s != null) {
				// Comma separated list (?)
				StringTokenizer st = new StringTokenizer(s, ",");
				this.identities.clear();
				while (st.hasMoreTokens()) {
					final String id = st.nextToken().toLowerCase();
					this.identities.put(id, new YahooIdentity(id));
				}
			}
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("identities list in list", pkt, ex);
		}

		// Stealth blocked list (people we don't want to see us!)
		try {
			final String string = pkt.getValue("185"); // Value for key "185"
			if (string != null) {
				final Set<YahooUser> usersOnList = new HashSet<YahooUser>();

				// Comma separated list (?)
				final StringTokenizer st = new StringTokenizer(string, ",");
				while (st.hasMoreTokens()) {
					final String userId = st.nextToken();
					final YahooUser yu = new YahooUser(userId);
					yu.setStealthBlocked(true);
					usersOnList.add(yu);
				}

				if (!usersOnList.isEmpty())
					// trigger listeners
					this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.StealthBlocked, usersOnList),
							ServiceType.LIST);
			}
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("ignored list in list", pkt, ex);
		}

		// Yahoo gives us three cookies, Y, T and C
		try {
			String[] ck = ConnectionHandler.extractCookies(pkt);
			this.cookieY = ck[NetworkConstants.COOKIE_Y]; // Y=<cookie>
			this.cookieT = ck[NetworkConstants.COOKIE_T]; // T=<cookie>
			this.cookieC = ck[NetworkConstants.COOKIE_C]; // C=<cookie>
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("cookies in list", pkt, ex);
		}

		// Primary identity: the *real* Yahoo ID for this account.
		// Only present if logging in under non-primary identity(?)
		try {
			if (pkt.exists("3"))
				this.primaryID = new YahooIdentity(pkt.getValue("3").trim());
			else
				this.primaryID = this.loginID;
		}
		catch (RuntimeException ex) {
			throw new YMSG9BadFormatException("primary identity in list", pkt, ex);
		}

		// Set the primary and login flags on the relevant YahooIdentity objects
		this.primaryID.setPrimaryIdentity(true);
		this.loginID.setLoginIdentity(true);
	}

	protected void receiveList15(final YMSG9Packet pkt) // 0x55
	{
		this.queueOfList15.add(pkt);
		if (pkt.status != 0)
			return;
		String username = null;
		YahooProtocol protocol = YahooProtocol.YAHOO;
		YahooGroup currentListGroup = null;
		ArrayList<YahooGroup> receivedGroups = new ArrayList<YahooGroup>();

		Set<YahooUser> usersOnFriendsList = new HashSet<YahooUser>();
		Set<YahooUser> usersOnIgnoreList = new HashSet<YahooUser>();
		Set<YahooUser> usersOnPendingList = new HashSet<YahooUser>();

		boolean isPending = false;
		boolean isStealth = false;

		for (YMSG9Packet qPkt : this.queueOfList15) {
			Iterator<String[]> iter = qPkt.entries().iterator();
			while (iter.hasNext()) {
				String[] s = iter.next();

				int key = Integer.valueOf(s[0]);
				String value = s[1];

				switch (key) {
					case 302:
						/*
						 * This is always 318 before a group, 319 before the first s/n in a group, 320 before any ignored
						 * s/n. It is not sent for s/n's in a group after the first. All ignored s/n's are listed last, so
						 * when we see a 320 we clear the group and begin marking the s/n's as ignored. It is always
						 * followed by an identical 300 key.
						 */
						if (value != null && value.equals("320"))
							currentListGroup = null;
						break;
					case 301:
						/* This is 319 before all s/n's in a group after the first. It is followed by an identical 300. */
						if (username != null) {
							YahooUser yu = null;
							if (currentListGroup != null) {
								for (YahooUser friend : usersOnFriendsList)
									if (friend.getId().equals(username)) {
										yu = friend;
										yu.addGroupId(currentListGroup.getName());
										if (!yu.getProtocol().equals(protocol)
												&& yu.getProtocol().equals(YahooProtocol.YAHOO)) {
											log.debug("Switching protocols because user is in list more that once: "
													+ yu.getId() + " from: " + yu.getProtocol() + " to: " + protocol);
											yu.update(protocol);
										}
									}
								if (yu == null) {
									/* This buddy is in a group */
									yu = new YahooUser(username, currentListGroup.getName(), protocol);
									if (isStealth)
										yu.setStealth(StealthStatus.STEALTH_PERMENANT);
									isStealth = false;
									usersOnFriendsList.add(yu);
								}
								currentListGroup.addUser(yu);
							}
							else {
								/* This buddy is on the ignore list (and therefore in no group) */
								yu = new YahooUser(username, new TreeSet<String>(), protocol);
								yu.setIgnored(true);
								usersOnIgnoreList.add(yu);
							}
							if (isPending)
								usersOnPendingList.add(yu);
//							if (isStealth)
//								yu.setStealth(StealthStatus.STEALTH_PERMENANT);
							username = null;
							isPending = false;
							//isStealth = false;
							protocol = YahooProtocol.YAHOO;
						}
						break;
					case 223: /* Pending add user request */
						isPending = true;
						break;
					case 300: /* This is 318 before a group, 319 before any s/n in a group, and 320 before any ignored s/n. */
						break;
					case 65: /* This is the group */
						currentListGroup = new YahooGroup(value);
						receivedGroups.add(currentListGroup);
						break;
					case 7: /* buddy's s/n */
						username = value;
						break;
					case 241: /* another protocol user */
						protocol = getUserProtocol(value, username);
						break;
					case 59: /* somebody told cookies come here too, but im not sure */
						break;
					case 317: /* Stealth Setting */
						isStealth = true;
						break;
				}
			}//end while

			if (username != null) {
				YahooUser yu = null;
				if (currentListGroup != null) {
					for (YahooUser friend : usersOnFriendsList)
						if (friend.getId().equals(username)) {
							yu = friend;
							yu.addGroupId(currentListGroup.getName());
						}
					if (yu == null) {
						/* This buddy is in a group */
						yu = new YahooUser(username, currentListGroup.getName(), protocol);
						usersOnFriendsList.add(yu);
					}
					currentListGroup.addUser(yu);
				}
				else {
					/* This buddy is on the ignore list (and therefore in no group) */
					yu = new YahooUser(username, new TreeSet<String>(), protocol);
					yu.setIgnored(true);
					usersOnIgnoreList.add(yu);
				}
				if (isPending)
					usersOnPendingList.add(yu);
				if (isStealth)
					yu.setStealth(StealthStatus.STEALTH_PERMENANT);
				username = null;
				isPending = false;
				isStealth = false;
				protocol = YahooProtocol.YAHOO;
			}


		}//end for

		if (!usersOnFriendsList.isEmpty())
			// receivedListFired = true;
			this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.Friends, usersOnFriendsList),
					ServiceType.LIST);

		if (!usersOnIgnoreList.isEmpty())
			this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.Ignored, usersOnIgnoreList),
					ServiceType.LIST);

		if (!usersOnPendingList.isEmpty())
			this.eventDispatchQueue.append(new SessionListEvent(this, ContactListType.Pending, usersOnPendingList),
					ServiceType.LIST);

		// Now that we've parsed the buddy list, we can consider login succcess
		this.sessionStatus = SessionState.LOGGED_ON;
		log.trace("Yahoo logged in successfully");

		// Only one identity with v16 login
		this.identities.put(this.loginID.getId(), new YahooIdentity(this.loginID.getId()));
		this.primaryID = this.loginID;

		// Set the primary and login flags on the relevant YahooIdentity objects
		this.primaryID.setPrimaryIdentity(true);
		this.loginID.setPrimaryIdentity(true);

		// Set initial presence to 'available'
		//CULPRIT : Ruining the initial status specified by the user
//		try {
//			setStatus(Status.AVAILABLE);
//		}
//		catch (java.io.IOException e) {
//			log.trace("Failed to set status to available");
//		}
	}

	/**
	 * Process an incoming LOGOFF packet. If we get one of these it means Yahoo wants to throw us off the system. When
	 * logging in using the same Yahoo ID using a second client, I noticed the Yahoo server sent one of these (just a
	 * header, no contents) and closed the socket.
	 */
	public void receiveLogoff(final YMSG9Packet pkt) // 0x02
	{
		try {
			// Is this packet about us, or one of our online friends?
			if (!pkt.exists("7")) // About us
			{
				log.info("Logging out because I received a logoff" + this.primaryID + "/" + pkt);
				// Note: when this method returns, the input thread loop
				// which called it exits.
				this.sessionStatus = SessionState.UNSTARTED;
				this.ipThread.stopMe();
				SessionLogoutEvent logoutEvent = new SessionLogoutEvent(AuthenticationState.YAHOO_LOGOFF);
				if (pkt.status == -1)
					logoutEvent = new SessionLogoutEvent(AuthenticationState.DUPLICATE_LOGIN);
				this.eventDispatchQueue.append(logoutEvent, ServiceType.LOGOFF);
				closeSession(false);
			}
			else
				// Process optional section, friends going offline
				try {
					updateFriendsStatus(pkt);
				}
			catch (Exception e) {
				throw new YMSG9BadFormatException("online friends in logoff", pkt, e);
			}
		}
		catch (IOException e) {
			log.error("error in receiveLogoff", e);
		}
	}

	/**
	 * Process an incoming LOGON packet. If we get one of these it means the logon process has been successful. If the
	 * user has friends already online, an extra section of varying length is appended, starting with a count, and then
	 * detailing each friend in turn.
	 * 
	 * @throws IOException
	 */
	protected void receiveLogon(final YMSG9Packet pkt) throws IOException // 0x01
	{
		try {
			// Is this packet about us, or one of our online friends?
			if (pkt.exists("7"))
				// Process optional section, friends currently online
				try {
					updateFriendsStatus(pkt);
				}
			catch (Exception e) {
				throw new YMSG9BadFormatException("online friends in logon", pkt, e);
			}
		}
		finally {
			if (this.sessionStatus != SessionState.LOGGED_ON) {
				// set inital presence state.
				setStatus(this.status);

				this.sessionStatus = SessionState.LOGGED_ON;
				this.eventDispatchQueue.append(ServiceType.LOGON);
			}
		}
	}

	/**
	 * Process an incoming MESSAGE packet. Message can be either online or offline, the latter having a datestamp of
	 * when they were sent.
	 */
	protected void receiveMessage(final YMSG9Packet pkt) // 0x06
	{
		try {
			if (!pkt.exists("14"))
				// Contains no message?
				return;

			if (pkt.status == Status.NOTINOFFICE.getValue()) {
				// Sent while we were offline
				int i = 0;
				// Read each message, until null
				while (pkt.getNthValue("31", i) != null) {
					final SessionEvent se;

					final String to = pkt.getNthValue("5", i);
					final String from = pkt.getNthValue("4", i);
					final String message = pkt.getNthValue("14", i);
					final String timestamp = pkt.getNthValue("15", i);

					if (timestamp == null || timestamp.length() == 0)
						se = new SessionEvent(this, to, from, message);
					else {
						final long timestampInMillis = 1000 * Long.parseLong(timestamp);
						se = new SessionEvent(this, to, from, message, timestampInMillis);
					}
					se.setStatus(pkt.status); // status!=0 means offline
					// message

					this.eventDispatchQueue.append(se, ServiceType.X_OFFLINE);
					i++;
				}
			}
			else {
				// Sent while we are online
				final String to = pkt.getValue("5");
				final String from = pkt.getValue("4");
				final String message = pkt.getValue("14");
				final String id = pkt.getValue("429");

				final SessionEvent se = new SessionEvent(this, to, from, message);
				if (se.getMessage().equalsIgnoreCase(NetworkConstants.BUZZ))
					this.eventDispatchQueue.append(se, ServiceType.X_BUZZ);
				else
					this.eventDispatchQueue.append(se, ServiceType.MESSAGE);

				if (id != null) {
					/*
					 * Send acknowledgement. If we don't do this then the official Yahoo Messenger client for Windows
					 * will send us the same message 7 seconds later as an offline message. This is true for at least
					 * version 9.0.0.2162 on Windows XP.
					 */
					PacketBodyBuffer body = new PacketBodyBuffer();
					body.addElement("1", this.loginID.getId());
					body.addElement("5", from);
					body.addElement("302", "430");
					body.addElement("430", id);
					body.addElement("303", "430");
					body.addElement("450", "0");
					sendPacket(body, ServiceType.MESSAGE_ACK, this.status);
				}
			}
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("message", pkt, e);
		}
	}

	protected void receiveMessageAck(final YMSG9Packet pkt)
	{
		String messageNumber = pkt.getValue("430"); //message number

		//remove from the resend queue
		synchronized(this.imQueue)
		{
			IM imToRemove = null;
			for(IM im : this.imQueue)
				if (im.getMessageNumberString().equals(messageNumber))
				{
					imToRemove = im;
					break;
				}
			if (imToRemove != null)
				this.imQueue.remove(imToRemove);
		}
	}

	/**
	 * Process an incoming NEWMAIL packet, informing us of how many unread Yahoo mail messages we have.
	 */
	protected void receiveNewMail(final YMSG9Packet pkt) // 0x0b
	{
		try {
			SessionNewMailEvent se;
			if (!pkt.exists("43"))
				se = new SessionNewMailEvent(this, pkt.getValue("9") // new
						// mail
						// count
						);
			else
				se = new SessionNewMailEvent(this, pkt.getValue("43"), // from
						pkt.getValue("42"), // email address
						pkt.getValue("18") // subject
						);
			this.eventDispatchQueue.append(se, ServiceType.NEWMAIL);
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("new mail", pkt, e);
		}
	}

	/**
	 * Process an incoming NOTIFY packet. "Typing" for example.
	 */
	protected void receiveNotify(final YMSG9Packet pkt) // 0x4b
	{
		try {
			// FIX: documentation says this should be Status.TYPING (0x16)
			if (pkt.status == 0x01) {
				SessionNotifyEvent se = SessionNotifyEvent.createSessionNotifyEvent(this, pkt.getValue("5"), // to
						pkt.getValue("4"), // from
						pkt.getValue("14"), // message (game)
						pkt.getValue("49"), // type (typing/game)
						pkt.getValue("13") // mode (on/off)
						);
				se.setStatus(pkt.status);
				this.eventDispatchQueue.append(se, ServiceType.NOTIFY);
			}
		}
		catch (Exception e) {
			throw new YMSG9BadFormatException("notify", pkt, e);
		}
	}

	/**
	 * Process and incoming USERSTAT packet.
	 */
	protected void receiveUserStat(final YMSG9Packet pkt) // 0x0a
	{
		try {
			this.status = Status.getStatus(pkt.status);
		}
		catch (IllegalArgumentException e) {
			// unknow status
		}
	}

	/**
	 * Process an error CHATLOGIN packet. The only time these seem to be sent is when we fail to connect to a chat room
	 * - perhaps because it is full (?)
	 */
	protected void erroneousChatLogin(final YMSG9Packet pkt) // 0x98
	{
		this.chatSessionStatus = SessionState.FAILED;
	}

	/**
	 * Note: the term 'packet' here refers to a YMSG message, not a TCP packet (although in almost all cases the two
	 * will be synonymous). This is to avoid confusion with a 'YMSG message' - the actual discussion packet.
	 * 
	 * service - the Yahoo service number status - the Yahoo status number (not sessionStatus above!) body - the payload
	 * of the packet
	 * 
	 * Note: it is assumed that the ConnectionHandler has been open()'d
	 */
	protected void sendPacket(final PacketBodyBuffer body, final ServiceType service, final Status status) throws IOException {
		log.debug("Sending packet on/to the network. SessionId[0x" + Long.toHexString(this.sessionId) + "] ServiceType["
				+ service + "] Status[" + status + "] Body[" + body + "]");
		this.network.sendPacket(body, service, status.getValue(), this.sessionId);
	}

	protected void sendPacket(final PacketBodyBuffer body, final ServiceType service) throws IOException {
		sendPacket(body, service, Status.AVAILABLE);
	}

	/**
	 * Convenience method - have we passed a given time? (More readable)
	 */
	private boolean past(final long time) {
		return (System.currentTimeMillis() > time);
	}

	/**
	 * Start threads
	 */
	private void openSession(final boolean createPingerTask, final boolean searchForAddress) throws IOException {
		// Open the socket, create input and output streams
		this.network.open(searchForAddress);
		// Create a thread to handle input from network
		initThread();
		// Add a TimerTask to periodically send ping packets for our connection
		if (createPingerTask) {
			this.pingerTask = new SessionPinger(this);
			this.scheduledPingerService = new Timer("OpenYMSG session ping timer", true);
			this.scheduledPingerService.schedule(this.pingerTask, NetworkConstants.KEEPALIVE_TIMEOUT_IN_SECS * 1000,
					NetworkConstants.KEEPALIVE_TIMEOUT_IN_SECS * 1000);
		}
	}

	protected void initThread() {
		this.ipThread = new InputThread(this);
		this.ipThread.start();
	}

	/**
	 * If the network isn't closed already, close it.
	 */
	private void closeSession(final boolean force) throws IOException {
		log.trace("close session");
		// Close the input thread (unless ipThread itself is calling us)
		this.sessionStatus = SessionState.UNSTARTED;
		if (this.ipThread != null && Thread.currentThread() != this.ipThread) {
			this.ipThread.stopMe();
			this.ipThread.interrupt();
			this.ipThread = null;
		}

		// Remove our pinger task from the timer
		if (this.pingerTask != null) {
			this.pingerTask.cancel();
			this.pingerTask = null;
		}

		// If the network is open, close it
		try {
			this.network.close();
		}
		finally {
			if (this.eventDispatchQueue != null) {
				this.eventDispatchQueue.kill();
				if(force)	// To indicate that the connection has been force closed
				{
					SessionExceptionEvent e = new SessionExceptionEvent(this, "Connection lost", new Exception(""));
					this.eventDispatchQueue.runEventNOW(new FireEvent(e, ServiceType.LOGOFF));
				}
				else
					this.eventDispatchQueue.runEventNOW(new FireEvent(null, ServiceType.LOGOFF));
				this.eventDispatchQueue = null;
			}
		}

	}

	/**
	 * Are we logged into Yahoo?
	 */
	protected void checkStatus() throws IllegalStateException {
		if (this.sessionStatus != SessionState.LOGGED_ON)
			throw new IllegalStateException("Not logged in");
	}

	private void checkChatStatus() throws IllegalStateException {
		if (this.chatSessionStatus != SessionState.LOGGED_ON)
			throw new IllegalStateException("Not logged in to a chatroom");
	}

	/**
	 * Preform a clean up of all data fields to 'reset' instance
	 */
	private void resetData() {
		this.primaryID = null;
		this.loginID = null;
		this.password = null;
		this.cookieY = null;
		this.cookieT = null;
		this.cookieC = null;
		this.imvironment = null;
		this.customStatusMessage = null;
		this.customStatusBusy = false;
		this.conferences.clear();
		removeSessionListener(this.roster);
		this.roster = null;
		this.identities.clear();
		this.loginException = null;
		this.timeoutCount = 0;
	}

	/**
	 * A key 16 was received, send an error message event
	 */
	void errorMessage(final YMSG9Packet pkt, String m) {
		if (m == null) m = pkt.getValue("16");
		SessionErrorEvent se = new SessionErrorEvent(this, m, pkt.service);
		if (pkt.exists("114")) se.setCode(Integer.parseInt(pkt.getValue("114").trim()));
		this.eventDispatchQueue.append(se, ServiceType.X_ERROR);
	}

	/**
	 * Handy method: alert application to exception via event
	 */
	void sendExceptionEvent(final Exception e, final String msg) {
		this.sessionStatus = SessionState.FAILED;
		SessionExceptionEvent se = new SessionExceptionEvent(Session.this, msg, e);
		this.eventDispatchQueue.append(se, ServiceType.X_EXCEPTION);
	}

	/**
	 * Chat logins sometimes use multiple packets. The clue is that incomplete packets carry a status of 5, and the
	 * final packet carries a status of 1. This method compounds incoming 0x98 packets and returns null until the last
	 * ('1') packet is delivered, when it returns the compounded packet.
	 * 
	 * @return null if the current packet being processed was not the final packet making up this login, otherwise the
	 *         compounded packet will be returned.
	 */
	private YMSG9Packet compoundChatLoginPacket(YMSG9Packet pkt) {
		if (pkt.status != 5 && pkt.status != 1)
			throw new IllegalArgumentException("Status must be either 1 or 5.");

		// Incomplete
		if (pkt.status == 5) {
			if (this.cachePacket == null)
				this.cachePacket = pkt;
			else
				this.cachePacket.append(pkt);
			return null;
		}

		// This is the last one making up the complete packet. Append and
		// return.
		if (this.cachePacket != null) {
			this.cachePacket.append(pkt);
			pkt = this.cachePacket;
			this.cachePacket = null;
		}
		return pkt;
	}

	/**
	 * LOGON packets can contain multiple friend status sections, ISAWAY and ISBACK packets contain only one. Update the
	 * YahooUser details and fire event.
	 */
	protected void updateFriendsStatus(final YMSG9Packet pkt) {
		// If LOGOFF packet, the packet's user status is wrong (available)
		final boolean logoff = (pkt.service == ServiceType.LOGOFF);
		// Process online friends data

		// Process each friend
		Iterator<String[]> iter = pkt.entries().iterator();
		long longStatus = 0;
		Boolean onChat = null;
		Boolean onPager = null;
		String visibility = null;
		String clearIdleTime = null;
		String idleTime = null;
		String customMessage = null;
		String customStatus = null;
		YahooProtocol protocol = YahooProtocol.YAHOO;
		String userId = null;
		while (iter.hasNext()) {
			String[] s = iter.next();

			int key = Integer.valueOf(s[0]);
			String value = s[1];
			// log.info("Key: " + key + ", value: " + value);
			switch (key) {
				case 300:
					// initial row, most of the time
					break;
				case 7:
					// check and see if we have one
					if (userId != null) {
						updateFriendStatus(logoff, userId, onChat, onPager, visibility, clearIdleTime, idleTime,
								customMessage, customStatus, longStatus, protocol, pkt.service);
						longStatus = 0;
						onChat = null;
						onPager = null;
						visibility = null;
						clearIdleTime = null;
						idleTime = null;
						customMessage = null;
						customStatus = null;
						userId = null;
						protocol = YahooProtocol.YAHOO;

					}
					userId = value;
					break;
				case 10:
					try {
						longStatus = Long.parseLong(value);
					}
					catch (NumberFormatException e) {
						customMessage = value;
					}
					break;
				case 17:
					onChat = value.equals("1");
					break;
				case 13: // one of these matters
					onPager = value.equals("1");
					visibility = value;
					break;
				case 19:
					customMessage = value;
					break;
				case 47:
					customStatus = value;
					break;
				case 138:
					clearIdleTime = value;
					break;
				case 241:
					protocol = getUserProtocol(value, userId);
					break;
				case 137:
					idleTime = value;
					break;
				case 301:
					// ending row, most of the time
					break;
			}
		}
		if (userId != null)
			updateFriendStatus(logoff, userId, onChat, onPager, visibility, clearIdleTime, idleTime, customMessage,
					customStatus, longStatus, protocol, pkt.service);

	}

	private void updateFriendStatus(boolean logoff, final String userId, final Boolean onChat, final Boolean onPager, final String visibility,
			final String clearIdleTime, final String idleTime, final String customMessage, final String customStatus, final long longStatus,
			final YahooProtocol protocol, final ServiceType type) {
		log.trace("UpdateFriendStatus arguments: logoff: " + logoff + ", user: " + userId + ", onChat: " + onChat
				+ ", onPager: " + onPager + ", visibility: " + visibility + ", clearIdleTime: " + clearIdleTime
				+ ", idleTime: " + idleTime + ", customMessage: " + customMessage + ", customStatus: " + customStatus
				+ ", longStatus: " + longStatus + ", protocol: " + protocol);
		Status newStatus = Status.AVAILABLE;
		YahooUser user = this.roster.getUser(userId);
		// When we add a friend, we get a status update before
		// getting a confirmation FRIENDADD packet (crazy!)
		if (user == null) {
			log.debug("Presence of a new friend seems to have arrived "
					+ "before the details of the new friend. Adding " + "them now: " + userId + "/" + protocol);
			// TODO: clean up the threading mess that can be caused by this.
			this.roster.dispatch(new FireEvent(new SessionFriendEvent(this, new YahooUser(userId, new TreeSet<String>(),
					protocol)), ServiceType.FRIENDADD));
			user = this.roster.getUser(userId);
		}

		if (user.getProtocol() == null || !user.getProtocol().equals(protocol))
			log.warn("In updateFriendStatus, Protocols do not match for user: " + user.getId() + " "
					+ user.getProtocol() + "/" + protocol);

		if (longStatus == -1 && (onPager == null || !onPager))
			// Offline -- usually federated or msn live
			logoff = true;
		try {
			newStatus = logoff ? Status.OFFLINE : Status.getStatus(longStatus);
		}
		catch (IllegalArgumentException e) {
			// unknown status
		}

		if (onChat != null)
			user.update(newStatus, onChat, onPager);
		else if (onPager != null)
			user.update(newStatus, visibility);
		else if (logoff)
			// logoff message doesn't have chat or pager info, but we reset those in this case.
			user.update(newStatus, false, false);
		else
			// status update with no chat, nor pager information, so leave those values alone.
			user.update(newStatus);
		if (customMessage != null)
			user.setCustom(customMessage, customStatus);

		if (clearIdleTime != null)
			user.setIdleTime(-1);
		if (idleTime != null)
			user.setIdleTime(Long.parseLong(idleTime));

		final SessionFriendEvent event = new SessionFriendEvent(this, user);
		// Fire event
		if (this.eventDispatchQueue != null)
			this.eventDispatchQueue.append(event, type);
	}

	/**
	 * Create chat user from a chat packet. Note: a YahooUser is created if necessary.
	 */
	private YahooChatUser createChatUser(final YMSG9Packet pkt, final int i) {
		pkt.generateQuickSetAccessors("109");

		final String userId = pkt.getNthValue("109", i);
		final YahooUser user;
		if (this.roster.containsUser(userId))
			user = this.roster.getUser(userId);
		else
			user = new YahooUser(userId);

		final int attributes = Integer.parseInt(pkt.getValueFromNthSetQA("113", i));
		final String alias = pkt.getValueFromNthSetQA("141", i); // optional
		final int age = Integer.parseInt(pkt.getValueFromNthSetQA("110", i));
		final String location = pkt.getValueFromNthSetQA("142", i); // optional

		return new YahooChatUser(user, attributes, alias, age, location);
	}

	/**
	 * Create a unique conference name
	 */
	private String getConferenceName(final String yid) {
		String uuid = UUID.randomUUID().toString();
		String conferencePart = uuid.substring(0, 12) + "--";
		return yid + "-" + conferencePart;
	}

	public YahooConference getConference(final String room) throws NoSuchConferenceException {
		YahooConference yc = this.conferences.get(room);
		if (yc == null)
			throw new NoSuchConferenceException("Conference " + room + " not found.");
		return yc;
	}

	YahooConference getOrCreateConference(final YMSG9Packet pkt) {
		String room = pkt.getValue("57");
		String message = pkt.getValue("58");
		YahooIdentity yid = this.identities.get(pkt.getValue("1").toLowerCase());
		YahooConference yc = this.conferences.get(room);
		if (yc == null) {
			yc = new YahooConference(yid, room, message, this);
			this.conferences.put(room, yc);
		}
		return yc;
	}

	/**
	 * Convenience method to add a event to the eventdispatcher (fire the event to all listeners).
	 * 
	 * @param event
	 *            The SessionEvent to be dispatched.
	 * @param type
	 *            Type of the event to be dispatched.
	 */
	void fire(final SessionEvent event, final ServiceType type) {
		if (event == null)
			throw new IllegalArgumentException("Argument 'event' cannot be null.");

		if (type == null)
			throw new IllegalArgumentException("Argument 'type' cannot be null.");

		this.eventDispatchQueue.append(event, type);
	}

	/**
	 * Returns the ID for this session object.
	 * 
	 * @return Session object ID.
	 */
	public long getSessionID() {
		return this.sessionId;
	}

	/**
	 * Sends out a request to receive a picture (avatar) from a contact
	 * 
	 * @param friend
	 *            The name of the contact for which a picture is requested.
	 * @throws IOException
	 */
	public void requestPicture(final String friend) throws IOException {
		final PacketBodyBuffer body = new PacketBodyBuffer();
		body.addElement("1", this.loginID.getId());
		body.addElement("5", friend);
		body.addElement("13", "1");
		sendPacket(body, ServiceType.PICTURE);
	}

	/**
	 * Processes an incoming 'PICTURE' packet, which contains avatar-like information of a contact.
	 * 
	 * @param pkt
	 *            The packet to parse.
	 */
	protected void receivePicture(final YMSG9Packet pkt) // 0xbe
	{
		final String imgUrlStr = pkt.getValue("20");

		if (imgUrlStr == null)
			return;

		InputStream imgIn = null;
		try {
			final URL imgUrl = new URL(imgUrlStr);

			imgIn = imgUrl.openStream();
			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			final byte[] buff = new byte[1024];
			int bytesRead;
			while ((bytesRead = imgIn.read(buff)) > 0)
				out.write(buff, 0, bytesRead);

			final SessionPictureEvent se = new SessionPictureEvent(this, pkt.getValue("5"), // to / us
					pkt.getValue("4"), // from
					out.toByteArray() // data
					);

			this.eventDispatchQueue.append(se, ServiceType.NOTIFY);

		}
		catch (MalformedURLException ex) {
			log.warn("Received a picture, but it appears to contain " + "an invalid image location.", ex);
		}
		catch (IOException ex) {
			log.warn("Received a picture, but reading its data caused " + "an unexpected exception.", ex);
		}
		finally {
			if (imgIn == null)
				return;
			try {
				imgIn.close();
			}
			catch (IOException ex) {
				log.warn("Unable to close the image stream object.", ex);
			}
		}
	}

	/**
	 * Sets the SessionPictureHandler for this session.
	 * 
	 * @param pictureHandler
	 */
	public void setSessionPictureHandler(final SessionPictureHandler pictureHandler) {
		this.pictureHandler = pictureHandler;
	}

	/**
	 * Returns the roster that contains the list of friends. The roster will be represented by a new object after each
	 * call to {@link Session#login(String, String)}
	 * 
	 * @return the roster that belongs to this Session
	 */
	public Roster getRoster() {
		return this.roster;
	}

	public YahooIdentity getLoginID() {
		return this.loginID;
	}

	public Collection<YahooConference> getConferences() {
		return Collections.unmodifiableCollection(this.conferences.values()) ;
	}



	private void trustEveryone() {
	        try {
	                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
	                        @Override
							public boolean verify(final String hostname, final SSLSession session) {
	                                return true;
	                        }});
	                SSLContext context = SSLContext.getInstance("TLS");
	                context.init(null, new X509TrustManager[]{new X509TrustManager(){
	                        @Override
							public void checkClientTrusted(final X509Certificate[] chain,
	                                        final String authType) throws CertificateException {}
	                        @Override
							public void checkServerTrusted(final X509Certificate[] chain,
	                                        final String authType) throws CertificateException {}
	                        @Override
							public X509Certificate[] getAcceptedIssuers() {
	                                return new X509Certificate[0];
	                        }}}, new SecureRandom());
	                HttpsURLConnection.setDefaultSSLSocketFactory(
	                                context.getSocketFactory());
	        } catch (Exception e) { // should never happen
	                e.printStackTrace();
	        }
	}
	
	public void setWaiting()
	{
		this.sessionStatus = SessionState.WAITING;
	}
	
	public void cancelWaiting()
	{
		this.sessionStatus = SessionState.UNSTARTED;
	}
}