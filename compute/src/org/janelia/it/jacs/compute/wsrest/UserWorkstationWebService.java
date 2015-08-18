package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import org.janelia.it.jacs.model.domain.DomainObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.model.domain.workspace.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.glassfish.jersey.server.model.Resource;
import javax.ws.rs.core.*;

@Path("/")
public class UserWorkstationWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    @GET
    @Path("/subject/workspace")
    @Produces("application/json")
    public Response getFolder(@QueryParam("subjectKey") String subjectKey,
                                     @Context UriInfo uriInfo) {
        DomainDAO dao = WebServiceContext.getDomainManager();

        Multimap<String, DomainObject> workspaceMap = ArrayListMultimap.<String,DomainObject>create();
        for (Workspace workspace : dao.getWorkspaces(subjectKey)) {
            List<DomainObject> foo = dao.getDomainObjects(subjectKey, workspace.getChildren());
            System.out.println (foo);
            System.out.println (workspace.getChildren().get(0));
            workspaceMap.putAll(workspace.getName(), foo);
        }

        return Response.status(200).entity(workspaceMap).build();
    }

    @GET
    @Path("/preferences")
    public Response getUserPreferences(@Context UriInfo uriInfo) {
        return Response.status(200).build();
    }

    @POST
    @Path("/preferences")
    @Produces("application/json")
    public Response setUserPreferences(@PathParam("workspaceId") Long workspaceId, @Context UriInfo uriInfo) {
        Response response = null;
        return response;
    }

    @POST
    @Path("/objectset")
    public void changePermissionsObjectSet(@Context UriInfo uriInfo) {

    }
}