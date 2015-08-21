package org.janelia.it.jacs.compute.wsrest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.*;

import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.janelia.it.jacs.model.domain.workspace.*;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@MongoMapped(collectionName = "mooNode")
@Path("/")
public class UserWorkstationWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    public UserWorkstationWebService() {
        register(JacksonJsonProvider.class);
    }

    // mapping using explicit object mapping; TO DO configure jackson integration with jersey
    @GET
    @Path("/workspace")
    @Produces(MediaType.APPLICATION_JSON)
    public String getWorkspace(@QueryParam("subjectKey") String subjectKey,
                            @QueryParam("option") String option,
                            @Context UriInfo uriInfo) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (option!=null && option.toLowerCase().equals("full")) {
                Map<String, List<DomainObject>> workspaceMap = new HashMap<String, List<DomainObject>>();
                for (Workspace workspace : dao.getWorkspaces(subjectKey)) {
                    List<DomainObject> children = dao.getDomainObjects(subjectKey, workspace.getChildren());
                    workspaceMap.put(workspace.getName(), children);
                }
                return mapper.writeValueAsString(workspaceMap);
            } else {
                Workspace workspace = dao.getDefaultWorkspace(subjectKey);
                return mapper.writeValueAsString(workspace);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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