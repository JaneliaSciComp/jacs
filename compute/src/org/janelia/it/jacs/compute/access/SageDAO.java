
package org.janelia.it.jacs.compute.access;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Simple JDBC access to the Sage database.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageDAO {

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
        getSessionFactory();
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
    public ResultSetIterator getImagesByDataSet(String dataSetName) throws DaoException {

        Connection connection = null;
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getJdbcConnection();
            pStatement = connection.prepareStatement(CORE_IMAGE_PROPERTY_SQL);
            pStatement.setString(1, dataSetName);
            pStatement.setFetchSize(Integer.MIN_VALUE);

            resultSet = pStatement.executeQuery();

        } catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection, log);
            throw new DaoException("Error querying SAGE", e);
        }

        return new ResultSetIterator(connection, pStatement, resultSet);
    }

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
     * @return a map of all Sage controlled vocabulary terms for light imagery and related vocabularies.
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
                        log.warn("Term with no name encountered in "+getUrl);
                        continue;
                    }

                    if (dataTypeNode==null) {
                        log.warn("Term with no type (name="+nameNode.getText()+") encountered in "+getUrl);
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
        StringBuffer hql = new StringBuffer("select term from CvTerm term ");
        hql.append("join term.cv cv ");
        hql.append("where cv.name = :cvName ");
        hql.append("and term.name = :termName ");
        Query query = session.createQuery(hql.toString());
        query.setString("cvName", cvName);
        query.setString("termName", termName);
        return (CvTerm)query.uniqueResult();
    }
    
    public CvTerm getCvTermByName(String name) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getLineIdByName(name="+name+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select term from CvTerm term ");
        hql.append("where term.name = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", name);
        return (CvTerm)query.uniqueResult();
    }
    
    public Line getLineByName(String name) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getLineIdByName(name="+name+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select line from Line line ");
        hql.append("where line.name = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", name);
        return (Line)query.uniqueResult();
        
//        Long lineId = null;
//        Connection connection = null;
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//        
//        try {
//            connection = getJdbcConnection();
//            statement = connection.prepareStatement(SELECT_LINE_BY_NAME_SQL);
//            statement.setString(1, name);
//            resultSet = statement.executeQuery();
//            while (resultSet.next()) {
//                lineId = resultSet.getLong("id");
//            }
//        }
//        catch (Exception e) {
//            throw new DaoException("Error querying SAGE for line by name", e);
//        }
//        finally {
//            try {    
//                if (resultSet != null) statement.close();
//                if (statement != null) statement.close();
//                if (connection != null)  connection.close();
//            }
//            catch (SQLException e) {
//                log.error("Failed to close JDBC", e);
//            }
//        }
//        
//        return lineId;
    }

    public List<Image> getImagesByCreator(String createdBy) {
        if (log.isTraceEnabled()) {
            log.trace("getImagesByCreator(createdBy="+createdBy+")");    
        }
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select image from Image image ");
        hql.append("where image.createdBy = :createdBy ");
        Query query = session.createQuery(hql.toString());
        query.setString("createdBy", createdBy);
        return (List<Image>)query.list();
    }
    
    public List<Image> getImagesByProperty(CvTerm term, String value) {
        if (log.isTraceEnabled()) {
            log.trace("getImagesByProperty(term="+term.getName()+", value="+value+")");    
        }
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select image from Image image ");
        hql.append("join image.imageProperties props ");
        hql.append("join props.type term ");
        hql.append("where term.id = :termId ");
        hql.append("and props.value like :value ");
        Query query = session.createQuery(hql.toString());
        query.setInteger("termId", term.getId());
        query.setString("value", value);
        return (List<Image>)query.list();
    }
    
    public List<Image> getImages(List<Integer> ids) {
        if (log.isTraceEnabled()) {
            log.trace("getImages(ids.size="+ids.size()+")");    
        }
        if (ids.isEmpty()) return new ArrayList<Image>();
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select image from Image image ");
        hql.append("where image.id in (:ids) ");
        Query query = session.createQuery(hql.toString());
        query.setParameterList("ids", ids);
        return (List<Image>)query.list();
    }
    
    public Image saveImage(Image image) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveImage(image.name="+image.getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(image);
        } 
        catch (Exception e) {
            throw new DaoException("Error creating new primary image in SAGE", e);
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
            throw new DaoException("Error creating new primary image in SAGE", e);
        }
        return secondaryImage;
    }
    
    public void removeImage(Image image) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("removeImage(image.name="+image.getName()+")");    
        }
        
        try {
            getCurrentSession().delete(image);
        } 
        catch (Exception e) {
            throw new DaoException("Error deleting primary image in SAGE", e);
        }
    }
    
    /**
     * @return map of static terms which are not part of any vocabulary.
     */
    private Map<String,SageTerm> getStaticTerms() {

        final String[][] terms = {
                //name           displayName        dataType     definition
                {"id",           "SAGE Id",         "integer",   "Identifier within SAGE database"},
                {"name",         "Image Path",      "text",      "Relative path to the image"},
                {"path",         "Full Image Path", "text",      "Absolute path to the image"},
                {"line",         "Fly Line",        "text",      "Name of the fly line"},
        };

        Map<String,SageTerm> map = new HashMap<String,SageTerm>();
        SageTerm term;
        for (String[] termData : terms) {
            term = new SageTerm(termData[0], termData[1], termData[2], termData[3]);
            map.put(term.getName(), term);
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
        List<String> list = new ArrayList<String>(256);

        final String sql = "select distinct ip1.type from image_property_vw ip1 " +
                "inner join (" +
                "  select i.id from image_vw i " +
                "  inner join image_property_vw ip2 on " +
                "    (ip2.image_id = i.id and ip2.type='data_set' and ip2.value=?) " +
                ") image_vw on (ip1.image_id = image_vw.id) order by ip1.type";
        PreparedStatement pStatement = null;
        ResultSet resultSet = null;
        try {
            pStatement = connection.prepareStatement(sql);
            pStatement.setString(1, dataSet);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
        } finally {
            ResultSetIterator.close(resultSet, pStatement, null, log);
        }

        if (log.isDebugEnabled()) {
            log.debug("getImagePropertyTypes: returning " + list);
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

        for (String typeName : propertyTypeNames) {
            sql.append(", max(IF(ip1.type='");
            sql.append(typeName);
            sql.append("', ip1.value, null)) AS `");
            sql.append(typeName);
            sql.append('`'); // must escape column names to prevent conflict with SQL reserved words like 'condition'
        }

        sql.append(ALL_IMAGE_PROPERTY_SQL_2);

        if (log.isDebugEnabled()) {
            log.debug("buildImagePropertySql: returning \"" + sql + "\"");
        }

        return sql.toString();
    }

    private static final String CORE_IMAGE_PROPERTY_SQL =
            "select i.id id, slide_code.value slide_code, i.path path, tile.value tile, line.name line, " +
            "channel_spec.value channel_spec, gender.value gender, age.value age, effector.value effector, " +
            "area.value area, channels.value channels, mounting_protocol.value mounting_protocol, objective.value objective, " +
            "voxel_size_x.value voxel_size_x, voxel_size_y.value voxel_size_y, voxel_size_z.value voxel_size_z, " +
            "dimension_x.value dimension_x, dimension_y.value dimension_y, dimension_z.value dimension_z " +
            "from image i " +
            "join line line on i.line_id = line.id " +
            "join image_property_vw slide_code on i.id = slide_code.image_id and slide_code.type = 'slide_code' " +
            "join image_property_vw data_set on i.id = data_set.image_id and data_set.type = 'data_set' " +
            "left outer join image_property_vw tile on i.id = tile.image_id and tile.type = 'tile' " +
            "left outer join image_property_vw channel_spec on i.id = channel_spec.image_id and channel_spec.type = 'channel_spec' " +
            "left outer join image_property_vw gender on i.id = gender.image_id and gender.type = 'gender' " +
            "left outer join image_property_vw age on i.id = age.image_id and age.type = 'age' " +
            "left outer join image_property_vw effector on i.id = effector.image_id and effector.type = 'effector' " +
            "left outer join image_property_vw area on i.id = area.image_id and area.type = 'area' " +
            "left outer join image_property_vw channels on i.id = channels.image_id and channels.type = 'channels' " +
            "left outer join image_property_vw mounting_protocol on i.id = mounting_protocol.image_id and mounting_protocol.type = 'mounting_protocol' " +
            "left outer join image_property_vw objective on i.id = objective.image_id and objective.type = 'objective' " +
            "left outer join image_property_vw voxel_size_x on i.id = voxel_size_x.image_id and voxel_size_x.type = 'voxel_size_x' " +
            "left outer join image_property_vw voxel_size_y on i.id = voxel_size_y.image_id and voxel_size_y.type = 'voxel_size_y' " +
            "left outer join image_property_vw voxel_size_z on i.id = voxel_size_z.image_id and voxel_size_z.type = 'voxel_size_z' " +
            "left outer join image_property_vw dimension_x on i.id = dimension_x.image_id and dimension_x.type = 'dimension_x' " +
            "left outer join image_property_vw dimension_y on i.id = dimension_y.image_id and dimension_y.type = 'dimension_y' " +
            "left outer join image_property_vw dimension_z on i.id = dimension_z.image_id and dimension_z.type = 'dimension_z' " +
            "where i.display=true and i.path is not null " +
            "and data_set.value=? " +
            "order by slide_code.value, i.path";

    private static final String ALL_IMAGE_PROPERTY_SQL_1 =
            "select image_vw.id, image_vw.line, image_vw.name, image_vw.path, image_vw.family, " +
            "image_vw.capture_date, image_vw.representative, image_vw.created_by";

    private static final String ALL_IMAGE_PROPERTY_SQL_2 =
            " from image_property_vw ip1 " +
            "inner join (" +
            "  select i.id, i.line, i.name, i.path, i.family, i.capture_date, i.representative, i.created_by" +
            "  from image_vw i" +
            "  inner join image_property_vw ip2 on (ip2.image_id=i.id and ip2.type='data_set' and ip2.value=?)" +
            "  inner join image_property_vw ip3 on (ip3.image_id=i.id and ip3.type='slide_code' and ip3.value is not null)" +
            "  where i.display=true and i.path is not null" +
            ") image_vw on (ip1.image_id = image_vw.id) " +
            "group by image_vw.id ";// +
            //"order by slide_code, image_vw.capture_date";
    
//    private static final String SELECT_LINE_BY_NAME_SQL = 
//            "select * from line where name =?";
//    
//    private static final String INSERT_PRIMARY_IMAGE_SQL = 
//            "insert into image (name, url, path, source_id, family_id, line_id, representative, display) values (?, ?, ?, ?, ?, ?, ?, ?)";
//
//    private static final String SELECT_PRIMARY_IMAGE_BY_NAME_AND_FAMILY_SQL = 
//            "select * from image where name=? and family_id=?";
//    
//    private static final String INSERT_SECONDARY_IMAGE_SQL = 
//            "insert into image (name, image_id, product_id, path, url) values (?, ?, ?, ?, ?)";
//    
//    private static final String SELECT_SECONDARY_IMAGE_BY_IMAGE_AND_PRODUCT_SQL = 
//            "select * from image where image_id=? and product_id=?";
//    
//    private static final String INSERT_IMAGE_PROPERTY_SQL = 
//            "insert into image_property (image_id, type_id, value) values (?, ?, ?)";
// 
//    private static final String CONSENSUS_IMAGE_PROPERTY_SQL = 
//            "select * from image_property where image_id in (?) order by type_id";
    
}