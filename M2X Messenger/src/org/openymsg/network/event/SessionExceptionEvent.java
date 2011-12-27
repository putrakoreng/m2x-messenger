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

/**
 * This class is used to pass exceptions from the input thread to the main application.
 * 
 * Message Exception inputExceptionThrown y y
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class SessionExceptionEvent extends SessionEvent {
    private static final long serialVersionUID = -5999099588327589758L;
    protected Exception exception;

    /**
     * CONSTRUCTORS
     */
    public SessionExceptionEvent(Object o, String m, Exception e) {
        super(o);
        message = m;
        exception = e;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Exception: message=\"").append(message).append("\" type=").append(
                exception.toString());
        return sb.toString();
    }
}
