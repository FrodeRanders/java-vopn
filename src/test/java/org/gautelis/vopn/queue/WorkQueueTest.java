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
package org.gautelis.vopn.queue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkQueueTest {

    @Test
    public void testSimpleQueueRunsTasks() throws Exception {
        assertQueueRunsTasks(WorkerQueueFactory.Type.Simple);
    }

    @Test
    public void testMultiQueueRunsTasks() throws Exception {
        assertQueueRunsTasks(WorkerQueueFactory.Type.Multi);
    }

    @Test
    public void testWorkStealingQueueRunsTasks() throws Exception {
        assertQueueRunsTasks(WorkerQueueFactory.Type.WorkStealing);
    }

    @Test
    public void testQueueSizeBeforeStart() {
        WorkQueue queue = WorkerQueueFactory.getWorkQueue(WorkerQueueFactory.Type.Simple, 1);

        assertTrue(queue.execute(() -> {}));
        assertTrue(queue.execute(() -> {}));

        assertEquals(2L, queue.size());
        assertFalse(queue.isEmpty());
        queue.start();
        queue.stop();
    }

    private void assertQueueRunsTasks(WorkerQueueFactory.Type type) throws Exception {
        WorkQueue queue = WorkerQueueFactory.getWorkQueue(type, 2);
        CountDownLatch latch = new CountDownLatch(3);
        queue.start();

        try {
            queue.execute(latch::countDown);
            queue.execute(latch::countDown);
            queue.execute(latch::countDown);

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            queue.stop();
        }
    }
}
