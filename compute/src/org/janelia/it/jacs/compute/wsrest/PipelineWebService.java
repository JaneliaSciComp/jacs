package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;

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