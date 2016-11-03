package org.janelia.it.jacs.compute.access;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.compute.service.domain.SageArtifactExportService;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Experiment;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.SageSession;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple JDBC access to the Sage database.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageDAO extends AbstractBaseDAO {

    private static Logger LOG = LoggerFactory.getLogger(SageDAO.class);

    public static final String IMAGE_PROP_LINE_TERM = "image_query_line";
    public static final String LINE_PROP_LINE_TERM = "line_query_line";

    @Resource(mappedName = "java://Sage_DataSource")
    private DataSource dataSource;

    public SageDAO(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Given an lsm name, get its representative slide image, without using a materialized view.  We want
     * immediate turnaround on data that has just been added to tables.
     *
     * @param lsmName find slide image for this.
     * @return fully-fleshed slide image.
     * @throws DaoException
     */
    public SlideImage getSlideImageByLSMName(String lsmName) throws DaoException {
        return getSlideImage(SLIDE_IMAGE_BY_LSMNAME_SQL, lsmName);
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
            connection = dataSource.getConnection();
            final List<String> propertyTypeNames = getImagePropertyTypesByDataSet(dataSetName, connection);
            final String sql = buildImagePropertySqlForPropertyList(propertyTypeNames);

            pStatement = connection.prepareStatement(sql);
            pStatement.setFetchSize(Integer.MIN_VALUE);
            pStatement.setString(1, dataSetName);

            resultSet = pStatement.executeQuery();

        } catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection);
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
            connection = dataSource.getConnection();
            final List<String> propertyTypeNames = getLinePropertyTypes(connection);
            final String sql = buildLinePropertySql(propertyTypeNames);

            pStatement = connection.prepareStatement(sql);
            pStatement.setFetchSize(Integer.MIN_VALUE);

            resultSet = pStatement.executeQuery();

        } catch (SQLException e) {
            ResultSetIterator.close(resultSet, pStatement, connection);
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
                        LOG.warn("Term with no name encountered in "+ cv);
                        continue;
                    }

                    if (dataTypeNode==null) {
                        LOG.warn("Term with no type (name="+nameNode.getText()+") encountered in "+ cv);
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
                    LOG.warn("Expecting <term>, got "+o);
                }
            }
        } catch (Exception e) {
            throw new DaoException("Error querying Sage Web Service", e);
        }

        return map;
    }

    public CvTerm getCvTermByName(String cvName, String termName) throws DaoException {
        LOG.trace("getCvTermByName(cvName="+cvName+", termName="+termName+")");
        String hql =
                "select term from CvTerm term " +
                "join term.cv cv " +
                "where cv.name = :cvName " +
                "and (term.name = :termName or term.displayName = :displayName) " +
                "order by term.id ";
        return findFirst(hql, 
                ImmutableMap.<String, Object>of(
                        "cvName", cvName,
                        "termName", termName,
                        "displayName", termName),
                CvTerm.class);
    }

    public Line getLineByName(String name) throws DaoException {
        LOG.trace("getLineIdByName(name="+name+")");
        String hql = "select line from Line line where line.name = :name ";
        return getAtMostOneResult(prepareQuery(hql,
                ImmutableMap.<String, Object>of(
                        "name", name),
                Line.class));
    }

    public Image getImageByName(String imageName) {
        LOG.trace("getImageByName(imageName="+imageName+")");
        String hql = "select image from Image image where image.name = :name ";
        return getAtMostOneResult(prepareQuery(hql,
                ImmutableMap.<String, Object>of(
                        "name", imageName),
                Image.class));
    }
    
    public SecondaryImage getSecondaryImageByName(String imageName) {
        LOG.trace("getSecondaryImageByName(imageName="+imageName+")");
        String hql = "select image from SecondaryImage image where image.name = :name ";
        return getAtMostOneResult(prepareQuery(hql,
                ImmutableMap.<String, Object>of(
                        "name", imageName),
                SecondaryImage.class));
    }

    public Image getImage(Integer id) {
        LOG.trace("getImage(id="+id+")");
        return findByNumericId(id, Image.class);
    }
    
    public List<Image> getImages(Collection<Integer> ids) {
        LOG.trace("getImages(ids.size="+ids.size()+")");
        if (ids.isEmpty()) return new ArrayList<>();
        String hql = "select image from Image image where image.id in (:ids) ";
        return findByQueryParams(hql,
                ImmutableMap.<String, Object>of(
                        "ids", ids),
                Image.class);
    }

    public List<Image> getImagesByPropertyValue(CvTerm propertyType, String value) throws DaoException {
        try {
            LOG.trace("getImagesByPropertyValue(propertyType.name=" + propertyType.getName() + ", value=" + value + ")");
            String hql = "select distinct ip.image from ImageProperty ip join ip.image where ip.type=:type and ip.value=:value ";
            return findByQueryParams(hql,
                    ImmutableMap.<String, Object>of(
                            "type", propertyType,
                            "value", value),
                    Image.class);
        } catch (Exception e) {
            throw new DaoException("Error deleting image property in SAGE", e);
        }
    }

    public String getImagePropertyValue(Image image, CvTerm propertyType) {
        for(ImageProperty property : image.getImageProperties()) {
            if (property.getType().equals(propertyType)) {
                return property.getValue();
            }
        }
        return null;
    }

    public void deleteImageProperty(ImageProperty imageProperty) throws DaoException {
        try {
            Image image = imageProperty.getImage();
            if (image!=null) {
                image.getImageProperties().remove(imageProperty);
            }
            delete(imageProperty);
        } catch (Exception e) {
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
        LOG.trace("saveImageProperty(image.id="+imageProperty.getImage().getId()+", type.name="+imageProperty.getType().getName()+")");
        try {
            save(imageProperty);
        } catch (Exception e) {
            throw new DaoException("Error saving image property in SAGE", e);
        }
        return imageProperty;
    }

    public ImageProperty setImageProperty(Image image, CvTerm type, String value) throws Exception {
        return setImageProperty(image, type, value, new Date());
    }
    
    public ImageProperty setImageProperty(Image image, CvTerm type, String value, Date createDate) throws Exception {
        for(ImageProperty property : image.getImageProperties()) {
            if (property.getType().equals(type)) {
                if (property.getValue().equals(value)) {
                    // Already at the correct value
                    return property;
                }
                else {
                    // Update existing property value
                    LOG.debug("Overwriting existing "+type.getName()+" value ("+property.getValue()+") with new value ("+value+") for image "+image.getId()+")");
                    property.setValue(value);
                    return saveImageProperty(property);
                }
            }
        }
        // Set new property
        ImageProperty prop = new ImageProperty(type, image, value, createDate);
        image.getImageProperties().add(prop);
        saveImageProperty(prop);
        return prop;
    }
    
    public Image saveImage(Image image) throws DaoException {
        LOG.trace("saveImage(image.name="+image.getName()+")");
        save(image);
        return image;
    }
    
    public SecondaryImage saveSecondaryImage(SecondaryImage secondaryImage) throws DaoException {
        LOG.trace("saveSecondaryImage(secondaryImage.name="+secondaryImage.getName()+")");
        save(secondaryImage);
        return secondaryImage;
    }

    public void deleteImage(Image image) throws DaoException {
        // This should cascade and delete all image properties and secondary images
        delete(image);
    }

    public void deleteSecondaryImage(SecondaryImage secondaryImage) throws DaoException {
        Image image = secondaryImage.getImage();
        if (image!=null) {
            image.getSecondaryImages().remove(secondaryImage);
        }
        delete(secondaryImage);
    }

    public SageSession getSageSession(String sessionName, CvTerm type, Experiment experiment) {
        LOG.trace("getSession(sessionName="+sessionName+")");
        String hql =
                "select session from SageSession session "
                + "where session.name = :name "
                + "and session.type = :type "
                + "and session.experiment = :experiment "
                + "order by session.id ";

        return findFirst(hql,
                ImmutableMap.<String, Object>of(
                        "name", sessionName,
                        "type", type,
                        "experiment", experiment),
                SageSession.class);
    }
    
    public SageSession saveSageSession(SageSession session) throws DaoException {
        LOG.trace("saveSession(sessionName="+session.getName()+")");

        try {
            save(session);
        } catch (Exception e) {
            throw new DaoException("Error saving session in SAGE", e);
        }
        return session;
    }

    public Experiment getExperiment(String experimentName, CvTerm type, String experimenter) {
        LOG.trace("getExperiment(experimentName="+experimentName+")");
        String hql =
                "select experiment from Experiment experiment "
                + "where experiment.name = :name "
                + "and experiment.type = :type "
                + "and experiment.experimenter = :experimenter "
                + "order by experiment.id ";
        return findFirst(hql,
                ImmutableMap.<String, Object>of(
                        "name", experimentName,
                        "type", type,
                        "experimenter", experimenter),
                Experiment.class);
    }
    
    public Experiment saveExperiment(Experiment experiment) throws DaoException {
        LOG.trace("saveExperiment(experiment="+experiment.getName()+")");
        try {
            save(experiment);
        } catch (Exception e) {
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

    private void fillImageProperties(SlideImage slideImage, Connection conn) throws DaoException {
        String sql = "select ip.cv, ip.type, ip.value from image_property_vw ip where ip.image_id = ? ";
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
                String key = "data_set".equals(type) ? type : cv + "_" + type;
                data.put(key, value);
            }
            slideImage.setDatasetName((String) data.get(SlideImage.DATA_SET));
            slideImage.setSlideCode((String) data.get(SlideImage.LIGHT_IMAGERY_SLIDE_CODE));
            slideImage.setTileType((String) data.get(SlideImage.LIGHT_IMAGERY_TILE));
            slideImage.setCrossBarcode((String) data.get(SlideImage.FLY_CROSS_BARCODE));
            slideImage.setChannelSpec((String) data.get(SlideImage.LIGHT_IMAGERY_CHANNEL_SPEC));
            slideImage.setGender((String) data.get(SlideImage.LIGHT_IMAGERY_GENDER));
            slideImage.setArea((String) data.get(SlideImage.LIGHT_IMAGERY_AREA));
            slideImage.setAge((String) data.get(SlideImage.LIGHT_IMAGERY_AGE));
            slideImage.setChannels((String) data.get(SlideImage.LIGHT_IMAGERY_CHANNELS));
            slideImage.setMountingProtocol((String) data.get(SlideImage.LIGHT_IMAGERY_MOUNTING_PROTOCOL));
            slideImage.setTissueOrientation((String) data.get(SlideImage.LIGHT_IMAGERY_TISSUE_ORIENTATION));
            slideImage.setVtLine((String) data.get(SlideImage.LIGHT_IMAGERY_VT_LINE));
            slideImage.setEffector((String)data.get(SlideImage.FLY_EFFECTOR));
            String objectiveStr = (String) data.get(SlideImage.LIGHT_IMAGERY_OBJECTIVE);
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
            String voxelSizeX = (String) data.get(SlideImage.LIGHT_IMAGERY_VOXEL_SIZE_X);
            String voxelSizeY = (String) data.get(SlideImage.LIGHT_IMAGERY_VOXEL_SIZE_Y);
            String voxelSizeZ = (String) data.get(SlideImage.LIGHT_IMAGERY_VOXEL_SIZE_Z);
            if (voxelSizeX!=null && voxelSizeY!=null && voxelSizeZ!=null) {
                slideImage.setOpticalRes(voxelSizeX,voxelSizeY,voxelSizeZ);
            }
            String imageSizeX = (String) data.get(SlideImage.LIGHT_IMAGERY_DIMENSION_X);
            String imageSizeY = (String) data.get(SlideImage.LIGHT_IMAGERY_DIMENSION_Y);
            String imageSizeZ = (String) data.get(SlideImage.LIGHT_IMAGERY_DIMENSION_Z);
            if (imageSizeX!=null && imageSizeY!=null && imageSizeZ!=null) {
                slideImage.setPixelRes(imageSizeX,imageSizeY,imageSizeZ);
            }
        } catch (SQLException sqle) {
            throw new DaoException(sqle);
        } finally {
            ResultSetIterator.close(rs, pstmt, null);
        }

    }

    /**
     *
     * @param  dataSet    image data set for filter.
     * @param  conn       current database connection.
     *
     * @return list of defined property types for the specified dataSet
     *
     * @throws SQLException
     *   if list query fails.
     */
    private List<String> getImagePropertyTypesByDataSet(String dataSet, Connection conn) throws SQLException {
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
            pStatement = conn.prepareStatement(sql);
            pStatement.setString(1, dataSet);
            resultSet = pStatement.executeQuery();
            while (resultSet.next()) {
                String cv = resultSet.getString(1);
                String term = resultSet.getString(2);
                list.add(cv+":"+term);
            }
        } finally {
            ResultSetIterator.close(resultSet, pStatement, null);
        }

        LOG.debug("getImagePropertyTypesByDataSet: returning {}", list);

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
            ResultSetIterator.close(resultSet, pStatement, null);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("getLinePropertyTypes: returning " + list);
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("buildImagePropertySqlForPropertyList: returning \"" + sql + "\"");
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

        LOG.debug("buildLinePropertySql: returning \"" + sql + "\"");

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

    private SlideImage getSlideImage(String sql, String... paramStrings) throws DaoException {
        SlideImage slideImage = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            pstmt = conn.prepareStatement(sql);
            int fieldIndex = 1;
            for (String paramString: paramStrings) {
                pstmt.setString(fieldIndex++, paramString);
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                slideImage = new SlideImage(new HashMap<String,Object>());
                slideImage.setSageId(rs.getInt(SlideImage.IMAGE_QUERY_ID));
                slideImage.setImageName(rs.getString(SlideImage.IMAGE_QUERY_NAME));
                slideImage.setImagePath(rs.getString(SlideImage.IMAGE_QUERY_PATH));
                slideImage.setJfsPath(rs.getString(SlideImage.IMAGE_QUERY_JFS_PATH));
                slideImage.setLine(rs.getString(SlideImage.IMAGE_QUERY_LINE));
                slideImage.setLab(rs.getString(SlideImage.IMAGE_LAB_NAME));
                Date createDate = rs.getTimestamp("image_query_create_date");
                if (createDate!=null) {
                    String tmogDate = ISO8601Utils.format(createDate);
                    if (tmogDate!=null) {
                        slideImage.setTmogDate(tmogDate);
                    }
                }
            }
            rs.close();
            rs = null;
            pstmt.close();
            pstmt = null;
            if (slideImage != null) {
                fillImageProperties(slideImage, conn);
            }
        } catch (SQLException sqle) {
            throw new DaoException(sqle);
        } finally {
            ResultSetIterator.close(rs, pstmt, conn);
        }
        return slideImage;
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

    public static final String SLIDE_IMAGE_BY_LSMNAME_SQL = "select " + COMMON_IMAGE_VW_ATTR + "," +
            "image_vw.id image_query_id,image_vw.name image_query_name,image_vw.path image_query_path," +
            "image_vw.jfs_path image_query_jfs_path,image_vw.line image_query_line,image_vw.family light_imagery_family," +
            "image_vw.capture_date light_imagery_capture_date,image_vw.representative light_imagery_representative," +
            "image_vw.created_by light_imagery_created_by,image_vw.create_date image_query_create_date,l.lab image_lab_name " +
            "from image_vw image_vw join line_vw l on (image_vw.line=l.name) where image_vw.name=?";

//    public static final String SLIDE_IMAGE_BY_LSMNAME_SQL = "select " + COMMON_IMAGE_VW_ATTR +
//            "i.id image_query_id,i.name image_query_name,i.path image_query_path," +
//            "i.jfs_path image_query_jfs_path,i.line image_query_line,i.family light_imagery_family," +
//            "i.capture_date light_imagery_capture_date,i.representative light_imagery_representative," +
//            "i.created_by light_imagery_created_by,i.create_date image_query_create_date,l.lab image_lab_name " +
//            "from image_vw i join line_vw l on (i.line=l.name) where i.name=?";

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
