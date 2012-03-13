package org.janelia.it.jacs.compute.api.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.ComputeException;

/**
 * A helper class for clients building SOLR queries against the Entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrQueryBuilder {
	
	private String username; 
	private Long rootId;
	private String queryString;
	private List<String> facets = new ArrayList<String>();
	
	public SolrQueryBuilder() {
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getRootId() {
		return rootId;
	}

	public void setRootId(Long rootId) {
		this.rootId = rootId;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public List<String> getFacets() {
		return facets;
	}

	public SolrQuery getQuery() throws ComputeException {
    	
    	StringBuffer qs = new StringBuffer();
    	
    	qs.append("(username:system");
    	if (username!=null) {
    		qs.append(" OR username:"+username);
    	}
    	qs.append(") ");
    	
    	if (rootId != null) {
    		qs.append("AND (ancestor_ids:"+rootId+") ");
    	}
    	qs.append("AND "+queryString);

    	SolrQuery query = new SolrQuery();
        query.setQuery(qs.toString());
        query.addFacetField(facets.toArray(new String[facets.size()]));
        query.setFacetMinCount(1);
        return query;
    }
}
