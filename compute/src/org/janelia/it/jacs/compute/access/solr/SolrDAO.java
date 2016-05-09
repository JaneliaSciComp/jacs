package org.janelia.it.jacs.compute.access.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.solr.SageTerm;

/**
 * Data access to the SOLR indexes.
 * 
 * @deprecated can probably be deleted
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
    	Map<Long,SolrDocument> docMap = new HashMap<>();
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

}
