/*
 * Copyright (C) 2026 Frode Randers
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
 */
package org.gautelis.vopn.server;

import org.junit.Test;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

    @Test
    public void testReadReturnsDataThenEOF() throws Exception {
        DummySocketChannel channel = new DummySocketChannel(
                new byte[][] { "hi".getBytes() }
        );
        Connection connection = new Connection(channel, new NoopServer());
        ByteBuffer buffer = ByteBuffer.allocate(16);

        int bytesRead = connection.read(buffer);

        assertEquals(2, bytesRead);
        byte[] actual = new byte[bytesRead];
        buffer.get(actual);
        assertEquals("hi", new String(actual));
    }

    @Test
    public void testReadEOFReturnsMinusOne() throws Exception {
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {});
        Connection connection = new Connection(channel, new NoopServer());
        ByteBuffer buffer = ByteBuffer.allocate(8);

        int bytesRead = connection.read(buffer);

        assertEquals(-1, bytesRead);
    }

    @Test
    public void testBuffersReturnDefaults() {
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {});
        Connection connection = new Connection(channel, new NoopServer());

        assertEquals(8192, connection.getReceiveBufferSize());
        assertEquals(65536, connection.getSendBufferSize());
    }

    @Test
    public void testRequestExposesSessionAndConnection() throws Exception {
        DummySelector selector = new DummySelector();
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {});
        SelectionKey key = new DummySelectionKey(channel, selector);

        Session session = new Session(key, new NoopServer());
        Request request = new Request(session);

        assertSame(session, request.getSession());
        assertSame(session.getConnection(), request.getConnection());
        assertSame(key, request.getKey());
    }

    @Test
    public void testNagleToggle() throws Exception {
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {});
        Connection connection = new Connection(channel, new NoopServer());

        connection.disableNagle();
        assertTrue(channel.socket().getTcpNoDelay());

        connection.enableNagle();
        assertFalse(channel.socket().getTcpNoDelay());
    }

    private static final class NoopServer implements Server {
        @Override
        public void requestShutdown(String reason) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stopping() {
        }

        @Override
        public void stopped() {
        }
    }

    private static final class DummySelector extends Selector {
        private boolean open = true;

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public SelectorProvider provider() {
            return SelectorProvider.provider();
        }

        @Override
        public Set<SelectionKey> keys() {
            return Collections.emptySet();
        }

        @Override
        public Set<SelectionKey> selectedKeys() {
            return Collections.emptySet();
        }

        @Override
        public int selectNow() {
            return 0;
        }

        @Override
        public int select(long timeout) {
            return 0;
        }

        @Override
        public int select() {
            return 0;
        }

        @Override
        public Selector wakeup() {
            return this;
        }

        @Override
        public void close() {
            open = false;
        }
    }

    private static final class DummySelectionKey extends SelectionKey {
        private final SocketChannel channel;
        private final Selector selector;
        private boolean valid = true;

        private DummySelectionKey(SocketChannel channel, Selector selector) {
            this.channel = channel;
            this.selector = selector;
        }

        @Override
        public SocketChannel channel() {
            return channel;
        }

        @Override
        public Selector selector() {
            return selector;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public void cancel() {
            valid = false;
        }

        @Override
        public int interestOps() {
            return 0;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            return this;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }

    private static final class DummySocketChannel extends SocketChannel {
        private final DummySocket socket;
        private final byte[][] chunks;
        private int chunkIndex;
        private int chunkOffset;

        private DummySocketChannel(byte[][] chunks) {
            super(SelectorProvider.provider());
            this.socket = new DummySocket();
            this.chunks = chunks;
        }

        @Override
        public DummySocket socket() {
            return socket;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isConnectionPending() {
            return false;
        }

        @Override
        public boolean connect(SocketAddress remote) {
            return false;
        }

        @Override
        public boolean finishConnect() {
            return false;
        }

        @Override
        public SocketChannel bind(SocketAddress local) {
            return this;
        }

        @Override
        public <T> SocketChannel setOption(java.net.SocketOption<T> name, T value) {
            return this;
        }

        @Override
        public <T> T getOption(java.net.SocketOption<T> name) {
            return null;
        }

        @Override
        public Set<java.net.SocketOption<?>> supportedOptions() {
            return Collections.emptySet();
        }

        @Override
        public SocketChannel shutdownInput() {
            return this;
        }

        @Override
        public SocketChannel shutdownOutput() {
            return this;
        }

        @Override
        public int read(ByteBuffer dst) {
            if (chunkIndex >= chunks.length) {
                return -1;
            }
            if (!dst.hasRemaining()) {
                return 0;
            }
            byte[] chunk = chunks[chunkIndex];
            int remaining = chunk.length - chunkOffset;
            int toCopy = Math.min(dst.remaining(), remaining);
            dst.put(chunk, chunkOffset, toCopy);
            chunkOffset += toCopy;
            if (chunkOffset >= chunk.length) {
                chunkIndex++;
                chunkOffset = 0;
            }
            return toCopy;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) {
            long total = 0;
            for (int i = offset; i < offset + length; i++) {
                total += read(dsts[i]);
            }
            return total;
        }

        @Override
        public int write(ByteBuffer src) {
            int remaining = src.remaining();
            src.position(src.limit());
            return remaining;
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) {
            long total = 0;
            for (int i = offset; i < offset + length; i++) {
                total += write(srcs[i]);
            }
            return total;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        protected void implCloseSelectableChannel() {
        }

        @Override
        protected void implConfigureBlocking(boolean block) {
        }
    }

    private static final class DummySocket extends Socket {
        private boolean tcpNoDelay = true;

        @Override
        public void setTcpNoDelay(boolean on) {
            tcpNoDelay = on;
        }

        @Override
        public boolean getTcpNoDelay() {
            return tcpNoDelay;
        }

        @Override
        public InetAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetAddress getInetAddress() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public int getPort() {
            return 0;
        }
    }
}
