package org.janelia.it.jacs.shared.solr;

import org.apache.solr.common.SolrDocumentList;

import java.util.List;
import java.util.Map;

/**
 * Created by schauderd on 2/4/16.
 */
public class SolrJsonResults {
    
    private final SolrDocumentList results;
    private final Map<String,List<FacetValue>> facetValues;
    private final long numFound;

    public SolrJsonResults(SolrDocumentList results, Map<String, List<FacetValue>> facetValues, long numFound) {
        this.results = results;
        this.facetValues = facetValues;
        this.numFound = numFound;
    }

    public SolrDocumentList getResults() {
        return results;
    }

    public Map<String,List<FacetValue>> getFacetValues() {
        return facetValues;
    }

    public long getNumFound() {
        return numFound;
    }
}
