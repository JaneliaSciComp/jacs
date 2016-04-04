package org.janelia.it.jacs.compute.wsrest.data;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.Reference;
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
public class OntologyWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(OntologyWebService.class);

    @Context
    SecurityContext securityContext;

    public OntologyWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Ontology> getOntologies(@QueryParam("subjectKey") final String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            log.debug("getOntologies({})",subjectKey);
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
            log.debug("createOntology({})",query);
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
            log.debug("removeOntology({},{})",subjectKey,ontologyId);
            Ontology ont = (Ontology)dao.getDomainObject(subjectKey, ontologyRef);
            IndexingHelper.sendRemoveFromIndexMessage(ont.getId());
            dao.remove(subjectKey, ont);
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
            log.debug("addTermsToOntology({})",query);
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
            log.debug("reorderOntology({})",query);
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
            log.debug("removeTermsFromOntology({},{},{},{})",subjectKey,ontologyId,parentTermId,termId);
            Ontology updateOntology = dao.removeTerm(subjectKey, ontologyId, parentTermId, termId);
            IndexingHelper.sendReindexingMessage(updateOntology);
            return updateOntology;
        } catch (Exception e) {
            log.error("Error occurred removing ontology terms " + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}