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

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class TimeDeltaTest {

    @Test
    public void testZeroDuration() {
        assertEquals("", TimeDelta.asHumanApproximate(0L));
    }

    @Test
    public void testMillisecondsOnly() {
        assertEquals("999ms", TimeDelta.asHumanApproximate(BigInteger.valueOf(999L)));
    }

    @Test
    public void testSecondsAndMilliseconds() {
        assertEquals("1s 500ms", TimeDelta.asHumanApproximate(1500L));
    }

    @Test
    public void testMinutesAndSeconds() {
        assertEquals("1min 1s", TimeDelta.asHumanApproximate(61_000L));
    }
}
