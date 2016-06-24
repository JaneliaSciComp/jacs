package org.janelia.it.jacs.compute.wsrest.data;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
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
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class OntologyWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(OntologyWebService.class);

    @Context
    SecurityContext securityContext;

    public OntologyWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/ontology")
    @ApiOperation(value = "Gets all the ontologies for a user",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully returned Ontologies", response=Ontology.class,
                    responseContainer = "List"),
            @ApiResponse( code = 500, message = "Internal Server Error getting list of Ontologies" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Ontology> getOntologies(@ApiParam @QueryParam("subjectKey") final String subjectKey) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("getOntologies({})",subjectKey);
            return dao.getOntologies(subjectKey);
        } catch (Exception e) {
            log.error("Error occurred getting ontology",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/ontology")
    @ApiOperation(value = "Creates An Ontology",
            notes = "Uses the DomainObject parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully created an Ontology", response=Ontology.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating an Ontology" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology createOntology(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("createOntology({})",query);
            return dao.save(query.getSubjectKey(), (Ontology)query.getDomainObject());
        } catch (Exception e) {
            log.error("Error occurred creating ontology",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/ontology")
    @ApiOperation(value = "Removes An Ontology",
            notes = "Uses the ontologyId to remove the ontology"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed an Ontology"),
            @ApiResponse( code = 500, message = "Internal Server Error removing an ontology" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeOntology(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                               @ApiParam @QueryParam("ontologyId") final String ontologyId) {
        DomainDAL dao = DomainDAL.getInstance();
        Reference ontologyRef = Reference.createFor(Ontology.class, new Long(ontologyId));
        try {
            log.debug("removeOntology({},{})",subjectKey,ontologyId);
            Ontology ont = (Ontology)dao.getDomainObject(subjectKey, ontologyRef);
            dao.deleteDomainObject(subjectKey, ont);
        } catch (Exception e) {
            log.error("Error occurred removing ontology",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/ontology/terms")
    @ApiOperation(value = "Adds Terms to an Ontology",
            notes = "Uses the ObjectId parameter of the DomainQuery (1st object is the ontology id," +
                    " second object is the parent id) and serialized JSON list of OntologyTerm as the " +
                    "ObjectList parameter."
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully added terms to Ontology", response=Ontology.class),
            @ApiResponse( code = 500, message = "Internal Server Error adding terms to an Ontology" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology addTermsToOntology(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("addTermsToOntology({})",query);
            List<Long> objectIds = query.getObjectIds();
            Long ontologyId = objectIds.get(0);
            Long parentId = objectIds.get(1);
            List<OntologyTerm> terms = new ArrayList<>();
            for (OntologyTerm term : query.getObjectList()) {
                terms.add(term);
            }
            return dao.addOntologyTerms(query.getSubjectKey(), ontologyId, parentId, terms, query.getOrdering().get(0));
        } catch (Exception e) {
            log.error("Error occurred adding ontology terms",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/ontology/terms")
    @ApiOperation(value = "Reorders Terms in an Ontology",
            notes = "Uses the ObjectId parameter of the DomainQuery (1st object is the ontology id," +
                    " second object is the parent id) and for ordering the Ordering parameter of the DomainQuery."
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully reordered terms in Ontology", response=Ontology.class),
            @ApiResponse( code = 500, message = "Internal Server Error reordered terms in Ontology" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology reorderOntology(@ApiParam DomainQuery query) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("reorderOntology({})",query);
            List<Long> objectIds = query.getObjectIds();
            Long ontologyId = objectIds.get(0);
            Long parentId = objectIds.get(1);
            int[] order = new int[query.getOrdering().size()];
            for (int i=0; i<order.length; i++) {
                order[i] = query.getOrdering().get(i);
            }
            return dao.reorderOntologyTerms(query.getSubjectKey(), ontologyId, parentId, order);
        } catch (Exception e) {
            log.error("Error occurred reordering ontology",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/ontology/terms")
    @ApiOperation(value = "Removes Terms from an Ontology",
            notes = "Uses the ontologyId, parentTermId to find the Ontology. " +
                    "The termId is the id in the ontology to remove."
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed a term from Ontology", response=Ontology.class),
            @ApiResponse( code = 500, message = "Internal Server Error removed term from Ontology" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Ontology removeTermsFromOntology(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                                            @ApiParam @QueryParam("ontologyId") final Long ontologyId,
                                            @ApiParam @QueryParam("parentTermId") final Long parentTermId,
                                            @ApiParam @QueryParam("termId") final Long termId) {
        DomainDAL dao = DomainDAL.getInstance();
        try {
            log.debug("removeTermsFromOntology({},{},{},{})",subjectKey,ontologyId,parentTermId,termId);
            return dao.removeOntologyTerm(subjectKey, ontologyId, parentTermId, termId);
        } catch (Exception e) {
            log.error("Error occurred removing ontology terms",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}