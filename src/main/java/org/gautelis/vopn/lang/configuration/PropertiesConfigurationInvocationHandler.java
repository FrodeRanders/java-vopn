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
package  org.gautelis.vopn.lang.configuration;

import org.gautelis.vopn.lang.Configurable;
import org.gautelis.vopn.lang.ConfigurationTool;
import org.gautelis.vopn.lang.StringMapConfigurationResolver;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Description of PropertiesConfigurationInvocationHandler:
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class PropertiesConfigurationInvocationHandler implements InvocationHandler {
    private static final String DESCRIPTION =
            "Dynamic configuration object handled by proxy by " +
                    PropertiesConfigurationInvocationHandler.class.getSimpleName();

    // Use comma as a separator
    // e.g. hosts=host1,host2,host3
    private static final String DEFAULT_SEPARATOR_RE = "\\s*,\\s*";

    private Collection<ConfigurationTool.ConfigurationResolver> resolvers = new ArrayList<ConfigurationTool.ConfigurationResolver>();

    /**
     * Creates a handler backed by a properties object.
     *
     * @param properties configuration properties
     */
    public PropertiesConfigurationInvocationHandler(Properties properties) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<?,?> entry : properties.entrySet()) {
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
        resolvers.add(new StringMapConfigurationResolver(map));
    }

    /**
     * Creates a handler backed by properties and additional resolvers.
     *
     * @param properties configuration properties
     * @param resolvers additional configuration resolvers
     */
    public PropertiesConfigurationInvocationHandler(
            Properties properties, Collection<ConfigurationTool.ConfigurationResolver> resolvers
    ) {
        this.resolvers.addAll(resolvers);
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<?,?> entry : properties.entrySet()) {
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
        resolvers.add(new StringMapConfigurationResolver(map));
    }

    /**
     * Resolves configuration values for proxy method invocations.
     *
     * @param proxy proxy instance
     * @param method configuration method
     * @param params invocation parameters
     * @return resolved value
     */
    public Object invoke(Object proxy, Method method, Object[] params) {
        if ("toString".equals(method.getName())) {
            return DESCRIPTION;
        }

        Configurable binding = method.getAnnotation(Configurable.class);
        if (null == binding) {
            throw new RuntimeException("Method is not bound to a Configurable property: " + method);
        }

        // If we have a Configurable property name, use it - otherwise fall back on method name...
        String key = (null != binding.property() && !binding.property().isEmpty()) ? binding.property() : method.getName();

        //-----------------------------------------------------------------------
        // Properties only deals with Strings, so we will have to cast the
        // value into the correct type - identified by the method return type.
        //-----------------------------------------------------------------------
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            Object value = resolver.resolve(key);
            if (null != value) {
                return cast(method.getName(), ((String)value).trim(), method.getReturnType());
            }
        }
        return cast(method.getName(), binding.value().trim(), method.getReturnType());
    }

    private Object cast(String name, String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // Handle array types first: String[], File[], int[], etc.
        if (targetType.isArray()) {
            Class<?> componentType = targetType.getComponentType();

            String[] parts = value.split(DEFAULT_SEPARATOR_RE);

            Object array = Array.newInstance(componentType, parts.length);
            for (int i = 0; i < parts.length; i++) {
                String elementValue = parts[i];

                // Scalar casting logic for each element
                Object element = castScalar(name + "[" + i + "]", elementValue, componentType);
                Array.set(array, i, element);
            }
            return array;
        }

        // Non-array types
        return castScalar(name, value, targetType);
    }

    /**
     * Handles only scalar (non-array) types.
     */
    private Object castScalar(String name, String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(String.class)) {
            return value;

        } else if (targetType == Integer.class || targetType == int.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Could not treat return value of " + name + " as integer: \"" + value + "\"");
            }

        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);

        } else if (targetType == File.class) {
            return new File(value);

        } else {
            // Fallback
            return value;
        }
    }
}
