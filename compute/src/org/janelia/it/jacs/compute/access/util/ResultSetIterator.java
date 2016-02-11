package org.janelia.it.jacs.compute.access.util;

import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Iterator-style wrapper for JDBC ResultSets.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSetIterator implements Iterator<Map<String,Object>> {

    private Logger logger = Logger.getLogger(ResultSetIterator.class);

    private final Connection conn;
    private final Statement stmt;
    private final ResultSet rs;

    private List<String> orderedColumnLabels;
    private Map<String, Object> nextRow;

    public ResultSetIterator(Connection conn, Statement stmt, ResultSet rs) {

        this.conn = conn;
        this.stmt = stmt;
        this.rs = rs;

        try {
            final ResultSetMetaData md = rs.getMetaData();
            final int columnCount = md.getColumnCount();
            orderedColumnLabels = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                orderedColumnLabels.add(md.getColumnLabel(i));
            }

            if (rs.next()) {
                nextRow = toMap(rs);
            }

        } catch (SQLException e) {
            rethrow(e);
        }
    }

    /**
     * Returns true if there are more records.
     */
    @Override
    public boolean hasNext() {
        return nextRow!=null;
    }

    /**
     * Return the next row in the result set. Only call this if hasNext() was just called and returned true.
     */
    @Override
    public Map<String,Object> next() {
        try {
            Map<String,Object> toReturn = nextRow;
            if (rs.next()) {
                nextRow = toMap(rs);
            }
            else {
                nextRow = null;
            }
            return toReturn;
        }
        catch (SQLException e) {
            rethrow(e);
            return null;
        }
    }

    /**
     * Not supported by this implementation.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Should be called when the client is done with the iterator.
     */
    public void close() {
        close(rs, stmt, conn, logger);
    }

    /**
     * Utility to close all non-null resources
     * (in reverse order: result set, statement, connection).
     *
     * @param  resultSet    result set to close.
     * @param  statement    statement to close.
     * @param  connection   connection to close.
     * @param  logger       logger for any close exceptions (which do not get thrown).
     */
    public static void close(ResultSet resultSet,
                             Statement statement,
                             Connection connection,
                             Logger logger) {

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("failed to close result set", e);
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("failed to close statement", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("failed to close connection", e);
            }
        }
    }

    private Map<String,Object> toMap(ResultSet rs) throws SQLException {

        final int columnCount = orderedColumnLabels.size();
        Map<String, Object> map = new HashMap<String,Object>(columnCount * 2);

        Object value;
        for (int i = 0; i < columnCount; i++) {
            value = rs.getObject(i + 1);
            if (value != null) {
                map.put(orderedColumnLabels.get(i), value);
            }
        }

        return map;
    }

    private void rethrow(SQLException e) {
        throw new RuntimeException(e.getMessage(), e);
    }

}