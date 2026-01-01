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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Wrapper around a {@link SocketChannel} providing convenience operations
 * for socket configuration and IO.
 */
public final class Connection implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Connection.class);

    private static final int INPUT_STREAM_BLOCK_SIZE = 8192;
    private static final int OUTPUT_STREAM_BLOCK_SIZE = 65536;

    private final SocketChannel channel;
    private final Server server;

    //
    private int inputBlockSize;
    private int outputBlockSize;

    /**
     * Creates a new connection wrapper around a socket channel.
     *
     * @param channel connected socket channel
     * @param server owning server instance
     */
    public Connection(SocketChannel channel, Server server) {
        this.channel = channel;
        this.server = server;

        inputBlockSize = INPUT_STREAM_BLOCK_SIZE;
        outputBlockSize = OUTPUT_STREAM_BLOCK_SIZE;
    }

    /**
     * Gets the socket channel associated with this connection.
     */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Returns the local bind address for this connection.
     *
     * @return local address or {@code null} if unavailable
     */
    public InetAddress getLocalAddress() {
        if (channel != null) {
            return channel.socket().getLocalAddress();
        }
        return null;
    }

    /**
     * Returns the local port for this connection.
     *
     * @return local port or 0 if unavailable
     */
    public int getLocalPort() {
        if (channel != null) {
            return channel.socket().getLocalPort();
        }
        return 0;
    }

    /**
     * Returns the peer address for this connection.
     *
     * @return peer address or {@code null} if unavailable
     */
    public InetAddress getPeerAddress() {
        if (channel != null) {
            return channel.socket().getInetAddress();
        }
        return null;
    }

    /**
     * Returns the peer port for this connection.
     *
     * @return peer port or 0 if unavailable
     */
    public int getPeerPort() {
        if (channel != null) {
            return channel.socket().getPort();
        }
        return 0;
    }

    /**
     * Explicitly disconnects (close socket).
     */
    public void disconnect() throws IOException {
        close();
    }

    /**
     * Explicitly disconnects and closes connection.
     */
    @Override
    public void close() throws IOException {
        if (channel != null) {

            String host;
            try {
                InetAddress remoteAddress = channel.socket().getInetAddress();
                host = remoteAddress.getHostAddress();

            } catch (Exception e) {
                host = "already disconnected";
            }

            try {
                channel.close();

            } finally {
                log.info("Disconnected from {}", host);
            }
        }
    }

    /**
     * Returns whether the underlying channel is connected.
     *
     * @return {@code true} if connected
     */
    public boolean isConnected() {
        return null != channel && channel.isConnected();
    }

    /**
     * Returns the receive buffer size hint.
     *
     * @return receive buffer size in bytes
     */
    public int getReceiveBufferSize() {
        if (inputBlockSize > 0) {
            return inputBlockSize;
        } else {
            if (channel != null) {
                try {
                    return channel.socket().getReceiveBufferSize();
                } catch (SocketException ignore) {
                }
            }
            return 1460;
        }
    }

    /**
     * Returns the send buffer size hint.
     *
     * @return send buffer size in bytes
     */
    public int getSendBufferSize() {
        if (outputBlockSize > 0) {
            return outputBlockSize;
        } else {
            if (channel != null) {
                try {
                    return channel.socket().getSendBufferSize();
                } catch (SocketException ignore) {
                }
            }
            return 1460;
        }
    }

    /**
     * Enable Nagle algorithm.
     * <p>
     * When the Nagle algorithm is enabled, individual write(s)
     * to a socket may not result in a TCP-packet. Especially
     * when writing small amounts of data, the data is buffered
     * (with a 0.2 seconds timeout) in order to avoid gross
     * network traffic.
     */
    public void enableNagle() throws SocketException {
        if (channel != null) {
            channel.socket().setTcpNoDelay(false);
        }
    }

    /**
     * Disable Nagle algorithm.
     * <p>
     * When the Nagle algorithm is disabled, individual write(s)
     * to a socket will result in individual TCP-packets. This
     * will increase the network traffic, but the buffering
     * mechanisms (with that lousy 0.2 seconds timeout) is avoided.
     */
    public void disableNagle() throws SocketException {
        if (channel != null) {
            channel.socket().setTcpNoDelay(true);
        }
    }

    /**
     * Write ByteBuffer to the connection.
     */
    public long write(ByteBuffer buffer) throws IOException {
        return channel.write(buffer);
    }

    /**
     * Read data into a ByteBuffer from the connection.
     * Reads up to the buffer limit, so ensure the limit is set explicitly.
     * Returns -1 if end-of-stream.
     */
    public int read(ByteBuffer buffer) throws IOException {

        int totalBytesRead = 0;

        while (buffer.hasRemaining()) {
            int bytesRead = channel.read(buffer);

            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
            } else if (bytesRead == -1) {
                if (totalBytesRead == 0) {
                    return -1; // proper EOF
                } else {
                    break; // got some data, but connection closed after
                }
            } else {
                break; // nothing read, don't block
            }
        }

        buffer.flip();
        return totalBytesRead;
    }

    /**
     * Returns a string representation of the underlying channel.
     *
     * @return channel description or {@code null}
     */
    public String toString() {
        if (channel != null) {
            return channel.toString();
        }
        return null;
    }
}

