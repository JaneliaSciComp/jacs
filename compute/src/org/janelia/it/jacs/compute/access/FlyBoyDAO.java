
package org.janelia.it.jacs.compute.access;

import java.sql.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Simple JDBC access to the FlyBoy database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyBoyDAO {

    protected Logger _logger;

    public Connection getJdbcConnection() throws DaoException {
    	try {
            String jdbcDriver = SystemConfigurationProperties.getString("flyboy.jdbc.driverClassName");
            String jdbcUrl = SystemConfigurationProperties.getString("flyboy.jdbc.url");
            String jdbcUser = SystemConfigurationProperties.getString("flyboy.jdbc.username");
            String jdbcPw = SystemConfigurationProperties.getString("flyboy.jdbc.password");
            Class.forName(jdbcDriver);
            Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            connection.setAutoCommit(false);
            return connection;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public FlyBoyDAO(Logger logger) {
        _logger = logger;
    }

    /**
     * Returns stock_name and robot_id for all GMR lines. There may be multiple robot ids for any given stock_name.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getGMRStockLines() throws DaoException {

    	try {
	    	String sql = "select Stock_Name, RobotID from StockFinder where stock_name like 'GMR\\_%' and robotid is not null order by robotId ";
        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
    		ResultSet rs = stmt.executeQuery();
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying FlyBoy", e);
    	}
    	
    }
    
}
