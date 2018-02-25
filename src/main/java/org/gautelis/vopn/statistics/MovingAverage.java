/*
 * Copyright (C) 2012-2016 Frode Randers
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

/*
 * Calculates statistics based on a Moving Average (MA) algorithm.
 * <p>
 * <p>
 * Created by Frode Randers at 2012-09-21 14:29
 * Amended by Göran Lindqvist at 2012-09-22 17:26
 */
public class MovingAverage {

    private double min = 0.0;
    private double max = 0.0;
    private double average = 0.0;
    private double stdDev = 0.0;
    private double cv = 0.0;

    private long count = 0L;
    private double _pwrSumAverage = 0.0; // computational use

    public MovingAverage() {
    }

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
        min = (sample < min ? sample : min);
        max = (sample > max ? sample : max);

        // Update average
        average += (sample - average) / ++count;
        _pwrSumAverage += ( sample * sample - _pwrSumAverage) / count;

        // Update variance
        stdDev = Math.sqrt((_pwrSumAverage * count - count * average * average) / (count - 1));
        cv = 100 * (stdDev / average);
    }
}
