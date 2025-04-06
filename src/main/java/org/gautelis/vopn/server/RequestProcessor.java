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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Queue;

public class RequestProcessor extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestProcessor.class);

    private static final String THREAD_GROUP_NAME = "Request processor group";
    private static final String THREAD_NAME = "Request processor";
    private static int sequenceNumber = 0;

    private static final ThreadGroup requestProcessorGroup = new ThreadGroup(THREAD_GROUP_NAME);

    private final Charset charset = StandardCharsets.UTF_8;


    //
    private Server server = null;
    private RequestQueue requestQueue = null;
    private volatile boolean shutdown = false;
    private SelectorQueue selectorQueue = null;

    //
    public long getId() {
        return threadId();
    }

    /**
     * Default constructor (needed since objects of this class are created dynamically).
     */
    public RequestProcessor() {
        super(requestProcessorGroup, THREAD_NAME + " #" + (++sequenceNumber));
    }

    /**
     * <b>Must</b> be called ahead of start().
     * <p>
     *
     * @param server
     * @param requestQueue
     * @param selectorQueue
     */
    public void initialize(Server server, RequestQueue requestQueue, SelectorQueue selectorQueue) {
        this.server = server;
        this.requestQueue = requestQueue;
        this.selectorQueue = selectorQueue;
    }

    public void run() {
        log.debug("thread#{} started...", getId());

        while (true) {
            // Are we supposed to quit now?
            synchronized (this) {
                if (shutdown) {
                    log.info("Shutting down client request thread#{}", getId());
                    // NOTE: We do not have a connection to terminate at this point
                    return;
                }
            }

            try {
                Session session = requestQueue.take(); // blocks until available

                Request request = new Request(session);
                SelectionKey key = request.getKey();

                if (!key.isValid()) {
                    log.trace("Handling invalid key: {}", key);
                    session.getConnection().close();
                    continue;
                }

                if (key.isWritable()) {
                    log.trace("Handling writable key: {}", key);
                    handleWrite(request, session);
                }

                if (key.isReadable()) {
                    log.trace("Handling readable key: {}", key);
                    handleRead(request, session);
                }

            } catch (InterruptedException e) {
                log.warn("RequestProcessor thread interrupted");
                Thread.currentThread().interrupt(); // restore interrupted flag

            } catch (Exception e) {
                log.warn("RequestProcessor fail: {}", e.getMessage(), e);
            }
        }
    }

    private void handleRead(Request request, Session session) throws Exception {
        log.trace("Handling read request: {}", request);

        try {
            ByteBuffer msgBytes = ByteBuffer.allocate(1460);
            msgBytes.limit(1460);

            Connection conn = request.getConnection();
            int bytesRead = conn.read(msgBytes);
            if (bytesRead == -1) {
                log.info("Client disappeared: {}", conn);
                conn.close();
                return;
            }

            // DUMMY FUNCTIONALITY
            if (bytesRead > 0) {
                log.trace("Received: {}", charset.decode(msgBytes));
                msgBytes.flip();
                request.getSession().queueWrite(msgBytes);
            }

            if (request.getKey().isValid()) {
                selectorQueue.addInterest(request, SelectionKey.OP_READ);
            } else {
                log.warn("Not adding selector task: key already invalid");
            }
        } catch (Exception e) {
            log.error("Could not handle request: {}", e.getMessage(), e);
        }
    }

    private void handleWrite(Request request, Session session) throws IOException {
        log.trace("Handling write request: {}", request);

        Queue<ByteBuffer> pendingWrites = session.getPendingWrites();
        Connection conn = session.getConnection();

        while (!pendingWrites.isEmpty()) {
            ByteBuffer buffer = pendingWrites.peek();
            conn.write(buffer);
            if (buffer.hasRemaining()) {
                // Couldn’t write it all — register interest in an OP_WRITE
                selectorQueue.addInterest(request, SelectionKey.OP_WRITE);
                return;
            }
            pendingWrites.remove(); // done with this buffer
        }

        selectorQueue.removeInterest(request, SelectionKey.OP_WRITE);
    }

    /**
     * Request shutdown of processor.
     */
    void shutdown() {
        synchronized (this) {
            shutdown = true;
        }
        log.info("thread#{} got shutdown signal", getId());
    }

    static ThreadGroup getProcessorThreadGroup() {
        return requestProcessorGroup;
    }

    static public Logger getLog() {
        return log;
    }

    /**
     * This is a simple default implementation of the execute method, that
     * will simply echo the command back to the caller. You have to override
     * this method since it is not much use in this behaviour.
     */
    public int execute(Request request, String command, StringBuffer reply) throws Exception {

        if (log.isTraceEnabled()) {
            log.trace("thread#{} got command: {}", getId(), command);
        }

        // Some default ;-)
        reply.append(command);
        return 0;
    }

    /**
     * Gets throwable's stack trace as a String
     *
     * @param t the throwable
     * @return the stack trace of the exception
     */
    private static String getStacktrace(Throwable t) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bytes, true);
        t.printStackTrace(writer);
        return bytes.toString();
    }
}
