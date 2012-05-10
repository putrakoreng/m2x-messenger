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
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openymsg.network.task.GetConnectionServer;

/**
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class DirectConnectionHandler extends ConnectionHandler {
    private String host; // Yahoo IM host

    private int port; // Yahoo IM port

    private boolean dontUseFallbacks = false; // Don't use fallback port

    private Socket socket; // Network connection

    private YMSG9InputStream ips; // For receiving messages

    private DataOutputStream ops; // For sending messages
    
    private  Integer socketSize;

    private static final Log log = LogFactory.getLog(DirectConnectionHandler.class);

    public DirectConnectionHandler(final String h, final int p, final Integer socketSize) {
        this.host = h;
        this.port = p;
        this.dontUseFallbacks = true;
        this.socketSize = socketSize;
    }

    public DirectConnectionHandler(final int p) {
        this(Util.directHost(), p, null);
    }

    public DirectConnectionHandler(final boolean fl) {
        this();
        this.dontUseFallbacks = fl;
    }

    public DirectConnectionHandler() {
        this(Util.directHost(), Util.directPort(), null);
        this.dontUseFallbacks = false;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    /**
     * Session calls this when a connection handler is installed
     */
    @Override
    void install(final Session ss) {
        // session=ss;
    }

    /**
     * Opens a socket to Yahoo IM, or throws an exception. If fallback ports are to be used, will attempt each port in
     * turn - upon failure will return the last exception (the one for the final port).
     */
    @Override
    void open(final boolean searchForAddress) throws SocketException, IOException {
        if (this.dontUseFallbacks) {
            if (searchForAddress) {
                GetConnectionServer getConnection = new GetConnectionServer();
                String otherHost = getConnection.getIpAddress();
                if (otherHost != null) {
                    log.info("Swapping host: " + otherHost);
                    this.host = otherHost;
                }
            }
            this.socket = new Socket(this.host, this.port);
            if (this.socketSize != null) {
                int oldSocketSize = this.socket.getReceiveBufferSize();
                this.socket.setReceiveBufferSize(this.socketSize);
                log.debug("Socket before: " + oldSocketSize + ", after: " + this.socket.getReceiveBufferSize());
            }
        }
        else {
            int[] fallbackPorts = Util.directPorts();
            int i = 0;
            while (this.socket == null)
				try {
                    this.socket = new Socket(this.host, fallbackPorts[i]);
                    this.port = fallbackPorts[i];
                }
                catch (SocketException e) {
                    this.socket = null;
                    i++;
                    if (i >= fallbackPorts.length) throw e;
                }
        }
        log.debug("Source socket: " + this.socket.getLocalSocketAddress() + " yahoo socket: " + this.socket.getInetAddress() + ":"
                + this.socket.getPort());
        this.ips = new YMSG9InputStream(this.socket.getInputStream());
        this.ops = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
    }

    @Override
    void close() throws IOException {
        if (this.socket != null) this.socket.close();
        this.socket = null;
        this.ips = null;
        this.ops = null;
    }

    /**
     * Note: the term 'packet' here refers to a YMSG message, not a TCP packet (although in almost all cases the two
     * will be synonymous). This is to avoid confusion with a 'YMSG message' - the actual discussion packet.
     * 
     * service - the Yahoo service number status - the Yahoo status number (not sessionStatus!) body - the payload of
     * the packet
     * 
     * Note: it is assumed that 'ops' will have been set by the time this method is called.
     */
    @Override
    protected void sendPacket(final PacketBodyBuffer body, final ServiceType service, final long status, final long sessionId)
            throws IOException {
        byte[] b = body.getBuffer();
        // Because the buffer is held at class member level, this method
        // is not automatically thread safe. Besides, we should be only
        // sending one message at a time!
        synchronized (this.ops) {
            // 20 byte header
            this.ops.write(NetworkConstants.MAGIC, 0, 4); // Magic code 'YMSG'
            this.ops.write(NetworkConstants.VERSION, 0, 4); // Version
            this.ops.writeShort(b.length & 0xFFFF); // Body length (16 bit unsigned)
            this.ops.writeShort(service.getValue() & 0xFFFF); // Service ID (16
            // bit unsigned
            this.ops.writeInt((int) (status & 0xFFFFFFFF)); // Status (32 bit
            // unsigned)
            this.ops.writeInt((int) (sessionId & 0xFFFFFFFF)); // Session id (32
            // bit unsigned)
            // Then the body...
            this.ops.write(b, 0, b.length);
            // Now send the buffer
            this.ops.flush();
        }
    }

    /**
     * Return a Yahoo message
     */
    @Override
    protected YMSG9Packet receivePacket() throws IOException {
        return this.ips.readPacket();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Direct connection: ").append(this.host).append(":").append(this.port);
        return sb.toString();
    }

    /**
     * Allow changing the host to open a new connection
     * @return ip address
     */
    public void setHost(final String host) {
        this.host = host;
        try {
            close();
        } catch (Exception ex) {
            // silently fail;
        }
    }
}
