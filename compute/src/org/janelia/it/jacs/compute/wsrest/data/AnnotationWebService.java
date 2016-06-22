package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class AnnotationWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(AnnotationWebService.class);

    @Context
    SecurityContext securityContext;

    public AnnotationWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/annotation")
    @ApiOperation(value = "Creates an annotation",
            notes = "creates a new annotation from the DomainObject parameter and assigns ownership to the SubjectKey parameter"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully created annotation" ,response = Annotation.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating Annotation" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation createAnnotation(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("createAnnotation({})",query);
            Annotation newAnnotation = (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(newAnnotation);
            return newAnnotation;
        } catch (Exception e) {
            log.error("Error occurred creating annotations", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/annotation")
    @ApiOperation(value = "Updates an annotation",
            notes = "updates an existing annotation using the DomainObject parameter"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully updated annotation",response = Annotation.class ),
            @ApiResponse( code = 500, message = "Internal Server Error updated Annotation" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation updateAnnotation(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("updateAnnotation({})",query);
            Annotation updateAnnotation = (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateAnnotation);
            return updateAnnotation;
        } catch (Exception e) {
            log.error("Error occurred updating annotations", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/annotation/details")
    @ApiOperation(value = "gets a list of Annotations",
            notes = "Gets a list of Annotations using the references paramter of the DomainQuery object"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got the list of annotations",response = Annotation.class,
                    responseContainer = "List" ),
            @ApiResponse( code = 500, message = "Internal Server Error getting the list of Annotations" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Annotation> getAnnotations(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("getAnnotations(DomainQuery [subjectKey={}, references={} items])",query.getSubjectKey(),query.getReferences().size());
            log.trace("getAnnotations({})",query);
            List<Annotation> annotations = dao.getAnnotations(query.getSubjectKey(), query.getReferences());
            return annotations;
        } catch (Exception e) {
            log.error("Error occurred getting annotations", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/annotation")
    @ApiOperation(value = "Removes an Annotation",
            notes = "Removes an annotation using the Annotation Id"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed the annotation" ),
            @ApiResponse( code = 500, message = "Internal Server Error removing the annotation" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeAnnotations(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                                  @ApiParam @QueryParam("annotationId") final String annotationId) {
        DomainDAL dao = DomainDAL.getInstance();
        Reference annotationRef = Reference.createFor(Annotation.class, new Long(annotationId));
        try {
            log.debug("removeAnnotations({},{})",subjectKey,annotationId);
            DomainObject deleteAnnotation = dao.getDomainObject(subjectKey, annotationRef);
            IndexingHelper.sendRemoveFromIndexMessage(deleteAnnotation.getId());
            dao.deleteDomainObject(subjectKey, deleteAnnotation);
        } catch (Exception e) {
            log.error("Error occurred removing annotations", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}