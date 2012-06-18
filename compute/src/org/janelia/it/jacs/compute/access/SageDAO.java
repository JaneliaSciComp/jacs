
package org.janelia.it.jacs.compute.access;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResultSetIterator getFlylightImages(String sageFamily) throws DaoException {

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
    	    	"where i.family_id = getCvTermId('family','"+sageFamily+"', NULL) "+
    	    	"and slide.type_id =  getCvTermId('light_imagery','slide_code', NULL) "+
    	    	"and tile.type_id =  getCvTermId('light_imagery','tile', NULL) "+
    	    	"and age.type_id =  getCvTermId('light_imagery','age', NULL) "+
    	    	"and gender.type_id =  getCvTermId('light_imagery','gender', NULL) "+
    	    	"and effector.type_id =  getCvTermId('light_imagery','effector', NULL) "+
    	    	"order by slide.value, i.name ";

        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
    		ResultSet rs = stmt.executeQuery();
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying Sage", e);
    	}
    }

    /**
     * Returns all the images in the FlyLight project, with properties as columns. You can get the column names
     * by calling getColumnNames() on the returned ResultSetIterator object.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getFlylightImageProperties(String sageImageFamily) throws DaoException {

    	try {
	    	String sql = "select * from image_data_mv where family = '"+sageImageFamily+"' ";
        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
    		ResultSet rs = stmt.executeQuery();
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying Sage", e);
    	}
    	
    }

    /**
     * Returns a map of all Sage controlled vocabulary terms for light imagery and related vocabularies. 
     * @return
     * @throws DaoException
     */
    public Map<String,SageTerm> getFlylightImageVocabulary() throws DaoException {

    	String getUrl = "http://sage.int.janelia.org/sage-ws/cvs/light_imagery/with-all-related-cvs";
    	Map<String,SageTerm> map = new HashMap<String,SageTerm>();
		map.putAll(getStaticTerms());
    	
    	try {
    		HttpClient client = new HttpClient();
    		HttpMethod method = new GetMethod(getUrl);
    		client.executeMethod(method);
    		InputStream body = method.getResponseBodyAsStream();

			SAXReader reader = new SAXReader();
			Document doc = reader.read(body);
		
			List termList = doc.selectNodes("//cv/termSet/term");
			for(Object o : termList) {
				if (o instanceof Element) {
					Element termElement = (Element)o;
					Node nameNode = termElement.selectSingleNode("name");
					Node dataTypeNode = termElement.selectSingleNode("dataType");
					Node displayNameNode = termElement.selectSingleNode("displayName");
					Node definitionNode = termElement.selectSingleNode("definition");
					
					if (nameNode==null) {
						_logger.warn("Term with no name encountered in "+getUrl);
						continue;
					}
					
					if (dataTypeNode==null) {
						_logger.warn("Term with no type (name="+nameNode.getText()+") encountered in "+getUrl);
						continue;
					}
					
					SageTerm st = new SageTerm();
					st.setName(nameNode.getText());
					st.setDataType(dataTypeNode.getText());
					st.setDisplayName(displayNameNode!=null?displayNameNode.getText():st.getName());
					st.setDefinition(definitionNode!=null?definitionNode.getText():"");
					map.put(st.getName(),st);
				}
				else {
					_logger.warn("Expecting <term>, got "+o);
				}
			}
			
    	}
    	catch (Exception e) {
    		throw new DaoException("Error querying Sage Web Service", e);
    	}
    	
    	return map;
    }

    /**
     * Some static terms which are not part of any vocabulary. 
     * @return
     */
    private Map<String,SageTerm> getStaticTerms() {
    	
    	Map<String,SageTerm> map = new HashMap<String,SageTerm>();
		
		SageTerm sageId = new SageTerm();
		sageId.setName("id");
		sageId.setDataType("integer");
		sageId.setDisplayName("SAGE Id");
		sageId.setDefinition("Identifier within SAGE database");
		map.put(sageId.getName(),sageId);

		SageTerm imagePath = new SageTerm();
		imagePath.setName("name");
		imagePath.setDataType("text");
		imagePath.setDisplayName("Image Path");
		imagePath.setDefinition("Relative path to the image");
		map.put(imagePath.getName(),imagePath);
		
		SageTerm line = new SageTerm();
		line.setName("line");
		line.setDataType("text");
		line.setDisplayName("Fly line");
		line.setDefinition("Name of the fly line");
		map.put(line.getName(),line);
		
		return map;
    }
    
    
    
}
