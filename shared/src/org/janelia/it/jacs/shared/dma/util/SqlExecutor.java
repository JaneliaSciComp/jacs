/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.shared.dma.util;

import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class is responsible for executing sql statements in a file or individually
 *
 * @author Tareq Nabeel
 */
public class SqlExecutor {
    private static DmaLogger dmaLogger = DmaLogger.getInstance();

    /**
     * Runs each command in the specified file in a separate transaction
     *
     * @param commandFilePath file in classpath containing SQL commands for execution
     */
    public static void executeCommandsInFile(String commandFilePath) throws IOException {
        if (commandFilePath == null || commandFilePath.trim().length() == 0) {
            return;
        }
        String commands = FileUtil.getResourceAsString(commandFilePath);
        if (dmaLogger.isInfoEnabled(SqlExecutor.class)) {
            dmaLogger.logInfo("Executing commands in " + commandFilePath, SqlExecutor.class);
        }
        execute(commands);
    }

    /**
     * Runs each command in a separate transaction.  Client should close the resultset
     * if it gets one
     *
     * @param commands "\n" separated list of SQL DDLs or DMLs
     * @return a resultset for select statements and null for everything else
     */
    public static ResultSet execute(String commands) {
        ResultSet rs = null;
        BufferedReader reader = new BufferedReader(new StringReader(commands));
        String sqlCommand = null;
        Connection conn = ConnPool.getConnection();
        try {
            Statement stmt = conn.createStatement();
            while ((sqlCommand = reader.readLine()) != null) {
                sqlCommand = sqlCommand.toLowerCase().trim();
                if (sqlCommand.equals("") || sqlCommand.startsWith("#")) {
                    continue;
                }
                if (dmaLogger.isInfoEnabled(SqlExecutor.class)) {
                    dmaLogger.logInfo("Executing SQL: " + sqlCommand, SqlExecutor.class);
                }
                long currentTimeMillis = System.currentTimeMillis();
                if (sqlCommand.startsWith("select")) {
                    rs = stmt.executeQuery(sqlCommand);
                }
                else {
                    stmt.executeUpdate(sqlCommand);
                }
                dmaLogger.logInfo(DateUtil.getElapsedTime("SQL execution of " + sqlCommand + "  took: ", (System.currentTimeMillis() - currentTimeMillis)), SqlExecutor.class);
            }
            if (rs == null) {
                stmt.close();
            }
            return rs;
        }
        catch (SQLException e) {
            try {
                conn.rollback();
            }
            catch (SQLException se) {
                // Do nothing, trying to rollback
            }
            throw new RuntimeException("Execution of " + sqlCommand + " failed:" + e.getMessage(), e);
        }
        catch (IOException e) {
            try {
                conn.rollback();
            }
            catch (SQLException se) {
                // Do nothing, trying to rollback
            }
            throw new RuntimeException("Execution of " + sqlCommand + " failed:", e);
        }
        finally {
            ConnPool.releaseConnection(conn);
        }
    }

    public static long getTableRowCount(String tableName) {
        ResultSet rs = execute("select count(*) as count from " + tableName);
        try {
            if (rs.next()) {
                return rs.getLong("count");
            }
            else {
                throw new RuntimeException("Could not retrieve table row count");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            }
            catch (SQLException e) {
                // Do nothing, trying to close the result set
            }
        }
    }

    public static String getSingleResultStringValue(String query, String columnName) {
        ResultSet rs = execute(query);
        try {
            if (rs.next()) {
                return rs.getString(columnName);
            }
            else {
                throw new RuntimeException("No rows returned for query: " + query);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            }
            catch (SQLException e) {
                // Do nothing, trying to close the result set
            }
        }
    }
}
