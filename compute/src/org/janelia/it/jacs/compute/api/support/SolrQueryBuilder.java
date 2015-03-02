package org.janelia.it.jacs.compute.api.support;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.*;

/**
 * A helper class for clients building SOLR queries against the Entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrQueryBuilder {

	private String searchString;
	private String auxString;
	private Long rootId;
	private List<String> ownerKeys = new ArrayList<String>();
	private Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
	private List<String> facets = new ArrayList<String>();
	private String sortField;
	private boolean ascending;
	private Date startDate;
	private Date endDate;
		
	public SolrQueryBuilder() {
	}
	
	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public String getAuxString() {
		return auxString;
	}

	public void setAuxString(String auxString) {
		this.auxString = auxString;
	}

	public Long getRootId() {
		return rootId;
	}

	public void setRootId(Long rootId) {
		this.rootId = rootId;
	}

	public List<String> getOwnerKeys() {
		return ownerKeys;
	}

	public void addOwnerKey(String ownerKey) {
		this.ownerKeys.add(ownerKey);
	}

	public Map<String, Set<String>> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, Set<String>> filters) {
		this.filters = filters;
	}

	public List<String> getFacets() {
		return facets;
	}

	public void setFacets(List<String> facets) {
		this.facets = facets;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	public boolean hasQuery() {
		return !StringUtils.isEmpty(searchString) || !StringUtils.isEmpty(auxString) || rootId!=null || !filters.isEmpty() || startDate!=null || endDate != null;
	}
	
	public SolrQuery getQuery() throws ComputeException {
    	
    	StringBuffer qs = new StringBuffer();
    	
    	if (!ownerKeys.isEmpty()) {
        	qs.append("+subjects:(");
        	int i = 0;
        	for(String ownerKey : ownerKeys) {
        		if (i++>0) qs.append(" OR ");
                String ownerName = ownerKey.split(":")[1];
        		qs.append(ownerName);
        	}
        	qs.append(")");
    	}
    	
    	if (rootId != null) {
    		qs.append(" AND (ancestor_ids:"+rootId+")");
    	}
    	
    	qs.append(" +doc_type:"+ SolrDocTypeEnum.ENTITY.toString());
    	
    	qs.append(" -entity_type:Ontology*");

		if (startDate!=null || endDate!=null) {
			String startDateStr = startDate==null ? "*" : SolrUtils.formatDate(startDate);
			String endDateStr = endDate==null ? "*" : SolrUtils.formatDate(endDate);
			qs.append(" +updated_date:["+startDateStr+" TO "+endDateStr+"]");
		}

		if (!StringUtils.isEmpty(auxString)) {
			qs.append(" ");
			qs.append(auxString);	
		}
		
		if (!StringUtils.isEmpty(searchString)) {
	        String escapedSearchString = searchString.replaceAll(":", "\\:");
	    	qs.append(" AND (("+escapedSearchString+")");
    	
        	for(String ownerKey : ownerKeys) {
                String ownerName = ownerKey.split(":")[1];
        		String fieldNamePrefix = SolrUtils.getFormattedName(ownerName);
        		qs.append(" OR "+fieldNamePrefix+"_annotations:("+escapedSearchString+")");	
        	}
        	
            qs.append(")");
		}
    	
    	SolrQuery query = new SolrQuery();
        query.setQuery(qs.toString());
        query.setFacetMinCount(1);
		query.addField("score");

		if (sortField!=null) {
			query.setSortField(sortField, ascending?ORDER.asc:ORDER.desc);
		}

		for(String fieldName : filters.keySet()) {
			Set<String> values = filters.get(fieldName);
			if (values==null||values.isEmpty()) continue;
			query.addFilterQuery(getFilterQuery(fieldName, values));
		}
		
		for(String facet : facets) {
			// Exclude the facet field from itself, to support multi-valued faceting
    		query.addFacetField("{!ex="+facet+"}"+facet);
		}
		
        return query;
    }

    /**
     * Returns a SOLR-style field query for the given field containing the given values. Also tags the
     * field so that it can be excluded in facets on other fields. 
     * @param fieldName
     * @param values
     * @return
     */
	protected String getFilterQuery(String fieldName, Set<String> values) {
    	StringBuffer sb = new StringBuffer("{!tag="+fieldName+"}"+fieldName);
    	sb.append(":("); // Sad face :/
    	for(String value : values) {
    		sb.append("\"");
    		sb.append(value);
    		sb.append("\" ");
    	}
    	sb.append(")");
    	return sb.toString();
    }
}
