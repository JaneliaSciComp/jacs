package org.janelia.it.jacs.compute.access.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Data access to the SOLR indexes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrDAO {

	protected static final int SOLR_LOADER_QUEUE_SIZE = 100;
	protected static final int SOLR_LOADER_THREAD_COUNT = 2;
	protected static final String SOLR_SERVER_URL = SystemConfigurationProperties.getString("Solr.ServerURL");
	
    protected Logger logger;
    protected SolrServer solr;

    public SolrDAO(Logger logger) throws DaoException {
    	this(logger, false);
    }
    
    public SolrDAO(Logger logger, boolean streamingUpdates) throws DaoException {
        this.logger = logger;
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

    public void clearIndex() throws DaoException {
		try {
	    	solr.deleteByQuery("*:*");
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }

    public void commit() throws DaoException {
		try {
	    	solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    public void index(Entity entity, List<SimpleAnnotation> annotations, List<Long> ancestorIds) throws DaoException {
    	if (entity==null) return;
    	try {
	    	solr.add(createDoc(entity, annotations, ancestorIds));
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    public void index(List<Entity> entities, Map<Long,List<SimpleAnnotation>> annotationMap, Map<Long,List<Long>> ancestorMap) throws DaoException {
    	if (entities.isEmpty()) return;
		try {
	    	Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
	    	for(Entity entity : entities) {
	    		List<SimpleAnnotation> annotations = annotationMap.get(entity.getId());
	    		List<Long> ancestors = ancestorMap.get(entity.getId());
	            docs.add(createDoc(entity, annotations, ancestors));
	    	}
	    	        
	    	solr.add(docs);
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    public void index(List<SolrInputDocument> docs) throws DaoException {
    	if (docs==null) return;
    	try {
	    	solr.add(docs);
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    public SolrInputDocument createDoc(Entity entity, List<SimpleAnnotation> annotations, List<Long> ancestorIds) {

    	SolrInputDocument doc = new SolrInputDocument();
    	doc.addField("id", entity.getId(), 1.0f);
    	doc.addField("name", entity.getName(), 1.0f);
    	doc.addField("creation_date", entity.getCreationDate(), 0.8f);
    	doc.addField("updated_date", entity.getUpdatedDate(), 0.9f);
    	doc.addField("username", entity.getUser().getUserLogin(), 1.0f);
    	doc.addField("entity_type", entity.getEntityType().getName(), 1.0f);
    	
    	Set<Long> childrenIds = new HashSet<Long>();
    	for(EntityData ed : entity.getEntityData()) {
    		if (ed.getValue()!=null) {
    			String attr = (ed.getEntityAttribute().getName());
    			doc.addField(getFieldName(attr)+"_txt", ed.getValue(), 1.0f);	
    		}
    		else if (ed.getChildEntity()!=null) {
    			childrenIds.add(ed.getChildEntity().getId());
    		}
    	}
    	
    	if (!childrenIds.isEmpty()) {
    		doc.addField("child_ids", childrenIds, 0.2f);
    	}
    	
    	if (annotations != null) {
    		for(SimpleAnnotation annotation : annotations) {
    			doc.addField("key_annot", annotation.getKey(), 1.0f);
    			if (annotation.getValue()!=null) {
    				doc.addField(getFieldName(annotation.getKey())+"_txt", annotation.getValue(), 1.0f);
    			}
    		}
    	}
    	
    	if (ancestorIds != null) {
    		doc.addField("ancestor_ids", ancestorIds, 0.2f);
    	}
    	
    	return doc;
    }

    public SolrInputDocument createDoc(SimpleEntity entity, List<SimpleAnnotation> annotations, List<Long> ancestorIds) {

    	SolrInputDocument doc = new SolrInputDocument();
    	doc.addField("id", entity.getId(), 1.0f);
    	doc.addField("name", entity.getName(), 1.0f);
    	doc.addField("creation_date", entity.getCreationDate(), 0.8f);
    	doc.addField("updated_date", entity.getUpdatedDate(), 0.9f);
    	doc.addField("username", entity.getUserLogin(), 1.0f);
    	doc.addField("entity_type", entity.getEntityTypeName(), 1.0f);
    	
    	for(KeyValuePair kv : entity.getAttrValues()) {
    		if (kv.getValue()!=null) {
    			doc.addField(getFieldName(kv.getKey())+"_txt", kv.getValue(), 1.0f);	
    		}
    	}
    	
    	if (!entity.getChildIds().isEmpty()) {
    		doc.addField("child_ids", entity.getChildIds(), 0.2f);
    	}
    	
    	if (annotations != null) {
    		for(SimpleAnnotation annotation : annotations) {
    			doc.addField("key_annot", annotation.getKey(), 1.0f);
    			if (annotation.getValue()!=null) {
    				doc.addField(getFieldName(annotation.getKey())+"_txt", annotation.getValue(), 1.0f);
    			}
    		}
    	}
    	
    	if (ancestorIds != null) {
    		doc.addField("ancestor_ids", ancestorIds, 0.2f);
    	}
    	
    	return doc;
    }
    
    public SolrDocumentList search(String queryString) throws DaoException {
    	SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        return search(query);
    }
    
    public SolrDocumentList search(SolrQuery query) throws DaoException {
    	try {
            QueryResponse rsp = solr.query(query);
            SolrDocumentList docs = rsp.getResults();
            return docs;
    	}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
    }
    
    private String getFieldName(String name) {
    	return name.toLowerCase().replaceAll("\\s+", "_");
    }
}
