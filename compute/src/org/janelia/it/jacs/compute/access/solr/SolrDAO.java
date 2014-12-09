package org.janelia.it.jacs.compute.access.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.CoreAdminParams;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.janelia.it.jacs.shared.solr.SolrUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Data access to the SOLR indexes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrDAO extends AnnotationDAO {
	
	protected static final int MAX_ID_LIST_SIZE = 200;
	
    protected static final int SOLR_LOADER_BATCH_SIZE = 20000;
    protected static final int SOLR_LOADER_COMMIT_SIZE = 200000;
	protected static final int SOLR_LOADER_QUEUE_SIZE = 100;
	protected static final int SOLR_LOADER_THREAD_COUNT = 2;
	
	protected static final String SOLR_SERVER_URL = SystemConfigurationProperties.getString("Solr.ServerURL");
	protected static final String SOLR_MAIN_CORE = SystemConfigurationProperties.getString("Solr.MainCore");
	protected static final String SOLR_BUILD_CORE = SystemConfigurationProperties.getString("Solr.BuildCore");
	
	protected final boolean useBuildCore;
	protected final boolean streamingUpdates;
	
	protected SolrServer solr;
	protected LargeOperations largeOp;    
    protected Map<String, SageTerm> sageVocab;
    protected Set<SageTerm> usedSageVocab;
    
    /**
     * Create a SolrDAO, specifying if the DAO will be used for building an index. 
     * @param log
     * @param build
     */
    public SolrDAO(Logger log, boolean useBuildCore, boolean streamingUpdates) {
        super(log);
        this.useBuildCore = useBuildCore;
        this.streamingUpdates = streamingUpdates;
    }

    private void init() throws DaoException {
    	if (solr==null) {
            try {
            	if (streamingUpdates) {
            		solr = new StreamingUpdateSolrServer(SOLR_SERVER_URL+(useBuildCore?SOLR_BUILD_CORE:SOLR_MAIN_CORE), SOLR_LOADER_QUEUE_SIZE, SOLR_LOADER_THREAD_COUNT);	
            	}
            	else {
            		solr = new CommonsHttpSolrServer(SOLR_SERVER_URL+(useBuildCore?SOLR_BUILD_CORE:SOLR_MAIN_CORE));
            	}
            	solr.ping();
            }
            catch (MalformedURLException e) {
            	throw new RuntimeException("Illegal Solr.ServerURL value in system properties: "+SOLR_SERVER_URL);
            }
            catch (IOException e) {
            	throw new DaoException("Problem pinging SOLR at: "+SOLR_SERVER_URL);
            }
            catch (SolrServerException e) {
            	throw new DaoException("Problem pinging SOLR at: "+SOLR_SERVER_URL);
            }
    	}
    }
        
    public void indexAllEntities(Map<String, SageTerm> sageVocab) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("indexAllEntities(sageVocab.size="+sageVocab.size()+")");
        }
    	
    	if (!useBuildCore || !streamingUpdates) {
    		throw new IllegalStateException("indexAllEntities called on SolrDAO which has useBuildCore=false or streamingUpdates=false");
    	}
    	
    	this.sageVocab = sageVocab;
    	this.usedSageVocab = new HashSet<SageTerm>();

    	log.info("Building disk-based entity maps");
    	this.largeOp = new LargeOperations(this);
    	largeOp.buildAncestorMap();
    	largeOp.buildAnnotationMap();
    	largeOp.buildSageImagePropMap();
    	
    	log.info("Getting entities");
    	
    	Map<Long,SimpleEntity> entityMap = new HashMap<Long,SimpleEntity>();
        int i = 0;
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, e.name, e.creation_date, e.updated_date, e.entity_type, e.owner_key, ed.entity_att, ed.value, ed.child_entity_id, p.subject_key ");
	        sql.append("from entity e ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entity_actor_permission p on p.entity_id = e.id ");
	        sql.append("where e.entity_type != ? ");
	        
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	        stmt.setString(1, EntityConstants.TYPE_ANNOTATION);
	        
			rs = stmt.executeQuery();
	    	log.info("    Processing results");
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				SimpleEntity entity = entityMap.get(entityId);
				if (entity==null) {
					if (i>0) {
		            	if (i%SOLR_LOADER_BATCH_SIZE==0) {
		                    List<SolrInputDocument> docs = createEntityDocs(entityMap.values());
		                    entityMap.clear();
		            		log.info("    Adding "+docs.size()+" docs (i="+i+")");
		            		index(docs);
		            	}
	            		if (i%SOLR_LOADER_COMMIT_SIZE==0) {
	            	    	log.info("    Committing SOLR index");
	            			commit();
	            		}
					}
					
					i++;
					entity = new SimpleEntity();
					entityMap.put(entityId, entity);
					entity.setId(entityId);
					entity.setName(rs.getString(2));
					entity.setCreationDate(rs.getDate(3));
					entity.setUpdatedDate(rs.getDate(4));
					entity.setEntityTypeName(rs.getString(5));
					String ownerKey = rs.getString(6);
					entity.setOwnerKey(ownerKey);
					entity.getSubjectKeys().add(ownerKey);
				}

				String key = rs.getString(7);
				String value = rs.getString(8);
				BigDecimal childIdBD = rs.getBigDecimal(9);
				String subjectKey = rs.getString(10);
				
				if (key!=null && value!=null) {
					entity.getAttributes().add(new KeyValuePair(key, value));
				}
				
				if (childIdBD != null) {
					entity.getChildIds().add(childIdBD.longValue());
				}
				
				if (subjectKey!=null) {
					entity.getSubjectKeys().add(subjectKey);
				}
			}

        	if (!entityMap.isEmpty()) {
                List<SolrInputDocument> docs = createEntityDocs(entityMap.values());
        		log.info("    Adding "+docs.size()+" docs (i="+i+")");
        		index(docs);
        	}
    	}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
        finally {
        	try {
        		if (rs!=null) rs.close();
        		if (stmt!=null) stmt.close();
        		if (conn!=null) conn.close();
        	}
            catch (Throwable e) {
        		log.warn("Error closing JDBC connection. Ignoring error.",e);
            }
        }

        try {
        	log.info("Indexing Sage vocabularies");
        	
        	index(createSageDocs(usedSageVocab));
        	
    		commit();
    		optimize();
        	log.info("Completed indexing "+i+" entities");
            swapBuildCore();
            log.info("Build core swapped to main core. The new index is now live.");
        }
        catch (Exception e) {
        	throw new DaoException(e);
        }
    }

    public void updateIndex(Entity entity) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateIndex(entity="+entity+")");
        }
        
    	if (entity==null) return;
    	List<Entity> entities = new ArrayList<Entity>();
    	entities.add(entity);
    	updateIndex(entities);
    }
    
    public void updateIndex(List<Entity> entities) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateIndex(entities.size="+entities.size()+")");
        }
        
    	List<Long> entityIds = new ArrayList<Long>();
    	for(Entity entity : entities) {
    		entityIds.add(entity.getId());
    	}
    	
    	// Get all annotations
    	
    	Map<Long,Set<SimpleAnnotation>> annotationMap = new HashMap<Long,Set<SimpleAnnotation>>();
		for(Entity annotationEntity : getAnnotationsByEntityId(null, entityIds)) {
			String key = annotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
			String value = annotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
			String ownerKey = annotationEntity.getOwnerKey();
			String entityIdStr = annotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
			
			Long entityId = null;
			try {
				entityId = new Long(entityIdStr);
			}
			catch (NumberFormatException e) {
				log.warn("Cannot parse annotation target id for annotation="+annotationEntity.getId());
			}
			
			Set<SimpleAnnotation> annotations = annotationMap.get(entityId);
			if (annotations == null) {
				annotations = new HashSet<SimpleAnnotation>();	
				annotationMap.put(entityId, annotations);
			}
			
			annotations.add(new SimpleAnnotation(annotationEntity.getName(), key, value, ownerKey));
		}
		
		// Get all Solr documents
    	Map<Long,SolrDocument> solrDocMap = search(entityIds);

    	// Create updated Solr documents
    	
    	List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>();
    	for(Entity entity : entities) {
    		SolrInputDocument inputDoc = null;
    		SolrDocument existingDoc = solrDocMap.get(entity.getId());
    		Set<SimpleAnnotation> annotations = annotationMap.get(entity.getId());
    		
        	if (existingDoc!=null) {
        		inputDoc = createDoc(existingDoc, simpleEntityFromEntity(entity), annotations, null, null);
        	}
        	else {
        		inputDoc = createDoc(null, simpleEntityFromEntity(entity), annotations, null, null);
        	}
        	
        	inputDocs.add(inputDoc);

        	log.info("Updating index for "+entity.getName()+" (id="+entity.getId()+") ");
    	}
    
    	// Index the entire batch

    	index(inputDocs);
    	commit();
    }
    
    protected void index(List<SolrInputDocument> docs) throws DaoException {
    	init();
    	if (docs==null||docs.isEmpty()) return;
    	try {
	    	solr.add(docs);
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }

    protected void swapBuildCore() throws Exception {
    	CoreAdminRequest car = new CoreAdminRequest();
    	car.setCoreName(SOLR_BUILD_CORE);
    	car.setOtherCoreName(SOLR_MAIN_CORE);
    	car.setAction(CoreAdminParams.CoreAdminAction.SWAP);
    	car.process(new CommonsHttpSolrServer(SOLR_SERVER_URL));
    }
    
    protected List<SolrInputDocument> createSageDocs(Collection<SageTerm> terms) {
    	
    	String dt = SolrDocTypeEnum.SAGE_TERM.toString();
    	
    	int id = 0;
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        for(SageTerm term : terms) {
        	SolrInputDocument doc = new SolrInputDocument();
        	doc.addField("id", dt+"_"+id, 1.0f);
        	doc.addField("doc_type", dt, 1.0f);
        	doc.addField("name", term.getName(), 1.0f);
        	doc.addField("data_type_t", term.getDataType(), 1.0f);
        	doc.addField("definition_t", term.getDefinition(), 1.0f);
        	doc.addField("display_name_t", term.getDisplayName(), 1.0f);        	
        	docs.add(doc);
        	id++;
        }
        return docs;
    }
    
    private List<SolrInputDocument> createEntityDocs(Collection<SimpleEntity> entities) {
    	
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        for(SimpleEntity se : entities) {
        	
        	Set<SimpleAnnotation> annotations = (Set<SimpleAnnotation>)largeOp.getValue(LargeOperations.ANNOTATION_MAP, se.getId());

        	AncestorSet ancestorSet = (AncestorSet)largeOp.getValue(LargeOperations.ANCESTOR_MAP, se.getId());
        	Set<Long> ancestors = ancestorSet==null ? null : ancestorSet.getAncestors(); 
        	
        	Map<String,Object> sageProps = (Map<String,Object>)largeOp.getValue(LargeOperations.SAGE_IMAGEPROP_MAP, se.getId());
        	
        	SolrInputDocument doc = createDoc(se, annotations, ancestors, sageProps);
        	docs.add(doc);
        }
        return docs;
    }
    
    private SolrInputDocument createDoc(SimpleEntity entity, Set<SimpleAnnotation> annotations, Set<Long> ancestorIds, Map<String,Object> sageProps) {
    	return createDoc(null, entity, annotations, ancestorIds, sageProps);
    }
    
    private SolrInputDocument createDoc(SolrDocument existingDoc, SimpleEntity entity, Set<SimpleAnnotation> annotations, Set<Long> ancestorIds, Map<String,Object> sageProps) {

    	SolrInputDocument doc = existingDoc==null ? new SolrInputDocument() : ClientUtils.toSolrInputDocument(existingDoc);
    	
    	doc.setField("id", entity.getId(), 1.0f);
    	doc.setField("doc_type", SolrDocTypeEnum.ENTITY.toString(), 1.0f);
    	doc.setField("name", entity.getName(), 1.0f);
    	doc.setField("creation_date", entity.getCreationDate(), 0.8f);
    	doc.setField("updated_date", entity.getUpdatedDate(), 0.9f);
    	doc.setField("username", entity.getOwnerName(), 1.0f);
    	doc.setField("entity_type", entity.getEntityTypeName(), 1.0f);
    	
    	if (sageVocab!=null && sageProps!=null) {
    		if (existingDoc!=null) {
        		for(String key : sageProps.keySet()) {
    				SageTerm sageTerm = sageVocab.get(key);
    				if (sageTerm==null) {
    					log.warn("Unrecognized SAGE term: "+key);
    					continue;
    				}
    				doc.removeField(SolrUtils.getSageFieldName(sageTerm));
        		}
    		}
    		for(String key : sageProps.keySet()) {
    			Object value = sageProps.get(key);
    			if (value != null) {
    				SageTerm sageTerm = sageVocab.get(key);
    				if (sageTerm==null) {
    					log.warn("Unrecognized SAGE term: "+key);
    					continue;
    				}
    				
    				// Keep track of the terms we use, so that they can be indexed as well
    				if (!usedSageVocab.contains(sageTerm)) usedSageVocab.add(sageTerm);
    				String fieldName = SolrUtils.getSageFieldName(sageTerm);
    				doc.addField(fieldName, value, 0.9f);
    			}
    		}
    	}

		if (existingDoc!=null) {
			// This is slightly flawed in that attributes are not removed from the index if they are removed from 
			// the entity. However, in that case we can tolerate a 1-day reindexing delay.
			for(KeyValuePair kv : entity.getAttributes()) {
    			doc.removeField(SolrUtils.getDynamicFieldName(kv.getKey()));
	    	}
		}
    	for(KeyValuePair kv : entity.getAttributes()) {
    		if (kv.getValue()!=null) {
    			doc.addField(SolrUtils.getDynamicFieldName(kv.getKey()), kv.getValue(), 1.0f);	
    		}
    	}
    	
    	doc.removeField("subjects");
		for(String subjectKey : entity.getSubjectNames()) {
			doc.addField("subjects", subjectKey);
    	}
		
//    	if (!entity.getChildIds().isEmpty()) {
//    		if (existingDoc!=null) {
//    			doc.removeField("child_ids");
//    		}
//    		doc.addField("child_ids", entity.getChildIds(), 0.2f);
//    	}

		if (existingDoc!=null) {
    		for(String fieldName : new ArrayList<String>(doc.getFieldNames())) {
    			if (fieldName.endsWith("_annotations") || fieldName.endsWith("_annot")) {
    				doc.removeField(fieldName);
    			}
    		}
		}
		
    	if (annotations != null) {
    		for(SimpleAnnotation annotation : annotations) {        
    			doc.addField(annotation.getOwner()+"_annotations", annotation.getTag(), 1.0f);
//    			if (annotation.getValue()!=null) {
//    				doc.addField(annotation.getOwner()+"_"+SolrUtils.getFormattedName(annotation.getKey())+"_annot", annotation.getValue(), 1.0f);
//    			}
    		}
    	}
    	
    	if (ancestorIds != null) {
    		if (existingDoc!=null) {
    			doc.removeField("ancestor_ids");
    		}
    		doc.addField("ancestor_ids", ancestorIds, 0.2f);
    	}
    	
    	return doc;
    }

    private SimpleEntity simpleEntityFromEntity(Entity entity) {
    	SimpleEntity simpleEntity = new SimpleEntity();
    	simpleEntity.setId(entity.getId());
    	simpleEntity.setName(entity.getName());
    	simpleEntity.setCreationDate(entity.getCreationDate());
    	simpleEntity.setUpdatedDate(entity.getUpdatedDate());
    	simpleEntity.setEntityTypeName(entity.getEntityTypeName());
    	simpleEntity.setOwnerKey(entity.getOwnerKey());
    	
    	Set<Long> childrenIds = new HashSet<Long>();
    	for(EntityData ed : entity.getEntityData()) {
    		if (ed.getValue()!=null) {
    			String attr = (ed.getEntityAttrName());
    			simpleEntity.getAttributes().add(new KeyValuePair(attr, ed.getValue()));
    		}
    		else if (ed.getChildEntity()!=null) {
    			childrenIds.add(ed.getChildEntity().getId());
    			simpleEntity.getChildIds().add(ed.getChildEntity().getId());
    		}
    	}
    	
    	Set<String> subjectKeys = simpleEntity.getSubjectKeys();
    	subjectKeys.add(entity.getOwnerKey());
    	for(EntityActorPermission perm : entity.getEntityActorPermissions()) {
    		subjectKeys.add(perm.getSubjectKey());	
    	}
		
    	return simpleEntity;
    }
    
    /** 
     * Commit any outstanding changes to the index.
     * @throws DaoException
     */
    public void commit() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("commit()");
        }
        
    	init();
		try {
	    	solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error commiting index with SOLR",e);
		}
    }
    
    /**
     * Clear the entire index and commit.
     * @throws DaoException
     */
    public void clearIndex() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("clearIndex()");
        }
        
    	init();
		try {
        	log.info("Clearing SOLR index");
	    	solr.deleteByQuery("*:*");
	    	solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with SOLR",e);
		}
    }

    /**
     * Optimize the index (this is a very expensive operation, especially if the index is large!)
     * @throws DaoException
     */
    public void optimize() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("optimize()");
        }
        
    	init();
		try {
        	log.info("Optimizing SOLR index");
	    	solr.optimize();
		}
		catch (Exception e) {
			throw new DaoException("Error optimizing index with SOLR",e);
		}
    }
    
    /**
     * Run the given query against the index.
     * @param query
     * @return
     * @throws DaoException
     */
    public QueryResponse search(SolrQuery query) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("search(query="+query.getQuery()+")");
        }
        
    	init();
    	try {
    		log.debug("Running SOLR query: "+query);
            return solr.query(query);
    	}
		catch (Exception e) {
			throw new DaoException("Error searching with SOLR",e);
		}
    }
    
    /**
     * Runs a special id query against the index, breaking it up into several queries if necessary.
     * @param query
     * @return
     * @throws DaoException
     */
    public Map<Long,SolrDocument> search(List<Long> entityIds) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("search(entityIds.size="+entityIds.size()+")");
        }
        
    	init();
    	Map<Long,SolrDocument> docMap = new HashMap<Long,SolrDocument>();
    	try {
    		int currSize = 0;
			StringBuffer sqBuf = new StringBuffer();
    		for(Long entityId : entityIds) {
    			
    			if (currSize>=MAX_ID_LIST_SIZE) {
        	    	SolrQuery query = new SolrQuery(sqBuf.toString());
        	    	query.setRows(currSize);
        	    	QueryResponse qr = search(query);
        			Iterator<SolrDocument> i = qr.getResults().iterator();
        			while (i.hasNext()) {
        				SolrDocument doc = i.next();
        				docMap.put(new Long(doc.get("id").toString()), doc);
        			}
    				sqBuf = new StringBuffer();
    				currSize = 0;
    			}
    			
    			if (sqBuf.length()>0) sqBuf.append(" OR ");
    			sqBuf.append("id:"+entityId);
    			currSize++;
    		}

    		if (currSize>0) {
		    	SolrQuery query = new SolrQuery(sqBuf.toString());
		    	query.setRows(currSize);
		    	QueryResponse qr = search(query);
				Iterator<SolrDocument> i = qr.getResults().iterator();
				while (i.hasNext()) {
					SolrDocument doc = i.next();
					docMap.put(new Long(doc.get("id").toString()), doc);
				}
    		}

            return docMap;
    	}
		catch (Exception e) {
			throw new DaoException("Error searching with SOLR",e);
		}
    }

    /**
     * Update the document for the given entity to add a new ancestor (usually a new parent).
     * @param entityIds
     * @param newAncestorId
     * @throws DaoException
     */
	public void addNewAncestor(Long entityId, Long newAncestorId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addNewAncestor(entityId="+entityId+", newAncestorId="+newAncestorId+")");
        }
        
		List<Long> entityIds = new ArrayList<Long>();
		entityIds.add(entityId);
		addNewAncestor(entityIds, newAncestorId);
	}
	
    /**
     * Update the documents for all of the given entities and add the same new ancestor (usually a new parent) to 
     * each one. 
     * @param entityIds
     * @param newAncestorId
     * @throws DaoException
     */
	public void addNewAncestor(List<Long> entityIds, Long newAncestorId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addNewAncestor(entityIds.size="+entityIds.size()+", newAncestorId="+newAncestorId+")");
        }
    	
		// Get all Solr documents
    	Map<Long,SolrDocument> solrDocMap = search(entityIds);

    	log.info("Adding new ancestor to "+entityIds.size()+" entities. Found "+solrDocMap.size()+" documents.");
    	
    	// Create updated Solr documents
    	List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>();
    	for(SolrDocument existingDoc : solrDocMap.values()) {
    		SolrInputDocument inputDoc = ClientUtils.toSolrInputDocument(existingDoc);
    		
    		Collection<Long> ancestorIds = null;
    		
    		SolrInputField field = inputDoc.getField("ancestor_ids");
    		if (field==null) {
    			ancestorIds = new ArrayList<Long>();
    		}
    		else {
    			ancestorIds = (Collection<Long>)field.getValue();
    			if (ancestorIds==null) {
        			ancestorIds = new ArrayList<Long>();
        		} 
    		}
    		
    		ancestorIds.add(newAncestorId);

			inputDoc.removeField("ancestor_ids");
    		inputDoc.addField("ancestor_ids", ancestorIds, 0.2f);
    		
    		inputDocs.add(inputDoc);
        	log.info("Updating index for "+inputDoc.getFieldValue("name")+" (id="+inputDoc.getFieldValue("id")+"), adding ancestor "+newAncestorId);
    	}
    
    	// Index the entire batch

    	index(inputDocs);
    	commit();
	}
}
