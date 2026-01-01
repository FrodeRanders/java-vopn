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
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Worker thread that handles IO for sessions queued by the server.
 */
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
    protected SelectorQueue selectorQueue = null;

    /**
     * Default constructor (needed since objects of this class are created dynamically).
     */
    public RequestProcessor() {
        super(requestProcessorGroup, THREAD_NAME + "#" + (++sequenceNumber) + " (" + Thread.currentThread().threadId() + ")");
    }

    /**
     * <b>Must</b> be called ahead of start().
     * <p>
     *
     * @param server owning server instance
     * @param requestQueue queue of sessions to process
     * @param selectorQueue queue for selector interest updates
     */
    public void initialize(Server server, RequestQueue requestQueue, SelectorQueue selectorQueue) {
        this.server = server;
        this.requestQueue = requestQueue;
        this.selectorQueue = selectorQueue;
    }

    /**
     * Returns the thread id.
     *
     * @return thread id
     */
    public long getId() {
        return threadId();
    }

    /**
     * Main processing loop for queued sessions.
     */
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
                    handleWrite(request);
                }

                if (key.isReadable()) {
                    log.trace("Handling readable key: {}", key);
                    handleRead(request);
                }

            } catch (InterruptedException e) {
                log.warn("RequestProcessor thread interrupted");
                Thread.currentThread().interrupt(); // restore interrupted flag

            } catch (Exception e) {
                log.warn("RequestProcessor fail: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Requests adding interest ops for a selection key.
     *
     * @param request request being processed
     * @param interest interest ops to add
     * @throws IOException if selector update fails
     */
    protected void addInterest(Request request, int interest) throws IOException {
        SelectionKey key = request.getKey();
        if (key.isValid()) {
            selectorQueue.addInterest(request, interest);
        } else {
            log.warn("Invalid key: Could not add interest {} from key {}", interest, key);
        }
    }

    /**
     * Requests removing interest ops for a selection key.
     *
     * @param request request being processed
     * @param interest interest ops to remove
     * @throws IOException if selector update fails
     */
    protected void removeInterest(Request request, int interest) throws IOException {
        SelectionKey key = request.getKey();
        if (key.isValid()) {
            selectorQueue.removeInterest(request, interest);
        } else {
            log.warn("Invalid key: Could not remove interest {} from key {}", interest, key);
        }
    }

    /**
     * Handles inbound read readiness for a request.
     *
     * @param request request being processed
     */
    protected void handleRead(Request request) {
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
                addInterest(request, SelectionKey.OP_WRITE);
            }

            addInterest(request, SelectionKey.OP_READ);

        } catch (Exception e) {
            log.error("Could not handle read request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles outbound write readiness for a request.
     *
     * @param request request being processed
     */
    protected void handleWrite(Request request) {
        log.trace("Handling write request: {}", request);

        Session session = request.getSession();
        Queue<ByteBuffer> pendingWrites = session.getPendingWrites();
        Connection conn = session.getConnection();

        try {
            while (!pendingWrites.isEmpty()) {
                ByteBuffer buffer = pendingWrites.peek();
                conn.write(buffer);
                if (buffer.hasRemaining()) {
                    // Couldn’t write it all — register interest in an OP_WRITE
                    addInterest(request, SelectionKey.OP_WRITE);
                    return;
                }
                pendingWrites.remove(); // done with this buffer
            }

            removeInterest(request, SelectionKey.OP_WRITE);
        }
        catch (NoSuchElementException nsee) {
            log.error("List of pending writes modified elsewhere: {}", nsee.getMessage(), nsee);
        }
        catch (IOException ioe) {
            log.error("Could not handle write request: {}", ioe.getMessage(), ioe);
        }
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

    /**
     * Returns the thread group holding request processors.
     *
     * @return request processor thread group
     */
    static ThreadGroup getProcessorThreadGroup() {
        return requestProcessorGroup;
    }

    /**
     * Returns the logger for request processors.
     *
     * @return logger instance
     */
    static public Logger getLog() {
        return log;
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
