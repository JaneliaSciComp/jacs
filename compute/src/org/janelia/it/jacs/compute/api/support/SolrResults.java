package org.janelia.it.jacs.compute.api.support;

import java.io.Serializable;
import java.util.*;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Encapsulate SOLR results with their mapped Entity objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrResults implements Serializable {

	private final QueryResponse response;
	private final List<Entity> resultList;
	
	// these are recalculated whenever they're needed
	private transient Map<Long,SolrDocument> docMap; 
	private transient List<EntityDocument> entityDocList;
	
	public SolrResults(QueryResponse response, List<Entity> resultList) {
		this.response = response;
		this.resultList = resultList;
	}

	public QueryResponse getResponse() {
		return response;
	}
	
	public List<Entity> getResultList() {
		return resultList;
	}
	
	public Map<Long,SolrDocument> getDocumentMap() {
		if (docMap==null) {
			docMap = new HashMap<Long,SolrDocument>();
			Iterator<SolrDocument> i = getResponse().getResults().iterator();
			while (i.hasNext()) {
				SolrDocument doc = i.next();
				docMap.put(new Long(doc.get("id").toString()), doc);
			}	  
		}
		return docMap;
	}
	
	public SolrDocument getDocumentById(Long entityId) {
		return getDocumentMap().get(entityId);
	}
	
	public List<EntityDocument> getEntityDocuments() {
		if (entityDocList==null) {
			entityDocList = new ArrayList<EntityDocument>();
			for(Entity entity : resultList) {
				entityDocList.add(new EntityDocument(entity, getDocumentById(entity.getId())));	
			}
		}
		return entityDocList;
	}
	
}
