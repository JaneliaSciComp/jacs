
package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbDAO;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
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
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class SolrBeanImpl implements SolrBeanLocal, SolrBeanRemote {
	
    private Logger _logger = Logger.getLogger(this.getClass());
    
    public static final String SOLR_EJB_PROP = "SolrEJB.Name";
    
    public SolrBeanImpl() {
    }

    public void indexAllEntities(boolean clearIndex) throws ComputeException {
    	try {
    		SolrDAO solrDAO = new SolrDAO(_logger, true);
    		if (clearIndex) {
    			solrDAO.clearIndex();
    		}
    		solrDAO.indexAllEntities();
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
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
    
	public SolrResults search(SolrQuery query, boolean mapToEntities) throws ComputeException {
		
		SolrDAO solrDAO = new SolrDAO(_logger, false);
		
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
}
