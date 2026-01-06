/*
 * Copyright (C) 2018-2026 Frode Randers
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
package  org.gautelis.vopn.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This configuration resolver will (try to) lookup configuration among the system properties.
 * <p>
 * Created by Frode Randers at 2014-02-06 16:13
 */
public class SystemPropertiesConfigurationResolver implements ConfigurationTool.ConfigurationResolver {
    private static final Logger log = LoggerFactory.getLogger(SystemPropertiesConfigurationResolver.class);

    @Override
    public Object resolve(String name) {
        String value = System.getProperty(name);

        if (null != value) {
            value = value.trim();

            if (log.isTraceEnabled()) {
                String info = "Successfully resolved \"" + name + "\" from system properties: " + value;
                /*
                Exception syntheticException = new Exception();
                for (StackTraceElement element : syntheticException.getStackTrace()) {
                    info += "\n at " + element.toString();
                }
                */
                log.trace(info);
            }
        } else {
            log.trace("Unable to resolve \"{}\" from system properties", name);
        }
        return value;
    }
}
