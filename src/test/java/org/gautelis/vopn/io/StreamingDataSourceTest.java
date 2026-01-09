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
package org.gautelis.vopn.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StreamingDataSourceTest {

    @Test
    public void testStreamingDataSource() throws IOException {
        byte[] data = "stream".getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(data);
        StreamingDataSource ds = new StreamingDataSource("data.txt", "text/plain", stream);

        assertEquals("data.txt", ds.getName());
        assertEquals("text/plain", ds.getContentType());

        byte[] read = ds.getInputStream().readAllBytes();
        assertEquals("stream", new String(read, StandardCharsets.UTF_8));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetOutputStreamThrows() throws IOException {
        StreamingDataSource ds = new StreamingDataSource("data.txt", "text/plain", new ByteArrayInputStream(new byte[0]));
        ds.getOutputStream();
    }
}
