/*
 * Copyright (C) 2011-2025 Frode Randers
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
package org.gautelis.vopn.db.utils;

/**
 * Description of Characteristics:
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class Characteristics {
    public String batchSeparator = "";
    public boolean ignoreCase = false;
    public boolean separatorAloneOnLine = false;

    /**
     * Creates characteristics with a batch separator, ignore-case enabled,
     * and separator assumed to be alone on the line.
     *
     * @param batchSeparator batch separator token
     */
    public Characteristics(String batchSeparator) {
        this.batchSeparator = batchSeparator;
    }

    /**
     * Creates characteristics with a batch separator and case handling.
     *
     * @param batchSeparator batch separator token
     * @param ignoreCase whether to ignore case when matching separator
     */
    public Characteristics(String batchSeparator, boolean ignoreCase) {
        this.batchSeparator = batchSeparator;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Creates characteristics with explicit parsing rules.
     *
     * @param batchSeparator batch separator token
     * @param ignoreCase whether to ignore case when matching separator
     * @param separatorAloneOnLine whether the separator must be alone on the line
     */
    public Characteristics(String batchSeparator, boolean ignoreCase, boolean separatorAloneOnLine) {
        this.batchSeparator = batchSeparator;
        this.ignoreCase = ignoreCase;
        this.separatorAloneOnLine = separatorAloneOnLine;
    }

    /**
     * Returns the configured batch separator.
     *
     * @return batch separator token
     */
    public String getSeparator() {
        return batchSeparator;
    }

    /**
     * Returns whether batch separator matching is case-insensitive.
     *
     * @return {@code true} if case is ignored
     */
    public boolean doIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Returns whether separators must appear alone on a line.
     *
     * @return {@code true} if the separator must be alone on a line
     */
    public boolean aloneOnLine() {
        return separatorAloneOnLine;
    }
}
