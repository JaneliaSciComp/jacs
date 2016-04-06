
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

import com.google.common.collect.ImmutableList;
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
import org.janelia.it.jacs.compute.service.entity.sample.SlideImage;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Experiment;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.Observation;
import org.janelia.it.jacs.model.sage.SageSession;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Simple JDBC access to the Sage database.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageDAO {

    public static final String IMAGE_PROP_PATH = "image_query_path";
    public static final String IMAGE_PROP_JFS_PATH = "image_query_jfs_path";
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
     * Retrieve the Sage image by the LSM path
     * @param lsmPath
     * @return
     */
    public SlideImage getSlideImageByDatasetAndLSMName(String datasetName, String lsmName) throws DaoException {
        String sql = "select " +
                    COMMON_IMAGE_VW_ATTR + "," +
                    "line_vw.lab as image_lab_name" +
                    buildPropertySql(ImmutableList.of("sample:data_set"), "ip1") + " " +
                    "from image_vw " +
                    "join image image_t on image_t.id = image_vw.id " +
                    "join line_vw on line_vw.id = image_t.line_id " +
                    "join (" +
                    "  select ip2.image_id, ip2.type, ip2.value from image_vw i " +
                    "  inner join image_property_vw ip2 on " +
                    "    (ip2.image_id = i.id and ip2.type='data_set' and ip2.value=?) " +
                    ") ip1 on (ip1.image_id = image_vw.id) " +
                    "where image_vw.name = ? ";
        log.debug("GetSlideImageByLSMName: " + sql + "(" + lsmName + ")");
        SlideImage slideImage = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            pstmt = conn.prepareStatement(sql);
            int fieldIndex = 1;
            pstmt.setString(fieldIndex++, datasetName);
            pstmt.setString(fieldIndex++, lsmName);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                slideImage = new SlideImage();
                slideImage.setDatasetName(rs.getString("sample_data_set"));
                slideImage.setSageId(rs.getLong("image_query_id"));
                slideImage.setImageName(rs.getString("image_query_name"));
                slideImage.setImagePath(rs.getString("image_query_path"));
                slideImage.setJfsPath(rs.getString("image_query_jfs_path"));
                slideImage.setLine(rs.getString("image_query_line"));
                slideImage.setLab(rs.getString("image_lab_name"));
                Date createDate = rs.getTimestamp("image_query_create_date");
                if (createDate!=null) {
                    String tmogDate = ISO8601Utils.format(createDate);
                    if (tmogDate!=null) {
                        slideImage.setTmogDate(tmogDate);
                    }
                }
            }
            ResultSetIterator.close(rs, pstmt, null, log);
            if (slideImage != null) {
                fillImageProperties(slideImage, conn);
            }
        } catch (SQLException sqle) {
            throw new DaoException(sqle);
        } finally {
            ResultSetIterator.close(rs, pstmt, conn, log);
        }
        return slideImage;
    }

    private void fillImageProperties(SlideImage slideImage, Connection conn) throws DaoException {
        String sql = "select " +
                "ip.cv, ip.type, ip.value " +
                "from image_property_vw ip " +
                "where ip.image_id = ? ";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, slideImage.getSageId());
            rs = pstmt.executeQuery();
            Map<String, Object> data = new HashMap<>();
            while (rs.next()) {
                String cv = rs.getString("cv");
                String type = rs.getString("type");
                Object value = rs.getObject("value");
                // prefix every type with the controlled vocabulary except the data_set type
                String key = cv + "_" + type;
                data.put(key, value);
            }
            slideImage.setSlideCode((String) data.get("light_imagery_slide_code"));
            slideImage.setTileType((String) data.get("light_imagery_tile"));
            slideImage.setCrossBarcode((String) data.get("fly_cross_barcode"));
            slideImage.setChannelSpec((String) data.get("light_imagery_channel_spec"));
            slideImage.setGender((String) data.get("light_imagery_gender"));
            slideImage.setArea((String) data.get("light_imagery_area"));
            slideImage.setAge((String) data.get("light_imagery_age"));
            slideImage.setChannels((String) data.get("light_imagery_channels"));
            slideImage.setMountingProtocol((String) data.get("light_imagery_mounting_protocol"));
            slideImage.setTissueOrientation((String) data.get("light_imagery_tissue_orientation"));
            slideImage.setVtLine((String) data.get("light_imagery_vt_line"));
            slideImage.setEffector((String)data.get("fly_effector"));
            String objectiveStr = (String) data.get("light_imagery_objective");
            if (objectiveStr!=null) {
                if (objectiveStr.contains(Objective.OBJECTIVE_10X.getName())) {
                    slideImage.setObjective(Objective.OBJECTIVE_10X.getName());
                }
                else if (objectiveStr.contains(Objective.OBJECTIVE_20X.getName())) {
                    slideImage.setObjective(Objective.OBJECTIVE_20X.getName());
                }
                else if (objectiveStr.contains(Objective.OBJECTIVE_40X.getName())) {
                    slideImage.setObjective(Objective.OBJECTIVE_40X.getName());
                }
                else if (objectiveStr.contains(Objective.OBJECTIVE_63X.getName())) {
                    slideImage.setObjective(Objective.OBJECTIVE_63X.getName());
                }
            }
            String voxelSizeX = (String) data.get("light_imagery_voxel_size_x");
            String voxelSizeY = (String) data.get("light_imagery_voxel_size_y");
            String voxelSizeZ = (String) data.get("light_imagery_voxel_size_z");
            if (voxelSizeX!=null && voxelSizeY!=null && voxelSizeZ!=null) {
                slideImage.setOpticalRes(voxelSizeX,voxelSizeY,voxelSizeZ);
            }
            String imageSizeX = (String) data.get("light_imagery_dimension_x");
            String imageSizeY = (String) data.get("light_imagery_dimension_y");
            String imageSizeZ = (String) data.get("light_imagery_dimension_z");
            if (imageSizeX!=null && imageSizeY!=null && imageSizeZ!=null) {
                slideImage.setPixelRes(imageSizeX,imageSizeY,imageSizeZ);
            }
        } catch (SQLException sqle) {
            throw new DaoException(sqle);
        } finally {
            ResultSetIterator.close(rs, pstmt, null, log);
        }

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
            final List<String> propertyTypeNames = getImagePropertyTypesByDataSet(dataSetName, connection);
            final String sql = buildImagePropertySqlForPropertyList(propertyTypeNames);

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
        entireVocabulary.putAll(getStaticTerms());
        entireVocabulary.putAll(getSageVocabulary("light_imagery"));
        entireVocabulary.putAll(getSageVocabulary("line"));
        entireVocabulary.putAll(getSageVocabulary("fly"));
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
                    // todo this traversal assumes all items with a name belong to the same cv passed in.  This is not the case as
                    // more than one cv can be retruned by the web service call.  There is a hierarchy.  We need to respect this.
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
                                          "and (term.name = :termName or term.displayName = :displayName) " + 
                                          "order by term.id ");
        query.setString("cvName", cvName);
        query.setString("termName", termName);
        query.setString("displayName", termName);
        List<CvTerm> list = query.list();
        if (list.isEmpty()) return null;
        // Because of a lack of constraints, SAGE could contain duplicate CV terms, so we assume the first one is best.
        return list.get(0);
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
        Query query = session.createQuery("select image from Image image where image.id = :id ");
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

    @SuppressWarnings("unchecked")
    public List<Image> getImagesByPropertyValue(CvTerm propertyType, String value) throws DaoException {
        try {
            if (log.isTraceEnabled()) {
                log.trace("getImagesByPropertyValue(propertyType.name="+propertyType.getName()+", value="+value+")");    
            }
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select distinct ip.image from ImageProperty ip ");
            hql.append("join ip.image ");
            hql.append("where ip.type=:type and ip.value=:value ");
            Query query = session.createQuery(hql.toString());
            query.setEntity("type", propertyType);
            query.setString("value", value);
            return query.list();
        } 
        catch (Exception e) {
            throw new DaoException("Error deleting image property in SAGE", e);
        }
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

    public SageSession getSageSession(String sessionName, CvTerm type, Experiment experiment) {
        if (log.isTraceEnabled()) {
            log.trace("getSession(sessionName="+sessionName+")");    
        }
        Session session = getCurrentSession();
        Query query = session.createQuery(
                "select session from SageSession session "
                + "where session.name = :name "
                + "and session.type = :type "
                + "and session.experiment = :experiment "
                + "order by session.id ");
        query.setString("name", sessionName);
        query.setEntity("type", type);
        query.setEntity("experiment", experiment);
        List<SageSession> sessions = query.list();
        if (sessions.isEmpty()) return null;
        return sessions.get(0);
    }
    
    public SageSession saveSageSession(SageSession session) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveSession(sessionName="+session.getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(session);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving session in SAGE", e);
        }
        return session;
    }

    public Observation saveObservation(Observation observation) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveObservation(observation.type.name="+observation.getType().getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(observation);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving observation in SAGE", e);
        }
        return observation;
    }

    public Experiment getExperiment(String experimentName, CvTerm type, String experimenter) {
        if (log.isTraceEnabled()) {
            log.trace("getExperiment(experimentName="+experimentName+")");    
        }
        
        Session session = getCurrentSession();
        Query query = session.createQuery(
                "select experiment from Experiment experiment "
                + "where experiment.name = :name "
                + "and experiment.type = :type "
                + "and experiment.experimenter = :experimenter "
                + "order by experiment.id ");
        query.setString("name", experimentName);
        query.setEntity("type", type);
        query.setString("experimenter", experimenter);
        List<Experiment> experiments = query.list();
        if (experiments.isEmpty()) return null;
        return experiments.get(0);
    }
    
    public Experiment saveExperiment(Experiment experiment) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveExperiment(experiment="+experiment.getName()+")");    
        }
        
        try {
            getCurrentSession().saveOrUpdate(experiment);
        } 
        catch (Exception e) {
            throw new DaoException("Error saving experiment in SAGE", e);
        }
        return experiment;
    }
    
    /**
     * @return map of static terms which are not part of any vocabulary.
     */
    private Map<String,SageTerm> getStaticTerms() {

        final String[][] terms = {
                //name           displayName        dataType     definition                                 vocabulary
                {"id",           "SAGE Id",         "integer",   "Image identifier within SAGE database",   "image_query"},
                {"name",         "Image Path",      "text",      "Relative path to the image",              "image_query"},
                {"path",         "Full Image Path", "text",      "Absolute path to the image",              "image_query"},
                {"jfs_path",     "JFS Path",        "text",      "Path to the image in JFS",                "image_query"},
                {"line",         "Fly Line",        "text",      "Name of the genetic line",                "image_query"},
                {"create_date",  "Create Date",     "date_time", "Date when the image was created in SAGE", "image_query"},
                
                {"id",           "SAGE Line Id",    "integer",   "Line identifier within SAGE database",    "line_query"},
                {"lab",          "Lab",             "text",      "Lab",                                     "line_query"},
                {"gene",         "Gene",            "text",      "Gene",                                    "line_query"},
                {"organism",     "Organism",        "text",      "Organism",                                "line_query"},
                {"line",         "Fly Line",        "text",      "Name of the genetic line",                "line_query"},
                {"synonyms",     "Synonyms",        "text",      "Synonyms for the genetic line",           "line_query"}
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
    private List<String> getImagePropertyTypesByDataSet(String dataSet,
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
            log.debug("getImagePropertyTypesByDataSet: returning " + list);
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
    private String buildImagePropertySqlForPropertyList(List<String> propertyTypeNames) {
        StringBuilder sql = new StringBuilder(2048);

        sql.append("select ");
        sql.append(COMMON_IMAGE_VW_ATTR);
        sql.append(buildPropertySql(propertyTypeNames, "ip1"));
        sql.append(IMAGE_PROPERTY_SQL_JOIN);

        if (log.isDebugEnabled()) {
            log.debug("buildImagePropertySqlForPropertyList: returning \"" + sql + "\"");
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

    private static final String COMMON_IMAGE_VW_ATTR =
                "image_vw.id image_query_id, image_vw.name image_query_name, image_vw.path image_query_path," +
                "image_vw.jfs_path image_query_jfs_path, image_vw.line image_query_line, image_vw.family light_imagery_family, " +
                "image_vw.capture_date light_imagery_capture_date, image_vw.representative light_imagery_representative, " +
                "image_vw.created_by light_imagery_created_by, image_vw.create_date image_query_create_date";

    private static final String IMAGE_PROPERTY_SQL_JOIN =
            " from image_property_vw ip1 " +
            "inner join ( " +
            "  select i.id, i.line, i.name, i.path, i.jfs_path, i.family, i.capture_date, i.representative, i.created_by, i.create_date " +
            "  from image_vw i " +
            "  inner join image_property_vw ip2 on (ip2.image_id=i.id and ip2.type='data_set' and ip2.value=?) " +
            "  where i.display=true and (i.path is not null or i.jfs_path is not null) " +
            "  and (i.created_by is null or i.created_by!='"+SageArtifactExportService.CREATED_BY+"') " +
            ") image_vw on ip1.image_id = image_vw.id " +
            "group by image_vw.id ";

    private static final String ALL_LINE_PROPERTY_SQL_1 =
            "select line_vw.id line_query_id, line_vw.name line_query_line, line_vw.lab line_query_lab, line_vw.gene line_query_gene, line_vw.organism line_query_organism, line_vw.synonyms line_query_synonyms";

    private static final String ALL_LINE_PROPERTY_SQL_2 =
            " from line_property_vw lp1  " +
            "inner join line_vw on lp1.line_id = line_vw.id " +
            "inner join ( " +
            "  select distinct line from image_data_mv where data_set is not null and display=true and (path is not null or jfs_path is not null) " +
            ") image_lines on line_vw.name = image_lines.line  " +
            "where lp1.cv in ('line','light_imagery','fly') " +
            "group by line_vw.id ";

}