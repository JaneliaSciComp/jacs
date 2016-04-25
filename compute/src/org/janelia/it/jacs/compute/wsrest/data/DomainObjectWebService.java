package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.*;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class DomainObjectWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DomainObjectWebService.class);

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders headers;

    public DomainObjectWebService() {
        register(JacksonFeature.class);
    }

    @POST
    @Path("/domainobject/details")
    @ApiOperation(value = "Gets an Domain Object's Details using either the references parameters or the objectType & objectIds",
                    notes = ""
            )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got a list of DomainObjectst", response=DomainObject.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of DomainObjects" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectDetails(@ApiParam DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<DomainObject> detailObjects = null;
            if (query.getReferences()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getReferences());
            } else if (query.getObjectIds()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getObjectType(),
                        query.getObjectIds());
            }
            return detailObjects;
        } catch (Exception e) {
            log.error("Error occurred processing Object Details ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/domainobject/name")
    @ApiOperation(value = "Gets DomainObjects by Name and DomainClass",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got a list of DomainObjectst", response=DomainObject.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of DomainObjects" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByName(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                                               @ApiParam @QueryParam("name") final String name,
                                               @ApiParam @QueryParam("domainClass") final String domainClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Class clazz = DomainUtils.getObjectClassByName(domainClass);
        try {
            return dao.getDomainObjectsByName(subjectKey, clazz, name);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects using name" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/domainobject/reverseLookup")
    @ApiOperation(value = "Gets a list of DomainObjects that are referring to this DomainObject",
            notes = "Uses reference attribute and reference class to determine type of parent reference to find"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got a list of DomainObjectst", response=DomainObject.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of DomainObjects" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByReverseRef(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                                                     @ApiParam @QueryParam("referenceId") final Long referenceId,
                                                     @ApiParam @QueryParam("count") final Long count,
                                                     @ApiParam @QueryParam("referenceAttr") final String referenceAttr,
                                                     @ApiParam @QueryParam("referenceClass") final String referenceClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ReverseReference reverseRef = new ReverseReference();
        reverseRef.setCount(count);
        reverseRef.setReferenceAttr(referenceAttr);
        reverseRef.setReferenceId(referenceId);
        reverseRef.setReferringClassName(referenceClass);
        try {
            return dao.getDomainObjects(subjectKey, reverseRef);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects using reverse ref" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/domainobject/references")
    @ApiOperation(value = "Gets Folder References to a DomainObject ",
            notes = "uses the DomainObject parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got a list of Folder References", response=Reference.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of Folder References" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Reference> getContainerReferences(@ApiParam DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getContainerReferences(query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred getting folder references",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/domainobject/update")
    @ApiOperation(value = "updates an exisitng DomainObject ",
            notes = "uses the DomainObject parameter of the DomainQuery, checks if the object exists to prevent this method from being abused"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully update the value of an existing Domain Object", response=DomainObject.class),
            @ApiResponse( code = 500, message = "Internal Server Error updating DomainObject" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DomainObject updateDomainObject (@ApiParam DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            if (dao.getDomainObject(query.getSubjectKey(), Reference.createFor(query.getDomainObject()))!=null) {
                return dao.save(query.getSubjectKey(), query.getDomainObject());
            }
        } catch (Exception e) {
            log.error("Error occurred updating Domain Object",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    @POST
    @Path("/domainobject/remove")
    @ApiOperation(value = "Removes a Domain Object",
            notes = "uses the References parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed domain objects"),
            @ApiResponse( code = 500, message = "Internal Server Error removing domain objects" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeDomainObject(@ApiParam DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            for (Reference objectRef : query.getReferences()) {
                // first check that it is an objectset or treeNode
                Class<? extends DomainObject> objClass = DomainUtils.getObjectClassByName(objectRef.getTargetClassName());
                if (objClass==TreeNode.class || objClass==ObjectSet.class) {
                    String subjectKey = query.getSubjectKey();
                    DomainObject domainObj = dao.getDomainObject(subjectKey, objectRef);
                    // check whether this subject has permissions to write to this object
                    if (domainObj.getWriters().contains(subjectKey)) {
                        IndexingHelper.sendRemoveFromIndexMessage(domainObj.getId());
                        dao.remove(subjectKey, domainObj);
                    }
                }

            }
        } catch (Exception e) {
            log.error("Error occurred removing object references",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/domainobject")
    @ApiOperation(value = "Updates an Object's Attribute",
            notes = "uses the ObjectType, ObjectId(first position), PropertyName, and PropertyValue parameters of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully updated a domain object's properties", response=DomainObject.class),
            @ApiResponse( code = 500, message = "Internal Server Error updating a domain object's properties" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DomainObject updateObjectProperty(@ApiParam DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            DomainObject updateObj = null;
            // TO DO: add check that parameters are valid
            List<Long> objIds = query.getObjectIds();
            if (objIds!=null && objIds.size()>0) {
                updateObj = dao.updateProperty(query.getSubjectKey(), query.getObjectType(), objIds.get(0),
                        query.getPropertyName(), query.getPropertyValue());
            }
            IndexingHelper.sendReindexingMessage(updateObj);

            return updateObj;

        } catch (Exception e) {
            log.error("Error occurred processing Domain Object Update Property ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}