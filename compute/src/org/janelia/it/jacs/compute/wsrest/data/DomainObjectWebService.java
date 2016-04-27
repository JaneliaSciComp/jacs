package org.janelia.it.jacs.compute.wsrest.data;

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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectDetails(DomainQuery query) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByName(@QueryParam("subjectKey") final String subjectKey,
                                               @QueryParam("name") final String name,
                                               @QueryParam("domainClass") final String domainClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Class clazz = DomainUtils.getObjectClassByName(domainClass);
        try {
            return dao.getDomainObjectsByName(subjectKey, clazz, name);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects using name" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Use for getting all examples of a given object type.  Only for small sets.
     * @param subjectKey omissible. If given, constrains by ownership.
     * @param domainClass required. constrains by collection/type.
     * @return all existing examples of things of this type.
     */
    @GET
    @Path("/domainobject/class")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByClass(@QueryParam("subjectKey") final String subjectKey,
                                                @QueryParam("domainClass") final String domainClass) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Class clazz = DomainUtils.getObjectClassByName(domainClass);
        try {
            return dao.getDomainObjects(subjectKey, clazz);
        } catch (Exception e) {
            log.error("Error occurred retrieving domain objects by class" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/domainobject/reverseLookup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainObject> getObjectsByReverseRef(@QueryParam("subjectKey") final String subjectKey,
                                                     @QueryParam("referenceId") final Long referenceId,
                                                     @QueryParam("count") final Long count,
                                                     @QueryParam("referenceAttr") final String referenceAttr,
                                                     @QueryParam("referenceClass") final String referenceClass) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Reference> getContainerReferences(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return dao.getContainerReferences(query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred getting treenode/objectset references",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @POST
    @Path("/domainobject/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeDomainObject(DomainQuery query) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DomainObject updateObjectProperty(DomainQuery query) {
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