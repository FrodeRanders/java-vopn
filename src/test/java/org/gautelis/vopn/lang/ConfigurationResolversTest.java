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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationResolversTest {

    interface DefaultConfig {
        @Configurable(property = "name")
        String name();

        @Configurable(property = "ints")
        int[] ints();

        @Configurable(property = "enabled")
        boolean enabled();

        @Configurable(property = "files")
        File[] files();

        @Configurable(property = "hosts")
        String[] hosts();
    }

    interface PropertiesConfig {
        @Configurable(property = "hosts")
        String[] hosts();
    }

    @Test
    public void testStringMapResolverTrims() {
        StringMapConfigurationResolver resolver = new StringMapConfigurationResolver(
                Map.of("key", "  value ")
        );

        assertEquals("value", resolver.resolve("key"));
    }

    @Test
    public void testSystemPropertiesResolver() {
        String key = "vopn.test.prop";
        System.setProperty(key, "  yes ");
        try {
            SystemPropertiesConfigurationResolver resolver = new SystemPropertiesConfigurationResolver();
            assertEquals("yes", resolver.resolve(key));
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void testSystemEnvironmentResolverMissing() {
        SystemEnvironmentConfigurationResolver resolver = new SystemEnvironmentConfigurationResolver();
        assertNull(resolver.resolve("vopn-test-env-does-not-exist-123"));
    }

    @Test
    public void testBundledPropertiesResolverProperties() throws Exception {
        BundledPropertiesConfigurationResolver resolver =
                new BundledPropertiesConfigurationResolver(ConfigurationResolversTest.class, "config-test.properties");

        assertEquals("alice", resolver.resolve("name"));
    }

    @Test
    public void testBundledPropertiesResolverXml() throws Exception {
        BundledPropertiesConfigurationResolver resolver =
                new BundledPropertiesConfigurationResolver(ConfigurationResolversTest.class, "config-test.xml");

        assertEquals("bob", resolver.resolve("xml-name"));
        assertEquals("true", resolver.resolve("xml-flag"));
    }

    @Test
    public void testLoadFromResourceProperties() throws Exception {
        Properties properties = ConfigurationTool.loadFromResource(
                ConfigurationResolversTest.class, "config-test.properties");

        assertEquals("alice", properties.getProperty("name").trim());
    }

    @Test
    public void testLoadFromResourceXml() throws Exception {
        Properties properties = ConfigurationTool.loadFromResource(
                ConfigurationResolversTest.class, "config-test.xml");

        assertEquals("bob", properties.getProperty("xml-name").trim());
    }

    @Test
    public void testBindWithResolversAndDefaults() {
        Map<String, Object> defaults = Map.of(
                "name", "default",
                "ints", "1,2,3",
                "enabled", "true",
                "files", List.of("a.txt", "b.txt"),
                "hosts", List.of("h1", "h2")
        );

        ConfigurationTool.ConfigurationResolver override = name -> {
            if ("name".equals(name)) {
                return "override";
            }
            return null;
        };

        DefaultConfig config = ConfigurationTool.bind(DefaultConfig.class, defaults, List.of(override));

        assertEquals("override", config.name());
        assertArrayEquals(new int[]{1, 2, 3}, config.ints());
        assertTrue(config.enabled());
        assertEquals("a.txt", config.files()[0].getPath());
        assertArrayEquals(new String[]{"h1", "h2"}, config.hosts());
    }

    @Test
    public void testBindPropertiesArrayParsing() {
        Properties properties = new Properties();
        properties.setProperty("hosts", "one, two, three");

        PropertiesConfig config = ConfigurationTool.bindProperties(PropertiesConfig.class, properties);

        assertArrayEquals(new String[]{"one", "two", "three"}, config.hosts());
    }
}
