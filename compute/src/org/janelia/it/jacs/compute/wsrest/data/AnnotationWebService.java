package org.janelia.it.jacs.compute.wsrest.data;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/data")
public class AnnotationWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(AnnotationWebService.class);

    @Context
    SecurityContext securityContext;

    public AnnotationWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation createAnnotation(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("createAnnotation({})",query);
            Annotation newAnnotation = (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(newAnnotation);
            return newAnnotation;
        } catch (Exception e) {
            log.error("Error occurred creating annotations " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation updateAnnotation(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("updateAnnotation({})",query);
            Annotation updateAnnotation = (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateAnnotation);
            return updateAnnotation;
        } catch (Exception e) {
            log.error("Error occurred updating annotations" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/annotation/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Annotation> getAnnotations(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getAnnotations({})",query);
            List<Annotation> annotations = dao.getAnnotations(query.getSubjectKey(), query.getReferences());
            return annotations;
        } catch (Exception e) {
            log.error("Error occurred getting annotations" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeAnnotations(@QueryParam("subjectKey") final String subjectKey,
                                  @QueryParam("annotationId") final String annotationId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Reference annotationRef = new Reference (Annotation.class.getName(), new Long(annotationId));
        try {
            log.debug("removeAnnotations({},{})",subjectKey,annotationId);
            DomainObject deleteAnnotation = dao.getDomainObject(subjectKey, annotationRef);
            IndexingHelper.sendRemoveFromIndexMessage(deleteAnnotation.getId());
            dao.remove(subjectKey, deleteAnnotation);
        } catch (Exception e) {
            log.error("Error occurred removing annotations" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}