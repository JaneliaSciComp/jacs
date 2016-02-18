package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.*;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class SemanticsWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SemanticsWebService.class);

    @Context
    SecurityContext securityContext;

    public SemanticsWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation createAnnotation(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
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
            DomainObject deleteAnnotation = dao.getDomainObject(subjectKey, annotationRef);
            dao.remove(subjectKey, deleteAnnotation);
            IndexingHelper.sendRemoveFromIndexMessage(deleteAnnotation);
        } catch (Exception e) {
            log.error("Error occurred removing annotations" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Ontology> getOntologies(@QueryParam("subjectKey") final String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Collection<Ontology> ontologies = dao.getOntologies(subjectKey);
            return new ArrayList<Ontology>(ontologies);
        } catch (Exception e) {
            log.error("Error occurred getting ontology" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology createOntology(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Ontology updateOntology = (Ontology)dao.save(query.getSubjectKey(), query.getDomainObject());
            IndexingHelper.sendReindexingMessage(updateOntology);
            return updateOntology;
        } catch (Exception e) {
            log.error("Error occurred creating ontology" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeOntology(@QueryParam("subjectKey") final String subjectKey,
                               @QueryParam("ontologyId") final String ontologyId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        Reference ontologyRef = new Reference (Ontology.class.getName(), new Long(ontologyId));
        try {
            Ontology ont = (Ontology)dao.getDomainObject(subjectKey, ontologyRef);
            dao.remove(subjectKey, ont);
            IndexingHelper.sendRemoveFromIndexMessage(ont);
        } catch (Exception e) {
            log.error("Error occurred removing ontology" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/ontology/terms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology addTermsToOntology(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<Long> objectIds = query.getObjectIds();
            Long ontologyId = objectIds.get(0);
            Long parentId = objectIds.get(1);
            List<OntologyTerm> terms = new ArrayList<>();
            for (OntologyTerm term : query.getObjectList()) {
                terms.add((OntologyTerm)term);
            }
            Ontology updateOntology = (Ontology)dao.addTerms(query.getSubjectKey(), ontologyId, parentId, terms, query.getOrdering().get(0));
            IndexingHelper.sendReindexingMessage(updateOntology);
            return updateOntology;
        } catch (Exception e) {
            log.error("Error occurred adding ontology terms" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/ontology/terms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology reorderOntology(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            List<Long> objectIds = query.getObjectIds();
            Long ontologyId = objectIds.get(0);
            Long parentId = objectIds.get(1);
            int[] order = new int[query.getOrdering().size()];
            for (int i=0; i<order.length; i++) {
                order[i] = query.getOrdering().get(i);
            }
            Ontology updateOntology =  dao.reorderTerms(query.getSubjectKey(), ontologyId, parentId, order);
            return updateOntology;
        } catch (Exception e) {
            log.error("Error occurred reordering ontology" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/ontology/terms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology removeTermsFromOntology(@QueryParam("subjectKey") final String subjectKey,
                                            @QueryParam("ontologyId") final Long ontologyId,
                                            @QueryParam("parentTermId") final Long parentTermId,
                                            @QueryParam("termId") final Long termId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Ontology updateOntology = dao.removeTerm(subjectKey, ontologyId, parentTermId, termId);
            IndexingHelper.sendReindexingMessage(updateOntology);
            return updateOntology;
        } catch (Exception e) {
            log.error("Error occurred removing ontology terms " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}