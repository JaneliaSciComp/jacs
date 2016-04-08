package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.security.LDAPProvider;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class TreeNodeWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(TreeNodeWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    LDAPProvider authenticator;

    public TreeNodeWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/treenode")
    @ApiOperation(value = "Creates a Folder",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode createTreeNode(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("createTreeNode({})",query);
            TreeNode updatedNode = (TreeNode)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updatedNode);
            return updatedNode;
        } catch (Exception e) {
            log.error("Error occurred creating tree node",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/treenode/reorder")
    @ApiOperation(value = "Reorders a Folder",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode reorderTreeNode(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("reorderTreeNode({})",query);
            List<Integer> orderList = query.getOrdering();
            int[] order = new int[orderList.size()];
            for (int i=0; i<orderList.size(); i++) {
                order[i] = orderList.get(i).intValue();
            }
            TreeNode updatedNode = (TreeNode)dao.reorderChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), order);
            return updatedNode;
        } catch (Exception e) {
            log.error("Error occurred reordering Tree Node",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/treenode/children")
    @ApiOperation(value = "Adds Items to a Folder",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode addChildren(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("addChildren({})",query);
            TreeNode updatedNode = (TreeNode)dao.addChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
            List<DomainObject> children = dao.getDomainObjects(query.getSubjectKey(),query.getReferences());
            for (DomainObject child: children) {
                IndexingHelper.sendAddAncestorMessage(child.getId(), updatedNode.getId());
            }
            return updatedNode;
        } catch (Exception e) {
            log.error("Error occurred add children to tree node ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @POST
    @Path("/treenode/children")
    @ApiOperation(value = "Removes items in a Folder",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TreeNode removeChildren(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("removeChildren({})",query);
            TreeNode updatedNode = (TreeNode)dao.removeChildren(query.getSubjectKey(), (TreeNode) query.getDomainObject(), query.getReferences());
            List<DomainObject> children = dao.getDomainObjects(query.getSubjectKey(),query.getReferences());
            for (DomainObject child: children) {
                IndexingHelper.sendReindexingMessage(child);
            }
            return updatedNode;
        } catch (Exception e) {
            log.error("Error occurred removing children from tree node ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}