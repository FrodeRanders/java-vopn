/*
 * Copyright (C) 2014-2016 Frode Randers
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This configuration resolver will (try to) lookup configuration in a Map&lt;String, String&gt;.
 * <p>
 * Created by Frode Randers at 2014-02-06 16:17
 */
public class StringMapConfigurationResolver implements ConfigurationTool.ConfigurationResolver {
    private static final Logger log = LoggerFactory.getLogger(StringMapConfigurationResolver.class);

    private final Map<String, String> map;

    public StringMapConfigurationResolver(final Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Object resolve(String name) {
        Object value = map.get(name);

        if (null != value && log.isDebugEnabled()) {
            String info = "Successfully resolved \"" + name + "\" from environment: " + value;
            log.debug(info);
        }

        return value;
    }
}
