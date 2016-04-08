package org.janelia.it.jacs.compute.wsrest.data;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.shared.solr.FacetValue;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;


@Path("/data")
public class SolrWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SolrWebService.class);

    @Context
    SecurityContext securityContext;

    public SolrWebService() {
        register(JacksonFeature.class);
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SolrJsonResults searchSolrIndices(SolrParams queryParams) {
        SolrConnector solr = WebServiceContext.getSolr();
        try {
            SolrQuery query = SolrQueryBuilder.deSerializeSolrQuery(queryParams);
            QueryResponse response = solr.search(query);
            Map<String,List<FacetValue>> facetValues = new HashMap<>();
            if (response.getFacetFields()!=null) {
                for (final FacetField ff : response.getFacetFields()) {
                    List<FacetValue> favetValues = new ArrayList<>();
                    if (ff.getValues() != null) {
                        for (final FacetField.Count count : ff.getValues()) {
                            favetValues.add(new FacetValue(count.getName(), count.getCount()));
                        }
                    }
                    facetValues.put(ff.getName(), favetValues);
                }
            }

            SolrDocumentList results = response.getResults();
            log.debug("searchSolrIndices called with {} and found {} results",queryParams,results.getNumFound());
            // TODO: why this reverse? 
//            if (query.getSortField()!=null && query.getSortField().endsWith("asc")) {
//                Collections.reverse(results);
//            }
            return new SolrJsonResults(results, facetValues, response.getResults().getNumFound());

        } catch (Exception e) {
            log.error("Error occurred executing search against SOLR",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}