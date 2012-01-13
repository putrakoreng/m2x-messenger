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

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openymsg.network.event.SessionConferenceInviteEvent;

/**
 * Thread for handling network input, dispatching incoming packets to appropriate methods based upon service id.
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class InputThread extends Thread {
    private volatile boolean quit = false; // Exit run in J2 compliant way

    protected final Session parentSession;

    private static final Log log = LogFactory.getLog(InputThread.class);

    /**
     * Constructs a new thread that starts processing immediately.
     * 
     * @param parentSession
     *            the parent session of this thread.
     */
    public InputThread(final Session parentSession) {
        super("jYMSG Input " + parentSession.getLoginID().getId());
        this.parentSession = parentSession;
    }

    /**
     * Stops the thread from running.
     */
    public void stopMe() {
        this.quit = true;
    }

    /**
     * Accept packets and send them for processing. Dies when (a) a LOGOFF packet sets quit, or (b) a null packet is
     * sent to process().
     */
    @Override
    public void run() {
        while (!this.quit)
			try {
                process(this.parentSession.network.receivePacket());
            }
            catch (UnknowServiceException e) {
                log.warn("unknow packet: " + e.getPacket().toString());
            }
            catch (Exception e) {
                // ignore SocketExceptions if we're closing the thread.
                if (this.quit && e instanceof SocketException) {
                    log.debug("logging out so don't handle exception", e);
                    return;
                }

                log.error("error on process packet", e);
                try {
                    this.parentSession.sendExceptionEvent(e, "Source: InputThread");
                }
                catch (Exception e2) {
                    log.error("error on sendException to the session", e2);
                }

                // IO exceptions? Close the connection!
                if (e instanceof IOException)
					this.quit = true;
            }
    }

    /**
     * Switch on packet type to handler code
     * 
     * @param pkt
     * @throws Exception
     */
    private void process(final YMSG9Packet pkt) throws Exception {
        // A null packet is sent when the input stream closes
        if (pkt == null) {
            this.quit = true;
            return;
        }

        // Process header
        if (pkt.sessionId != 0)
			// Some chat packets send zero
            // log.trace("Received a packet - status: " + pkt.status + " service: " + pkt.service.getValue() +
            // " packet:" + pkt);
            // Update session id in outer class
            this.parentSession.sessionId = pkt.sessionId;

        // Error header?
        if (pkt.status == -1 && processError(pkt) == true)
			return;

        log.trace("Incoming packet: " + pkt);

        // Process payload
        processPayload(pkt);
    }

    protected void processPayload(final YMSG9Packet pkt) throws IOException, YahooException {
        log.trace("processPayload " + pkt.service + "/" + pkt.status);
        switch (pkt.service) {
        case ADDIGNORE:
            this.parentSession.receiveAddIgnore(pkt);
            break;
        case AUTH:
            this.parentSession.receiveAuth(pkt);
            break;
        case AUTHRESP:
            this.parentSession.receiveAuthResp(pkt);
            break;
        case CHATCONNECT:
            this.parentSession.receiveChatConnect(pkt);
            break;
        case CHATDISCONNECT:
            this.parentSession.receiveChatDisconnect(pkt);
            break;
        case CHATEXIT:
            this.parentSession.receiveChatExit(pkt);
            break;
        case CHATJOIN:
            this.parentSession.receiveChatJoin(pkt);
            break;
        case CHATMSG:
            this.parentSession.receiveChatMsg(pkt);
            break;
        case CHATPM:
            this.parentSession.receiveChatPM(pkt);
            break;
        case CONFADDINVITE:
            receiveConfAddInvite(pkt);
            break;
        case CONFDECLINE:
            this.parentSession.receiveConfDecline(pkt);
            break;
        case CONFINVITE:
            receiveConfInvite(pkt);
            break;
        case CONFLOGOFF:
            this.parentSession.receiveConfLogoff(pkt);
            break;
        case CONFLOGON:
            this.parentSession.receiveConfLogon(pkt);
            break;
        case CONFMSG:
            this.parentSession.receiveConfMsg(pkt);
            break;
        case CONTACTIGNORE:
            this.parentSession.receiveContactIgnore(pkt);
            break;
        case CONTACTNEW:
            this.parentSession.receiveContactNew(pkt);
            break;
        case FILETRANSFER:
            this.parentSession.receiveFileTransfer(pkt);
            break;
        case FRIENDADD:
            this.parentSession.receiveFriendAdd(pkt);
            break;
        case FRIENDREMOVE:
            this.parentSession.receiveFriendRemove(pkt);
            break;
        case GOTGROUPRENAME:
            this.parentSession.receiveGroupRename(pkt);
            break;
        case IDACT:
            this.parentSession.receiveIdAct(pkt);
            break;
        case IDDEACT:
            this.parentSession.receiveIdDeact(pkt);
            break;
//        case ISAWAY:
//            parentSession.receiveIsAway(pkt);
//            break;
//        case ISBACK:
//            parentSession.receiveIsBack(pkt);
//            break;
        case LIST:
            this.parentSession.receiveList(pkt);
            break;
        case LIST_15:
            this.parentSession.receiveList15(pkt);
            break;
        case LOGOFF:
            this.parentSession.receiveLogoff(pkt);
            break;
        case LOGON:
            this.parentSession.receiveLogon(pkt);
            break;
        case MESSAGE:
            this.parentSession.receiveMessage(pkt);
            break;
        case MESSAGE_ACK:
        	this.parentSession.receiveMessageAck(pkt);
        	break;
        case NEWMAIL:
            this.parentSession.receiveNewMail(pkt);
            break;
        case NOTIFY:
            this.parentSession.receiveNotify(pkt);
            break;
        case USERSTAT:
            this.parentSession.receiveUserStat(pkt);
            break;
        case Y6_STATUS_UPDATE:
        	this.parentSession.receiveStatusUpdate(pkt);
            break;
        case STATUS_15:
            this.parentSession.receiveStatus15(pkt);
            break;
        case GROUPRENAME:
            this.parentSession.receiveGroupRename(pkt);
            break;
        case CONTACTREJECT:
            this.parentSession.receiveContactRejected(pkt);
            break;
        case PICTURE:
            this.parentSession.receivePicture(pkt);
            break;
        case Y7_AUTHORIZATION:
            this.parentSession.receiveAuthorization(pkt);
            break;
        case PING:
            // As we're sending pings back, it's probably safe to ignore the
            // incoming pings from Yahoo.
            log.debug("Received PING (but ignoring it).");
            break;
        case UNKNOWN002:
            // As we're sending pings back, it's probably safe to ignore the
            // incoming pings from Yahoo.
            log.debug("Received 239 (but ignoring it).");
            break;
        default:
            log.info("Don't know how to handle service type '" + pkt.service.getValue()
                    + "'. The original packet was: " + pkt.toString());
        }
    }

    /**
     * Called when status == -1. Returns true if no further processing is required (process() returns) otherwise false
     * (process() continues).
     * 
     * @param pkt
     * @return
     * @throws Exception
     */
    private boolean processError(final YMSG9Packet pkt) throws Exception {
        // Jump to service-specific code
        switch (pkt.service) {
        case AUTHRESP:
            this.parentSession.receiveAuthResp(pkt);
            return true;
        case CHATJOIN:
            this.parentSession.receiveChatJoin(pkt);
            return true;
        case LOGOFF:
            this.parentSession.receiveLogoff(pkt);
            return true;
        default:
            this.parentSession.errorMessage(pkt, null);
            return (pkt.body.length <= 2);
        }
    }

    /**
     * Process an incoming CONFINVITE packet. We get one of these when we are being invited to join someone else's
     * existing conference. To all intent and purpose this (I assume) is the same as a regular invite packet, except it
     * is only delivered to one source, not everyone on the list (?)
     * 
     * @param pkt
     */
    private void receiveConfAddInvite(final YMSG9Packet pkt) // 0x01c
    {
        final YahooConference yc = this.parentSession.getOrCreateConference(pkt);
        if (yc.isClosed())
			yc.reopenConference();
        receiveConfInvite(pkt);
    }

    /**
     * Process an incoming CONFINVITE packet. We get one of these when we are being invited to join someone else's
     * conference. Note: it is possible for conference packets (ie: logon) can arrive before the invite. These are
     * buffered until the invite is received.
     * 
     * @param pkt
     */
    private void receiveConfInvite(final YMSG9Packet pkt) // 0x18
    {
        try {
            final String[] invitedUserIds = pkt.getValues("52");
            final String[] currentUserIds = pkt.getValues("53");
            if (invitedUserIds.length == 0 && currentUserIds.length == 0)
            {
                log.debug("Correctly not handling empty invite: " + pkt);
                return;
            }
            final YahooConference yc = this.parentSession.getOrCreateConference(pkt);
            String otherInvitedUserIdsCommaSeparated = pkt.getValue("51");
            String to = pkt.getValue("1");
            String from = pkt.getValue("50");
            Set<YahooUser> invitedUsers = getUsers(invitedUserIds);
            Set<YahooUser> currentUsers = getUsers(currentUserIds);
            Set<YahooUser> otherInvitedUsers = getUsers(otherInvitedUserIdsCommaSeparated);
            invitedUsers.addAll(otherInvitedUsers);
            
            // Create event
            SessionConferenceInviteEvent se = new SessionConferenceInviteEvent(this, to,
                    from, yc, invitedUsers, currentUsers);
            // Add the users
            yc.addUsers(invitedUserIds);
            yc.addUsers(currentUserIds);
            yc.addUser(from);
            // Fire invite event
            if (!yc.isClosed()) // Should never be closed for invite!
                this.parentSession.fire(se, ServiceType.CONFINVITE);
            // Set invited status and work through buffered conference
            synchronized (yc) {
                Queue<YMSG9Packet> buffer = yc.inviteReceived();
                for (YMSG9Packet packet : buffer)
					process(packet);
            }
        }
        catch (Exception e) {
            throw new YMSG9BadFormatException("conference invite", pkt, e);
        }
    }

    private Set<YahooUser> getUsers(final String otherInvitedUserIdsCommaSeparated) {
        if (otherInvitedUserIdsCommaSeparated == null 
                || otherInvitedUserIdsCommaSeparated.length() == 0)
			return Collections.emptySet();
        String[] ids = otherInvitedUserIdsCommaSeparated.split(",");
        return getUsers(ids);
    }

    private Set<YahooUser> getUsers(final String[] users) {
        final Set<YahooUser> conferenceUsers = new HashSet<YahooUser>();
        for (final String userId : users) {
            YahooUser user = this.parentSession.getRoster().getUser(userId);
            if (user == null)
				user = new YahooUser(userId);
            conferenceUsers.add(user);
        }
        return conferenceUsers;
    }
}
