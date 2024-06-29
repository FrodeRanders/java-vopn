/*
 * Copyright (C) 2011-2020 Frode Randers
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
package org.gautelis.vopn.db;

import org.gautelis.vopn.lang.Configurable;
import org.gautelis.vopn.lang.ConfigurationTool;
import org.gautelis.vopn.lang.DynamicLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;
import java.util.Properties;

/**
 * Description of Database:
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private static int DEADLOCK_MAX_RETRIES = 100;
    private static int DEADLOCK_SLEEP_TIME = 200; // milliseconds

    private static final int DEFAULT_MAX_ACTIVE = 20;
    private static final int DEFAULT_MAX_IDLE = 2;

    /**
     * The database configuration can be accessed through this interface,
     * that is implemented behind the scene using proxy objects and bound
     * to the provided configuration properties.
     */
    public interface Configuration {
        @Configurable(value = "derby")
        String manager();

        @Configurable(value = "org.apache.derby.jdbc.EmbeddedDataSource")
        String driver();

        @Configurable(value = "jdbc:derby:temporary-db;create=true")
        String url();

        @Configurable
        String user();

        @Configurable
        String password();

        @Configurable(value = "temporary-db")
        String database();

        @Configurable(value = "localhost")
        String server();

        @Configurable(value = "" + DEFAULT_MAX_ACTIVE)
        int maxActive();

        @Configurable(value = "" + DEFAULT_MAX_IDLE)
        int maxIdle();

        /* {PostgreSQL, 5432} */
        @Configurable
        int port();
    }

    /** Prepares a datasource for use, by providing additional configuration
     */
    public interface DataSourcePreparation<T extends DataSource> {
        default T prepare(T dataSource, Configuration config) {
            return Objects.requireNonNull(dataSource);
        };
    }

    // 
    private static DynamicLoader<DataSource> loader = new DynamicLoader<>("datasource");
    
    //
    private Database() {
    }


    /**
     * Deduces configuration from properties, handling default values where appropriate...
     * @param properties containing key/value pairs, where key name matches entries in the Configuration interface.
     * @return a configuration proxy object bound to the provided properties.
     */
    public static Configuration getConfiguration(Properties properties) {
        Configuration config = ConfigurationTool.bindProperties(Configuration.class, properties);
        return config;
    }

    /**
     * Gets a datasource for the database, based on some common configuration.
     * <p>
     * NOTE: Additional configuration may have to be provided!
     * @see #getDataSource(Configuration, DataSourcePreparation)
     * <p>
     * @param config the configuration for accessing the database (driver etc).
     * @return a datasource matching the provided configuration.
     * @throws DatabaseException if a suitable driver was not found or could not be instantiated.
     */
    public static DataSource getDataSource(Configuration config) throws DatabaseException {
        Objects.requireNonNull(config, "config");

        // Class implementing the DataSource
        String driver = config.driver();
        if (null == driver || driver.isEmpty()) {
            throw new DatabaseException("Could not determine JDBC driver name (driver)");
        }
        driver = driver.trim();

        // Now instantiate a DataSource
        try {
            return createDataSource(driver, loadDataSource(driver));
        } catch (ClassNotFoundException cnfe) {
            String info = "Could not instantiate DataSource: ";
            info += cnfe.getMessage();
            throw new DatabaseException(info, cnfe);
        }
    }

    /**
     * Gets a datasource for the database.
     * <p>
     * Now, these are the naked facts regarding data sources:
     *
     * There is no uniform way to configure a data source - it is
     * highly proprietary and depends on the JDBC driver.
     *
     * Depending on what data source you have configured, you will
     * have to use a construction along the lines of these examples
     * on the returned DataSource.
     * <pre>
     * Map&lt;String, String&gt; init = Map.of(
     *       "manager", "db2",
     *       "driver", "com.ibm.db2.jcc.DB2SimpleDataSource",
     *       //
     *       "server", "localhost",
     *       "port", "50000",
     *       //
     *       "url", "jdbc:db2://localhost:50000/ledger",
     *       "database", "ledger",
     *       //
     *       "user", "ledgerapplication",
     *       "password", "sosecret"
     * );
     *
     * Properties properties = new Properties();
     * properties.putAll(init);
     *
     * // DB2
     * DataSource db2 = Database.getDataSource(
     *        Database.getConfiguration(properties),
     *        new Database.DataSourcePreparation&lt;com.ibm.db2.jcc.DB2SimpleDataSource&gt;() {
     *                &#x40;Override
     *                public com.ibm.db2.jcc.DB2SimpleDataSource prepare(
     *                       com.ibm.db2.jcc.DB2SimpleDataSource ds,
     *                       Database.Configuration cf
     *                ) {
     *                       ds.setDescription("MyApplication"); // std
     *                       ds.setDatabaseName(cf.database());  // std
     *                       ds.setUser(cf.user());  // std
     *                       ds.setPassword(cf.password());  // std
     *
     *                       ds.setDriverType(4);  // DB2 specific
     *
     *                       return ds;
     *                }
     *         }
     * );
     *
     * // Derby
     * DataSource derby = Database.getDataSource(
     *        Database.getConfiguration(properties),
     *        new Database.DataSourcePreparation&lt;org.apache.derby.jdbc.EmbeddedDataSource&gt;() {
     *                &#x40;Override
     *                public org.apache.derby.jdbc.EmbeddedDataSource prepare(
     *                       org.apache.derby.jdbc.EmbeddedDataSource ds,
     *                       Database.Configuration cf
     *                ) {
     *                       ds.setDescription("MyApplication"); // std
     *                       ds.setDatabaseName(cf.database()); // std
     *                       ds.setUser(cf.user()); // std
     *                       ds.setPassword(cf.password()); // std
     *
     *                       ds.setCreateDatabase("create");  // derby specific
     *
     *                       return ds;
     *                }
     *         }
     * );
     *
     * // SQL Server
     * DataSource mssql = Database.getDataSource(
     *        Database.getConfiguration(properties),
     *        new Database.DataSourcePreparation&lt;net.sourceforge.jtds.jdbcx.JtdsDataSource&gt;() {
     *                &#x40;Override
     *                public net.sourceforge.jtds.jdbcx.JtdsDataSource prepare(
     *                       net.sourceforge.jtds.jdbcx.JtdsDataSource ds,
     *                       Database.Configuration cf
     *                ) {
     *                       ds.setDescription("MyApplication"); // std
     *                       ds.setDatabaseName(cf.database()); // std
     *                       ds.setUser(cf.user()); // std
     *                       ds.setPassword(cf.password()); // std
     *
     *                       ds.setServerName(cf.server());  // jtds specific
     *                       ds.setPortNumber(cf.port()); // jtds specific
     *
     *                       return ds;
     *                }
     *         }
     * );
     * </pre>
     * <p>
     * @param config the configuration for accessing the database (driver etc).
     * @param preparation a function used to prepare the datasource (config wise)
     * @return a datasource matching the provided configuration.
     * @throws DatabaseException if a suitable driver was not found or could not be instantiated.
     */
    public static DataSource getDataSource(Configuration config, DataSourcePreparation preparation) throws DatabaseException {
        Objects.requireNonNull(preparation, "preparation");

        DataSource ds = getDataSource(config);
        return preparation.prepare(ds, config);
    }

    /**
     * Dynamically loads the named class (fully qualified classname).
     * <p>
     * @param className specifies the fully qualified class name of a class (implementing DataSource).
     * @return Class matching specified parameter class name
     * @throws ClassNotFoundException if no Class matches specified class name
     */
    public static Class loadDataSource(String className) throws ClassNotFoundException {
        return loader.createClass(className);
    }

    /**
     * Creates a DataSource object instance from a DataSource class.
     * <p>
     * @param className specifies the fully qualified class name of a class (implementing DataSource).
     * @param clazz specifies the class from which the object will be drawn.
     * @return datasource
     */
    public static DataSource createDataSource(String className, Class clazz) throws ClassNotFoundException, ClassCastException {

        if (!DataSource.class.isAssignableFrom(clazz)) {
            String info = "A " + className + " does not qualify as a " + DataSource.class.getName() + "! ";

            Class candidate = clazz;
            while (null != candidate) {
                if (Driver.class.isAssignableFrom(candidate)) {
                    info += "This is a " + Driver.class.getName() + ", which is not quite the same.";
                    break;
                } else if (DriverManager.class.isAssignableFrom(candidate)) {
                    info += "This is a " + DriverManager.class.getName() + ", which is not quite the same.";
                    break;
                }
                candidate = candidate.getSuperclass();
            }
            throw new ClassCastException(info); // kind of
        }

        return loader.createObject(className, clazz);
    }
    
    /**
     * Support for explicit logging of SQL exceptions to error log, by extracting all relevant information
     * from the SQLException.
     * <p>
     * @param sqle an SQLException from which to extract information
     */
    public static String squeeze(SQLException sqle) {
        StringBuilder buf = new StringBuilder();

        SQLException e = sqle;
        while (e != null) {
            buf.append(e.getClass().getSimpleName())
               .append(" [")
               .append(e.getMessage())
               .append("], SQLstate(")
               .append(e.getSQLState())
               .append("), Vendor code(")
               .append(e.getErrorCode())
               .append(")");
            e = e.getNextException();

            if (null != e) {
                buf.append(" >> ");
            }
        }
        return buf.toString();
    }

    /**
     * Support for explicit logging of SQL warnings to warning log, by extracting all relevant information
     * from the SQLWarning.
     * <p>
     * @param sqlw an SQLWarning from which to extract information.
     */
    public static String squeeze(SQLWarning sqlw) {
        StringBuilder buf = new StringBuilder();

        SQLWarning w = sqlw;
        while (w != null) {
            buf.append(w.getClass().getSimpleName())
               .append(" [")
               .append(w.getMessage())
               .append("], SQLstate(")
               .append(w.getSQLState())
               .append("), Vendor code(")
               .append(w.getErrorCode())
               .append(")");
            w = w.getNextWarning();

            if (null != w) {
                buf.append(" >> ");
            }
        }
        return buf.toString();
    }

    //
    private interface ExecutableCall {
        boolean execute() throws SQLException;
    }

    private interface QueryCall {
        ResultSet query() throws SQLException;
    }

    private interface UpdateCall {
        int update() throws SQLException;
    }



    /**
     * Wraps an execute in deadlock detection
     */
    private static boolean executeWithDD(ExecutableCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.execute();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeWithDD, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Execute, retries=" + i);
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;
    }

    /**
     * Wraps a query in deadlock detection
     */
    private static ResultSet queryWithDD(QueryCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.query();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeQuery, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;
    }

    /**
     * Wraps an update in deadlock detection
     */
    private static int updateWithDD(UpdateCall call) throws SQLException {
        SQLException sqle = null;
        int i = DEADLOCK_MAX_RETRIES;
        do {
            try {
                return call.update();

            } catch (SQLException se) {
                sqle = se;
                // Is SQLException a deadlock? (40001)
                if (se.getSQLState() != null && se.getSQLState().startsWith("40")) {
                    log.info("Database deadlock has occurred during executeUpdate, trying again");
                    try {
                        Thread.sleep(DEADLOCK_SLEEP_TIME);
                    } catch (Exception ignore) {
                    }
                } else /* other SQLException */ {
                    throw se;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Update, retries=" + i);
            }
        } while (--i > 0);
        log.error("Giving up deadlock retry");
        throw sqle;

    }
    /**
     * Manages call to Statement.executeBatch(), providing support for deadlock
     * detection and statement reruns.
     */
    public static void executeBatch(final Statement stmt) throws SQLException {
        executeWithDD(() -> {
            int[] results = stmt.executeBatch();

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning: {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return true; // any value will do
        });
    }

    /**
     * Manages call to Statement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     */
    public static boolean execute(final Statement stmt, final String sql) throws SQLException {
        return executeWithDD(() -> {
            boolean results = stmt.execute(sql);

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return results;
        });
    }

    /**
     * Manages call to PreparedStatement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     */
    public static boolean execute(final PreparedStatement stmt) throws SQLException {
        return executeWithDD(() -> {
            boolean result = stmt.execute();

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return result;
        });
    }

    /**
     * Manages call to Statement.executeWithDD(), providing support for deadlock
     * detection and statement reruns.
     * <p/>
     * Accepts Statement, PreparedStatement and CallableStatement arguments.
     * <p/>
     * We do not expect any result sets from this call.
     *
    public static void execute(final Statement stmt, final String sql) throws SQLException {
        executeWithDD(() -> {
            for (int i = 0, updateCount; i < 255; i++) {
                try {
                    boolean result = (i == 0) ? stmt.execute(sql) : stmt.getMoreResults();

                    SQLWarning warning = stmt.getWarnings();
                    for (int j = 0; j < 255 && null != warning; j++) {
                        log.info(squeeze(warning));
                        warning = warning.getNextWarning();
                    }

                    stmt.clearWarnings();

                    if (result) {
                        if (/* first time through * / 0 == i) {
                            String info = "Unexpected result(s) from SQL execute";
                            Exception syntheticException = new Exception(info);
                            log.info(info, syntheticException);
                        }

                        // Log some information for debug purposes
                        try (ResultSet rs = stmt.getResultSet()) {
                            ResultSetMetaData m = rs.getMetaData();

                            while (rs.next()) {
                                for (int c = 1; c <= m.getColumnCount(); c++) {
                                    String info = " \"" + m.getColumnName(c) +"\": " + rs.getInt(c);
                                    log.info(info);
                                }
                            }
                        }
                    }
                    else if ((updateCount = stmt.getUpdateCount()) != -1)
                        System.out.println("Update Count: " + updateCount);
                    else
                        break;
                }
                catch (SQLException e) {
                    log.info(squeeze(e));
                }
            }
        });
    }*/

    /**
     * Manages call to PreparedStatement.executeQuery(), providing support for deadlock
     * detection and statement reruns.
     */
    public static ResultSet executeQuery(final PreparedStatement stmt) throws SQLException {
        return queryWithDD(() -> {
            ResultSet rs = stmt.executeQuery();

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            warning = rs.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Result set warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return rs;
        });
    }

    /**
     * Manages call to Statement.executeQuery(), providing support for deadlock
     * detection and statement reruns.
     */
    public static ResultSet executeQuery(final Statement stmt, final String sql) throws SQLException {
        return queryWithDD(() -> {
            ResultSet rs = stmt.executeQuery(sql);

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            warning = rs.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Result set warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return rs;
        });
    }

    /**
     * Manages call to Statement.executeUpdate(), providing support for deadlock
     * detection and statement reruns.
     */
    public static int executeUpdate(final Statement stmt, final String sql) throws SQLException {
        return updateWithDD(() -> {
            int rows = stmt.executeUpdate(sql);

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return rows;
        });
    }

    public static int executeUpdate(final PreparedStatement stmt) throws SQLException {
        return updateWithDD(() -> {
            int rows = stmt.executeUpdate();

            // Handle warnings, if applicable
            SQLWarning warning = stmt.getWarnings();
            for (int i = 0; i < 255 && null != warning; i++) {
                if (log.isTraceEnabled()) {
                    log.trace("Statement warning, problem? {}", squeeze(warning), new Exception("Synthetic exception to get a stack trace"));
                }
                warning = warning.getNextWarning();
            }

            return rows;
        });
    }
}
