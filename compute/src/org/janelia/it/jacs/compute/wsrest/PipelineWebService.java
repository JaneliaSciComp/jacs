package org.janelia.it.jacs.compute.wsrest;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class PipelineWebService {
    @Context
    SecurityContext securityContext;

    @PUT
    @Path("pipeline/task")
    public void addTask(@Context UriInfo uriInfo) {
    }

    @GET
    @Path("search")
    @Produces("application/json")
    public Response searchDomainObjects(@Context UriInfo uriInfo) {
        Response response = null;
        return response;
    }
}