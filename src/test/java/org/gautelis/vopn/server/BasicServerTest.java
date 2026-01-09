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
package org.gautelis.vopn.server;

import org.gautelis.vopn.lang.ConfigurationTool;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicServerTest {
    private static final Logger log = LoggerFactory.getLogger(BasicServerTest.class);

    @Test
    public void testServer() {
        Server[] server = { null };
        Thread t1 = new Thread(() -> {
            try {
                Properties properties = new Properties();
                properties.setProperty("local-host", "localhost");
                properties.setProperty("local-port", "10081");

                Configuration config = ConfigurationTool.bindProperties(Configuration.class, properties);

                server[0] = new BasicServer<>(config, null);
                server[0].start();
            } catch (Throwable t) {
                String info = "Server failure: " + t.getMessage();
                log.error(info, t);
                System.err.println(info);
            }
        });

        try {
            t1.start();
            Thread.sleep(2000);
        } catch (InterruptedException ignore) {}

        String host = "localhost";
        int port = 10081;

        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            String message = "Hello from client!\n";
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            assertTrue(bytesRead > 0);
            String reply = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            assertEquals(message, reply);
        } catch (IOException e) {
            throw new RuntimeException("Client error: " + e.getMessage(), e);
        }

        try {
            server[0].requestShutdown("test is over");
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {}

        t1.interrupt();
    }
}
