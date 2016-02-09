/*
 * Copyright (C) 2011-2016 Frode Randers
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
package eu.ensure.vopn;

import eu.ensure.vopn.lang.LoggingUtils;
import eu.ensure.vopn.lang.TimeDelta;
import eu.ensure.vopn.statistics.MovingAverage;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

/**
 * <p>
 * Created by Frode Randers at 2012-10-23 11:00
 */
public class CommonTest extends TestCase {
    private static final Logger log = LoggingUtils.setupLoggingFor(CommonTest.class, "log4j2.xml");

    @Test
    public void testTimeDelta() {
        System.out.println("\nTesting human readable time periods (millisecs to text):");

        long halfASecond = 500;
        System.out.println("500 ms -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(halfASecond)));

        long oneAndAHalfSecond = 1500;
        System.out.println("1500 ms -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(oneAndAHalfSecond)));

        long sixtyFiveSeconds = 65 * 1000;
        System.out.println("65 s -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(sixtyFiveSeconds)));

        long sixtyFiveMinutes = 65 * 60 * 1000;
        System.out.println("65 min -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(sixtyFiveMinutes)));

        long twentyFiveHours = 25 * 60 * 60 * 1000;
        System.out.println("25 hours -> " + TimeDelta.asHumanApproximate(BigInteger.valueOf(twentyFiveHours)));

        BigInteger thirtyFiveDays = BigInteger.valueOf(35).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("35 days -> " + TimeDelta.asHumanApproximate(thirtyFiveDays));

        BigInteger thirteenMonths = BigInteger.valueOf(13 * 30).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("13 months -> " + TimeDelta.asHumanApproximate(thirteenMonths));

        BigInteger tenYears = BigInteger.valueOf(10 * 12 * 30).multiply(BigInteger.valueOf(24 * 60 * 60 * 1000));
        System.out.println("10 years -> " + TimeDelta.asHumanApproximate(tenYears));
    }

    @Test
    public void testStatistics() {
        {
            MovingAverage ma1 = new MovingAverage();

            int sum = 0;
            int i;

            for (i = 0; i < 100; i++) {
                sum += i;
                ma1.update(i); // cast
            }

            assertEquals(((double) sum) / i, ma1.getAverage());
            assertEquals(i, ma1.getCount());
        }
        {
            MovingAverage ma2 = new MovingAverage();
            int[] samples = {3, 7, 5, 13, 20, 23, 39, 23, 40, 23, 14, 12, 56, 23, 29};
            for (int sample : samples) {
                ma2.update(sample); // cast
            }

            double sum = 0;
            double average = ma2.getAverage();
            for (int sample : samples) {
                sum += Math.pow(sample - average, 2);
            }
            double stdDev = Math.sqrt(sum / (samples.length - 1));

            assertEquals(22.0, ma2.getAverage());
            assertEquals(samples.length, ma2.getCount());
            assertEquals(stdDev, ma2.getStdDev(), /* acceptable delta */ 1E-13);
        }
        {
            double sum = 0.0;
            int i;

            MovingAverage ma3 = new MovingAverage();
            for (i = 0; i < 100; i++) {
                double sample = Math.random();
                ma3.update(sample);
                sum += sample;
            }

            assertEquals(sum / i, ma3.getAverage(), /* acceptable delta */ 1E-15);
            assertEquals(i, ma3.getCount());
        }
    }
}
