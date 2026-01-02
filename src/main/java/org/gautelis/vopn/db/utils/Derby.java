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
package org.gautelis.vopn.db.utils;

import org.gautelis.vopn.db.Database;
import org.gautelis.vopn.db.DatabaseException;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// For shutdown() purposes

/**
 * Description of Derby:
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class Derby extends Manager {

    /**
     * Creates a Derby manager using a pre-configured datasource.
     *
     * @param dataSource datasource instance
     * @param options manager options
     */
    public Derby(DataSource dataSource, Options options) {
        super(dataSource, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: Derby");
    }

    /**
     * Creates a Derby manager using configuration properties.
     *
     * @param properties database configuration properties
     * @param options manager options
     * @param preparer datasource preparation callback
     * @throws Exception if initialization fails
     */
    public Derby(Properties properties, Options options, PrepareDataSource preparer) throws Exception {
        super(preparer, properties, options, ";", /* ignore case? */ false, /* alone on line? */ false);

        // Always print database name to stdout.
        System.out.println("Target database: Derby");
    }

    /**
     * Attempts a clean shutdown of the embedded Derby engine.
     */
    public void shutdown() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");

        } catch (SQLException sqle) {
            // This connection request explicitly shuts down the database, which
            // ensures that database files are in a consistent state and there are
            // no outstanding records in the transaction log.
            // SQL state "08006" indicates no error per se -
            // it just confirms a successful shutdown.

            String info = "Database shutdown ";
            String state = sqle.getSQLState();
            if ("08006".equals(state) || "XJ015".equals(state)) {
                info += "was successful";
            } else {
                info += "reports error: ";
                info += Database.squeeze(sqle);
            }
            System.err.println(info);
        }
    }

    /**
     * Builds an embedded Derby datasource based on the provided configuration.
     *
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

        DataSource dataSource = Database.getDataSource(config);

        org.apache.derby.jdbc.EmbeddedDataSource ds = (org.apache.derby.jdbc.EmbeddedDataSource) dataSource;
        ds.setDescription(applicationName);
        ds.setDatabaseName(config.database());
        ds.setUser(config.user());  // Not really needed since we are running Derby embedded
        ds.setPassword(config.password());  // Not really needed since we are running Derby embedded
        ds.setCreateDatabase("create");  // Create database if it does not exist

        return dataSource;
    }
}
