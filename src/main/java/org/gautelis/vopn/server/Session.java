/*
 * Copyright (C) 2025-2026 Frode Randers
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

    public void queueWrite(ByteBuffer data) {
        pendingWrites.add(data);
    }

    public Queue<ByteBuffer> getPendingWrites() {
        return pendingWrites;
    }

    public SelectionKey getKey() {
        return key;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getUserId() {
        return userId;
    }

    void authenticate(String userId, String credentials) {
        this.userId = userId;
        this.credentials = credentials;

        // Very complicated authentication mechanism :-o
        authenticated = true;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
