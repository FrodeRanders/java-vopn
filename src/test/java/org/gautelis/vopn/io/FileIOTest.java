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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileIOTest {

    @Test
    public void testWriteToTempFileFromString() throws IOException {
        File tmp = FileIO.writeToTempFile("hello", "vopn", "txt");
        String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);

        assertEquals("hello", content);
        assertTrue(FileIO.delete(tmp));
    }

    @Test
    public void testWriteToTempFileFromByteBuffer() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap("data".getBytes(StandardCharsets.UTF_8));
        File tmp = FileIO.writeToTempFile(buffer, "vopn", "bin");
        String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);

        assertEquals("data", content);
        assertTrue(FileIO.delete(tmp));
    }

    @Test
    public void testWriteToTempFileFromByteArrays() throws IOException {
        List<byte[]> parts = List.of(
                "a".getBytes(StandardCharsets.UTF_8),
                "b".getBytes(StandardCharsets.UTF_8),
                "c".getBytes(StandardCharsets.UTF_8)
        );
        File tmp = FileIO.writeToTempFile(parts, "vopn", "bin");
        String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);

        assertEquals("abc", content);
        assertTrue(FileIO.delete(tmp));
    }

    @Test
    public void testCopyAndDeleteDirectory() throws IOException {
        File srcDir = Files.createTempDirectory("vopn-src").toFile();
        File destDir = Files.createTempDirectory("vopn-dest").toFile();
        File srcFile = new File(srcDir, "input.txt");
        Files.writeString(srcFile.toPath(), "copy", StandardCharsets.UTF_8);

        FileIO.copy(srcDir, destDir);

        File copied = new File(destDir, "input.txt");
        assertTrue(copied.exists());
        assertEquals("copy", Files.readString(copied.toPath(), StandardCharsets.UTF_8));

        assertTrue(FileIO.delete(srcDir));
        assertTrue(FileIO.delete(destDir));
        assertFalse(srcDir.exists());
        assertFalse(destDir.exists());
    }
}
