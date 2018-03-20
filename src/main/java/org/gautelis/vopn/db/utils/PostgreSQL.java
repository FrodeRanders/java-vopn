/*
 * Copyright (C) 2016 Frode Randers
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
 * <p>
 * Created by Frode Randers at 2016-02-18 10:29
 */
public class PostgreSQL extends Manager {

    public PostgreSQL(DataSource dataSource, Options options) {
        super(dataSource, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: PostgreSQL");
    }

    public PostgreSQL(Properties properties, Options options, PrepareDataSource preparer) throws Exception {
        super(preparer, properties, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: PostgreSQL");
    }

    /**
     * Creates datasource.
     * <p>
     * @param applicationName
     * @param config
     * @return
     * @throws DatabaseException
     * @throws ClassCastException
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
        dataSource.setDriverClassName(config.driver());

        String database = config.database();
        if (null != database && database.length() > 0) {
            dataSource.addConnectionProperty("databaseName", database);
        }

        String user = config.user();
        if (null != user && user.length() > 0) {
            dataSource.setUsername(user);
        }

        String password = config.password();
        if (null != password && password.length() > 0) {
            dataSource.setPassword(password);
        }

        String url = config.url();
        if (null != url && url.length() > 0) {
            dataSource.setUrl(url);
        }

        dataSource.setMaxActive(config.maxActive());
        dataSource.setMaxIdle(config.maxIdle());

        dataSource.addConnectionProperty("description", applicationName);

        return dataSource;
    }
}
