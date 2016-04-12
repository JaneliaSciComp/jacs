package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class WorkspaceWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    public WorkspaceWebService() {
        register(JacksonFeature.class);
    }

    // mapping using explicit object mapping; TO DO configure jackson integration with jersey
    @GET
    @Path("/workspace")
    @ApiOperation(value = "Gets the Default Workspace for a User",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got default workspace", response=Workspace.class),
            @ApiResponse( code = 500, message = "Internal Server Error getting default workspace" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getWorkspace(@ApiParam @QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getAllWorkspace({})", subjectKey);
            return dao.getDefaultWorkspace(subjectKey);
        } catch (Exception e) {
            log.error("Error occurred getting default workspace",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/workspaces")
    @ApiOperation(value = "Gets all the workspaces a user can read",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got all workspaces", response=Workspace.class,
                responseContainer =  "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting workspaces" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getAllWorkspace(@QueryParam("subjectKey") String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getAllWorkspace({})",subjectKey);
            return new ArrayList<Workspace>(dao.getWorkspaces(subjectKey));
        } catch (Exception e) {
            log.error("Error occurred getting default workspace",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


}