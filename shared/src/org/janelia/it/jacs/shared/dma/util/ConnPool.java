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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for maintaining a pool of jdbc connections
 *
 * @author Tareq Nabeel
 */
public class ConnPool {

    private static final int MIN_POOL_SIZE = SystemConfigurationProperties.getInt("dma.jdbc.min.connPoolSize");

    private static List<Connection> connections = new ArrayList<Connection>();
    private static boolean reportedTargetConnection;

    static {
        synchronized (connections) {
            for (int i = 0; i < MIN_POOL_SIZE; i++) {
                connections.add(createConnection());
            }
        }
    }

    public static void closeConnections() {
        try {
            synchronized (connections) {
                for (Connection connection : connections) {
                    connection.close();
                }
                connections.clear();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        synchronized (connections) {
            if (connections.size() == 0) {
                connections.add(createConnection());
            }
            return connections.remove(connections.size() - 1);
        }
    }

    public static void releaseConnection(Connection conn) {
        try {
            if (conn == null) {
                return;
            }
            conn.setAutoCommit(true);
            synchronized (connections) {
                connections.add(conn);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection createConnection() {
        try {
            Class.forName(SystemConfigurationProperties.getString("dma.jdbc.driverClassName"));
            Connection connection = DriverManager.getConnection(
                    SystemConfigurationProperties.getString("dma.jdbc.url"),
                    SystemConfigurationProperties.getString("dma.jdbc.username"),
                    SystemConfigurationProperties.getString("dma.jdbc.password"));
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(true);
            if (!reportedTargetConnection) {
                Logger.getLogger(ConnPool.class).info("******************* TARGET DATABASE: " + SystemConfigurationProperties.getString("dma.jdbc.url"));
                reportedTargetConnection = true;
            }
            return connection;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
