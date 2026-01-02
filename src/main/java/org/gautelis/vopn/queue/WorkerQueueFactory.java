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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates different types of worker queues:
 * {@link SimpleWorkQueue one queue with multiple workers},
 * {@link MultiWorkQueue multiple queues with corresponding workers},
 */
public class WorkerQueueFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkerQueueFactory.class);

     //* {@link WorkStealingQueue multiple queues with workers stealing from each others queues}.

    /**
     * Supported queue types.
     */
    public enum Type
    {
        Simple,
        Multi,
        WorkStealing
    }

    /**
     * Returns a thread-backed queue.
     * @param type {@link Type} of queue
     * @param nThreads number of threads tending to the queue
     * @return a worker queue
     */
	public static WorkQueue getWorkQueue(Type type, int nThreads) {
        return switch (type) {
            case Simple -> new SimpleWorkQueue(nThreads);
            case WorkStealing -> new WorkStealingQueue(nThreads);
            default -> new MultiWorkQueue(nThreads);
        };
	}
}
