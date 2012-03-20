
package org.janelia.it.jacs.compute.access;

import java.sql.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Simple JDBC access to the Sage database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageDAO {

    protected Logger _logger;

    public Connection getJdbcConnection() throws DaoException {
    	try {
            String jdbcDriver = SystemConfigurationProperties.getString("sage.jdbc.driverClassName");
            String jdbcUrl = SystemConfigurationProperties.getString("sage.jdbc.url");
            String jdbcUser = SystemConfigurationProperties.getString("sage.jdbc.username");
            String jdbcPw = SystemConfigurationProperties.getString("sage.jdbc.password");
            Class.forName(jdbcDriver);
            Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            connection.setAutoCommit(false);
            return connection;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public SageDAO(Logger logger) {
        _logger = logger;
    }

    /**
     * Returns all the images in the FlyLight project. The result rows have the following columns:
     * <ol>
     * <li>slide code (e.g. "20110325_3_B2")</li> 
     * <li>image path (e.g. "20110407/FLFL_20110911231819146_1571.lsm")</li> 
     * <li>tile type (e.g. "Right Optic Lobe")</li> 
     * <li>age (e.g. "A01")</li> 
     * <li>gender (e.g. "f")</li> 
     * <li>effector (e.g. "1xLwt_attp40_4stop1")</li> 
     * <li>fly line (e.g. "GMR_57C10_AD_01")</li> 
     * </ol>
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getFlylightImages() throws DaoException {

    	try {
        	String sql = "select slide.value slide_code, i.name image_path, tile.value tile_type, " +
        			"age.value age, gender.value gender, effector.value effector, l.name line "+
    	    	"from image i "+
    	    	"join line l on i.line_id = l.id "+
    	    	"join image_property slide on i.id = slide.image_id "+ 
    	    	"join image_property tile on i.id = tile.image_id "+ 
    	    	"join image_property age on i.id = age.image_id "+
    	    	"join image_property gender on i.id = gender.image_id "+
    	    	"join image_property effector on i.id = effector.image_id "+
    	    	"where i.family_id = getCvTermId('family','flylight_flip', NULL) "+
    	    	"and slide.type_id =  getCvTermId('light_imagery','slide_code', NULL) "+
    	    	"and tile.type_id =  getCvTermId('light_imagery','tile', NULL) "+
    	    	"and age.type_id =  getCvTermId('light_imagery','age', NULL) "+
    	    	"and gender.type_id =  getCvTermId('light_imagery','gender', NULL) "+
    	    	"and effector.type_id =  getCvTermId('light_imagery','effector', NULL) "+
    	    	"order by slide.value, i.name ";

        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    		ResultSet rs = stmt.executeQuery();
    		_logger.info("Got result set");
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying Sage", e);
    	}
    }

}
