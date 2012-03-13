package org.janelia.it.jacs.compute.api.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Encapsulate SOLR results with their mapped Entity objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrResults implements Serializable {

	private QueryResponse response;
	private List<Entity> resultList = new ArrayList<Entity>();

	public QueryResponse getResponse() {
		return response;
	}
	public void setResponse(QueryResponse response) {
		this.response = response;
	}
	public List<Entity> getResultList() {
		return resultList;
	}
	public void setResultList(List<Entity> resultList) {
		this.resultList = resultList;
	}
}
