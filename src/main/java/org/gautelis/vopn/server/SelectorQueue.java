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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;

public class SelectorQueue extends LinkedList<SelectorTask> {
    private static final Logger log = LoggerFactory.getLogger(SelectorQueue.class);

    public void addInterest(Request request, int interest) {
        if (log.isTraceEnabled()) {
            SelectionKey key = request.getKey();
            Session session = request.getSession();
            log.trace("Add interest: request={}, session={}, key={}, interest={}", request, session, key, interest);
        }

        AdditiveSelectorTask ast = new AdditiveSelectorTask(request, interest);
        synchronized (this) {
            addLast(ast);
            @SuppressWarnings("resource")
            Selector selector = request.getKey().selector();
            if (selector.isOpen()) {
                selector.wakeup();
            }
        }
    }
    
    public void removeInterest(Request request, int interest) {
        if (log.isTraceEnabled()) {
            SelectionKey key = request.getKey();
            Session session = request.getSession();
            log.trace("Remove interest: request={}, session={}, key={}, interest={}", request, session, key, interest);
        }

        SubtractiveSelectorTask sst = new SubtractiveSelectorTask(request, interest);
        synchronized (this) {
            addLast(sst);
            @SuppressWarnings("resource")
            Selector selector = request.getKey().selector();
            if (selector.isOpen()) {
                selector.wakeup();
            }
        }
    }

    @Override
    public synchronized SelectorTask remove() {
        if (this.isEmpty()) {
            return null;
        }
        SelectorTask task = removeFirst();
        log.trace("Picking selector task: " + task);
        return task;
    }
}
