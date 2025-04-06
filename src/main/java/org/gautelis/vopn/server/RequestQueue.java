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

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class RequestQueue {
    private static final Logger log = LoggerFactory.getLogger(RequestQueue.class);

    private final BlockingQueue<Session> queue = new LinkedBlockingQueue<>();

    public boolean insert(Session session) {
        log.trace("Adding session: {}", session);
        return queue.offer(session); // or queue.put(session) to block if full
    }

    public Session take() throws InterruptedException {
        return queue.take(); // blocks until available
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
