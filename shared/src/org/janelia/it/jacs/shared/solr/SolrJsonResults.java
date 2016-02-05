package org.janelia.it.jacs.shared.solr;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by schauderd on 2/4/16.
 */
public class SolrJsonResults {
    private SolrDocumentList results;
    private Map<String,List<FacetValue>> facetValues;

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
        this.facetValues = facetValues;
    }
}
