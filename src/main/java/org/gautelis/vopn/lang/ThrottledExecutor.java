/*
 * Copyright (C) 2016-2020 Frode Randers
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
package org.gautelis.vopn.lang;

import org.gautelis.vopn.statistics.MovingAverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by froran on 2016-02-03.
 */
public class ThrottledExecutor {
    private static final NumberFormat dec2Format = NumberFormat.getNumberInstance();
    static {
        dec2Format.setMaximumFractionDigits(1);
    }

    private static final List<String> IGNORE_REQUESTS = new ArrayList<>();

    private static final int INFORM_EACH_N_UNITS = 500;

    private final long maxUnitsProcessedPerMinute;
    private final long maxUnitsProcessedInSequence;
    private long numberOfErrorsAccepted;
    private final Object semaphore;
    private final List<String> shutdownRequests;
    private final PrintWriter out;

    public class StatusContext {
        private boolean finished = false;
        private Boolean success = null;
        private String[] failureInfo = null;

        public void success() {
            success = true;
        }

        public void failure(String... info) {
            success = false;
            failureInfo = info;
        }

        public void finished() {
            finished = true;
        }
        Boolean getStatus() {
            return success; // may be null
        }

        String[] getFailureInfo() {
            return failureInfo;
        }

        boolean isFinished() {
            return finished;
        }

        void reset() {
            success = null;
        }
    }

    public ThrottledExecutor(
            final long maxUnitsProcessedPerMinute,
            final long maxUnitsProcessedInSequence
    ) {
        this(maxUnitsProcessedPerMinute, maxUnitsProcessedInSequence,
                /* number of errors accepted */ 0L, IGNORE_REQUESTS,
                IGNORE_REQUESTS, new PrintWriter(System.out));
    }

    public ThrottledExecutor(
            final long maxUnitsProcessedPerMinute,
            final long maxUnitsProcessedInSequence,
            final long numberOfErrorsAccepted
    ) {
        this(maxUnitsProcessedPerMinute, maxUnitsProcessedInSequence, numberOfErrorsAccepted,
                IGNORE_REQUESTS, IGNORE_REQUESTS, new PrintWriter(System.out));
    }

    public ThrottledExecutor(
            final long maxUnitsProcessedPerMinute,
            final long maxUnitsProcessedInSequence,
            final long numberOfErrorsAccepted,
            final PrintWriter out
    ) {
        this(maxUnitsProcessedPerMinute, maxUnitsProcessedInSequence, numberOfErrorsAccepted,
                IGNORE_REQUESTS, IGNORE_REQUESTS, out);
    }

    public ThrottledExecutor(
            final long maxUnitsProcessedPerMinute,
            final long maxUnitsProcessedInSequence,
            final long numberOfErrorsAccepted,
            final List<String> shutdownRequests,
            final PrintWriter out
    ) {
        this(maxUnitsProcessedPerMinute, maxUnitsProcessedInSequence, numberOfErrorsAccepted, shutdownRequests, shutdownRequests, out);
    }

    public ThrottledExecutor(
            final long maxUnitsProcessedPerMinute,
            final long maxUnitsProcessedInSequence,
            final long numberOfErrorsAccepted,
            final Object semaphore,
            final List<String> shutdownRequests,
            final PrintWriter out) {

        this.maxUnitsProcessedPerMinute = maxUnitsProcessedPerMinute;
        this.maxUnitsProcessedInSequence = maxUnitsProcessedInSequence;
        this.numberOfErrorsAccepted = numberOfErrorsAccepted;
        this.semaphore = semaphore;
        this.shutdownRequests = shutdownRequests;
        this.out = out;
    }

    private void inform(long numProcessed, MovingAverage averageTPD, MovingAverage averageDelay) {
        StringBuilder buf = new StringBuilder();
        buf.append("* Processed ").append(numProcessed).append(" units");

        if (averageTPD.getCount() > 0) {
            buf.append(", avg time=").append(dec2Format.format(averageTPD.getAverage())).append("ms");
            if (averageTPD.getCount() > 1) {
                buf.append(" (cv=").append(dec2Format.format(averageTPD.getCV())).append("%)");
            }
        }

        if (averageDelay.getCount() > 0) {
            buf.append(", avg delay=").append(dec2Format.format(averageDelay.getAverage())).append("ms");
            if (averageDelay.getCount() > 1) {
                buf.append(" (cv=").append(dec2Format.format(averageDelay.getCV())).append("%)");
            }
        }

        out.println(buf.toString());
    }

    /*
        ThrottledExecutor executor = new ThrottledExecutor(30, 1000);
        int x = 0;

        executor.execute((s) -> {
            x++;
            s.success();
        });
     */
    public long execute(Consumer<StatusContext> block) {
        long numUnitsProcessed = 0L;


        // Keep track of average time used per unig (average time per unit)
        MovingAverage averageTPU = new MovingAverage();

        // Average imposed delay
        MovingAverage averageDelay = new MovingAverage();

        StatusContext statusContext = new StatusContext();

        while (numUnitsProcessed < maxUnitsProcessedInSequence) {

            statusContext.reset();

            synchronized (semaphore) {
                if (shutdownRequests.size() > 0) {
                    String info = "Shutting down processing due to request: " + shutdownRequests.remove(0);
                    out.println(info);
                    inform(numUnitsProcessed, averageTPU, averageDelay);

                    return numUnitsProcessed;
                }
            }

            long startTime = System.currentTimeMillis();
            block.accept(statusContext);
            long endTime = System.currentTimeMillis();

            if (statusContext.isFinished() || (!statusContext.getStatus() && --numberOfErrorsAccepted < 0)) {
                return numUnitsProcessed;
            }

            averageTPU.update(endTime - startTime);
            ++numUnitsProcessed;

            // Possibly delay processing so that we do not process more than 'maxUnitsProcessedPerMinute' - on average
            final double slotTime = (60000) / maxUnitsProcessedPerMinute;   // milliseconds
            double averageProcessingTime = averageTPU.getAverage(); // milliseconds
            long waitTime = (long) (slotTime - Math.ceil(averageProcessingTime));  // milliseconds

            if (waitTime > 0) {
                averageDelay.update(waitTime);
                synchronized (semaphore) {
                    try {
                        semaphore.wait(waitTime);
                    } catch (InterruptedException ignore) {}
                }
            }
            synchronized (semaphore) {
                if (shutdownRequests.size() > 0) {
                    String info = "Shutting down processing due to request: " + shutdownRequests.remove(0);
                    out.println(info);
                    inform(numUnitsProcessed, averageTPU, averageDelay);

                    return numUnitsProcessed;
                }
            }

            if ((numUnitsProcessed % INFORM_EACH_N_UNITS) == 0) {
                // Inform each N:th time through
                inform(numUnitsProcessed, averageTPU, averageDelay);
            }
        }
        if ((numUnitsProcessed % INFORM_EACH_N_UNITS) != 0) {
            // Only inform if we have not just done that :)
            inform(numUnitsProcessed, averageTPU, averageDelay);
        }
        return numUnitsProcessed;
    }
}
