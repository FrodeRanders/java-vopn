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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A work queue backed by a set of pool workers, internally
 * using a single dequeue.
 */
public class SimpleWorkQueue implements WorkQueue {
    private static final Logger log = LoggerFactory.getLogger(SimpleWorkQueue.class);

    private final int nThreads;
    private final PoolWorker[] threads;
    private final BlockingDeque<Runnable> queue;
    private volatile boolean stopRequested = false;
    
    private final Object lock = new Object();
    
    /* 
     * constructor to initiate worker threads and queue associated with it
     */
    /* package visible only */ SimpleWorkQueue(int nThreads)
    {
        this.nThreads = nThreads;
        queue = new LinkedBlockingDeque<>();
        threads = new PoolWorker[nThreads];
    }
    
    public void start() {
    	for (int i=0; i<nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].start();
        }

        log.trace("Starting work queue...");
    }

    public void stop() {
        log.trace("Stopping work queue...");

        stopRequested = true;
        doInterruptAllWaitingThreads();

        log.trace("Work queue stopped");
    }

    /*
     * Executes the given task in the future.
     * Queues the task and notifies the waiting thread. Also it makes
     * the Work assigner to wait if the queued task reaches to threshold
     */
    @SuppressWarnings("unchecked")
    public boolean execute(Runnable r) {
    	try {
			queue.putFirst(r);
            return true;

		} catch (InterruptedException e) {
            String info = "Failed to enqueue task: ";
            Throwable baseCause = org.gautelis.vopn.lang.Stacktrace.getBaseCause(e);
            info += baseCause.getMessage();
            log.warn(info, e);
		}
        return false;
    }

    /*
     * Checks whether queue is empty (or not)
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }


    /*
     * Returns size of work queue
     */
    public synchronized long size() {
        return queue.size();
    }

    /*
     * Clean-up the worker thread when all the tasks are done
     */
    private void doInterruptAllWaitingThreads() {
    	//Interrupt all the threads
    	for (int i=0; i<nThreads; i++) {
    		threads[i].interrupt();
    	}
    	
    	synchronized(lock) {
    		lock.notify();
    	}
    }

    /*
     * Worker thread to execute user tasks
     */
    private class PoolWorker extends Thread {
    	/*
    	 * Method to retrieve task from worker queue and start executing it.
    	 * This thread will wait for a task if there is no task in the queue. 
    	 */
        public void run() {
            while (!stopRequested) {
                Runnable r;

            	try {
					r = queue.takeLast();
				}
                catch (InterruptedException e1) {
					continue; // and check if we are requested to stop
				}

                try {
                    log.trace("Running pool worker task");
                    r.run();
                }
                catch (Throwable t) {
                    String info = "Failed to run queued task: ";
                    Throwable baseCause = org.gautelis.vopn.lang.Stacktrace.getBaseCause(t);
                    info += baseCause.getMessage();
                    log.info(info, t);
                }
            }
        }
    }
}
