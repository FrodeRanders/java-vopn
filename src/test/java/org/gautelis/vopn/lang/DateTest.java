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
package org.gautelis.vopn.lang;

import org.junit.Test;

import java.sql.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class DateTest {

    @Test
    public void testDateConversions() {
        Locale locale = Locale.forLanguageTag("sv");

        assertEquals(Date.valueOf("1945-09-28"), org.gautelis.vopn.lang.Date.convertDate("1945-09-28", locale));
        assertEquals(Date.valueOf("2016-09-10"), org.gautelis.vopn.lang.Date.convertDate("2016-09-10", locale));
        assertEquals(Date.valueOf("3016-09-10"), org.gautelis.vopn.lang.Date.convertDate("3016-09-10", locale));
    }
}
