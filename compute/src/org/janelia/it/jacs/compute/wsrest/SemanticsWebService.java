package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
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
            return (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating annotations\n " + e.getMessage());
            return null;
        }
    }

    @POST
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation updateAnnotation(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            return (Annotation)dao.save(query.getSubjectKey(), query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred updating annotations\n " + e.getMessage());
            return null;
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
            log.error("Error occurred getting annotations\n " + e.getMessage());
            return null;
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
            dao.remove(subjectKey, dao.getDomainObject(subjectKey, annotationRef));
        } catch (Exception e) {
            log.error("Error occurred removing annotations\n " + e.getMessage());
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
            e.printStackTrace();
        }
        return null;
    }

    @PUT
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology createOntology(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        System.out.println ("TRACE NONO");
        try {
            return (Ontology)dao.save(query.getSubjectKey(), query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating ontology\n " + e.getMessage());
            return null;
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
            System.out.println (ont);
            dao.remove(subjectKey, ont);
        } catch (Exception e) {
            log.error("Error occurred removing ontology\n " + e.getMessage());
            e.printStackTrace();
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
            return dao.addTerms(query.getSubjectKey(), ontologyId, parentId, terms, query.getOrdering().get(0));
        } catch (Exception e) {
            log.error("Error occurred adding ontology terms\n " + e.getMessage());
            e.printStackTrace();
            return null;
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
            return dao.reorderTerms(query.getSubjectKey(), ontologyId, parentId, order);
        } catch (Exception e) {
            log.error("Error occurred reordering ontology\n " + e.getMessage());
            e.printStackTrace();
            return null;
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
            System.out.println (ontologyId + ":" + parentTermId + ":" + termId);
            return dao.removeTerm(subjectKey, ontologyId, parentTermId, termId);
        } catch (Exception e) {
            log.error("Error occurred removing ontology terms\n " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}