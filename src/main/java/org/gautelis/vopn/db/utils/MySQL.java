/*
 * Copyright (C) 2011-2016 Frode Randers
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
import java.util.Properties;

/**
 * Description of MySQL:
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class MySQL extends Manager {

    public MySQL(DataSource dataSource, Options options) throws Exception {
        super(dataSource, options, "//.");

        // Always print database name to stdout.
        System.out.println("Target database: MySQL");
    }

    public MySQL(Properties properties, Options options, PrepareDataSource preparer) throws Exception {
        super(preparer, properties, options, "//.");

        // Always print database name to stdout.
        System.out.println("Target database: MySQL");
    }

    public static DataSource getDataSource(
            String applicationName,
            Database.Configuration config
    ) throws DatabaseException, ClassCastException {

        DataSource dataSource = Database.getDataSource(config);

        // If anything in particular could be done with a MySQL datasource to
        // make it dance, this is where it should be done.

        return dataSource;
    }
}

