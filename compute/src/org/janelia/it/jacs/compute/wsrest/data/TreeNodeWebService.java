package org.janelia.it.jacs.compute.wsrest.data;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class TreeNodeWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(TreeNodeWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    public TreeNodeWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/treenode")
    @ApiOperation(value = "Creates A TreeNode",
            notes = "Uses the DomainObject parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully creating TreeNode", response=TreeNode.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating TreeNode" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode createTreeNode(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("createTreeNode({})",query);
            return dao.save(query.getSubjectKey(), (TreeNode)query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating tree node",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/treenode/reorder")
    @ApiOperation(value = "Reorders the items in a TreeNode",
            notes = "Uses the DomainObject parameter of the DomainQuery and the Ordering parameter for the new ordering."
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully reordered TreeNode", response=TreeNode.class),
            @ApiResponse( code = 500, message = "Internal Server Error reordering TreeNode" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode reorderTreeNode(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("reorderTreeNode({})",query);
            List<Integer> orderList = query.getOrdering();
            int[] order = new int[orderList.size()];
            for (int i=0; i<orderList.size(); i++) {
                order[i] = orderList.get(i).intValue();
            }
            return dao.reorderChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), order);
        } catch (Exception e) {
            log.error("Error occurred reordering Tree Node",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/treenode/children")
    @ApiOperation(value = "Adds items to a TreeNode",
            notes = "Uses the DomainObject parameter of the DomainQuery for the Treenode, " +
                    "the References parameter for the list of items to add"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully added items to the TreeNode", response=TreeNode.class),
            @ApiResponse( code = 500, message = "Internal Server Error adding items to the TreeNode" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode addChildren(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("addChildren({})",query);
            return dao.addChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred add children to tree node ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @POST
    @Path("/treenode/children")
    @ApiOperation(value = "Removes items from a TreeNode",
            notes = "Uses the DomainObject parameter of the DomainQuery for the Treenode, " +
                    "the References parameter for the list of items to remove"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed items from the TreeNode", response=TreeNode.class),
            @ApiResponse( code = 500, message = "Internal Server Error removing items from the TreeNode" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode removeChildren(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("removeChildren({})",query);
            return dao.removeChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
        } catch (Exception e) {
            log.error("Error occurred removing children from tree node ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}