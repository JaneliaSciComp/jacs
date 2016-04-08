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
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class ObjectSetWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(ObjectSetWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    public ObjectSetWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/objectset")
    @ApiOperation(value = "Creates an Object Set",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet createObjectSet(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("createObjectSet({})",query);
            ObjectSet updatedObjectSet = (ObjectSet)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updatedObjectSet);
            return updatedObjectSet;
        } catch (Exception e) {
            log.error("Error occurred creating object set",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/objectset/member")
    @ApiOperation(value = "Adds Items to a Set",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet addMembers(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("addMembers({})",query);
            ObjectSet updatedObjectSet = (ObjectSet)dao.addMembers(query.getSubjectKey(), (ObjectSet) query.getDomainObject(), query.getReferences());
            List<DomainObject> children = dao.getDomainObjects(query.getSubjectKey(),query.getReferences());
            for (DomainObject child: children) {
                IndexingHelper.sendAddAncestorMessage(child.getId(), updatedObjectSet.getId());
            }
            return updatedObjectSet;
        } catch (Exception e) {
            log.error("Error occurred adding members to object set",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/objectset/member")
    @ApiOperation(value = "Removes items from a Set",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectSet removeMembers(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("removeMembers({})",query);
            ObjectSet updatedObjectSet = (ObjectSet) dao.removeMembers(query.getSubjectKey(), (ObjectSet) query.getDomainObject(), query.getReferences());
            IndexingHelper.sendReindexingMessage(updatedObjectSet);
            List<DomainObject> children = dao.getDomainObjects(query.getSubjectKey(),query.getReferences());
            for (DomainObject child: children) {
                IndexingHelper.sendReindexingMessage(child);
            }
            return updatedObjectSet;
        } catch (Exception e) {
            log.error("Error occurred removing members from Object Set",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}