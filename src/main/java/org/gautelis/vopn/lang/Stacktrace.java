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
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package  org.gautelis.vopn.lang;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Utility helpers for rendering stack traces and extracting root causes.
 */
public class Stacktrace {
    /**
     * Renders a throwable stack trace as a string.
     *
     * @param t throwable to render
     * @return stack trace string
     */
	public static String asString(Throwable t) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bytes, true);
        if (null == t) {
            t = new IllegalArgumentException("no argument");
        }
        t.printStackTrace(writer);
        return bytes.toString();
	}

    /**
     * Returns the deepest cause in a throwable chain.
     *
     * @param t throwable to inspect
     * @return deepest cause or the original throwable
     */
    public static Throwable getBaseCause(Throwable t) {
        Throwable cause = null;
        Throwable c = t.getCause();
        if (null != c) {
            do {
                cause = c;
                c = c.getCause();
            } while (null != c);
        }

        if (null != cause) {
            t = cause;
        }

        return t;
    }
}
