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

import org.gautelis.vopn.lang.fixtures.DynamicTestPlugin;
import org.gautelis.vopn.lang.fixtures.NoDefaultConstructorPlugin;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicLoaderTest {

    @Test
    public void testLoadWithInitializerAndAssignKey() throws Exception {
        DynamicLoader<DynamicTestPlugin> loader = new DynamicLoader<>("plugin");
        Properties properties = new Properties();
        properties.setProperty("alpha", DynamicTestPlugin.class.getName());

        loader.load(properties, plugin -> plugin.setName("initialized"), true);

        assertTrue(loader.containsKey("alpha"));
        DynamicTestPlugin plugin = loader.get("alpha");
        assertEquals("alpha", plugin.getAssignedKey());
        assertEquals("initialized", plugin.getName());
    }

    @Test
    public void testCallMethodWithExplicitParameterTypes() throws Throwable {
        DynamicLoader<DynamicTestPlugin> loader = new DynamicLoader<>("plugin");
        DynamicTestPlugin plugin = new DynamicTestPlugin();

        List<String> values = new ArrayList<>();
        values.add("a");
        values.add("b");

        loader.callMethodOn(plugin, "acceptList", new Object[]{values}, new Class<?>[]{List.class});
        assertEquals(2, plugin.getListSize());
    }

    @Test(expected = ClassNotFoundException.class)
    public void testCallMethodWithInferredTypesFailsForInterfaces() throws Throwable {
        DynamicLoader<DynamicTestPlugin> loader = new DynamicLoader<>("plugin");
        DynamicTestPlugin plugin = new DynamicTestPlugin();

        List<String> values = new ArrayList<>();
        loader.callMethodOn(plugin, "acceptList", new Object[]{values});
    }

    @Test(expected = ClassNotFoundException.class)
    public void testCreateObjectFailsWithoutNoArgConstructor() throws Exception {
        DynamicLoader<NoDefaultConstructorPlugin> loader = new DynamicLoader<>("plugin");
        Class<?> clazz = loader.createClass(NoDefaultConstructorPlugin.class.getName());
        loader.createObject(NoDefaultConstructorPlugin.class.getName(), clazz);
    }
}
