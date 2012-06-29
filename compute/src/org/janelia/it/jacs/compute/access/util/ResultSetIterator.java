package org.janelia.it.jacs.compute.access.util;

import java.sql.*;
import java.util.*;


/**
 * Iterator-style wrapper for JDBC ResultSets. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultSetIterator implements Iterator<Map<String,Object>> {

	private final Connection conn;
	private final Statement stmt;
	private final ResultSet rs;
	
	private Map<String,Object> nextRow;
   
    public ResultSetIterator(Connection conn, Statement stmt, ResultSet rs) {
    	this.conn = conn;
    	this.stmt = stmt;
        this.rs = rs;
        try {
        	rs.next();
        	nextRow = toMap(rs);
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
    	try {
    		if (rs!=null) rs.close();
    		if (stmt!=null) stmt.close();
    		if (conn!=null) conn.close();
    	}
    	catch (SQLException e) {
            rethrow(e);
    	}
    }
    
    public List<String> getColumnNames() {
		List<String> cols = new ArrayList<String>();
    	try {
        	ResultSetMetaData md = rs.getMetaData();
        	for (int i = 1; i <= md.getColumnCount(); i++) {
        		cols.add(md.getColumnName(i));
        	}
    	}
    	catch (SQLException e) {
            rethrow(e);
    	}
    	return cols;
    }
    
    public ResultSet getResultSet() {
    	return rs;
    }

    private Map<String,Object> toMap(ResultSet rs) throws SQLException {
    	Map<String,Object> map = new HashMap<String,Object>();
        ResultSetMetaData md = rs.getMetaData();
    	for (int i = 1; i <= md.getColumnCount(); i++) {
    		String columnName = md.getColumnName(i);
    		Object value = rs.getObject(columnName);
    		map.put(columnName, value);
    	}
        return map;
    }
    
    private void rethrow(SQLException e) {
        throw new RuntimeException(e.getMessage(), e);
    }

}