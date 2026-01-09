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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultiDigestInputStreamTest {

    @Test
    public void testDigestsForKnownInput() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String[] algorithms = { "CRC32", "MD5" };

        MultiDigestInputStream in = new MultiDigestInputStream(algorithms, new ByteArrayInputStream(data));
        byte[] buffer = new byte[2];
        while (in.read(buffer) != -1) {
            // drain
        }

        Map<String, byte[]> digests = in.getDigests();
        assertTrue(digests.containsKey("CRC32"));
        assertTrue(digests.containsKey("MD5"));

        CRC32 crc32 = new CRC32();
        crc32.update(data);
        byte[] expectedCrc = ByteBuffer.allocate(8).putLong(crc32.getValue()).array();

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] expectedMd5 = md5.digest(data);

        assertArrayEquals(expectedCrc, digests.get("CRC32"));
        assertArrayEquals(expectedMd5, digests.get("MD5"));
    }

    @Test
    public void testMarkNotSupported() throws IOException {
        MultiDigestInputStream in = new MultiDigestInputStream(new ByteArrayInputStream(new byte[]{1}));
        assertFalse(in.markSupported());
    }
}
