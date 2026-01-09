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

import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RequestProcessorTest {

    @Test
    public void testHandleReadQueuesWriteAndInterest() throws Exception {
        byte[] payload = "ping".getBytes();
        DummySocketChannel channel = new DummySocketChannel(new byte[][] { payload }, Integer.MAX_VALUE);
        TestRequestProcessor processor = new TestRequestProcessor();

        SelectorQueue selectorQueue = new SelectorQueue();
        processor.initialize(new NoopServer(), new RequestQueue(), selectorQueue);

        Session session = sessionWith(channel, selectorQueue);
        Request request = new Request(session);

        processor.handleRead(request);

        Queue<ByteBuffer> pendingWrites = session.getPendingWrites();
        assertEquals(1, pendingWrites.size());
        ByteBuffer buffer = pendingWrites.peek();
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertEquals("ping", new String(actual));

        SelectorTask first = selectorQueue.remove();
        SelectorTask second = selectorQueue.remove();
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.isAdditive());
        assertTrue(second.isAdditive());
        assertEquals(SelectionKey.OP_WRITE, first.getInterest());
        assertEquals(SelectionKey.OP_READ, second.getInterest());
    }

    @Test
    public void testHandleReadEOFAddsNoInterest() throws Exception {
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {}, Integer.MAX_VALUE);
        TestRequestProcessor processor = new TestRequestProcessor();

        SelectorQueue selectorQueue = new SelectorQueue();
        processor.initialize(new NoopServer(), new RequestQueue(), selectorQueue);

        Session session = sessionWith(channel, selectorQueue);
        Request request = new Request(session);

        processor.handleRead(request);

        assertTrue(session.getPendingWrites().isEmpty());
        assertNull(selectorQueue.remove());
    }

    @Test
    public void testHandleWritePartialThenComplete() throws Exception {
        DummySocketChannel channel = new DummySocketChannel(new byte[][] {}, 1);
        TestRequestProcessor processor = new TestRequestProcessor();

        SelectorQueue selectorQueue = new SelectorQueue();
        processor.initialize(new NoopServer(), new RequestQueue(), selectorQueue);

        Session session = sessionWith(channel, selectorQueue);
        Request request = new Request(session);

        ByteBuffer buffer = ByteBuffer.wrap("ab".getBytes());
        session.queueWrite(buffer);

        processor.handleWrite(request);

        SelectorTask first = selectorQueue.remove();
        assertNotNull(first);
        assertTrue(first.isAdditive());
        assertEquals(SelectionKey.OP_WRITE, first.getInterest());
        assertFalse(session.getPendingWrites().isEmpty());

        processor.handleWrite(request);

        SelectorTask second = selectorQueue.remove();
        assertNotNull(second);
        assertFalse(second.isAdditive());
        assertEquals(SelectionKey.OP_WRITE, second.getInterest());
        assertTrue(session.getPendingWrites().isEmpty());
    }

    private static Session sessionWith(DummySocketChannel channel, SelectorQueue selectorQueue) throws Exception {
        DummySelector selector = new DummySelector();
        SelectionKey key = new DummySelectionKey(channel, selector);
        Session session = new Session(key, new NoopServer());
        assertSame(selector, key.selector());
        return session;
    }

    private static final class TestRequestProcessor extends RequestProcessor {
        @Override
        public void handleRead(Request request) {
            super.handleRead(request);
        }

        @Override
        public void handleWrite(Request request) {
            super.handleWrite(request);
        }
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
        private final byte[][] readChunks;
        private int readIndex;
        private int readOffset;
        private final int maxWritePerCall;

        private DummySocketChannel(byte[][] readChunks, int maxWritePerCall) {
            super(SelectorProvider.provider());
            this.socket = new DummySocket();
            this.readChunks = readChunks;
            this.maxWritePerCall = maxWritePerCall;
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
            if (readIndex >= readChunks.length) {
                return -1;
            }
            if (!dst.hasRemaining()) {
                return 0;
            }
            byte[] chunk = readChunks[readIndex];
            int remaining = chunk.length - readOffset;
            int toCopy = Math.min(dst.remaining(), remaining);
            dst.put(chunk, readOffset, toCopy);
            readOffset += toCopy;
            if (readOffset >= chunk.length) {
                readIndex++;
                readOffset = 0;
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
            int toWrite = Math.min(remaining, maxWritePerCall);
            src.position(src.position() + toWrite);
            return toWrite;
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
        @Override
        public void setTcpNoDelay(boolean on) {
        }

        @Override
        public boolean getTcpNoDelay() {
            return false;
        }
    }
}
