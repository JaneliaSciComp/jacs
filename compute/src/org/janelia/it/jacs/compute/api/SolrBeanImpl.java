package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbImport;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbMaintainer;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.access.neo4j.Neo4jCSVExportDao;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

/**
 * Implementation of SOLR indexing and searching operations. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "SolrEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class SolrBeanImpl implements SolrBeanLocal, SolrBeanRemote {
	
	private static final Logger log = Logger.getLogger(SolrBeanImpl.class);
	
    //public static final String SOLR_EJB_PROP = "SolrEJB.Name";
    
    public void updateIndex(DomainObject domainObj) {
		log.info("AAAAAAAAAAAAAAA");
		IndexingHelper.sendReindexingMessage(domainObj);
    }

	public void removeFromIndex(Long domainObjId) {
		IndexingHelper.sendRemoveFromIndexMessage(domainObjId);
	}

	public void addAncestorToIndex(Long domainObjId, Long ancestorId) {
		IndexingHelper.sendAddAncestorMessage(domainObjId, ancestorId);
	}

    public void indexAllEntities(boolean clearIndex) throws ComputeException {
    	try {
    	    SolrConnector solr = new SolrConnector(DomainDAOManager.getInstance().getDao(), true, true);
    		if (clearIndex) {
    		    solr.clearIndex();
    		}
    		solr.indexAllDocuments();
    	}
    	catch (Exception e) {
            log.error("Error connecting to Mongo",e);
    		throw new ComputeException("Error connecting to Mongo",e);
    	}
    }

    public void indexAllEntitiesInTree(Long entityId) throws ComputeException {
    	throw new UnsupportedOperationException("No longer supported");
    }
    
    // TODO: move this to its own bean, or rename this one
    public void mongoAllDomainObjects(boolean clearDb) throws ComputeException {
    	
    	long t1=0,t2=0,t3=0,t4=0,t5=0;
    	
        try {
            t1 = System.currentTimeMillis(); 
            
            MongoDbImport mongoDbImport = new MongoDbImport();
            if (clearDb) mongoDbImport.dropDatabase();
            mongoDbImport.loadAllEntities();

            t2 = System.currentTimeMillis(); 
            
            MongoDbMaintainer refresh = new MongoDbMaintainer();
            refresh.refreshPermissions();
            
            t3 = System.currentTimeMillis();
            
            refresh.ensureIndexes();

            t4 = System.currentTimeMillis(); 
            
            indexAllEntities(true);
            
            t5 = System.currentTimeMillis();
        }
        catch (Exception e) {
            log.error("Error loading into MongoDB",e);
            throw new ComputeException("Error loading into MongoDB",e);
        }
        finally {
        	if (t2>0) log.info("Loading MongoDB took "+((double)(t2-t1)/1000/60/60)+" hours");
	        if (t3>0) log.info("Refreshing MongoDB permissions took "+((double)(t3-t2)/1000/60/60)+" hours");
	        if (t4>0) log.info("Ensuring MongoDB indexes took "+((double)(t4-t3)/1000/60/60)+" hours");
	        if (t5>0) log.info("Indexing MongoDB to Solr took "+((double)(t5-t4)/1000/60/60)+" hours");
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
		System.out.println (query);
		QueryResponse response = solrDAO.search(query);
		List<Entity> resultList = null;
		if (mapToEntities) {
			List<Long> ids = new ArrayList<>();
			SolrDocumentList docs = response.getResults();
			for (SolrDocument doc : docs) {
				String idStr = (String) doc.get("id");
				try {
					if (idStr != null) {
						Long id = new Long(idStr);
						ids.add(id);
					}
				}
				catch (NumberFormatException e) {
					log.warn("Error parsing id from index: " + idStr);
				}
			}
			resultList = solrDAO.getEntitiesInList(subjectKey, ids);
		}
		
		return new SolrResults(response, resultList);
	}
    
    public Map<String, SageTerm> getImageVocabulary() throws ComputeException {
    	SolrDAO solrDAO = new SolrDAO(log, false, false);
		Map<String, SageTerm> vocab = new HashMap<>();
		
		SolrQuery query = new SolrQuery("doc_type:"+SolrDocTypeEnum.SAGE_TERM);
		query.setSortField("name", ORDER.asc);
		query.setRows(Integer.MAX_VALUE);
		QueryResponse response = solrDAO.search(query);

		for (SolrDocument doc : response.getResults()) {
			SageTerm term = new SageTerm();
			term.setName(getStringValue(doc, "name"));
			term.setDataType(getStringValue(doc, "data_type_t"));
			term.setDisplayName(getStringValue(doc, "display_name_t"));
			term.setDefinition(getStringValue(doc, "definition_t"));
			term.setCv(getStringValue(doc, "cv_t"));
			vocab.put(term.getKey(), term);
		}
		
    	return vocab;
    }
    
    private String getStringValue(SolrDocument doc, String key) {
    	Object value = doc.getFieldValue(key);
    	if (value == null) return null;
    	return value.toString();
    }
}
