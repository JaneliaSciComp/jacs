
package org.janelia.it.jacs.compute.access;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.entity.SageArtifactExportService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Simple JDBC access to the Sage database.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageDAO {

    public static final String IMAGE_PROP_PATH = "image_query_path";
    public static final String IMAGE_PROP_LINE_TERM = "image_query_line";
    public static final String LINE_PROP_LINE_TERM = "line_query_line";
    
    private final String jndiPath = SystemConfigurationProperties.getString("sage.jdbc.jndiName", null);
    private final String jdbcDriver = SystemConfigurationProperties.getString("sage.jdbc.driverClassName", null);
    private final String jdbcUrl = SystemConfigurationProperties.getString("sage.jdbc.url", null);
    private final String jdbcUser = SystemConfigurationProperties.getString("sage.jdbc.username", null);
    private final String jdbcPw = SystemConfigurationProperties.getString("sage.jdbc.password", null);

    protected Logger log;
    protected SessionFactory sessionFactory;
    protected Session externalSession;
    
    public Connection getJdbcConnection() throws DaoException {
        Connection connection;
        try {
            if (!StringUtils.isEmpty(jndiPath)) {
                log.debug("getJdbcConnection() using these parameters: jndiPath="+jndiPath);
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) PortableRemoteObject.narrow(ctx.lookup(jndiPath), DataSource.class);
                connection = ds.getConnection();
            } else {
                log.debug("getJdbcConnection() using these parameters: driverClassName="+jdbcDriver+" url="+jdbcUrl+" user="+jdbcUser);
                Class.forName(jdbcDriver);
                connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            }
            connection.setAutoCommit(false);
        } catch (Exception e) {
            throw new DaoException(e);
        }
        return connection;
    }

    public SageDAO(Logger log) {
        this.log = log;
    }

    private SessionFactory getSessionFactory() {
        try {
            if (sessionFactory==null) {
                sessionFactory = (SessionFactory) new InitialContext().lookup("java:/hibernate/SageSessionFactory");
            }
            return sessionFactory;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Session getCurrentSession() {
        if (externalSession == null) {
            return getSessionFactory().getCurrentSession();
        }
        else {
            return externalSession;
        }
    }

    public Session getSession() {
        return getCurrentSession();
    }

    /**
     * Returns all the images in a given image family with a null data set, with their properties as columns. 
     * You can get the column names by calling getColumnNames() on the returned ResultSetIterator object.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
    public ResultSetIterator getImagesByFamily(String sageImageFamily) throws DaoException {

        Connection connection = null;
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;

        try {
            final String sql = "select * from image_data_mv where family=? and data_set is null and display=true order by slide_code, name";
            connection = getJdbcConnection();
            pStatement = connection.prepareStatement(sql);
            pStatement.setString(1, sageImageFamily);
            pStatement.setFetchSize(Integer.MIN_VALUE);
            resultSet = pStatement.executeQuery();

        } catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection, log);
            throw new DaoException("Error querying SAGE", e);
        }

        return new ResultSetIterator(connection, pStatement, resultSet);
    }

    /**
     * Returns all the images in a given data set, with their "core" properties as columns.
     * The client must call close() on the returned iterator when finished with it. 
     * @return Iterator over the JDBC result set. 
     * @throws DaoException
     */
//    public ResultSetIterator getImagesByDataSet(String dataSetName) throws DaoException {
//
//        Connection connection = null;
//        PreparedStatement pStatement = null;
//        ResultSet resultSet = null;
//
//        try {
//            connection = getJdbcConnection();
//            pStatement = connection.prepareStatement(CORE_IMAGE_PROPERTY_SQL);
//            pStatement.setString(1, dataSetName);
//            pStatement.setFetchSize(Integer.MIN_VALUE);
//
//            resultSet = pStatement.executeQuery();
//
//        }
//        catch (SQLException e) {
//            ResultSetIterator.close(resultSet, pStatement, connection, log);
//            throw new DaoException("Error querying SAGE", e);
//        }
//
//        return new ResultSetIterator(connection, pStatement, resultSet);
//    }

    /**
     * Returns all the images in a given data set, with ALL their non-null properties as columns.
     * The client must call close() on the returned iterator when finished with it.
     * @return Iterator over the JDBC result set.
     * @throws DaoException
     */
    public ResultSetIterator getAllImagePropertiesByDataSet(String dataSetName) throws DaoException {

        Connection connection = null;
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getJdbcConnection();
            final List<String> propertyTypeNames = getImagePropertyTypes(dataSetName, connection);
            final String sql = buildImagePropertySql(propertyTypeNames);

            pStatement = connection.prepareStatement(sql);
            pStatement.setFetchSize(Integer.MIN_VALUE);
            pStatement.setString(1, dataSetName);

            resultSet = pStatement.executeQuery();

        } catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection, log);
            throw new DaoException("Error querying SAGE", e);
        }

        return new ResultSetIterator(connection, pStatement, resultSet);
    }

    /**
     * Returns all the images in a given data set, with ALL their non-null line properties as columns.
     * The client must call close() on the returned iterator when finished with it.
     * @return Iterator over the JDBC result set.
     * @throws DaoException
     */
    public ResultSetIterator getAllLineProperties() throws DaoException {

        Connection connection = null;
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getJdbcConnection();
            final List<String> propertyTypeNames = getLinePropertyTypes(connection);
            final String sql = buildLinePropertySql(propertyTypeNames);

            pStatement = connection.prepareStatement(sql);
            pStatement.setFetchSize(Integer.MIN_VALUE);

            resultSet = pStatement.executeQuery();

        }
        catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection, log);
            throw new DaoException("Error querying SAGE", e);
        }

        return new ResultSetIterator(connection, pStatement, resultSet);
    }

    public Map<String,SageTerm> getSageVocabulary() throws DaoException {
        Map<String, SageTerm> entireVocabulary = new HashMap<>();
        entireVocabulary.putAll(getSageVocabulary("light_imagery"));
        entireVocabulary.putAll(getSageVocabulary("line"));
        return entireVocabulary;
    }

    /**
     * @return a map of all Sage controlled vocabulary terms for light imagery and related vocabularies.
     * @throws DaoException
     */
    Map<String,SageTerm> getSageVocabulary(String cv) throws DaoException {
        String pathPrefix = "http://sage.int.janelia.org/sage-ws/cvs/";
        String pathSuffix = "/with-all-related-cvs";

        Map<String,SageTerm> map = new HashMap<>();
        map.putAll(getStaticTerms());

        try {
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(pathPrefix+cv+pathSuffix);
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
                        log.warn("Term with no name encountered in "+ cv);
                        continue;
                    }

                    if (dataTypeNode==null) {
                        log.warn("Term with no type (name="+nameNode.getText()+") encountered in "+ cv);
                        continue;
                    }

                    SageTerm st = new SageTerm();
                    st.setName(nameNode.getText());
                    st.setDataType(dataTypeNode.getText());
                    st.setDisplayName(displayNameNode != null ? displayNameNode.getText() : st.getName());
                    st.setDefinition(definitionNode != null ? definitionNode.getText() : "");
                    st.setCv(cv);
                    map.put(st.getKey(),st);
                }
                else {
                    log.warn("Expecting <term>, got "+o);
                }
            }
        }
        catch (Exception e) {
            throw new DaoException("Error querying Sage Web Service", e);
        }

        return map;
    }

    public CvTerm getCvTermByName(String cvName, String termName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getCvTermByName(cvName="+cvName+", termName="+termName+")");    
        }
        
        Session session = getCurrentSession();
        Query query = session.createQuery("select term from CvTerm term " +
                                          "join term.cv cv " +
                                          "where cv.name = :cvName " +
                                          "and term.name = :termName ");
        query.setString("cvName", cvName);
        query.setString("termName", termName);
        return (CvTerm)query.uniqueResult();
    }

    public Line getLineByName(String name) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getLineIdByName(name="+name+")");    
        }
        
        Session session = getCurrentSession();
        Query query = session.createQuery("select line from Line line where line.name = :name ");
        query.setString("name", name);
        return (Line)query.uniqueResult();
    }

    public Image getImageByName(String imageName) {
        if (log.isTraceEnabled()) {
            log.trace("getImageByName(imageName="+imageName+")");    
        }
        Session session = getCurrentSession();
        Query query = session.createQuery("select image from Image image where image.name = :name ");
        query.setString("name", imageName);
        return (Image)query.uniqueResult();
    }

    public SecondaryImage getSecondaryImageByName(String imageName) {
        if (log.isTraceEnabled()) {
            log.trace("getSecondaryImageByName(imageName="+imageName+")");    
        }
        Session session = getCurrentSession();
        Query query = session.createQuery("select image from SecondaryImage image where image.name = :name ");
        query.setString("name", imageName);
        return (SecondaryImage)query.uniqueResult();
    }

    public Image getImage(Integer id) {
        if (log.isTraceEnabled()) {
            log.trace("getImages(id="+id+")");    
        }
        Session session = getCurrentSession();
        Query query = session.createQuery("select image from Image image where image.id = :ids ");
        query.setInteger("id", id);
        return (Image)query.uniqueResult();
    }
    
    @SuppressWarnings("unchecked")
	public List<Image> getImages(Collection<Integer> ids) {
        if (log.isTraceEnabled()) {
            log.trace("getImages(ids.size="+ids.size()+")");    
        }
        if (ids.isEmpty()) return new ArrayList<>();
        Session session = getCurrentSession();
        Query query = session.createQuery("select image from Image image where image.id in (:ids) ");
        query.setParameterList("ids", ids);
        return (List<Image>) query.list();
    }

    public void deleteImageProperty(ImageProperty imageProperty) throws DaoException {
        try {
        	Image image = imageProperty.getImage();
        	if (image!=null) {
        		image.getImageProperties().remove(imageProperty);
        	}
            getCurrentSession().delete(imageProperty);
        } 
        catch (Exception e) {
            throw new DaoException("Error deleting image property in SAGE", e);
        }
    }
    
    public ImageProperty saveImageProperty(ImageProperty imageProperty) throws DaoException {
    	if (imageProperty.getImage()==null) {
    		throw new DaoException("Image property is not associated with an image");
    	}
    	if (imageProperty.getType()==null) {
    		throw new DaoException("Image property has no type");
    	}
        if (log.isTraceEnabled()) {
            log.trace("saveImageProperty(image.id="+imageProperty.getImage().getId()+", type.name="+imageProperty.getType().getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(imageProperty);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving image property in SAGE", e);
        }
        return imageProperty;
    }

	public ImageProperty setImageProperty(Image image, CvTerm type, String value) throws Exception {
		return setImageProperty(image, type, value, new Date());
	}
	
	public ImageProperty setImageProperty(Image image, CvTerm type, String value, Date createDate) throws Exception {
    	for(ImageProperty property : image.getImageProperties()) {
    		if (property.getType().equals(type) && !property.getValue().equals(value)) {
    			// Update existing property value
    			log.debug("Overwriting existing "+type.getName()+" value ("+property.getValue()+") with new value ("+value+") for image "+image.getId()+")");
    			property.setValue(value);
    			return saveImageProperty(property);
    		}
    	}
    	// Set new property
        ImageProperty prop = new ImageProperty(type, image, value, createDate);
        image.getImageProperties().add(prop);
        saveImageProperty(prop);
        return prop;
    }
	
    public Image saveImage(Image image) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveImage(image.name="+image.getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(image);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving primary image in SAGE", e);
        }
        return image;
    }
    
    public SecondaryImage saveSecondaryImage(SecondaryImage secondaryImage) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveSecondaryImage(secondaryImage.name="+secondaryImage.getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(secondaryImage);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving secondary image in SAGE", e);
        }
        return secondaryImage;
    }
    
    /**
     * @return map of static terms which are not part of any vocabulary.
     */
    private Map<String,SageTerm> getStaticTerms() {

        final String[][] terms = {
                //name           displayName        dataType     definition                               vocabulary
                {"id",           "SAGE Id",         "integer",   "Image identifier within SAGE database", "image_query"},
                {"name",         "Image Path",      "text",      "Relative path to the image",            "image_query"},
                {"path",         "Full Image Path", "text",      "Absolute path to the image",            "image_query"},
                {"line",         "Fly Line",        "text",      "Name of the genetic line",                  "image_query"},
                
                {"id",           "SAGE Line Id",    "integer",   "Line identifier within SAGE database",  "line_query"},
                {"lab",          "Lab",             "text",      "Lab",                                   "line_query"},
                {"gene",         "Gene",            "text",      "Gene",                                  "line_query"},
                {"organism",     "Organism",        "text",      "Organism",                              "line_query"},
                {"line",         "Fly Line",        "text",      "Name of the genetic line",              "line_query"},
                {"synonyms",     "Synonyms",        "text",      "Synonyms for the genetic line",         "line_query"}
        };

        Map<String,SageTerm> map = new HashMap<>();
        for (String[] termData : terms) {
            SageTerm term = new SageTerm(termData[0], termData[1], termData[2], termData[3], termData[4]);
            map.put(term.getKey(), term);
        }

        return map;
    }

    /**
     *
     * @param  dataSet     image data set for filter.
     * @param  connection  current database connection.
     *
     * @return list of defined property types for the specified dataSet
     *
     * @throws SQLException
     *   if list query fails.
     */
    private List<String> getImagePropertyTypes(String dataSet,
                                               Connection connection) throws SQLException {
        List<String> list = new ArrayList<>(256);

        final String sql = "select distinct ip1.cv,ip1.type from image_property_vw ip1 " +
                "inner join (" +
                "  select i.id from image_vw i " +
                "  inner join image_property_vw ip2 on " +
                "    (ip2.image_id = i.id and ip2.type='data_set' and ip2.value=?) " +
                ") image_vw on (ip1.image_id = image_vw.id) " +
                "order by ip1.type";
        
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;
        try {
            pStatement = connection.prepareStatement(sql);
            pStatement.setString(1, dataSet);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                String cv = resultSet.getString(1);
                String term = resultSet.getString(2);
                list.add(cv+":"+term);
            }
        } 
        finally {
            ResultSetIterator.close(resultSet, pStatement, null, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("getImagePropertyTypes: returning " + list);
        }

        return list;
    }

    /**
     *
     * @param  connection  current database connection.
     *
     * @return list of defined line property types 
     *
     * @throws SQLException
     *   if list query fails.
     */
    private List<String> getLinePropertyTypes(Connection connection) throws SQLException {
        List<String> list = new ArrayList<>();

        final String sql = "select distinct lp1.cv,lp1.type from line_property_vw lp1 " +
                           "where lp1.cv in ('line','light_imagery') ";
        
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;
        try {
            pStatement = connection.prepareStatement(sql);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                String cv = resultSet.getString(1);
                String term = resultSet.getString(2);
                list.add(cv+":"+term);
            }
        } 
        finally {
            ResultSetIterator.close(resultSet, pStatement, null, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("getLinePropertyTypes: returning " + list);
        }

        return list;
    }

    /**
     *
     * @param  propertyTypeNames  names of image properties to include in query.
     *
     * @return dynamically built SQL statement for the specified property type names.
     */
    private String buildImagePropertySql(List<String> propertyTypeNames) {
        StringBuilder sql = new StringBuilder(2048);

        sql.append(ALL_IMAGE_PROPERTY_SQL_1);
        sql.append(buildPropertySql(propertyTypeNames, "ip1"));

        sql.append(ALL_IMAGE_PROPERTY_SQL_2);

        if (log.isDebugEnabled()) {
            log.debug("buildImagePropertySql: returning \"" + sql + "\"");
        }

        return sql.toString();
    }
    
    /**
     *
     * @param  propertyTypeNames names of line properties to include in query.
     *
     * @return dynamically built SQL statement for the specified property type names.
     */
    private String buildLinePropertySql(List<String> propertyTypeNames) {
        StringBuilder sql = new StringBuilder(2048);

        sql.append(ALL_LINE_PROPERTY_SQL_1);
        sql.append(buildPropertySql(propertyTypeNames, "lp1"));
        
        sql.append(ALL_LINE_PROPERTY_SQL_2);

        if (log.isDebugEnabled()) {
            log.debug("buildLinePropertySql: returning \"" + sql + "\"");
        }

        return sql.toString();
    }
    
    /**
     * @param propertyTypeNames names of qualified properties to include in the query (with format "<cv>:<term>")
     * @param tableAlias alias of the table to select property values from
     * @return returns the property SQL
     */
    private String buildPropertySql(List<String> propertyTypeNames, String tableAlias) {
        StringBuilder sql = new StringBuilder();
        for (String qualifiedTypeName : propertyTypeNames) {
            String typeLabel = qualifiedTypeName.replaceFirst(":", "_");
            String typeName = qualifiedTypeName.substring(qualifiedTypeName.indexOf(':')+1);
            sql.append(", max(IF(").append(tableAlias).append(".type='");
            sql.append(typeName);
            sql.append("', ").append(tableAlias).append(".value, null)) AS `");
            sql.append(typeLabel);
            sql.append('`'); // must escape column names to prevent conflict with SQL reserved words like 'condition'
        }
        return sql.toString();
    }

//    private static final String CORE_IMAGE_PROPERTY_SQL =
//            "select i.id id, slide_code.value slide_code, i.path path, tile.value tile, line.name line, " +
//            "channel_spec.value channel_spec, gender.value gender, age.value age, effector.value effector, " +
//            "area.value area, channels.value channels, mounting_protocol.value mounting_protocol, tissue_orientation.value tissue_orientation, objective.value objective, " +
//            "voxel_size_x.value voxel_size_x, voxel_size_y.value voxel_size_y, voxel_size_z.value voxel_size_z, " +
//            "dimension_x.value dimension_x, dimension_y.value dimension_y, dimension_z.value dimension_z, cross_barcode.value cross_barcode " +
//            "from image i " +
//            "join line line on i.line_id = line.id " +
//            "join image_property_vw slide_code on i.id = slide_code.image_id and slide_code.type = 'slide_code' " +
//            "join image_property_vw data_set on i.id = data_set.image_id and data_set.type = 'data_set' " +
//            "left outer join image_property_vw tile on i.id = tile.image_id and tile.type = 'tile' " +
//            "left outer join image_property_vw channel_spec on i.id = channel_spec.image_id and channel_spec.type = 'channel_spec' " +
//            "left outer join image_property_vw gender on i.id = gender.image_id and gender.type = 'gender' " +
//            "left outer join image_property_vw age on i.id = age.image_id and age.type = 'age' " +
//            "left outer join image_property_vw effector on i.id = effector.image_id and effector.type = 'effector' " +
//            "left outer join image_property_vw area on i.id = area.image_id and area.type = 'area' " +
//            "left outer join image_property_vw channels on i.id = channels.image_id and channels.type = 'channels' " +
//            "left outer join image_property_vw mounting_protocol on i.id = mounting_protocol.image_id and mounting_protocol.type = 'mounting_protocol' " +
//            "left outer join image_property_vw tissue_orientation on i.id = tissue_orientation.image_id and tissue_orientation.type = 'tissue_orientation' " +
//            "left outer join image_property_vw objective on i.id = objective.image_id and objective.type = 'objective' " +
//            "left outer join image_property_vw voxel_size_x on i.id = voxel_size_x.image_id and voxel_size_x.type = 'voxel_size_x' " +
//            "left outer join image_property_vw voxel_size_y on i.id = voxel_size_y.image_id and voxel_size_y.type = 'voxel_size_y' " +
//            "left outer join image_property_vw voxel_size_z on i.id = voxel_size_z.image_id and voxel_size_z.type = 'voxel_size_z' " +
//            "left outer join image_property_vw dimension_x on i.id = dimension_x.image_id and dimension_x.type = 'dimension_x' " +
//            "left outer join image_property_vw dimension_y on i.id = dimension_y.image_id and dimension_y.type = 'dimension_y' " +
//            "left outer join image_property_vw dimension_z on i.id = dimension_z.image_id and dimension_z.type = 'dimension_z' " +
//            "left outer join image_property_vw cross_barcode on i.id = cross_barcode.image_id and cross_barcode.type = 'cross_barcode' " +
//            "where i.display=true and i.path is not null " +
//            "and data_set.value=? " +
//            "and i.created_by!='"+SageArtifactExportService.CREATED_BY+"' " +
//            "order by slide_code.value, i.path";
//
    private static final String ALL_IMAGE_PROPERTY_SQL_1 =
            "select image_vw.id image_query_id, image_vw.name image_query_name, image_vw.path image_query_path, image_vw.line image_query_line, image_vw.family light_imagery_family, " +
            "image_vw.capture_date light_imagery_capture_date, image_vw.representative light_imagery_representative, image_vw.created_by light_imagery_created_by";

    private static final String ALL_IMAGE_PROPERTY_SQL_2 =
            " from image_property_vw ip1 " +
            "inner join (" +
            "  select i.id, i.line, i.name, i.path, i.family, i.capture_date, i.representative, i.created_by" +
            "  from image_vw i" +
            "  inner join image_property_vw ip2 on (ip2.image_id=i.id and ip2.type='data_set' and ip2.value=?)" +
            "  where i.display=true and i.path is not null" +
            "  and i.created_by!='"+SageArtifactExportService.CREATED_BY+"' " +
            ") image_vw on ip1.image_id = image_vw.id " +
            "where ip1.cv in ('light_imagery') " +
            "group by image_vw.id ";

    private static final String ALL_LINE_PROPERTY_SQL_1 =
            "select line_vw.id line_query_id, line_vw.name line_query_line, line_vw.lab line_query_lab, line_vw.gene line_query_gene, line_vw.organism line_query_organism, line_vw.synonyms line_query_synonyms";

    private static final String ALL_LINE_PROPERTY_SQL_2 =
            " from line_property_vw lp1  " +
            "inner join line_vw on lp1.line_id = line_vw.id " +
            "inner join ( " +
            "  select distinct line from image_data_mv where data_set is not null and display=true and path is not null " +
            ") image_lines on line_vw.name = image_lines.line  " +
            "where lp1.cv in ('line','light_imagery') " +
            "group by line_vw.id ";
            
}