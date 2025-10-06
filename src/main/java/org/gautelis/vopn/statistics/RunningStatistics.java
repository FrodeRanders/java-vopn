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
 */
package org.gautelis.vopn.statistics;

import java.time.Instant;

/**
 * Running statistics (mean, variance, stdev, CV, min, max, total).
 * Uses Welford’s algorithm for numerical stability.
 */
public final class RunningStatistics {
    private long   count  = 0L;
    private double mean   = 0.0;
    private double m2     = 0.0; // sum of squared deviations
    private double min    = Double.NaN;
    private double max    = Double.NaN;
    private long   total  = 0L; // integral total (where meaningful)

    public void addSample(double x) {
        if (count == 0) {
            min = max = x;
        } else {
            min = Math.min(min, x);
            max = Math.max(max, x);
        }

        total += Math.round(x);

        count++;
        final double delta = x - /* original */ mean;
        mean += delta / count;
        final double delta2 = x - /* updated */ mean;
        m2 += delta * delta2;
    }

    public void addSample(Instant start, Instant end) {
        addSample(end.toEpochMilli() - start.toEpochMilli());
    }

    public long getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public long getTotal() {
        return total;
    }

    /**
     * Sample variance (n-1 in denominator). Defined for n >= 2.
     */
    public double getVariance() {
        return count > 1 ? m2 / (count - 1) : Double.NaN;
    }

    /**
     * Standard deviation (from mean). Defined for n >= 2.
     * <p>
     * It has the same physical unit as the measurements themselves (e.g. milliseconds).
     * The standard deviation is the “typical” distance of a data point from the mean.
     * <p>
     * What it tells you:
     * <li><i>Spread:</i> A larger s means the observations are more dispersed;
     *       a smaller s means they are tightly clustered around the mean.
     * </li>
     * <li><i>Scale-aware:</i> Because s uses the original units, you can read
     *      it directly: "latency is ~ 80 ms +/- 7 ms".
     * </li>
     */
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    /**
     * Coefficient of variation (CV) in %. Defined for |mean| > 0 and n >= 2.
     * <p>
     * Unit-free relative spread. CV expresses dispersion relative to the mean,
     * so the unit cancels out.
     * <p>
     * What it tells you:
     * <li><i>Comparability across metrics:</i> You can line up the CV of packet
     *       latency (ms), request size (kB) and CPU load (%) and immediately
     *       see which one fluctuates most relative to its typical value.
     * </li>
     * <li><i>Dimensionless scaling:</i> Useful when the magnitudes differ by orders
     * 	     (e.g. response times measured in both seconds and minutes).
     * </li>
     */
    public double getCV() {
        return (count > 1 && mean != 0.0) ? 100.0 * getStdDev() / mean : Double.NaN;
    }
}