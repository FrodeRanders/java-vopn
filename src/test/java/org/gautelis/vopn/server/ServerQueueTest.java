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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServerQueueTest {

    @Test
    public void testRequestQueueInsertAndTake() throws Exception {
        try (SessionBundle bundle = new SessionBundle()) {
            RequestQueue queue = new RequestQueue();

            assertTrue(queue.insert(bundle.session));
            assertFalse(queue.isEmpty());

            Session taken = queue.take();
            assertSame(bundle.session, taken);
            assertTrue(queue.isEmpty());
        }
    }

    @Test
    public void testSelectorQueueTasks() throws Exception {
        try (SessionBundle bundle = new SessionBundle()) {
            SelectorQueue selectorQueue = new SelectorQueue();

            selectorQueue.addInterest(bundle.request, SelectionKey.OP_WRITE);
            SelectorTask first = selectorQueue.remove();
            assertTrue(first.isAdditive());
            assertEquals(SelectionKey.OP_WRITE, first.getInterest());
            assertSame(bundle.request, first.getRequest());

            selectorQueue.removeInterest(bundle.request, SelectionKey.OP_READ);
            SelectorTask second = selectorQueue.remove();
            assertFalse(second.isAdditive());
            assertEquals(SelectionKey.OP_READ, second.getInterest());
        }
    }

    @Test
    public void testSelectorQueueEmptyRemove() {
        SelectorQueue selectorQueue = new SelectorQueue();
        assertNull(selectorQueue.remove());
    }

    private static final class SessionBundle implements AutoCloseable {
        private final DummySelector selector;
        private final DummySocketChannel channel;
        private final SelectionKey key;
        private final Session session;
        private final Request request;

        private SessionBundle() throws Exception {
            selector = new DummySelector();
            channel = new DummySocketChannel();
            key = new DummySelectionKey(channel, selector);

            session = new Session(key, new NoopServer());
            request = new Request(session);
        }

        @Override
        public void close() throws IOException {
            key.cancel();
            selector.close();
            channel.close();
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
        private final SelectableChannel channel;
        private final Selector selector;
        private int interestOps;
        private boolean valid = true;

        private DummySelectionKey(SelectableChannel channel, Selector selector) {
            this.channel = channel;
            this.selector = selector;
        }

        @Override
        public SelectableChannel channel() {
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
            return interestOps;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            this.interestOps = ops;
            return this;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }

    private static final class DummySocketChannel extends SocketChannel {
        private DummySocketChannel() {
            super(SelectorProvider.provider());
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
        public SocketChannel shutdownInput() {
            return this;
        }

        @Override
        public SocketChannel shutdownOutput() {
            return this;
        }

        @Override
        public Socket socket() {
            return new Socket();
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
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public int read(ByteBuffer dst) {
            return -1;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) {
            return -1;
        }

        @Override
        public int write(ByteBuffer src) {
            return 0;
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) {
            return 0;
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

        @Override
        public Set<java.net.SocketOption<?>> supportedOptions() {
            return Collections.emptySet();
        }
    }
}
