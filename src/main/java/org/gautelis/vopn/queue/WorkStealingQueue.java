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

import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A work queue backed by a set of pool workers, internally
 * using multiple dequeues, where the pool workers steal work
 * from each other.
 */
public class WorkStealingQueue implements WorkQueue {
    private static final Logger log = LoggerFactory.getLogger(WorkStealingQueue.class);

	private final int nThreads;
    private int queue_no = 0;
    private final PoolWorker[] threads;
    private final Vector<BlockingDeque<Runnable>> queues;
    private volatile boolean stopRequested = false;
    
    private final Object lock = new Object();
    
    /* 
     * constructor to initiate worker threads and queue associated with it
     */
    /* package private */ WorkStealingQueue(int nThreads)
    {
        this.nThreads = nThreads;
        threads = new PoolWorker[nThreads];

        queues = new Vector<>(nThreads);
        for (int i=0; i<nThreads; i++) {
        	queues.add(new LinkedBlockingDeque<>());
        }
    }
    
    private Runnable stealWork(int index) throws InterruptedException {
    	for (int i=0; i<nThreads; i++) {
    		if (i != index) {
                synchronized (queues) {
                    BlockingDeque<Runnable> queue = queues.elementAt(i);

                    if (!queue.isEmpty()) {
                        Runnable r = queue.takeFirst();
                        // log.trace("Worker [{}] stealing work from [{}]", index, i);
                        return r;
                    }
                }
    		}
    	}
    	
    	return null;
    }
    
    public void start() {
    	for (int i=0; i<nThreads; i++) {
            threads[i] = new PoolWorker(i);
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
            synchronized (queues) {
                queues.elementAt(queue_no++ % nThreads).putFirst(r);
                if (queue_no == nThreads) {
                    queue_no = 0;
                }
                queues.notifyAll();
            }
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
    public boolean isEmpty() {
        synchronized (queues) {
            for (BlockingDeque<Runnable> queue : queues) {
                if (!queue.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Returns size of work queue
     */
    public long size() {
        long _size = 0L;
        synchronized (queues) {
            for (BlockingDeque<Runnable> queue : queues) {
                _size += queue.size();
            }
        }
        return _size;
    }

    /*
     * Clean-up the worker thread when all the tasks are done
     */
    private synchronized void doInterruptAllWaitingThreads() {
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
    	
    	private final int index;
    	
    	PoolWorker(int index) {
    		this.index = index;
    	}
    	
    	   	
    	/*
    	 * Method to retrieve task from worker queue and start executing it.
    	 * This thread will wait for a task if there is no task in the queue. 
    	 */
        public void run() {

            while (!stopRequested) {
                Runnable r;
                try {
                    synchronized (queues) {
                        BlockingDeque<Runnable> queue = queues.elementAt(index);

                        if (queue.isEmpty()) {
                            r = stealWork(index);
                        } else {
                            r = queue.takeLast();
                            // log.trace("Worker [{}] claiming task", index);
                        }

                        if (r == null) {
                            queues.wait();
                            continue;
                        }
                    }
                } catch (InterruptedException e) {
                    continue;
                }

                // If we don't catch RuntimeException, 
                // the pool could leak threads
                try {
                    log.trace("Running pool worker [{}] task", index);
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
