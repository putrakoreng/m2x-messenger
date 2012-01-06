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
package org.openymsg.network.event;

import org.openymsg.network.FireEvent;

/**
 * Empty-method implementation of the {@link SessionListener} interface. A typical usage of this class would be to
 * extend it, and override just that one particular method that you're interested in.
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class SessionAdapter implements SessionListener {
    public void fileTransferReceived(final SessionFileTransferEvent event) {
        // override this function if you want to do something with it.
    }

    public void listReceived(final SessionListEvent event) {
        // override this function if you want to do something with it.
    }

    public void messageReceived(final SessionEvent event) {
        // override this function if you want to do something with it.
    }

    public void buzzReceived(final SessionEvent event) {
        // override this function if you want to do something with it.
    }

    public void offlineMessageReceived(final SessionEvent event) {
        // override this function if you want to do something with it.
    }

    public void errorPacketReceived(final SessionErrorEvent event) {
        // override this function if you want to do something with it.
    }

    public void inputExceptionThrown(final SessionExceptionEvent event) {
        // override this function if you want to do something with it.
    }

    public void newMailReceived(final SessionNewMailEvent event) {
        // override this function if you want to do something with it.
    }

    public void notifyReceived(final SessionNotifyEvent event) {
        // override this function if you want to do something with it.
    }

    public void contactRequestReceived(final SessionAuthorizationEvent event) {
        // override this function if you want to do something with it.
    }

    public void contactRejectionReceived(final SessionFriendRejectedEvent event) {
        // override this function if you want to do something with it.
    }

    public void contactAcceptedReceived(final SessionFriendAcceptedEvent event) {
        // override this function if you want to do something with it.
    }

    public void conferenceInviteReceived(final SessionConferenceInviteEvent event) {
        // override this function if you want to do something with it.
    }

    public void conferenceInviteDeclinedReceived(final SessionConferenceDeclineInviteEvent event) {
        // override this function if you want to do something with it.
    }

    public void conferenceLogonReceived(final SessionConferenceLogonEvent event) {
        // override this function if you want to do something with it.
    }

    public void conferenceLogoffReceived(final SessionConferenceLogoffEvent event) {
        // override this function if you want to do something with it.
    }

    public void conferenceMessageReceived(final SessionConferenceMessageEvent event) {
        // override this function if you want to do something with it.
    }
    
    public void friendSignedOff(final SessionFriendEvent event) {
        // override this function if you want to do something with it.
    }
    
    public void friendSignedOn(final SessionFriendEvent event) {
        // override this function if you want to do something with it.
    }

    public void friendsUpdateReceived(final SessionFriendEvent event) {
        // override this function if you want to do something with it.
    }

    public void friendsUpdateFailureReceived(final SessionFriendFailureEvent event) {
        // override this function if you want to do something with it.
    }

    public void friendAddedReceived(final SessionFriendEvent event) {
        // override this function if you want to do something with it.
    }

    public void friendRemovedReceived(final SessionFriendEvent event) {
        // override this function if you want to do something with it.
    }

    public void groupRenameReceived(final SessionGroupEvent event) {
        // override this function if you want to do something with it.
    }

    public void chatJoinReceived(final SessionChatEvent event) {
        // override this function if you want to do something with it.
    }

    public void chatExitReceived(final SessionChatEvent event) {
        // override this function if you want to do something with it.
    }

    public void chatMessageReceived(final SessionChatEvent event) {
        // override this function if you want to do something with it.
    }

    public void chatUserUpdateReceived(final SessionChatEvent event) {
        // override this function if you want to do something with it.
    }

    public void chatConnectionClosed(final SessionEvent event) {
        // override this function if you want to do something with it.
    }

    public void pictureReceived(final SessionPictureEvent ev) {
        // override this function if you want to do something with it.
    }

    // A chat captcha has been requested.
    // captchMessage - full message from captcha
    // captchaURL - captcha URL
    // lobby - YahooChatLobby connecting to
    public void chatCaptchaReceived(final SessionChatEvent ev) {
    }

    /**
     * Dispatches an event immediately to all listeners, instead of queuing. it.
     * 
     * @param event
     *            The event to be dispatched.
     */
    @Override
	public void dispatch(final FireEvent event) {
        final SessionEvent ev = event.getEvent();

        switch (event.getType()) {
        case LOGOFF:		//	if it is not about us, then it means that a contact has gone offline
        	if(ev instanceof SessionFriendEvent)
        		friendSignedOff((SessionFriendEvent)ev);
            break;
        case Y6_STATUS_UPDATE:	// a contact has changed status message
            friendsUpdateReceived((SessionFriendEvent) ev);
            break;
        case STATUS_15:		//a contact has logged on
            friendSignedOn((SessionFriendEvent) ev);
            break;
        case MESSAGE:
            messageReceived(ev);
            break;
        case X_OFFLINE:
            offlineMessageReceived(ev);
            break;
        case NEWMAIL:
            newMailReceived((SessionNewMailEvent) ev);
            break;
        case CONTACTNEW:
            contactRequestReceived((SessionAuthorizationEvent) ev);
            break;
        case CONFDECLINE:
            conferenceInviteDeclinedReceived((SessionConferenceDeclineInviteEvent) ev);
            break;
        case CONFINVITE:
            conferenceInviteReceived((SessionConferenceInviteEvent) ev);
            break;
        case CONFLOGON:
            conferenceLogonReceived((SessionConferenceLogonEvent) ev);
            break;
        case CONFLOGOFF:
            conferenceLogoffReceived((SessionConferenceLogoffEvent) ev);
            break;
        case CONFMSG:
            conferenceMessageReceived((SessionConferenceMessageEvent) ev);
            break;
        case FILETRANSFER:
            fileTransferReceived((SessionFileTransferEvent) ev);
            break;
        case NOTIFY:
            if (ev instanceof SessionNotifyEvent)
				notifyReceived((SessionNotifyEvent) ev);
			else	// this is a SessionPictureEvent
				pictureReceived((SessionPictureEvent) ev);
            break;
        case LIST:
            listReceived((SessionListEvent) ev);
            break;
        case FRIENDADD:
            SessionFriendEvent friendAddEvent = (SessionFriendEvent) ev;
            if (friendAddEvent.isFailure())
				friendsUpdateFailureReceived((SessionFriendFailureEvent) ev);
			else
				friendAddedReceived((SessionFriendEvent) ev);
            break;
        case FRIENDREMOVE:
            friendRemovedReceived((SessionFriendEvent) ev);
            break;
        case GOTGROUPRENAME:
            groupRenameReceived((SessionGroupEvent) ev);
            break;
        case GROUPRENAME:
            groupRenameReceived((SessionGroupEvent) ev);
            break;
        case CONTACTREJECT:
            contactRejectionReceived((SessionFriendRejectedEvent) ev);
            break;
        case CHATJOIN:
            chatJoinReceived((SessionChatEvent) ev);
            break;
        case CHATEXIT:
            chatExitReceived((SessionChatEvent) ev);
            break;
        case CHATDISCONNECT:
            chatConnectionClosed(ev);
            break;
        case CHATMSG:
            chatMessageReceived((SessionChatEvent) ev);
            break;
        case X_CHATUPDATE:
            chatUserUpdateReceived((SessionChatEvent) ev);
            break;
        case X_ERROR:
            errorPacketReceived((SessionErrorEvent) ev);
            break;
        case X_EXCEPTION:
            inputExceptionThrown((SessionExceptionEvent) ev);
            break;
        case X_BUZZ:
            buzzReceived(ev);
            break;
        case LOGON:
            logonReceived(ev);
            break;
        case X_CHATCAPTCHA:
            chatCaptchaReceived((SessionChatEvent) ev);
            break;
        case PICTURE:
            pictureReceived((SessionPictureEvent) ev);
            break;
        case Y7_AUTHORIZATION:
            if (ev instanceof SessionAuthorizationEvent)
				contactRequestReceived((SessionAuthorizationEvent) ev);
			else if (ev instanceof SessionFriendRejectedEvent)
				contactRejectionReceived((SessionFriendRejectedEvent) ev);
			else if (ev instanceof SessionFriendAcceptedEvent)
				contactAcceptedReceived((SessionFriendAcceptedEvent) ev);
			else
				throw new IllegalArgumentException("Don't know how to handle '" + event.getType() + "' event: " + event);
            break;
        default:
            throw new IllegalArgumentException("Don't know how to handle service type '" + event.getType() + "'");
        }
    }

    /**
     * @param ev
     */
    private void logonReceived(final SessionEvent ev) {
        // TODO Auto-generated method stub

    }
}
