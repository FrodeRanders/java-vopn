/*
 * Copyright (C) 2017-2025 Frode Randers
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
package org.gautelis.vopn.queue;

import java.io.Closeable;
import java.io.IOException;

/**
 * Queue abstraction for running tasks across worker threads.
 */
public interface WorkQueue extends Closeable {
    /**
     * Starts worker threads.
     */
    void start();

    /**
     * Signals worker threads to stop.
     */
    void stop();

    /**
     * Enqueues a task for execution.
     *
     * @param t runnable task
     * @return {@code true} if the task was accepted
     */
    boolean execute(Runnable t);

    /**
     * Returns whether the queue is empty.
     *
     * @return {@code true} if no pending tasks
     */
    boolean isEmpty();

    /**
     * Returns the number of queued tasks.
     *
     * @return queue size
     */
    long size();

    default void close() throws IOException { stop(); }
}
