package org.janelia.it.jacs.shared.solr;

import org.apache.solr.client.solrj.SolrQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by schauderd on 2/4/16.
 */
public class SolrParams {
    private String query;
    private String sortField;
    private String[] filterQueries;
    private String[] facetField;
    private int facetMinCount;
    private List<String> fields = new ArrayList<String>();

    public SolrParams() {
        facetMinCount = 1;
        fields.add("score");
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String[] getFilterQueries() {
        return filterQueries;
    }

    public void setFilterQueries(String[] filterQueries) {
        this.filterQueries = filterQueries;
    }

    public String[] getFacetField() {
        return facetField;
    }

    public void setFacetField(String[] facetField) {
        this.facetField = facetField;
    }
}
