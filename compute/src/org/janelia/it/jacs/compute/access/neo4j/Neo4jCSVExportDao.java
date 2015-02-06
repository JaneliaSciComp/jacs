package org.janelia.it.jacs.compute.access.neo4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.FileUtil;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Batch data insertion to the Neo4j data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jCSVExportDao extends AnnotationDAO {

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    private String outputDir = "/home/rokickik/dev/neo4j-batch-import/fw";
    
    protected LargeOperations largeOp;

    protected static final String REFNODE_COMMON_ROOTS = "COMMON_ROOTS";
    protected static final String REFNODE_SUBJECTS = "SUBJECTS";
    
    protected int numNodesExported = 0;
    protected int numRelationshipsExported = 0;

    private int n = 1;
    private int r = 1;
    
    private CSVWriter refNodeCsvWriter;
    private CSVWriter refSubjectCsvWriter;
    private CSVWriter refEntityCsvWriter;
    private CSVWriter subjectNodeCsvWriter;
    private CSVWriter subjectSubjectRelsCsvWriter;
    private CSVWriter permissionRelsCsvWriter;
    private CSVWriter entityNodeCsvWriter;
    private CSVWriter entityRelsCsvWriter;
    private Set<CSVWriter> writers = new HashSet<CSVWriter>();
    
    private List<EntityAttribute> attrs;
    private Map<String,Long> userIdBySubjectKey = new HashMap<String,Long>();

    public Neo4jCSVExportDao(Logger _logger) {
        super(_logger);
        this.largeOp = new LargeOperations(this);
    }
    
    public void dropDatabase() throws DaoException {
        FileUtil.deleteDirectory(outputDir);
    }
    
    public void loadAllEntities() throws DaoException {

        try {
            FileUtil.ensureDirExists(outputDir);
            
            this.refNodeCsvWriter = getNextNodeWriter();
            this.refSubjectCsvWriter = getNextRelsWriter();
            this.refEntityCsvWriter = getNextRelsWriter();
            
            this.subjectNodeCsvWriter = getNextNodeWriter();
            this.subjectSubjectRelsCsvWriter = getNextRelsWriter();
            this.permissionRelsCsvWriter = getNextRelsWriter();
            
            this.entityNodeCsvWriter = getNextNodeWriter();
            this.entityRelsCsvWriter = getNextRelsWriter();

            writeCols(refNodeCsvWriter, "name:string:reference");
            writeCols(refNodeCsvWriter, REFNODE_COMMON_ROOTS);
            writeCols(refNodeCsvWriter, REFNODE_SUBJECTS);
            writeCols(refSubjectCsvWriter, "name:string:reference", "subject_id:long:subject", "type:string");
            writeCols(refEntityCsvWriter, "name:string:reference", "entity_id:long:entity", "type:string");
            writeCols(subjectNodeCsvWriter, "subject_id:long:subject", "type:string", "subject_key:string:subject", "fullName:string", "email:string");
            writeCols(permissionRelsCsvWriter, "subject_id:long:subject", "entity_id:long:entity", "type:string", "permissions:string");
            writeCols(subjectSubjectRelsCsvWriter, "subject_id:long:subject", "subject_id:long:subject", "type:string");
            
            this.attrs = getAllEntityAttributes();
            
            List<String> nodeColNames = new ArrayList<String>();
            nodeColNames.add("entity_id:long:entity");
            nodeColNames.add("entity_type:string:entity");
            nodeColNames.add("name:string");
            nodeColNames.add("creation_date:string");
            nodeColNames.add("updated_date:string");
            nodeColNames.add("owner_key:string");
            for(EntityAttribute attr : attrs) {
                String attrName = getFormattedFieldName(attr.getName());
                if (attr.getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) {
                    attrName = "child";
                }
                else if (attr.getName().equals(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID)) {
                    attrName = "annotation_ontology_key_entity";
                }
                else if (attr.getName().equals(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID)) {
                    attrName = "annotation_ontology_value_entity";
                }
                nodeColNames.add(attrName);
            }
            writeCols(entityNodeCsvWriter, nodeColNames);
            writeCols(entityRelsCsvWriter, "entity_id:long:entity", "entity_id:long:entity", "type:string", "entity_data_id:long", "creation_date:string", "updated_date:string", "owner_key:string", "order_index:long");
            
            log.info("Clearing Neo4j id cache...");
            largeOp.clearCache(LargeOperations.NEO4J_MAP);

            log.info("Exporting database into: " + outputDir);

            loadSubjects();
            log.info("Completed exporting subjects.");

            loadSubjectRelationships();
            log.info("Completed exporting subject relationships.");
            
            loadEntities();
            log.info("Completed exporting entities.");
            
            loadRelationships();
            log.info("Completed exporting relationships.");
            
            loadPermissions();
            log.info("Completed exporting permissions.");
            
            
            log.info("numNodesExported: "+numNodesExported);
            log.info("numRelationshipsExported: "+numRelationshipsExported);
            log.info("numNodeColNames: "+nodeColNames.size());
            log.info("numRelColNames: 5");
            
            log.info("Neo4j CSV export complete.");

            for(CSVWriter writer : writers) {
                writer.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error creating output directory: "+outputDir,e);
        }     
        
    }

    public void loadSubjects() throws DaoException {
                
        log.info("Exporting all subjects");
                
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            
            StringBuffer sql = new StringBuffer();
            sql.append("select id,discriminator,subject_key,name,fullName,email from subject ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
                
            rs = stmt.executeQuery();
            while (rs.next()) {
                Long subjectId = rs.getBigDecimal(1).longValue();
                String type = rs.getString(2);
                String subjectKey = rs.getString(3);
                String name = rs.getString(4);
                String fullName = rs.getString(5);
                String email = rs.getString(6);
                userIdBySubjectKey.put(subjectKey, subjectId);
                
                loadSubject(subjectId, type, subjectKey, name, fullName, email);
                writeCols(refSubjectCsvWriter, REFNODE_SUBJECTS, subjectId.toString(), "SUBJECT");
            }
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }

    public void loadSubjectRelationships() throws DaoException {
        
        log.info("Exporting all subject relationships");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            
            StringBuffer sql = new StringBuffer();
            sql.append("select id,group_subject_id,user_subject_id,relationship_type from subject_relationship ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
                
            rs = stmt.executeQuery();
            while (rs.next()) {
                Long relId = rs.getBigDecimal(1).longValue();
                Long groupId = rs.getBigDecimal(2).longValue();
                Long userId = rs.getBigDecimal(3).longValue();
                String type = rs.getString(4);
                writeCols(subjectSubjectRelsCsvWriter, userId.toString(), groupId.toString(), type.toUpperCase());
            }
            
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }

    public void loadEntities() throws DaoException {
                
        log.info("Exporting all entities and attributes");
                
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            
            StringBuffer sql = new StringBuffer();
            sql.append("select e.id, e.name, e.creation_date, e.updated_date, e.entity_type, e.owner_key, ed.entity_att, ed.value ");
            sql.append("from entity e ");
            sql.append("left outer join entityData ed on e.id=ed.parent_entity_id and ed.value is not null ");
//            sql.append("where e.owner_key = 'user:asoy' ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);

            SimpleEntity simpleEntity = null;
                
            rs = stmt.executeQuery();
            while (rs.next()) {
                Long entityId = rs.getBigDecimal(1).longValue();
                
                if (simpleEntity==null || !entityId.equals(simpleEntity.id)) {
                    if (simpleEntity!=null) {
                        loadEntity(simpleEntity);
                    }
                    simpleEntity = new SimpleEntity();
                    simpleEntity.id = entityId;
                    simpleEntity.name = rs.getString(2);
                    simpleEntity.creationDate = rs.getDate(3);
                    simpleEntity.updatedDate = rs.getDate(4);
                    simpleEntity.entityTypeName = rs.getString(5);
                    simpleEntity.owner = rs.getString(6);
                }
                
                String key = rs.getString(7);
                String value = rs.getString(8);
                if (key!=null && value!=null) {
                    simpleEntity.attributes.put(key, value);
                }
            }

            if (simpleEntity!=null) {
                loadEntity(simpleEntity);
            }
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }

    /**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void loadRelationships() throws DaoException {

        log.info("Exporting all relationships");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            
            StringBuffer sql = new StringBuffer();
            sql.append("select ed.parent_entity_id, ed.child_entity_id, ea.name, ed.id, ed.owner_key, ed.creation_date, ed.updated_date, ed.orderIndex ");
            sql.append("from entityData ed ");
            sql.append("join entityAttribute ea on ed.entity_att_id = ea.id ");
            sql.append("where ed.parent_entity_id is not null and ed.child_entity_id is not null ");
//            sql.append("and ed.owner_key = 'user:asoy' ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            
            rs = stmt.executeQuery();
            while (rs.next()) {
                Long parentId = rs.getBigDecimal(1).longValue();
                Long childId = rs.getBigDecimal(2).longValue();
                String type = rs.getString(3);
                Long edId = rs.getBigDecimal(4).longValue();
                String ownerKey = rs.getString(5);
                Date creationDate = rs.getDate(6);
                Date updatedDate = rs.getDate(7);
                Long orderIndex = rs.getLong(8);
                loadRelationship(parentId, childId, type, edId, creationDate, updatedDate, ownerKey, orderIndex);
            }
            
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();   
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }

    /**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void loadPermissions() throws DaoException {

        log.info("Exporting all relationships");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getJdbcConnection();
            
            StringBuffer sql = new StringBuffer();
            sql.append("select id,entity_id,subject_key,permissions from entity_actor_permission ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            
            rs = stmt.executeQuery();
            while (rs.next()) {
                Long permissionId = rs.getBigDecimal(1).longValue();
                Long entityId = rs.getBigDecimal(2).longValue();
                String subjectKey = rs.getString(3);
                String permissions = rs.getString(4);
                loadPermission(permissionId, entityId, subjectKey, permissions);
            }
            
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();   
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }
    
    private void loadSubject(Long subjectId, String type, String subjectKey, String name, String fullName, String email) {
        writeCols(subjectNodeCsvWriter, subjectId.toString(), getFormattedTypeName(type), subjectKey, name, fullName, email);
        numNodesExported++;
    }
    
    private void loadEntity(SimpleEntity simpleEntity) throws Exception {

        List<String> values = new ArrayList<String>();
        values.add(simpleEntity.id.toString());
        values.add(getFormattedTypeName(simpleEntity.entityTypeName));
        values.add(simpleEntity.name);
        values.add(getFormattedDateTime(simpleEntity.creationDate));
        values.add(getFormattedDateTime(simpleEntity.updatedDate));
        values.add(simpleEntity.owner);
        for (EntityAttribute attr : attrs) {
            String value = simpleEntity.attributes.get(attr.getName());
            if (attr.getName().equals(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID)) {
                if (value!=null) {
                    try {
                        Long targetId = new Long(value);
                        loadRelationship(simpleEntity.id, targetId, "ANNOTATION_TARGET", null, simpleEntity.creationDate, simpleEntity.updatedDate, simpleEntity.owner, null);    
                    }
                    catch (NumberFormatException e) {
                        log.warn("Could not parse target id: "+value,e);
                    }
                }
            }
            else {
                values.add(getFormattedValue(value));
            }
        }
        
        writeCols(entityNodeCsvWriter, values);
        numNodesExported++;
        
        if (simpleEntity.attributes.containsKey(EntityConstants.ATTRIBUTE_COMMON_ROOT)) {
            writeCols(refEntityCsvWriter, REFNODE_COMMON_ROOTS, simpleEntity.id.toString(), "COMMON_ROOT");
        }
    }
    
    private void loadRelationship(Long parentId, Long childId, String type, Long entityDataId, Date creationDate, Date updatedDate, String ownerKey, Long orderIndex) throws Exception {

        String relType = getFormattedTypeName(type);
        if ("ENTITY".equals(relType)) {
            relType = "CHILD";
        }
        
        writeCols(entityRelsCsvWriter, 
                parentId.toString(), 
                childId.toString(), 
                relType, 
                entityDataId==null?"":entityDataId.toString(), 
                getFormattedDateTime(creationDate), 
                getFormattedDateTime(updatedDate), 
                ownerKey, 
                orderIndex==null?"":orderIndex.toString());
        numRelationshipsExported++;
    }

    private void loadPermission(Long permissionId, Long entityId, String subjectKey, String permissions) throws Exception {
        
        Long subjectId = userIdBySubjectKey.get(subjectKey);
        if (subjectId==null) {
            log.warn("No subject found for id="+subjectId);
            return;
        }
        
        writeCols(permissionRelsCsvWriter, subjectId.toString(), entityId.toString(), "PERMISSION", permissions);
        numRelationshipsExported++;
    }

    private CSVWriter getWriter(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(new File(outputDir, filename), false);
        CSVWriter writer = new CSVWriter(fileWriter, '\t', CSVWriter.NO_QUOTE_CHARACTER, '\\', "\n");
        writers.add(writer);
        return writer;
    }
    
    private CSVWriter getNextNodeWriter() throws IOException {
        return getWriter("nodes"+(n++)+".csv");
    }
    
    private CSVWriter getNextRelsWriter() throws IOException {
        return getWriter("rels"+(r++)+".csv");
    }
    
    private void writeCols(CSVWriter writer, String... cols) {
        writer.writeNext(cols);
    }
    
    private void writeCols(CSVWriter writer, List<String> cols) {
        writer.writeNext(cols.toArray(new String[cols.size()]));
    }
    
    
    /**
     * Sanitize a value for export.
     * @param value
     * @return
     */
    private String getFormattedValue(String value) {
        if (value==null) return "";
        return value.replaceAll("\n","[NEWLINE]").replaceAll("\t","[TAB]");
    }
    
    /**
     * Format the given date according to the standard ISO 8601 format.
     * @param date
     * @return
     */
    private String getFormattedDateTime(Date date) {
        return date==null?null:df.format(date);
    }
    
    /**
     * Format the given name in lowercase, with underscores instead of spaces.
     * For example, "Channel Specification" -> "channel_specification"
     * 
     * @param name
     * @return
     */
    private String getFormattedFieldName(String name) {
        return name.toLowerCase().replaceAll("\\W+", "_");
    }

    /**
     * Format the given name with underscores instead of spaces.
     * For example, "Channel Specification" -> "CHANNEL_SPECIFICATION"
     * 
     * @param name
     * @return
     */
    private String getFormattedTypeName(String name) {
        return name.toUpperCase().replaceAll("\\W+", "_");
    }
    
    private class SimpleEntity {
        private Long id;
        private String name;
        private String owner;
        private String entityTypeName;
        private Date creationDate;
        private Date updatedDate;
        private final Map<String, String> attributes = new HashMap<String, String>();
    }
    
}
