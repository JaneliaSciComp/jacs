
package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbDAO;
import org.janelia.it.jacs.compute.access.neo4j.Neo4jDAO;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.api.support.SageTerm;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.*;

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
    
    private static final Logger _logger = Logger.getLogger(SolrBeanImpl.class);

    private void updateIndex(Long entityId) {
    	IndexingHelper.updateIndex(entityId);
    }

    public void indexAllEntities(boolean clearIndex) throws ComputeException {

    	_logger.info("Getting FlyLight vocabularies from SAGE web service...");
    	Map<String, SageTerm> sageVocab = null;
    	try {
    		sageVocab = new SageDAO(_logger).getFlylightImageVocabulary();	
    	}
    	catch (DaoException e) {
    		_logger.error("Error retrieving FlyLight vocabularies",e);
    	}
    	_logger.info("Got "+sageVocab.size()+" vocabulary terms from SAGE web service");
    	
    	try {
    		SolrDAO solrDAO = new SolrDAO(_logger, true, true);
    		if (clearIndex) {
    			solrDAO.clearIndex();
    		}
    		solrDAO.indexAllEntities(sageVocab);
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }

    public void indexAllEntitiesInTree(Long entityId) throws ComputeException {
    	AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    	Entity root = _annotationDAO.getEntityById(entityId.toString());
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
    public void mongoAllEntities(boolean clearDb) throws ComputeException {
    	try {
    		MongoDbDAO mongodbDAO = new MongoDbDAO(_logger);
    		if (clearDb) {
    			mongodbDAO.dropDatabase();
    		}
    		mongodbDAO.loadAllEntities();
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }

    // TODO: move this to its own bean, or rename this one
    public void neo4jAllEntities(boolean clearDb) throws ComputeException {
    	try {
    		Neo4jDAO neo4jDAO = new Neo4jDAO(_logger);
    		if (clearDb) {
    			neo4jDAO.dropDatabase();
    		}
    		neo4jDAO.loadAllEntities();
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }
    
	public SolrResults search(SolrQuery query, boolean mapToEntities) throws ComputeException {
		SolrDAO solrDAO = new SolrDAO(_logger, false, false);
		
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
	    			_logger.warn("Error parsing id from index: "+idStr);
	    			continue;
	    		}
	    	}
			resultList = solrDAO.getEntitiesInList(ids);
		}
		
		return new SolrResults(response, resultList);
	}
    
    public Map<String, SageTerm> getFlyLightVocabulary() throws ComputeException {
    	SolrDAO solrDAO = new SolrDAO(_logger, false, false);
		Map<String, SageTerm> vocab = new HashMap<String, SageTerm>();
		
		SolrQuery query = new SolrQuery("doc_type:"+SolrUtils.DocType.SAGE_TERM);
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
