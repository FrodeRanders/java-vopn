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

public class NumberTest {

    @Test
    public void testBytesBelowThreshold() {
        assertEquals("2047B", Number.asHumanApproximate(2047L));
    }

    @Test
    public void testKibibytesThreshold() {
        assertEquals("2KiB", Number.asHumanApproximate(2048L));
    }

    @Test
    public void testMebibytesThreshold() {
        assertEquals("2MiB", Number.asHumanApproximate(2_097_152L));
    }

    @Test
    public void testGibibytesWithSeparator() {
        assertEquals("2 GiB", Number.asHumanApproximate(2_147_483_648L, " "));
    }
}
