
package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbImport;
import org.janelia.it.jacs.compute.access.neo4j.Neo4jCSVExportDao;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrDocTypeEnum;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * Implementation of SOLR indexing and searching operations. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "SolrEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class SolrBeanImpl implements SolrBeanLocal, SolrBeanRemote {

    public static final String SOLR_EJB_PROP = "SolrEJB.Name";
    
    private static final Logger log = Logger.getLogger(SolrBeanImpl.class);

    private void updateIndex(Long entityId) {
    	IndexingHelper.updateIndex(entityId);
    }

    public void indexAllEntities(boolean clearIndex) throws ComputeException {

    	log.info("Getting FlyLight vocabularies from SAGE web service...");
    	Map<String, SageTerm> sageVocab = null;
    	try {
    		sageVocab = new SageDAO(log).getFlylightImageVocabulary();	
    	}
    	catch (DaoException e) {
    		log.error("Error retrieving FlyLight vocabularies",e);
    	}
    	log.info("Got "+sageVocab.size()+" vocabulary terms from SAGE web service");
    	
    	try {
    		SolrDAO solrDAO = new SolrDAO(log, true, true);
    		if (clearIndex) {
    			solrDAO.clearIndex();
    		}
    		solrDAO.indexAllEntities(sageVocab);
    	}
    	catch (DaoException e) {
            log.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }

    public void indexAllEntitiesInTree(Long entityId) throws ComputeException {
    	AnnotationDAO _annotationDAO = new AnnotationDAO(log);
    	Entity root = _annotationDAO.getEntityById(entityId);
    	indexAllEntitiesInTree(root, new HashSet<Long>());
    }

    private Entity indexAllEntitiesInTree(Entity entity, Set<Long> visited) {
    	if (entity == null) return null;
    	if (visited.contains(entity.getId())) {
    		return entity;
    	}
    	visited.add(entity.getId());
    	updateIndex(entity.getId());
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null) {
    			indexAllEntitiesInTree(child, visited);
    		}
    	}
    	return entity;
    }
    
    // TODO: move this to its own bean, or rename this one
    public void mongoAllDomainObjects(boolean clearDb) throws ComputeException {
        try {
            MongoDbImport mongodbDAO = new MongoDbImport(log);
            if (clearDb) {
                mongodbDAO.dropDatabase();
            }
            mongodbDAO.loadAllEntities();
        }
        catch (DaoException e) {
            log.error("Error loading into MongoDB",e);
            throw new ComputeException("Error loading into MongoDB",e);
        }
    }

    // TODO: move this to its own bean, or rename this one
    public void neo4jAllEntities(boolean clearDb) throws ComputeException {
    	try {
//    		Neo4jBatchDAO neo4jDAO = new Neo4jBatchDAO(_logger);
    		Neo4jCSVExportDao neo4jDAO = new Neo4jCSVExportDao(log);
    		if (clearDb) {
    			neo4jDAO.dropDatabase();
    		}
    		neo4jDAO.loadAllEntities();
    	}
    	catch (DaoException e) {
            log.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }
    
	public SolrResults search(String subjectKey, SolrQuery query, boolean mapToEntities) throws ComputeException {
		SolrDAO solrDAO = new SolrDAO(log, false, false);
		
		QueryResponse response = solrDAO.search(query);
		List<Entity> resultList = null;
		if (mapToEntities) {
			List<Long> ids = new ArrayList<Long>();
			SolrDocumentList docs = response.getResults();
			Iterator<SolrDocument> i = docs.iterator();
    		while (i.hasNext()) {
    			SolrDocument doc = i.next();
        		String idStr = (String)doc.get("id");
	    		try {
	    			if (idStr!=null) {
	    				Long id = new Long(idStr);
	    				if (id!=null) ids.add(id);
	    			}
	    		} 
	    		catch (NumberFormatException e) {
	    			log.warn("Error parsing id from index: "+idStr);
	    			continue;
	    		}
	    	}
			resultList = solrDAO.getEntitiesInList(subjectKey, ids);
		}
		
		return new SolrResults(response, resultList);
	}
    
    public Map<String, SageTerm> getFlyLightVocabulary() throws ComputeException {
    	SolrDAO solrDAO = new SolrDAO(log, false, false);
		Map<String, SageTerm> vocab = new HashMap<String, SageTerm>();
		
		SolrQuery query = new SolrQuery("doc_type:"+SolrDocTypeEnum.SAGE_TERM);
		query.setSortField("name", ORDER.asc);
		query.setRows(Integer.MAX_VALUE);
		QueryResponse response = solrDAO.search(query);
		
		Iterator<SolrDocument> i = response.getResults().iterator();
		while (i.hasNext()) {
			SolrDocument doc = i.next();
			SageTerm term = new SageTerm();
			term.setName(getStringValue(doc,"name"));
			term.setDataType(getStringValue(doc,"data_type_t"));
			term.setDisplayName(getStringValue(doc,"display_name_t"));
			term.setDefinition(getStringValue(doc,"definition_t"));
			vocab.put(term.getName(), term);
		}
		
    	return vocab;
    }
    
    private String getStringValue(SolrDocument doc, String key) {
    	Object value = doc.getFieldValue(key);
    	if (value == null) return null;
    	return value.toString();
    }
}
