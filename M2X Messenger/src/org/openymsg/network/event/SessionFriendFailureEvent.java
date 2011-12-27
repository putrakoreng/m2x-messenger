package org.openymsg.network.event;

import org.openymsg.network.YahooUser;

public class SessionFriendFailureEvent extends SessionFriendEvent {
    private static final long serialVersionUID = 3164873522561082665L;
    String failure;

    public SessionFriendFailureEvent(Object source, YahooUser user, String groupName, String failure) {
        super(source, user, groupName);
        this.failure = failure;
    }

    public boolean isFailure() {
        return true;
    }

    public String getFailure() {
        return failure;
    }

}
