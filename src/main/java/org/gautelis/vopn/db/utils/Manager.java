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
package org.gautelis.vopn.db.utils;

import org.gautelis.vopn.db.Database;
import org.gautelis.vopn.db.DatabaseException;

import javax.sql.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.*;
import java.util.List;
import java.util.Properties;


/**
 * Description of Manager:
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public abstract class Manager {
    public interface PrepareDataSource {
        void prepare(DataSource ds, Database.Configuration configuration);
    }

    protected static final boolean ACCEPT_FAILURE = true;
    protected static final boolean BREAK_ON_FAILURE = false;

    //
    private final Options options;

    //
    protected DataSource dataSource;

    // Information on how to read batch file
    protected Characteristics characteristics = null;

    private Manager(PrepareDataSource preparer, Properties properties, Options options) throws DatabaseException {
        this.options = options;

        Database.Configuration configuration = Database.getConfiguration(properties);
        dataSource = Database.getDataSource(configuration);
        preparer.prepare(dataSource, configuration);
    }

    /**
     * Constructs a Manager object that will parse the DDL, demanding
     * batch separator alone on line (ignoring case).
     */
    protected Manager(PrepareDataSource preparer, Properties properties, Options options, String batchSeparator) throws DatabaseException {
        this(preparer, properties, options);
        characteristics = new Characteristics(batchSeparator);
    }

    /**
     * Constructs a Manager object that will parse the DDL,
     * as specified by the parameters.
     *
     * @see Characteristics
     */
    protected Manager(
            PrepareDataSource preparer, Properties properties,
            Options options, String batchSeparator, boolean ignoreCase, boolean separatorAloneOnLine
    ) throws DatabaseException {

        this(preparer, properties, options);
        characteristics = new Characteristics(batchSeparator, ignoreCase, separatorAloneOnLine);
    }

    /**
     * Constructs a Manager object that will parse DDL.
     */
    protected Manager(DataSource dataSource, Options options,
                    String batchSeparator, boolean ignoreCase, boolean separatorAloneOnLine) {
        this.dataSource = dataSource;
        this.options = options;

        characteristics = new Characteristics(batchSeparator, ignoreCase, separatorAloneOnLine);
    }

    /**
     * Constructs a Manager object that will parse DDL, demanding
     * batch separator alone on line (ignoring case).
     */
    protected Manager(DataSource dataSource, Options options,
                    String batchSeparator) {
        this.dataSource = dataSource;
        this.options = options;

        characteristics = new Characteristics(batchSeparator);
    }

    /**
     * Returns the internal DataSource, in case operations should continue after Manager ceases its work.
     * @return
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Executes any SQL script file
     */
    public void execute(String name, Reader sqlCode, PrintWriter out) throws Exception {
        runScript(name, sqlCode, out);
    }

    /**
     * Check if file exists
     */
    protected boolean fileExist(String filename) {
        File file = new File(filename);
        return file.exists();
    }


    /**
     */
    protected void runScript(String name, Reader sqlCode, PrintWriter out) throws Exception {
        try {
            BatchReader batch = new BatchReader(options, characteristics);
            List<String> sqls = batch.readFile(sqlCode);

            out.println("Running script \"" + name + "\"...");
            out.flush();
            for (String sql : sqls) {
                execute(sql, out, ACCEPT_FAILURE); // or BREAK_ON_FAILURE?
            }
        } catch (SQLException sqle) {
            out.println(" - Script failed");
            out.flush();

            if (options.debug) {
                out.println(Database.squeeze(sqle));
            }
            throw new Exception("Caught exception when running script\n" + sqle, sqle);

        } catch (Exception e) {
            e.printStackTrace(out);
            throw new Exception("Caught exception when running script\n" + e);
        }
    }


    /**
     * Executes an SQL statement.
     *
     * @param sqlStatement
     * @throws Exception
     */
    protected void execute(String sqlStatement, PrintWriter out, boolean acceptFailure) throws Exception {

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                boolean success = execute(stmt, sqlStatement, out, acceptFailure);

                // Loop over all kinds of results.
                while (success) { /* 'success' is not modified below this line */
                    int rowCount = stmt.getUpdateCount();

                    if (rowCount > 0) {
                        // --------------------------------------------------
                        // Result of successful INSERT or UPDATE or the like
                        // --------------------------------------------------
                        if (options.debug) {
                            out.println("Rows affected: " + rowCount);
                            out.flush();
                        }

                        if (stmt.getMoreResults()) {
                            continue;
                        }

                    } else if (rowCount == 0) {
                        // --------------------------------------------------
                        // Either a DDL command or 0 updates
                        // --------------------------------------------------
                        if (options.debug) {
                            out.println("No rows affected or statement was DDL command");
                            out.flush();
                        }

                        boolean moreResults;
                        try {
                            moreResults = stmt.getMoreResults();
                        } catch (SQLException sqle) {

                            // State: 24000 - Invalid cursor state
                            //  [Teradata: Continue request submitted but no response to return]
                            if (sqle.getSQLState().startsWith("24")) {
                                break;

                            } else {
                                throw sqle;
                            }
                        }
                        if (moreResults) {
                            continue;
                        }

                    } else { // rowCount < 0
                        // --------------------------------------------------
                        // Either we have a result set or no more results...
                        // --------------------------------------------------
                        ResultSet rs = stmt.getResultSet();
                        if (null != rs) {
                            // Ignore resultset
                            rs.close();

                            if (stmt.getMoreResults()) {
                                continue;
                            }
                        }
                    }

                    // No more results
                    break;
                }
            }
        } catch (SQLException sqle) {
            out.println("Failed to execute statement: \n" + sqlStatement);
            out.println("\n\nDescription of failure: \n" + Database.squeeze(sqle));
            out.flush();
            throw sqle;

        } catch (Exception e) {
            out.println("Failed to execute statement: \n" + sqlStatement);
            out.println("\n\nDescription of failure: \n" + e.getMessage());
            out.flush();
            throw e;
        }
    }

    protected boolean execute(Statement stmt, String sqlStatement, PrintWriter out, boolean acceptFailure) throws SQLException {
        int i = 20; // number of retries (see variable info below)

        try {
            SQLException last = null;
            do {

                try {
                    stmt.execute(sqlStatement);
                    return true; // success

                } catch (SQLException sqle) {
                    last = sqle;

                    // State: 40001 - Deadlock
                    if (sqle.getSQLState().startsWith("40")) {
                        if (options.debug) {
                            out.println("Deadlock detected in database, retrying " + (i - 1) + " times...");
                            out.flush();
                        }
                        try {
                            Thread.sleep(200 /* ms to sleep */);
                        } catch (Exception e) { /* ignore */ }
                    } else {
                        throw sqle;
                    }
                }
            } while (--i > 0);
            String info = "Giving up on deadlock after 20 retries";
            out.println(info);
            out.flush();

            throw last;

        } catch (SQLException sqle) {
            if (!acceptFailure) {
                out.println("-------------------------------------------------------------------------");
                out.println(Database.squeeze(sqle));
                out.println("Incorrect statement was: ");
                out.println(sqlStatement);
                out.println("-------------------------------------------------------------------------");
                out.flush();
            }
            
            if (acceptFailure) {
                // State: 01000 - Generic warning
                //  [SQL Server: Object already exists]
                if (sqle.getSQLState().equals("01000")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: Object already exists - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
                // State: 23000 - Integrity constraint/key violation
                //  [SQL Server: Data already exists]
                //  [Oracle:     Data already exists]
                else if (sqle.getSQLState().startsWith("23")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: Data already exists - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
                // State: 42000 - Syntax error or access violation
                //  [Oracle: Object already exists]
                //  [Oracle: Table or view does not exist]
                else if (sqle.getSQLState().startsWith("42")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: Either object already exists or it does not exist - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
                // State: 72000 - SQL execute phase errors
                // [Oracle: such column list already indexed]
                else if (sqle.getSQLState().startsWith("72")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: An index covering these columns already exists - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
                // State: S0002 - Base table not found
                //  [Teradata: Macro 'xyz' does not exist] - when dropping objects
                else if (sqle.getSQLState().equalsIgnoreCase("S0002")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: Object does not exist - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
                // State: X0Y32 - <value> '<value>' already exists in <value> '<value>'.
                // State: X0Y68 - <value> '<value>' already exists.
                //  [Derby: Table/View 'XYZ' already exists in Schema 'ZYX'.]
                //  [Derby: PROCEDURE 'XYZ' already exists.]
                else if (sqle.getSQLState().startsWith("X0Y")) {
                    if (options.debug) {
                        out.println("Statement: " + sqlStatement);
                        out.println(Database.squeeze(sqle));
                        out.println("\n[OK]: Object already exists - ACCEPTED!");
                        out.flush();
                    }
                    return false; // no success - but acceptable
                }
            }
            //
            // This is something we do not accept
            //
            throw sqle;
        }
    }

    public void shutdown() {}

    public Shell getShell() {
        return new Shell(this);
    }
}

