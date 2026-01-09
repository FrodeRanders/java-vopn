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

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.fail;

public class LocalDatabaseTest {

    @Test
    public void testCreateObjectIgnoresAlreadyExists() throws Exception {
        Connection connection = connectionWithPreparedStatement(sqlStateException("X0Y32"));
        LocalDatabase.createObject(connection, "create table t (id int)");
    }

    @Test
    public void testCreateObjectThrowsOnOtherErrors() throws Exception {
        Connection connection = connectionWithPreparedStatement(sqlStateException("XX999"));
        try {
            LocalDatabase.createObject(connection, "create table t (id int)");
            fail("Expected SQLException");
        } catch (SQLException expected) {
            // expected
        }
    }

    private static SQLException sqlStateException(String state) {
        return new SQLException("boom", state);
    }

    private static Connection connectionWithPreparedStatement(SQLException toThrow) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        return preparedStatementWithException(toThrow);
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static PreparedStatement preparedStatementWithException(SQLException toThrow) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("executeUpdate".equals(method.getName())) {
                        throw toThrow;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
