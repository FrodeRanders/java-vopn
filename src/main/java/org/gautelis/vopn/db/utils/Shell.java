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
import org.gautelis.vopn.lang.TimeDelta;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.*;

/**
 * This class implements a simple SQL REPL.
 * <p>
 * Created by froran on 2016-02-19.
 */
public class Shell {

    private final Manager manager;

    /**
     * Creates a SQL shell bound to a manager.
     *
     * @param manager database manager
     */
    public Shell(Manager manager) {
        this.manager = manager;
    }

    /**
     * Starts a simple SQL REPL using the provided streams.
     *
     * @param is input stream for user commands
     * @param os output stream for responses
     * @throws IOException if terminal IO fails
     */
    public void prompt(InputStream is, OutputStream os) throws IOException {
        final String PROMPT = "SQL> ";

        LineReaderBuilder builder = LineReaderBuilder.builder();
        LineReader reader = builder.build();

        final PrintWriter out = reader.getTerminal().writer();
        int screenWidth = reader.getTerminal().getWidth();

        // Command loop
        while (true) {
            try {
                String cmd = reader.readLine(PROMPT);
                if (null != cmd && !(cmd = cmd.trim()).isEmpty()) {
                    if ("exit".equalsIgnoreCase(cmd)) {
                        out.println();
                        out.flush();
                        return;
                    }

                    long startTime = System.currentTimeMillis();
                    execute(screenWidth, cmd, out);
                    long endTime = System.currentTimeMillis();

                    if (endTime > startTime) {
                        out.println("[" + TimeDelta.asHumanApproximate(BigInteger.valueOf(endTime - startTime)) + "]");
                    }
                }
                out.flush();

            } catch (Throwable t) {
                String msg = t.getMessage();
                if (null == msg || msg.isEmpty()) {
                    msg = t.getClass().getSimpleName();
                }
                out.println("error: " + msg);
                out.flush();
            }
        }
    }

    /**
     * Executes a single SQL statement and prints results to the writer.
     *
     * @param screenWidth terminal width used for formatting
     * @param sql SQL statement to execute
     * @param out output writer
     */
    public void execute(int screenWidth, String sql, PrintWriter out) {
        if (null == sql || sql.isEmpty()) {
            return;
        }

        try (Connection conn = manager.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                boolean success = manager.execute(stmt, sql, out, /* accept failure? */ false);
                while (success) { /* success is not modified below this line */
                    int updateCount = stmt.getUpdateCount();
                    if (updateCount > 0) {
                        out.println("Rows affected: " + updateCount);
                        out.flush();

                    } else if (updateCount == 0) {
                        out.println("No rows affected or statement was DDL command");
                        out.flush();
                    }

                    try (ResultSet rs = stmt.getResultSet()) {
                        printResultData(screenWidth, rs, out);
                    }

                    if (!stmt.getMoreResults() && updateCount == -1) {
                        break;
                    }
                }
            }
        } catch (SQLException sqle) {
            out.println("---------------------------------------------------------------------------");
            out.println("Failed to execute statement: \n" + sql);
            out.println("\n\nDescription of failure: \n" + Database.squeeze(sqle));
            out.println("---------------------------------------------------------------------------");
            out.flush();

        } catch (Throwable t) {
            out.println("---------------------------------------------------------------------------");
            out.println("Failed to execute statement: \n" + sql);
            out.println("\n\nDescription of failure: \n" + t.getMessage());
            out.println("---------------------------------------------------------------------------");
            out.flush();
        }
    }


    private void printResultData(int screenWidth, ResultSet rs, PrintWriter out) throws Exception {
        if (null == rs) {
            return;
        }

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        String format = printResultHeader(screenWidth, metaData, out);

        int rowCount = 0;
        while (rs.next()) {
            String[] values = new String[columnCount];
            for (int i = 0; i < columnCount; ++i) {
                String value = rs.getString(i+1);
                if (rs.wasNull()) {
                    value = "NULL";
                } else {
                    value = value.trim();
                }
                values[i] = value;
            }
            out.format(format, values);
            out.flush();

            ++rowCount;
        }

        out.println("Number of rows: " + rowCount);
        out.flush();
    }

    private String printResultHeader(int screenWidth, ResultSetMetaData metaData, PrintWriter out) throws Exception {
        String format = "|";

        int columnCount = metaData.getColumnCount();
        int columnWidth = Math.floorDiv(screenWidth, columnCount) - 2;

        String[] names = new String[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            String name = metaData.getColumnName(i+1).trim();
            format += "%" + columnWidth + "s|";
            names[i] = String.format("%-" + ((columnWidth / 2) + (name.length() / 2)) + "s", name);
        }
        format += "\n";

        String header = String.format(format, names);

        out.print("+");
        for (int i=3; i < header.length(); i++) {
            out.print("-");
        }
        out.println("+");
        out.print(header);
        out.print("+");
        for (int i=3; i < header.length(); i++) {
            out.print("-");
        }
        out.println("+");
        out.flush();

        return format;
    }
}
