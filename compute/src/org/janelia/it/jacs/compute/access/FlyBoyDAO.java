
package org.janelia.it.jacs.compute.access;

import java.sql.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Simple JDBC access to the FlyBoy database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyBoyDAO {

    protected Logger _logger;

    private final String jndiPath = SystemConfigurationProperties.getString("sage.jdbc.jndiName", null);
    private final String jdbcDriver = SystemConfigurationProperties.getString("sage.jdbc.driverClassName", null);
    private final String jdbcUrl = SystemConfigurationProperties.getString("sage.jdbc.url", null);
    private final String jdbcUser = SystemConfigurationProperties.getString("sage.jdbc.username", null);
    private final String jdbcPw = SystemConfigurationProperties.getString("sage.jdbc.password", null);

    public Connection getJdbcConnection() throws DaoException {
        try {
            Connection connection = null;
            if (!StringUtils.isEmpty(jndiPath)) {
                _logger.debug("getJdbcConnection() using these parameters: jndiPath="+jndiPath);
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) PortableRemoteObject.narrow(ctx.lookup(jndiPath), DataSource.class);
                connection = ds.getConnection();
            }
            else {
                _logger.debug("getJdbcConnection() using these parameters: driverClassName="+jdbcDriver+" url="+jdbcUrl+" user="+jdbcUser);
                Class.forName(jdbcDriver);
                connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            }
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
