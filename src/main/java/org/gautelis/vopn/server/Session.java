/*
 * Copyright (C) 2025 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.gautelis.vopn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Tracks state for a connected client, including pending writes and
 * authentication metadata.
 */
public class Session {
    private static final Logger log = LoggerFactory.getLogger(Session.class);

    // The selection key (logical read/write channel) for which this
    // session is associated.
    private final SelectionKey key;
    private final Connection connection;

    //
    private final Queue<ByteBuffer> pendingWrites = new LinkedList<>();

    // User identification stuff
    private String userId = null;
    private String credentials = null;

    // Authentication state
    private boolean authenticated = false;

    /**
     * Creates a new session bound to a selection key.
     *
     * @param key selection key for the socket channel
     * @param server owning server instance
     * @throws Exception if the connection cannot be initialized
     */
    /* package visible only */ Session(SelectionKey key, Server server) throws Exception {
        this.key = key;

        try {
            SocketChannel channel = (SocketChannel) key.channel();
            this.connection = new Connection(channel, server);

        } catch (Exception e) {
            String info = "Could not initiate session: ";
            info += e.getMessage();
            throw new Exception(info, e);
        }
    }

    /**
     * Queues data to be written back to the client.
     *
     * @param data buffer containing outgoing bytes
     */
    public void queueWrite(ByteBuffer data) {
        pendingWrites.add(data);
    }

    /**
     * Returns the queue of pending writes.
     *
     * @return queue of byte buffers
     */
    public Queue<ByteBuffer> getPendingWrites() {
        return pendingWrites;
    }

    /**
     * Returns the selection key for this session.
     *
     * @return selection key
     */
    public SelectionKey getKey() {
        return key;
    }

    /**
     * Returns the connection tied to this session.
     *
     * @return connection instance
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns the user id for this session if authenticated.
     *
     * @return user id or {@code null}
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Marks this session as authenticated.
     *
     * @param userId user identifier
     * @param credentials credentials used during authentication
     */
    void authenticate(String userId, String credentials) {
        this.userId = userId;
        this.credentials = credentials;

        // Very complicated authentication mechanism :-o
        authenticated = true;
    }

    /**
     * Returns whether the session has been authenticated.
     *
     * @return {@code true} if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
}
