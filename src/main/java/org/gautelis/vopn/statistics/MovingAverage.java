/*
 * Copyright (C) 2012-2026 Frode Randers
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
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package  org.gautelis.vopn.statistics;

/**
 * Calculates statistics based on a moving-average algorithm over a data series.
 * <pre>
 * (defun running-average (avg new n)
 *    "Calculate new average given previous average over n data points"
 *    (/ (+ new (* avg n)) (1+ n)))
 * </pre>
 *
 * @deprecated Use {@link RunningStatistics} instead.
 */
@Deprecated
public class MovingAverage {

    private double min = 0.0;
    private double max = 0.0;
    private double average = 0.0;
    private double stdDev = 0.0;
    private double cv = 0.0;

    private long count = 0L;
    private double _pwrSumAverage = 0.0; // computational use

    /**
     * Creates an empty moving average.
     */
    public MovingAverage() {
    }

    /**
     * Returns the number of samples processed.
     *
     * @return sample count
     */
    public long getCount() {
        return count;
    }

    /**
     * Get average.
     * @return average of all samples
     */
    public double getAverage() {
        return average;
    }

    /**
     * Get min value.
     * @return minimal sample
     */
    public double getMin() {
        return min;
    }

    /**
     * Get max value.
     * @return maximal sample
     */
    public double getMax() {
        return max;
    }

    /**
     * Get standard deviation.
     * @return standard deviation for series of samples
     */
    public double getStdDev() {
        return stdDev;
    }

    /**
     * Get CV.
     * @return CV for series of samples
     */
    public double getCV() {
        return cv;
    }

    /**
     * Updates statistics with 'sample'
     * <p>
     * @param sample a sample added to the series
     */
    public void update(double sample) {
        // Adjust min&max
        if (0L == count) {
            min = max = sample;
        } else {
            min = Math.min(sample, min);
            max = Math.max(sample, max);
        }

        // Update average
        average += (sample - average) / ++count;
        _pwrSumAverage += ( sample * sample - _pwrSumAverage) / count;

        // Update variance
        stdDev = Math.sqrt((_pwrSumAverage * count - count * average * average) / (count - 1));
        cv = 100 * (stdDev / average);
    }
}
