package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
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

    @POST
    @Path("/annotation")
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
    public void removeAnnotations(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            // TODO: fix this. We are trying to eliminate use of bare GUIDs
            List<Annotation> annotationList = dao.getAnnotations(query.getSubjectKey(), query.getReferences());
            for (Annotation annotation : annotationList) {
                dao.remove(query.getSubjectKey(), annotation);
            }
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
}