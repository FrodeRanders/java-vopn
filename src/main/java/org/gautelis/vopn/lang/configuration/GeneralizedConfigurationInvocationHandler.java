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
package org.gautelis.vopn.lang.configuration;

import org.gautelis.vopn.lang.Configurable;
import org.gautelis.vopn.lang.ConfigurationTool;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Description of GeneralizedConfigurationInvocationHandler:
 * <p>
 * Created by Frode Randers at 2013-01-19 12:55
 */
public class GeneralizedConfigurationInvocationHandler implements InvocationHandler {
    private static final String DESCRIPTION =
            "Dynamic configuration object handled by proxy by " +
                    GeneralizedConfigurationInvocationHandler.class.getSimpleName();

    // Use comma as a separator
    // e.g. hosts=host1,host2,host3
    private static final String DEFAULT_SEPARATOR_RE = "\\s*,\\s*";

    private final Collection<ConfigurationTool.ConfigurationResolver> resolvers =
            new ArrayList<>();

    /**
     * Simple map-backed resolver. Keys are property names, values are Objects.
     */
    private static class DefaultResolver implements ConfigurationTool.ConfigurationResolver {
        private final Map<String, Object> map;

        DefaultResolver(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public Object resolve(String name) {
            return map.get(name);
        }
    }

    public GeneralizedConfigurationInvocationHandler(Map<String, Object> map) {
        this.resolvers.add(new DefaultResolver(map));
    }

    /**
     * Creates a handler with additional resolvers, falling back to the provided map.
     *
     * @param map default values map
     * @param resolvers additional configuration resolvers
     */
    public GeneralizedConfigurationInvocationHandler(
            Map<String, Object> map,
            Collection<ConfigurationTool.ConfigurationResolver> resolvers
    ) {
        this.resolvers.addAll(resolvers);
        this.resolvers.add(new DefaultResolver(map));
    }

    /**
     * Resolves configuration values for proxy method invocations.
     *
     * @param proxy proxy instance
     * @param method configuration method
     * @param params invocation parameters
     * @return resolved value
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] params) {
        if ("toString".equals(method.getName())) {
            return DESCRIPTION;
        }

        Configurable binding = method.getAnnotation(Configurable.class);
        if (binding == null) {
            throw new RuntimeException("Method is not bound to a Configurable property: " + method);
        }

        // If we have a Configurable property name, use it - otherwise fall back on method name...
        String key = (binding.property() != null && !binding.property().isEmpty())
                ? binding.property()
                : method.getName();

        Class<?> targetType = method.getReturnType();

        // Try all resolvers
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            Object value = resolver.resolve(key);
            if (value != null) {
                return cast(method.getName(), value, targetType);
            }
        }

        // No configured value - fall back to default from annotation (if any)
        String defaultValue = binding.value();
        if (defaultValue != null) {
            defaultValue = defaultValue.trim();
        }
        if (defaultValue != null && !defaultValue.isEmpty()) {
            return cast(method.getName(), defaultValue, targetType);
        }

        // No value and no default
        return null;
    }

    /**
     * Casts a raw value (usually from a resolver or annotation) to the method's return type.
     * Supports arrays and scalar types.
     */
    private Object cast(String name, Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }

        // Already of correct type
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        // Arrays: String[], File[], int[], etc.
        if (targetType.isArray()) {
            return castArray(name, rawValue, targetType);
        }

        // Scalars
        return castScalar(name, rawValue, targetType);
    }

    /**
     * Handles array-typed return values.
     *
     * Supported sources:
     *  - Already an array (compatible with component type)
     *  - A Collection<?> (elements cast individually)
     *  - A String, split with DEFAULT_SEPARATOR_RE
     *  - A single scalar is treated as single-element array
     */
    private Object castArray(String name, Object rawValue, Class<?> arrayType) {
        Class<?> componentType = arrayType.getComponentType();

        // Already an array: try to cast elements
        if (rawValue.getClass().isArray()) {
            int length = Array.getLength(rawValue);
            Object newArray = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                Object elementRaw = Array.get(rawValue, i);
                Object element = castScalar(name + "[" + i + "]", elementRaw, componentType);
                Array.set(newArray, i, element);
            }
            return newArray;
        }

        // Collection: cast each element
        if (rawValue instanceof Collection<?>) {
            Collection<?> coll = (Collection<?>) rawValue;
            Object newArray = Array.newInstance(componentType, coll.size());
            int i = 0;
            for (Object elementRaw : coll) {
                Object element = castScalar(name + "[" + i + "]", elementRaw, componentType);
                Array.set(newArray, i, element);
                i++;
            }
            return newArray;
        }

        // String: split on separator and cast each piece
        if (rawValue instanceof String) {
            String[] parts = ((String) rawValue).split(DEFAULT_SEPARATOR_RE);
            Object newArray = Array.newInstance(componentType, parts.length);
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                Object element = castScalar(name + "[" + i + "]", part, componentType);
                Array.set(newArray, i, element);
            }
            return newArray;
        }

        // Single scalar or single-element array
        Object newArray = Array.newInstance(componentType, 1);
        Object element = castScalar(name + "[0]", rawValue, componentType);
        Array.set(newArray, 0, element);
        return newArray;
    }

    /**
     * Handles only scalar (non-array) types.
     */
    private Object castScalar(String name, Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }

        // If already correct type, done.
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        // We'll mostly normalize via String representation when needed
        String asString = rawValue.toString();

        // String
        if (targetType == String.class) {
            return asString;
        }

        // Integer / int
        if (targetType == Integer.class || targetType == int.class) {
            try {
                return Integer.parseInt(asString);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException(
                        "Could not treat return value of " + name + " as integer: \"" + asString + "\"", nfe
                );
            }
        }

        // Boolean / boolean
        if (targetType == Boolean.class || targetType == boolean.class) {
            // Accept actual Boolean, "true"/"false", etc.
            if (rawValue instanceof Boolean) {
                return rawValue;
            }
            return Boolean.parseBoolean(asString);
        }

        // File
        if (targetType == File.class) {
            if (rawValue instanceof File) {
                return rawValue;
            }
            return new File(asString);
        }

        // Other numeric types could be added here (Long, Double, etc.)

        // Fallback – if we get here, we don't know how to cast
        throw new RuntimeException(
                "Could not treat return value of " + name +
                " as '" + targetType.getName() +
                "' when in fact it was '" + rawValue.getClass().getName() + "'"
        );
    }
}
