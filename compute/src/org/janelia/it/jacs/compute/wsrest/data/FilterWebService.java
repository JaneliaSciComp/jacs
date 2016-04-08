package org.janelia.it.jacs.compute.wsrest.data;


import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/data")
public class FilterWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(FilterWebService.class);

    @Context
    SecurityContext securityContext;

    public FilterWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter createFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter newFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(newFilter);
            return newFilter;
        } catch (Exception e) {
            log.error("Error occurred creating Search Filter ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter updateFilter(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Filter updateFilter = (Filter)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateFilter);
            return updateFilter;
        } catch (Exception e) {
            log.error("Error occurred updating search filter ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}