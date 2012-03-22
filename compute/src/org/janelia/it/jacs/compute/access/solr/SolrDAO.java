package org.janelia.it.jacs.compute.access.solr;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Data access to the SOLR indexes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrDAO extends AnnotationDAO {

    protected final static int SOLR_LOADER_BATCH_SIZE = 25000;
    protected final static int SOLR_LOADER_COMMIT_SIZE = 500000;
	protected static final int SOLR_LOADER_QUEUE_SIZE = 100;
	protected static final int SOLR_LOADER_THREAD_COUNT = 2;
	protected static final String SOLR_SERVER_URL = SystemConfigurationProperties.getString("Solr.ServerURL");

	protected LargeOperations largeOp;
    protected SolrServer solr;
    protected boolean streamingUpdates;

    public SolrDAO(Logger _logger) {
    	this(_logger, false);
    }
    
    public SolrDAO(Logger _logger, boolean streamingUpdates) {
        super(_logger);
        this.streamingUpdates = streamingUpdates;
        this.largeOp = new LargeOperations(this);
    }

    private void init() throws DaoException {
    	if (solr!=null) return;
        try {
        	if (streamingUpdates) {
        		solr = new StreamingUpdateSolrServer(SOLR_SERVER_URL, SOLR_LOADER_QUEUE_SIZE, SOLR_LOADER_THREAD_COUNT);	
        	}
        	else {
        		solr = new CommonsHttpSolrServer(SOLR_SERVER_URL);
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
    
    public void indexAllEntities() throws DaoException {
    	
    	largeOp.buildAnnotationMap();
    	largeOp.buildAncestorMap();
 
    	_logger.info("Getting entities");
    	
    	Map<Long,SimpleEntity> entityMap = new HashMap<Long,SimpleEntity>();
        int i = 0;
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, e.name, e.creation_date, e.updated_date, et.name, u.user_login, ea.name, ed.value, ed.child_entity_id ");
	        sql.append("from entity e ");
	        sql.append("join user_accounts u on e.user_id = u.user_id ");
	        sql.append("join entityType et on e.entity_type_id = et.id ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entityAttribute ea on ed.entity_att_id = ea.id ");
	        sql.append("where e.entity_type_id != ? ");
	        
	        EntityType annotationType = getEntityTypeByName(EntityConstants.TYPE_ANNOTATION);
	        
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	        stmt.setLong(1, annotationType.getId());
	        
			rs = stmt.executeQuery();
	    	_logger.info("    Processing results");
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				SimpleEntity entity = entityMap.get(entityId);
				if (entity==null) {
					if (i>0) {
		            	if (i%SOLR_LOADER_BATCH_SIZE==0) {
		                    List<SolrInputDocument> docs = createEntityDocs(entityMap.values());
		                    entityMap.clear();
		            		_logger.info("    Adding "+docs.size()+" docs (i="+i+")");
		            		index(docs);
		            	}
	            		if (i%SOLR_LOADER_COMMIT_SIZE==0) {
	            	    	_logger.info("    Committing SOLR index");
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
					entity.setUserLogin(rs.getString(6));
				}

				String key = rs.getString(7);
				String value = rs.getString(8);
				BigDecimal childIdBD = rs.getBigDecimal(9);
				
				if (key!=null && value!=null) {
					entity.getAttributes().add(new KeyValuePair(key, value));
				}
				
				if (childIdBD != null) {
					entity.getChildIds().add(childIdBD.longValue());
				}
			}

        	if (!entityMap.isEmpty()) {
                List<SolrInputDocument> docs = createEntityDocs(entityMap.values());
        		_logger.info("    Adding "+docs.size()+" docs (i="+i+")");
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
            catch (Exception e) {
        		throw new DaoException(e);
            }
        }

    	_logger.info("  Committing SOLR index");
		commit();
    	_logger.info("  Optimizing SOLR index");
		optimize();
    	_logger.info("Completed indexing "+i+" entities");
    	
    }
    
    private List<SolrInputDocument> createEntityDocs(Collection<SimpleEntity> entities) {
    	
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        for(SimpleEntity se : entities) {
        	Set<SimpleAnnotation> annotations = (Set<SimpleAnnotation>)largeOp.getValue("annotationMapCache", se.getId());
        	AncestorSet ancestorSet = (AncestorSet)largeOp.getValue("ancestorMapCache", se.getId());
        	Set<Long> ancestors = ancestorSet==null ? null : ancestorSet.getAncestors(); 
        	SolrInputDocument doc = createDoc(se, annotations, ancestors);
        	docs.add(doc);
        }
        return docs;
    }
    
    
    public void clearIndex() throws DaoException {
    	init();
		try {
	    	solr.deleteByQuery("*:*");
	    	solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with SOLR",e);
		}
    }

    public void commit() throws DaoException {
    	init();
		try {
	    	solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error commiting index with SOLR",e);
		}
    }

    public void optimize() throws DaoException {
    	init();
		try {
	    	solr.optimize();
		}
		catch (Exception e) {
			throw new DaoException("Error optimizing index with SOLR",e);
	
		}
    }
    
    public void index(Entity entity, Set<SimpleAnnotation> annotations, Set<Long> ancestorIds) throws DaoException {
    	List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    	docs.add(createDoc(entity, annotations, ancestorIds));
    	index(docs);
    }
    
    public void index(List<SolrInputDocument> docs) throws DaoException {
    	init();
    	if (docs==null) return;
    	try {
	    	solr.add(docs);
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    private SimpleEntity simpleEntityFromEntity(Entity entity) {
    	SimpleEntity simpleEntity = new SimpleEntity();
    	simpleEntity.setId(entity.getId());
    	simpleEntity.setName(entity.getName());
    	simpleEntity.setCreationDate(entity.getCreationDate());
    	simpleEntity.setUpdatedDate(entity.getUpdatedDate());
    	
    	Set<Long> childrenIds = new HashSet<Long>();
    	for(EntityData ed : entity.getEntityData()) {
    		if (ed.getValue()!=null) {
    			String attr = (ed.getEntityAttribute().getName());
    			simpleEntity.getAttributes().add(new KeyValuePair(attr, ed.getValue()));
    		}
    		else if (ed.getChildEntity()!=null) {
    			childrenIds.add(ed.getChildEntity().getId());
    			simpleEntity.getChildIds().add(ed.getChildEntity().getId());
    		}
    	}
    	
    	return simpleEntity;
    }
    
    public SolrInputDocument createDoc(Entity entity, Set<SimpleAnnotation> annotations, Set<Long> ancestorIds) {
    	SimpleEntity simpleEntity = simpleEntityFromEntity(entity);
    	return createDoc(simpleEntity, annotations, ancestorIds);
    }

    public SolrInputDocument createDoc(SimpleEntity entity, Set<SimpleAnnotation> annotations, Set<Long> ancestorIds) {

    	SolrInputDocument doc = new SolrInputDocument();
    	doc.addField("id", entity.getId(), 1.0f);
    	doc.addField("name", entity.getName(), 1.0f);
    	doc.addField("creation_date", entity.getCreationDate(), 0.8f);
    	doc.addField("updated_date", entity.getUpdatedDate(), 0.9f);
    	doc.addField("username", entity.getUserLogin(), 1.0f);
    	doc.addField("entity_type", entity.getEntityTypeName(), 1.0f);
    	
    	for(KeyValuePair kv : entity.getAttributes()) {
    		if (kv.getValue()!=null) {
    			doc.addField(SolrUtils.getDynamicFieldName(kv.getKey()), kv.getValue(), 1.0f);	
    		}
    	}
    	
    	if (!entity.getChildIds().isEmpty()) {
    		doc.addField("child_ids", entity.getChildIds(), 0.2f);
    	}
    	
    	if (annotations != null) {
    		for(SimpleAnnotation annotation : annotations) {
    			doc.addField(annotation.getOwner()+"_annotations", annotation.getTag(), 1.0f);
    			if (annotation.getValue()!=null) {
    				doc.addField(annotation.getOwner()+"_"+SolrUtils.getFormattedName(annotation.getKey())+"_annot", annotation.getValue(), 1.0f);
    			}
    		}
    	}
    	
    	if (ancestorIds != null) {
    		doc.addField("ancestor_ids", ancestorIds, 0.2f);
    	}
    	
    	return doc;
    }

    public QueryResponse search(SolrQuery query) throws DaoException {
    	init();
    	try {
            return solr.query(query);
    	}
		catch (Exception e) {
			throw new DaoException("Error searching with SOLR",e);
		}
    }
}
