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
package org.gautelis.vopn.statistics;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunningStatisticsTest {

    @Test
    public void testEmptyStatistics() {
        RunningStatistics stats = new RunningStatistics();

        assertEquals(0L, stats.getCount());
        assertTrue(Double.isNaN(stats.getMin()));
        assertTrue(Double.isNaN(stats.getMax()));
        assertTrue(Double.isNaN(stats.getVariance()));
        assertTrue(Double.isNaN(stats.getStdDev()));
        assertTrue(Double.isNaN(stats.getCV()));
        assertEquals(0L, stats.getTotal());
    }

    @Test
    public void testSimpleSequence() {
        RunningStatistics stats = new RunningStatistics();
        stats.addSample(1.0);
        stats.addSample(2.0);
        stats.addSample(3.0);

        assertEquals(3L, stats.getCount());
        assertEquals(1.0, stats.getMin(), 0.0);
        assertEquals(3.0, stats.getMax(), 0.0);
        assertEquals(2.0, stats.getMean(), 0.0);
        assertEquals(1.0, stats.getVariance(), 1E-12);
        assertEquals(1.0, stats.getStdDev(), 1E-12);
        assertEquals(50.0, stats.getCV(), 1E-12);
        assertEquals(6L, stats.getTotal());
    }

    @Test
    public void testInstantSample() {
        RunningStatistics stats = new RunningStatistics();
        Instant start = Instant.ofEpochMilli(1000L);
        Instant end = Instant.ofEpochMilli(2500L);

        stats.addSample(start, end);

        assertEquals(1L, stats.getCount());
        assertEquals(1500.0, stats.getMean(), 0.0);
        assertEquals(1500L, stats.getTotal());
        assertTrue(Double.isNaN(stats.getVariance()));
    }
}
