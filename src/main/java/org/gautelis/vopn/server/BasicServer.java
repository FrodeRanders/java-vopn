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

import org.gautelis.vopn.lang.DynamicLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class BasicServer<T extends RequestProcessor> implements Server {
    private static final Logger log = LoggerFactory.getLogger(BasicServer.class);

    private static final String THREAD_NAME = "Server::eventloop";
    private static int sequenceNumber = 0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Configuration config;
    private final Consumer<T> consumer;

    // The selector is managed explicitly and closed during shutdown.
    // Suppressing IntelliJ's warning about try-with-resources.
    @SuppressWarnings("resource")
    private final Selector selector;

    private volatile String shutdownRequest = null;

    //
    private final RequestQueue requestQueue = new RequestQueue();
    private final SelectorQueue selectorQueue = new SelectorQueue();

    //

    public BasicServer(Configuration config, Consumer<T> consumer) throws IOException {
        this.config = config;
        this.consumer = consumer;
        this.selector = SelectorProvider.provider().openSelector();
    }

    private void boot() throws ClassNotFoundException {
        // Determine request processor plugin (from configuration)
        String className = config.requestProcessorClassname();
        log.info("Loading client request processor: {}", className);

        DynamicLoader<RequestProcessor> loader = new DynamicLoader<>("request processor");
        Class<?> pluginClass = loader.createClass(className);

        // Create and start request processor threads
        for (int i = config.numRequestThreads(); i > 0; i--) {
            RequestProcessor processor = loader.createObject(className, pluginClass, /* no dynamic init */ null);
            processor.initialize(this, requestQueue, selectorQueue);
            if (null != consumer) {
                consumer.accept((T)processor);
            }
            processor.start();
        }
    }

    public void requestShutdown(String reason) {
        synchronized (this) {
            shutdownRequest = (null != reason ? reason : "?unknown? reason");
        }
        selector.wakeup(); // will break out of select() safely
    }

    private void shutdown(String reason) {
        String info = "Shutting down server due to " + reason + ". ";
        info += "This will take around " + config.shutdownGracePeriod() + " seconds...";
        log.info(info);
        System.out.println(info); // Leave in production code also

        // Await all processors terminating before termination group
        ThreadGroup requestProcessorGroup = RequestProcessor.getProcessorThreadGroup();

        // Kindly request processor termination
        {
            Thread[] processors = new Thread[config.numRequestThreads()];

            for (Thread p : processors) {
                RequestProcessor processor = (RequestProcessor) p;
                processor.shutdown();
                processor.interrupt();
            }
        }

        // Notify subclasses
        try {
            stopping();
        } catch (Throwable t) {
            log.error("Local shutdown activities failed: {}", t.getMessage(), t);
        }

        // Grace period
        synchronized (this) {
            try {
                wait(config.shutdownGracePeriod() * 1000L);
            } catch (InterruptedException ie) {
                // ignore
            }
        }

        // Brutal termination (if needed)
        {
            Thread[] processors = new Thread[config.numRequestThreads()];
            int count = requestProcessorGroup.enumerate(processors);
            if (count > 0) {
                log.info("Killing dangling request processor threads...");

                for (Thread processor1 : processors) {
                    RequestProcessor processor = (RequestProcessor) processor1;
                    if (processor.isAlive()) {
                        log.info("Killing thread#{}", processor.getId());
                        processor.interrupt();
                    }
                }
            }
        }

        // Notify subclasses
        try {
            stopped();
        } catch (Throwable t) {
            log.error("Local shutdown activities failed: {}", t.getMessage(), t);
        }
    }
    

    /**
     * The selector comes here, polling for tasks such as
     * reactivating READ interest on ready keys.
     */
    private void checkSelectorTasks() throws IOException {
        synchronized (selectorQueue) {
            SelectorTask task;
            
            while (null != (task = selectorQueue.remove())) {
                Request request = task.getRequest();
                SelectionKey key = request.getKey();
                @SuppressWarnings("resource")
                SelectableChannel channel = key.channel();

                if (key.isValid() && channel.isOpen() &&  key.selector().isOpen()) {
                    if (task.isAdditive()) {
                        // Add interest [COMMON TASK]
                        key.interestOps(key.interestOps() | task.getInterest());
                    } else {
                        // Remove interest [RARE TASK (if ever) since this is
                        // done during normal operation in start()]
                        key.interestOps(key.interestOps() & ~task.getInterest());
                    }
                }
                else {
                    log.warn("Skipping selector interest update: key valid? {}, channel open? {}, selector open? {}",
                            key.isValid(), key.channel().isOpen(), key.selector().isOpen());
                }
            }
        }
    }

    public void start() {
        // Start the event loop on a separate thread.
        //executorService.submit(this::eventLoop);
        new Thread(this::eventLoop).start();
    }

    private void eventLoop() {
        Thread.currentThread().setName(THREAD_NAME + "#" + (++sequenceNumber) + " (" + Thread.currentThread().threadId() + ")");

        try {
            String info = "Starting server on port " + config.localPort() + "...";
            log.info(info);
            System.out.println(info); // Leave in production code also

            // Boot request processor threads
            boot();
            
            // Workaround in order to guarantee at least one (dummy) key
            // with the selector (at all times), anchoring the selector.
            Pipe pipe = Pipe.open();
            SelectableChannel dummyChannel = pipe.source();
            dummyChannel.configureBlocking(false);
            dummyChannel.register(selector, SelectionKey.OP_READ);

            // Setup
            InetSocketAddress address = new InetSocketAddress(config.localPort());

            try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
                // Bind to server port
                serverChannel.socket().bind(address); // will fail if port is occupied

                // Register interest in ACCEPT event
                serverChannel.configureBlocking(/* block? */ false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                while (true) {
                    log.trace("New loop cycle");

                    // Check pending selector tasks
                    checkSelectorTasks();

                    // Should we shut down?
                    synchronized (this) {
                        if (null != shutdownRequest) {
                            break;  // exit loop
                        }
                    }

                    if (!selector.isOpen()) {
                        log.warn("Selector unexpectedly closed!");
                        break;
                    }

                    // The call to select() blocks until an event is ready
                    int numReadyKeys = selector.select();
                    if (numReadyKeys == 0) {
                        log.trace("No keys are ready after select, this is possibly a wakeup");
                        continue;
                    }
                    else {
                        log.trace("{} key(s) are ready from select on port {}", numReadyKeys, config.localPort());
                    }

                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();

                        // Do we have a new client connecting?
                        if (key.isAcceptable()) {
                            log.trace("Key is acceptable: {}", key);

                            // Accepting incoming connections could have been done
                            // in a dedicated thread in order not to disturb the
                            // existing sessions...

                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel channel = server.accept();

                            // Register interest in READ events
                            channel.configureBlocking(/* block? */ false);
                            SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);

                            // Associate a session with this connection
                            try {
                                Session session = new Session(readKey, this);
                                readKey.attach(session);
                                log.debug("New session processor channel: {}", readKey);

                            } catch (Throwable t) {
                                log.error("Failed to associate session with connection: {}", t.getMessage(), t);
                            }
                        }

                        if (key.isWritable()) {
                            log.trace("Key is writable: {}", key);

                            //---------------------------------------------------------
                            // IMPORTANT:
                            //   Deregister the triggered operations(s) so that we
                            //   do not receive more of these while we are busy
                            //   handling them.
                            //
                            //   This is extremely important since a new WRITE event
                            //   would activate *another* processor thread which
                            //   could write packet bytes from the current processor.
                            //
                            //   We will activate WRITE interest in this key after
                            //   having processed the request.
                            //---------------------------------------------------------
                            int readyOps = key.readyOps();
                            key.interestOps(key.interestOps() & ~readyOps);

                            // Queue session for request handling by a processor thread
                            Session session = (Session) key.attachment();
                            if (!requestQueue.insert(session)) {
                                log.warn("Could not queue request");
                            }
                        }

                        // Did we get a packet from a client?
                        if (key.isReadable()) {
                            log.trace("Key is readable: {}", key);

                            //---------------------------------------------------------
                            // IMPORTANT:
                            //   Deregister the triggered operations(s) so that we
                            //   do not receive more of these while we are busy
                            //   handling them.
                            //
                            //   This is extremely important since a new READ event
                            //   would activate *another* processor thread which
                            //   could read packet bytes from the current processor.
                            //
                            //   We will activate READ interest in this key after
                            //   having processed the request.
                            //---------------------------------------------------------
                            int readyOps = key.readyOps();
                            key.interestOps(key.interestOps() & ~readyOps);

                            // Queue session for request handling by a processor thread
                            Session session = (Session) key.attachment();
                            if (!requestQueue.insert(session)) {
                                log.warn("Could not queue request");
                            }
                        }
                    }
                }
            }
            finally {
                try {
                    log.info("Shutting down server components");
                    if (selector.isOpen()) {
                        selector.close();
                    }
                } catch (IOException e) {
                    log.warn("Error closing selector: {}", e.getMessage(), e);
                }
            }
        }
        catch (ClosedSelectorException cse) {
            log.info("Selector was unexpectedly closed: {}", cse.getMessage(), cse);
        }
        catch (CancelledKeyException cke) {
            log.info("Key was cancelled: {}", cke.getMessage(), cke);
        }
        catch (Exception e) {
            log.info("Server failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Called by the internal server machinery when the server is
     * shutting down.
     * <p>
     * You may override this method to accomplish shutdown of
     * local background activities.
     */
    public void stopping() {
        // Does nothing
    }

    /**
     * Called by the internal server machinery when the server has
     * shut down.
     * <p>
     * You may override this method to accomplish cleanup of
     * local background activities.
     */
    public void stopped() {
        // Does nothing
    }
}
