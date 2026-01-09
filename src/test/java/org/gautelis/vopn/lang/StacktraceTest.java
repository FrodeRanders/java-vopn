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
package org.gautelis.vopn.lang;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StacktraceTest {

    @Test
    public void testAsStringIncludesMessage() {
        IllegalStateException exception = new IllegalStateException("boom");
        String stack = Stacktrace.asString(exception);

        assertTrue(stack.contains("IllegalStateException"));
        assertTrue(stack.contains("boom"));
    }

    @Test
    public void testAsStringNullUsesPlaceholder() {
        String stack = Stacktrace.asString(null);

        assertTrue(stack.contains("IllegalArgumentException"));
        assertTrue(stack.contains("no argument"));
    }

    @Test
    public void testGetBaseCause() {
        Throwable root = new IllegalArgumentException("root");
        Throwable mid = new RuntimeException("mid", root);
        Throwable top = new Exception("top", mid);

        assertEquals(root, Stacktrace.getBaseCause(top));
    }
}
