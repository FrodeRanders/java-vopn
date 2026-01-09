/*
 * Copyright (C) 2011-2026 Frode Randers
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

import static org.junit.Assert.assertEquals;

public class MovingAverageTest {

    @Test
    public void testIntegerSeries() {
        MovingAverage ma1 = new MovingAverage();

        int sum = 0;
        int i;

        for (i = 0; i < 100; i++) {
            sum += i;
            ma1.update(i);
        }

        assertEquals(((double) sum) / i, ma1.getAverage(), 0.0);
        assertEquals(i, ma1.getCount());
    }

    @Test
    public void testKnownSeries() {
        MovingAverage ma2 = new MovingAverage();
        int[] samples = {3, 7, 5, 13, 20, 23, 39, 23, 40, 23, 14, 12, 56, 23, 29};
        for (int sample : samples) {
            ma2.update(sample);
        }

        double sum = 0;
        double average = ma2.getAverage();
        for (int sample : samples) {
            sum += Math.pow(sample - average, 2);
        }
        double stdDev = Math.sqrt(sum / (samples.length - 1));

        assertEquals(22.0, ma2.getAverage(), 0.0);
        assertEquals(samples.length, ma2.getCount());
        assertEquals(stdDev, ma2.getStdDev(), 1E-13);
    }

    @Test
    public void testRandomSeries() {
        double sum = 0.0;
        int i;

        MovingAverage ma3 = new MovingAverage();
        for (i = 0; i < 100; i++) {
            double sample = Math.random();
            ma3.update(sample);
            sum += sample;
        }

        assertEquals(sum / i, ma3.getAverage(), 1E-15);
        assertEquals(i, ma3.getCount());
    }
}
