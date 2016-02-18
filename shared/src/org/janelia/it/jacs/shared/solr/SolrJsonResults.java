package org.janelia.it.jacs.shared.solr;

import org.apache.solr.common.SolrDocumentList;

import java.util.List;
import java.util.Map;

/**
 * Created by schauderd on 2/4/16.
 */
public class SolrJsonResults {
    private SolrDocumentList results;
    private Map<String,List<FacetValue>> facetValues;
    private Long numFound;

    public SolrDocumentList getResults() {
        return results;
    }

    public void setResults(SolrDocumentList results) {
        this.results = results;
    }

    public Map<String,List<FacetValue>> getFacetValues() {
        return facetValues;
    }

    public void setFacetValues(Map<String, List<FacetValue>> facetFields) {
        this.facetValues = facetFields;
    }

    public Long getNumFound() {
        return numFound;
    }

    public void setNumFound(Long numFound) {
        this.numFound = numFound;
    }
}
