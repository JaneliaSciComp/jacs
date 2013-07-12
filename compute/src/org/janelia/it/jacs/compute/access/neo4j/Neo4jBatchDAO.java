package org.janelia.it.jacs.compute.access.neo4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.kernel.DefaultFileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

/**
 * Batch data insertion to the Neo4j data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jBatchDAO extends AnnotationDAO {

    private static final String LABEL_COMMON_ROOT = "CommonRoot";
    private static final String LABEL_ENTITY = "Entity";
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    private String loadDatabaseDir = "/home/rokickik/dev/neo4j-community-2.0.0-M03/data/load.db";
    private FileSystemAbstraction fileSystem;

    protected LargeOperations largeOp;
    protected BatchInserter inserter;
    protected Label commonRootLabel;
    protected Label entityLabel;

    protected int numNodesAdded = 0;
    protected int numRelationshipsAdded = 0;
    protected int numAnnotationsAdded = 0;
    protected int numOntologiesAdded = 0;

    private RelationshipType target_rel = DynamicRelationshipType.withName("target");
    private RelationshipType key_term_rel = DynamicRelationshipType.withName("key_term");
    private RelationshipType value_term_rel = DynamicRelationshipType.withName("value_term");
    
    public Neo4jBatchDAO(Logger _logger) {
        super(_logger);
        this.fileSystem = new DefaultFileSystemAbstraction();
        this.largeOp = new LargeOperations(this);
    }

    public void loadAllEntities() throws DaoException {

        _logger.info("Clearing Neo4j id cache...");
        largeOp.clearCache(LargeOperations.NEO4J_MAP);

        _logger.info("Loading new database into: " + loadDatabaseDir);

        this.inserter = BatchInserters.inserter(loadDatabaseDir, fileSystem);
        this.commonRootLabel = DynamicLabel.label(LABEL_COMMON_ROOT);
        this.entityLabel = DynamicLabel.label(LABEL_ENTITY);

        List<Entity> roots = getUserEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_COMMON_ROOT, EntityConstants.ATTRIBUTE_COMMON_ROOT);
        _logger.info("Found "+roots.size()+" common roots");

        for(Entity root : roots) {
            _logger.info("Loading common root "+root.getName());
            EntityData rootEd = new EntityData();
            rootEd.setChildEntity(root);
            loadDescendants(null, rootEd);
        }

        _logger.info("Completed loading " + numNodesAdded + " nodes and " + numRelationshipsAdded + " relationships.");
        
        List<Entity> ontologyRoots = getEntitiesByTypeName(null, EntityConstants.TYPE_ONTOLOGY_ROOT);
        for(Entity ontologyRoot : ontologyRoots) {
            EntityData rootEd = new EntityData();
            rootEd.setChildEntity(ontologyRoot);
            loadDescendants(null, rootEd);
            numOntologiesAdded++;
            
            // Free memory
            Session session = getCurrentSession();
            session.evict(ontologyRoot);
            ontologyRoot.setEntityData(null);
        }

        _logger.info("Completed loading " + numOntologiesAdded + " ontologies.");

        
        List<Entity> annotations = getEntitiesByTypeName(null, EntityConstants.TYPE_ANNOTATION);
        for(Entity annotation : annotations) {
            loadAnnotation(annotation);
            
            // Free memory
            Session session = getCurrentSession();
            session.evict(annotation);
            annotation.setEntityData(null);
        }
        
        _logger.info("Completed loading " + numAnnotationsAdded + " annotations.");

        inserter.shutdown();
        
        // Add indexes
        // Because inserter.createDeferredSchemaIndex does not work, we need to connect as an embedded database and 
        // then create the indexes.

        _logger.info("Creating indexes...");
        
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(loadDatabaseDir);
        
        Schema schema = graphDb.schema();
        Transaction tx = graphDb.beginTx();
        try {
            schema.indexFor(commonRootLabel).on("entity_id").create();
            schema.indexFor(commonRootLabel).on("name").create();
            schema.indexFor(commonRootLabel).on("owner_key").create();
            schema.indexFor(entityLabel).on("entity_id").create();
            schema.indexFor(entityLabel).on("name").create();
            schema.indexFor(entityLabel).on("owner_key").create();
            tx.success();
        } 
        finally {
            tx.finish();
        }
        
        _logger.info("Awaiting index population...");
        schema.awaitIndexesOnline(24, TimeUnit.HOURS);

        graphDb.shutdown();
        
        _logger.info("Index population complete. Neo4j is ready.");
        
    }

    private Long loadDescendants(Long parentNeoId, EntityData ed) throws DaoException {

        Entity entity = ed.getChildEntity();
        if (entity==null || entity.getEntityData()==null) return null;
        
        try {
            populateChildren(entity);
        }
        catch (Exception e) {
            throw new DaoException("Error populating entity "+entity.getId(), e);
        }
        
        Long neoId = loadEntityData(parentNeoId, ed);    

        for (EntityData childEd : entity.getEntityData()) {
            Entity child = childEd.getChildEntity();
            if (child != null) {
                loadDescendants(neoId, childEd);
            }
        }
        
        // Free memory
        Session session = getCurrentSession();
        session.evict(entity);
        entity.setEntityData(null);
        
        return neoId;
    }

    private Long loadEntityData(Long parentNeoId, EntityData ed) throws DaoException {

        Entity entity = ed.getChildEntity();
        if (entity == null) return null;

        Long neoId = (Long) largeOp.getValue(LargeOperations.NEO4J_MAP, entity.getId());
        if (neoId != null) {
            loadRelationship(parentNeoId, neoId, ed);
            return neoId;
        }

        _logger.info("loadEntity " + entity.getId() + " (with parentNeoId=" + parentNeoId + ")");

        try {
            Map<String, Object> properties = getEntityProperties(entity);
            neoId = inserter.createNode(properties);
            numNodesAdded++;
            
            Label entityTypeLabel = DynamicLabel.label(getFormattedLabelName(entity.getEntityType().getName()));

            if (parentNeoId != null) {
                inserter.setNodeLabels(neoId, entityLabel, entityTypeLabel);
                loadRelationship(parentNeoId, neoId, ed);
            } 
            else {
                inserter.setNodeLabels(neoId, entityLabel, entityTypeLabel, commonRootLabel);
            }
            
            largeOp.putValue(LargeOperations.NEO4J_MAP, entity.getId(), neoId);
            return neoId;
            
        }
        catch (Exception e) {
            throw new DaoException("Error indexing entity "+entity.getId(), e);
        }
    }
    
    private void loadRelationship(Long parentNeoId, Long childNeoId, EntityData ed) {
        RelationshipType childRel = DynamicRelationshipType.withName(getFormattedFieldName(ed.getEntityAttribute().getName()));
        Map<String, Object> properties = new HashMap<String, Object>();
        addIfNotNull(properties, "entity_data_id", ed.getId());
        addIfNotNull(properties, "type", ed.getEntityAttribute().getName());
        addIfNotNull(properties, "creation_date", getFormattedDateTime(ed.getCreationDate()));
        addIfNotNull(properties, "updated_date", getFormattedDateTime(ed.getUpdatedDate()));
        addIfNotNull(properties, "order_index", ed.getOrderIndex());
        addIfNotNull(properties, "owner_key", ed.getOwnerKey());
        inserter.createRelationship(parentNeoId, childNeoId, childRel, properties);
        numRelationshipsAdded++;
    }
    
    private void loadAnnotation(Entity annotation) {
        
        String targetId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        Entity key = annotation.getChildByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
        Entity value = annotation.getChildByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
        Long targetNeoId = (Long) largeOp.getValue(LargeOperations.NEO4J_MAP, targetId);

        _logger.info("loadAnnotation " + annotation.getId() + " (with targetId=" + targetNeoId + ")");
        
        Map<String, Object> properties = getEntityProperties(annotation);
        Long annotationNeoId = inserter.createNode(properties);
        numAnnotationsAdded++;
        
        Label entityTypeLabel = DynamicLabel.label(getFormattedLabelName(annotation.getEntityType().getName()));
        inserter.setNodeLabels(annotationNeoId, entityLabel, entityTypeLabel);

        inserter.createRelationship(annotationNeoId, targetNeoId, target_rel, new HashMap<String, Object>());
        
        if (key!=null) {
            Long keyNeoId = (Long) largeOp.getValue(LargeOperations.NEO4J_MAP, key.getId());
            inserter.createRelationship(annotationNeoId, keyNeoId, key_term_rel, new HashMap<String, Object>());
        }
        
        if (value!=null) {
            Long valueNeoId = (Long) largeOp.getValue(LargeOperations.NEO4J_MAP, value.getId());
            inserter.createRelationship(annotationNeoId, valueNeoId, value_term_rel, new HashMap<String, Object>());
        }
    }

    private Map<String, Object> getEntityProperties(Entity entity) {
        Map<String, Object> properties = new HashMap<String, Object>();
        addIfNotNull(properties, "entity_id", entity.getId());
        addIfNotNull(properties, "name", entity.getName());
        addIfNotNull(properties, "type", entity.getEntityType().getName());
        addIfNotNull(properties, "creation_date", getFormattedDateTime(entity.getCreationDate()));
        addIfNotNull(properties, "updated_date", getFormattedDateTime(entity.getUpdatedDate()));
        addIfNotNull(properties, "owner_key", entity.getOwnerKey());
        for (EntityData childEd : entity.getEntityData()) {
            if (childEd.getValue() != null) {
                addIfNotNull(properties, getFormattedFieldName(childEd.getEntityAttribute().getName()), childEd.getValue());
            }
        }
        return properties;
    }
    
    public void dropDatabase() throws DaoException {
        _logger.info("Deleting existing database at " + loadDatabaseDir);
        FileUtil.deleteDirectory(loadDatabaseDir);
    }
    
    /**
     * Add the key/value pair to the given map if the key and value are not null.
     */
    public static void addIfNotNull(Map<String, Object> map, String key, Object value) {
            if (key==null || value==null) return;
            map.put(key, value);
    }

    /**
     * Format the given date according to the standard ISO 8601 format.
     * @param date
     * @return
     */
    public static String getFormattedDateTime(Date date) {
        return date==null?null:df.format(date);
    }
    
    /**
     * Format the given name in lowercase, with underscores instead of spaces.
     * For example, "Tiling Pattern" -> "tiling_pattern"
     * 
     * @param name
     * @return
     */
    public static String getFormattedFieldName(String name) {
        return name.toLowerCase().replaceAll("\\W+", "_");
    }

    /**
     * Format the given name in title case without spaces. For example,
     * "Tiling Pattern" -> "TilingPattern"
     * 
     * @param name
     * @return
     */
    public static String getFormattedLabelName(String name) {
        return name.replaceAll("\\s+", "");
    }
}
