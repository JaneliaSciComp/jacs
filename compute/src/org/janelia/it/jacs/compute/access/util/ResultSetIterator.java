package org.janelia.it.jacs.compute.access.util;

import java.sql.*;
import java.util.Iterator;

/**
 * Iterator-style wrapper for JDBC ResultSets. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSetIterator implements Iterator<Object[]> {

	private final Connection conn;
	private final Statement stmt;
	private final ResultSet rs;
    
	private Object[] nextRow;
   
    public ResultSetIterator(Connection conn, Statement stmt, ResultSet rs) {
    	this.conn = conn;
    	this.stmt = stmt;
        this.rs = rs;
        try {
        	rs.next();
        	nextRow = toArray(rs);
        }
        catch (SQLException e) {
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
    public Object[] next() {
        try {
        	Object[] toReturn = nextRow;
            if (rs.next()) {
            	nextRow = toArray(rs);
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
    	try {
        	if (stmt!=null) stmt.close();
        	if (conn!=null) conn.close();
    	}
    	catch (SQLException e) {
            rethrow(e);
    	}
    }
    
    private Object[] toArray(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        Object[] result = new Object[cols];

        for (int i = 0; i < cols; i++) {
            result[i] = rs.getObject(i + 1);
        }

        return result;
    }
    

    private void rethrow(SQLException e) {
        throw new RuntimeException(e.getMessage(), e);
    }

}