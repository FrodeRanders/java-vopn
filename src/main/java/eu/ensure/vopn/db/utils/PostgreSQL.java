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
package eu.ensure.vopn.db.utils;

import eu.ensure.vopn.db.Database;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
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
}
