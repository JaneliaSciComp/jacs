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

    private String loadDatabaseDir = "/home/rokickik/dev/neo4j-community-2.0.0-M03/data/graph.db";
    private FileSystemAbstraction fileSystem;

    protected LargeOperations largeOp;
    protected BatchInserter inserter;
    protected Label commonRootLabel;
    protected Label entityLabel;

    protected int numNodesAdded = 0;
    protected int numRelationshipsAdded = 0;

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
        
//        Entity root = getEntityById(1882947953992138923L);
//        populateDescendants(null, root);
//        EntityData rootEd = new EntityData();
//        rootEd.setChildEntity(root);
//        Long rootNeoId = loadDescendants(null, rootEd);

        _logger.info("Completed loading " + numNodesAdded + " nodes and " + numRelationshipsAdded
                + " relationships into the Neo4j database.");

        inserter.shutdown();
        
        // Add indexes
        // Because inserter.createDeferredSchemaIndex does not work, we need to connect as an embedded database and 
        // then create the indexes.

        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(loadDatabaseDir);
        
        Schema schema = graphDb.schema();
        Transaction tx = graphDb.beginTx();
        try {
            schema.indexFor(commonRootLabel).on("entity_id").create();
            schema.indexFor(commonRootLabel).on("name").create();
            schema.indexFor(entityLabel).on("entity_id").create();
            schema.indexFor(entityLabel).on("name").create();
            tx.success();
        } 
        finally {
            tx.finish();
        }
        
        _logger.info("Awaiting index population...");
        schema.awaitIndexesOnline(10, TimeUnit.MINUTES);

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
        
        Long neoId = loadEntity(parentNeoId, ed);    
        

        for (EntityData childEd : entity.getEntityData()) {
            Entity child = childEd.getChildEntity();
            if (child != null) {
                loadDescendants(neoId, childEd);
            }
        }
        
        Session session = getCurrentSession();
        session.evict(entity);
        entity.setEntityData(null);
        
        return neoId;
    }

    private Long loadEntity(Long parentNeoId, EntityData ed) throws DaoException {

        Entity entity = ed.getChildEntity();
        if (entity == null) return null;

        Long neoId = (Long) largeOp.getValue(LargeOperations.NEO4J_MAP, entity.getId());
        if (neoId != null) return neoId;

        _logger.info("loadEntity " + entity.getId() + " (with parentNeoId=" + parentNeoId + ")");

        try {
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
    
            neoId = inserter.createNode(properties);
            numNodesAdded++;
    
            Label entityTypeLabel = DynamicLabel.label(getFormattedLabelName(entity.getEntityType().getName()));
    
            if (parentNeoId != null) {
                inserter.setNodeLabels(neoId, entityLabel, entityTypeLabel);
                RelationshipType childRel = DynamicRelationshipType.withName(getFormattedFieldName(ed.getEntityAttribute()
                        .getName()));
                properties = new HashMap<String, Object>();
                addIfNotNull(properties, "creation_date", getFormattedDateTime(ed.getCreationDate()));
                addIfNotNull(properties, "updated_date", getFormattedDateTime(ed.getUpdatedDate()));
                addIfNotNull(properties, "owner_key", ed.getOwnerKey());
                inserter.createRelationship(parentNeoId, neoId, childRel, properties);
                numRelationshipsAdded++;
            } else {
                inserter.setNodeLabels(neoId, entityLabel, entityTypeLabel, commonRootLabel);
            }
    
            largeOp.putValue(LargeOperations.NEO4J_MAP, entity.getId(), neoId);
            return neoId;
            
        }
        catch (Exception e) {
            throw new DaoException("Error indexing entity "+entity.getId(), e);
        }
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
