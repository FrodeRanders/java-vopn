/*
 * Copyright (C) 2014-2020 Frode Randers
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
package org.gautelis.vopn.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This configuration resolver will (try to) lookup configuration from the system environment.
 * <p>
 * Names will be adjusted to meet system environment requirements (e.g. the '-' is not accepted
 * in an environment variable and environment variables are traditionally uppercase).
 * </p>
 */
public class SystemEnvironmentConfigurationResolver implements ConfigurationTool.ConfigurationResolver {
    private static final Logger log = LoggerFactory.getLogger(SystemEnvironmentConfigurationResolver.class);

    @Override
    public Object resolve(String name) {
        //
        // Just in case: "name-part" -> "NAME_PART"
        //
        String _name = name.toUpperCase().replace('-', '_');
        String value = System.getenv(_name);

        if (null != value) {
            value = value.trim();

            if (log.isTraceEnabled()) {
                String info = "Successfully resolved \"" + _name + "\" from system environment: " + value;
                Exception syntheticException = new Exception();
                for (StackTraceElement element : syntheticException.getStackTrace()) {
                    info += "\n at " + element.toString();
                }
                log.trace(info);
            }
        } else {
            log.info("Unable to resolve \"{}\" from system environment", name);
        }
        return value;
    }
}
