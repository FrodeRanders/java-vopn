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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StringBufferDataSourceTest {

    @Test
    public void testMetadataAndRead() throws IOException {
        StringBuffer buffer = new StringBuffer("hello");
        StringBufferDataSource dataSource = new StringBufferDataSource("data", "text/plain", buffer);

        assertEquals("data", dataSource.getName());
        assertEquals("text/plain", dataSource.getContentType());

        try (InputStream in = dataSource.getInputStream()) {
            byte[] data = in.readAllBytes();
            assertEquals("hello", new String(data, StandardCharsets.UTF_8));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetOutputStreamThrows() throws IOException {
        StringBufferDataSource dataSource = new StringBufferDataSource("data", "text/plain", new StringBuffer());
        dataSource.getOutputStream();
    }
}
