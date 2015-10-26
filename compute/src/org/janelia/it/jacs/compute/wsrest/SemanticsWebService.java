package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.support.DomainDAO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Path("/")
public class SemanticsWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    public SemanticsWebService() {
        register(JacksonJsonProvider.class);
    }

    @GET
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getAnnotations(@QueryParam("subjectKey") final String subjectKey,
                                 @QueryParam("annotationIds") final List<Long> annotationIds) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Annotation> annotations = dao.getAnnotations(subjectKey, annotationIds);
            return mapper.writeValueAsString(annotations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @DELETE
    @Path("/annotation")
    @Consumes(MediaType.APPLICATION_JSON)
    public void getAnnotations(@QueryParam("subjectKey") final String subjectKey,
                                 @QueryParam("annotationId") final Long annotationId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Annotation> annotationList = dao.getAnnotations(subjectKey, annotationId);
            if (annotationList!=null && annotationList.size()==1) {
                dao.remove(subjectKey, annotationList.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("/ontology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getOntologies(@QueryParam("subjectKey") final String subjectKey) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            Collection<Ontology> ontologies = dao.getOntologies(subjectKey);
            return mapper.writeValueAsString(new ArrayList<Ontology>(ontologies));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}