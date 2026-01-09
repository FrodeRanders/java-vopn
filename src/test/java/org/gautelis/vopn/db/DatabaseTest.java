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
package org.gautelis.vopn.db;

import org.gautelis.vopn.db.fixtures.DummyDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseTest {

    @Test
    public void testSqueezeSQLExceptionChain() {
        SQLException first = new SQLException("first", "40001", 10);
        SQLException second = new SQLException("second", "23505", 20);
        first.setNextException(second);

        String result = Database.squeeze(first);

        assertTrue(result.contains("SQLException [first]"));
        assertTrue(result.contains("SQLstate(40001)"));
        assertTrue(result.contains("Vendor code(10)"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("SQLException [second]"));
    }

    @Test
    public void testSqueezeSQLWarningChain() {
        SQLWarning first = new SQLWarning("warn1", "01000", 1);
        SQLWarning second = new SQLWarning("warn2", "01002", 2);
        first.setNextWarning(second);

        String result = Database.squeeze(first);

        assertTrue(result.contains("SQLWarning [warn1]"));
        assertTrue(result.contains("SQLstate(01000)"));
        assertTrue(result.contains("Vendor code(1)"));
        assertTrue(result.contains(">>"));
        assertTrue(result.contains("SQLWarning [warn2]"));
    }

    @Test
    public void testLoadAndCreateDataSource() throws Exception {
        Class<? extends DataSource> clazz = Database.loadDataSource(DummyDataSource.class.getName());
        DataSource ds = Database.createDataSource(DummyDataSource.class.getName(), clazz);

        assertNotNull(ds);
        assertEquals(DummyDataSource.class, ds.getClass());
    }

    @Test(expected = ClassCastException.class)
    public void testCreateDataSourceRejectsNonDataSource() throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends DataSource> clazz = (Class<? extends DataSource>) (Class<?>) DriverManager.class;
        Database.createDataSource(DriverManager.class.getName(), clazz);
    }

    @Test
    public void testGetDataSourceFromConfiguration() throws Exception {
        Properties props = new Properties();
        props.setProperty("driver", DummyDataSource.class.getName());

        Database.Configuration config = Database.getConfiguration(props);
        DataSource ds = Database.getDataSource(config);

        assertEquals(DummyDataSource.class, ds.getClass());
    }
}
