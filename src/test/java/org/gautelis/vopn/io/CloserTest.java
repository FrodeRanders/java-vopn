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

import java.io.Closeable;
import java.io.IOException;

public class CloserTest {

    @Test
    public void testCloseHandlesNull() {
        Closer.close(null);
    }

    @Test
    public void testCloseSuppressesException() {
        Closeable failing = new Closeable() {
            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        };

        Closer.close(failing);
    }
}
