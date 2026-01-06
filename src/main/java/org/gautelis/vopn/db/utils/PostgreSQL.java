/*
 * Copyright (C) 2016-2026 Frode Randers
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
package org.gautelis.vopn.db.utils;

import org.gautelis.vopn.db.Database;
import org.gautelis.vopn.db.DatabaseException;

import org.apache.commons.dbcp.BasicDataSource;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Description of PostgreSQL:
 * <p>
 * Created by Frode Randers at 2016-02-18 10:29
 */
public class PostgreSQL extends Manager {

    /**
     * Creates a PostgreSQL manager using a pre-configured datasource.
     *
     * @param dataSource datasource instance
     * @param options manager options
     */
    public PostgreSQL(DataSource dataSource, Options options) {
        super(dataSource, options, ";", /* ignore case? */ false, /* alone on line? */ true);

        // Always print database name to stdout.
        System.out.println("Target database: PostgreSQL");
    }

    /**
     * Creates a PostgreSQL manager using configuration properties.
     *
     * @param properties database configuration properties
     * @param options manager options
     * @param preparer datasource preparation callback
     * @throws Exception if initialization fails
     */
    public PostgreSQL(Properties properties, Options options, PrepareDataSource preparer) throws Exception {
        super(preparer, properties, options, ";", /* ignore case? */ false, /* alone on line? */ true);

        // Always print database name to stdout.
        System.out.println("Target database: PostgreSQL");
    }

    /**
     * Creates datasource.
     * <p>
     * @param applicationName application name to store on the datasource
     * @param config database configuration
     * @return configured datasource
     * @throws DatabaseException if a driver cannot be loaded
     * @throws ClassCastException if the datasource type is unexpected
     */
    public static DataSource getDataSource(
            String applicationName,
            Database.Configuration config
    ) throws DatabaseException, ClassCastException {

        /*
         * PGPoolingDataSource has been deprecated.
         *
        DataSource dataSource = Database.getDataSource(config);

        org.postgresql.ds.PGPoolingDataSource ds = (org.postgresql.ds.PGPoolingDataSource) dataSource;
        ds.setApplicationName(applicationName);
        ds.setDatabaseName(config.database());
        ds.setUser(config.user());
        ds.setPassword(config.password());
        ds.setServerName(config.server());
        ds.setPortNumber(config.port());
        ds.setMaxConnections(10);
        */
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setPoolPreparedStatements(true);
        dataSource.setDriverClassName(config.driver());

        String database = config.database();
        if (null != database && !database.isEmpty()) {
            dataSource.addConnectionProperty("databaseName", database);
        }

        String user = config.user();
        if (null != user && !user.isEmpty()) {
            dataSource.setUsername(user);
        }

        String password = config.password();
        if (null != password && !password.isEmpty()) {
            dataSource.setPassword(password);
        }

        String url = config.url();
        if (null != url && !url.isEmpty()) {
            dataSource.setUrl(url);
        }

        dataSource.setMaxActive(config.maxActive());
        dataSource.setMaxIdle(config.maxIdle());

        dataSource.addConnectionProperty("description", applicationName);

        return dataSource;
    }
}
