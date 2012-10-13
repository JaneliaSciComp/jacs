
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
     * Returns all the images in a given image family with a null data set, with their properties as columns. 
     * You can get the column names by calling getColumnNames() on the returned ResultSetIterator object.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getImagesByFamily(String sageImageFamily) throws DaoException {

    	try {
	    	String sql = "select * from image_data_mv where family = '"+sageImageFamily+
	    		"' and data_set is null order by slide_code,name ";
        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
    		ResultSet rs = stmt.executeQuery();
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying SAGE", e);
    	}
    }
    
    /**
     * Returns all the images in a given data set, with their properties as columns. You can get the column names
     * by calling getColumnNames() on the returned ResultSetIterator object.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getImagesByDataSet(String dataSetName) throws DaoException {

    	try {
	    	String sql = "select * from image_data_mv where data_set = '"+dataSetName+"' order by slide_code,name ";
        	Connection conn = getJdbcConnection();
        	PreparedStatement stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
    		ResultSet rs = stmt.executeQuery();
    		return new ResultSetIterator(conn, stmt, rs);    		
    	}
    	catch (SQLException e) {
    		throw new DaoException("Error querying SAGE", e);
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

		SageTerm fullImagePath = new SageTerm();
		fullImagePath.setName("path");
		fullImagePath.setDataType("text");
		fullImagePath.setDisplayName("Full Image Path");
		fullImagePath.setDefinition("Absolute path to the image");
		map.put(fullImagePath.getName(),fullImagePath);
		
		SageTerm line = new SageTerm();
		line.setName("line");
		line.setDataType("text");
		line.setDisplayName("Fly line");
		line.setDefinition("Name of the fly line");
		map.put(line.getName(),line);

		SageTerm dataset = new SageTerm();
		dataset.setName("data_set");
		dataset.setDataType("text");
		dataset.setDisplayName("Data Set");
		dataset.setDefinition("Identifier of the data set");
		map.put(dataset.getName(),dataset);
		
		return map;
    }
}
