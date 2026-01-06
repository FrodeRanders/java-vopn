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
package org.gautelis.vopn;

import org.gautelis.vopn.lang.Configurable;
import org.gautelis.vopn.lang.ConfigurationTool;
import org.gautelis.vopn.lang.TimeDelta;
import org.gautelis.vopn.server.BasicServer;
import org.gautelis.vopn.server.Configuration;
import org.gautelis.vopn.server.RequestProcessor;
import org.gautelis.vopn.server.Server;
import org.gautelis.vopn.statistics.MovingAverage;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Created by Frode Randers at 2012-10-23 11:00
 */
public class CommonTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(CommonTest.class);

    @Test
    public void testDateConversions() {
        Locale locale = Locale.forLanguageTag("sv");
        {
            String date = "1945-09-28";

            java.sql.Date convertedDate = org.gautelis.vopn.lang.Date.convertDate(date, locale);
            System.out.println("Input date: " + date + " -> " + convertedDate);
        }
        {
            String date = "2016-09-10";

            java.sql.Date convertedDate = org.gautelis.vopn.lang.Date.convertDate(date, locale);
            System.out.println("Input date: " + date + " -> " + convertedDate);
        }
        {
            String date = "3016-09-10";

            java.sql.Date convertedDate = org.gautelis.vopn.lang.Date.convertDate(date, locale);
            System.out.println("Input date: " + date + " -> " + convertedDate);
        }
    }

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

            log.debug("average=" + average + " stddev=" + stdDev);
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

    interface ProxyTest {
        @Configurable(property = "stringConfigurable", value = "default value")
        String stringTest();

        @Configurable(property = "intConfigurable", value = "43")
        int intTest();

        @Configurable(property = "booleanConfigurable", value = "false")
        boolean booleanTest();
    }

    @Test
    public void testConfigurable() {
        Properties properties = new Properties();
        properties.putAll(Map.of(
        "stringConfigurable", "string value",
        "intConfigurable", "42",
        "booleanConfigurable", "true"
        ));

        ProxyTest pt = ConfigurationTool.bindProperties(ProxyTest.class, properties);

        assertEquals("string value", pt.stringTest());
        assertEquals(42, pt.intTest());
        assertTrue(pt.booleanTest());
    }

    @Test
    public void testServer() {
        Server[] server = { null };
        Thread t1 = new Thread(() -> {
            try {
                // Initialize configuration
                final Properties properties = new Properties();
                properties.setProperty("local-host", "localhost");
                properties.setProperty("local-port", "10081"); // famdc

                Configuration config = ConfigurationTool.bindProperties(Configuration.class, properties);

                server[0] = new BasicServer<>(config, null);
                server[0].start();
            }
            catch (Throwable t) {
                String info = "Server failure: " + t.getMessage();
                log.error(info, t);
                System.err.println(info);
            }
        });

        try {
            t1.start();
            Thread.sleep(2000);
        }
        catch (InterruptedException ignore) {}
        {
            String host = "localhost";
            int port = 10081;

            try (Socket socket = new Socket(host, port)) {
                System.out.println("Connected to server: " + host + ":" + port);

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                String message = "Hello from client!\n";
                out.write(message.getBytes(StandardCharsets.UTF_8));
                out.flush();

                System.out.println("Sent: " + message.trim());

                // Read response (echo)
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);

                if (bytesRead != -1) {
                    String reply = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    System.out.println("Received: " + reply.trim());
                } else {
                    System.out.println("Server closed the connection.");
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        try {
            server[0].requestShutdown("test is over");
            Thread.sleep(5000);
        }
        catch (InterruptedException ignore) {}

        t1.interrupt();
    }
}