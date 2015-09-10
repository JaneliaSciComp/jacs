package org.janelia.it.jacs.compute.wsrest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.*;

import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Path("/")
public class DataViewsWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    public DataViewsWebService() {
        register(JacksonJsonProvider.class);
    }

    @POST
    @Path("/domainobject/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getObjectDetails(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<DomainObject> detailObjects = null;
            if (query.getReferences()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getReferences());
            } else if (query.getObjectIds()!=null) {
                detailObjects = dao.getDomainObjects(query.getSubjectKey(), query.getObjectType().toLowerCase(),
                        query.getObjectIds());
            }
            return mapper.writeValueAsString(detailObjects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/domainobject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateObjectProperty(DomainQuery query) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            DomainObject updateObj = null;
            // TO DO: add check that parameters are valid
            List<Long> objIds = query.getObjectIds();
            if (objIds!=null && objIds.size()>0) {
                updateObj = dao.updateProperty(query.getSubjectKey(), query.getObjectType(), objIds.get(0),
                        query.getPropertyName(), query.getPropertyValue());
            }
            return mapper.writeValueAsString(updateObj);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PUT
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFilter(@QueryParam("subjectKey") final String subjectKey,
                               Filter filter) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            Filter newFilter = dao.save(subjectKey, filter);
            return mapper.writeValueAsString(newFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateFilter(@QueryParam("subjectKey") final String subjectKey,
                               Filter filter) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        ObjectMapper mapper = new ObjectMapper();
        try {
            Filter newFilter = dao.save(subjectKey, filter);
            return mapper.writeValueAsString(newFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // data sets
    @GET
    @Path("/dataset/{datasetId}")
    @Produces("application/json")
    public Response getDataSet(@PathParam("datasetId") Long datasetId, @Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @PUT
    @Path("/dataset")
    public void setDataSet(@PathParam("workspaceId") Long workspaceId, @Context UriInfo uriInfo) {

    }

    @POST
    @Path("/dataset")
    public void updateDataSet(@PathParam("datasetId") Long datasetId, @Context UriInfo uriInfo) {

    }

    @DELETE
    @Path("/dataset")
    public void deleteDataSet(@PathParam("datasetId") Long datasetId, @Context UriInfo uriInfo) {

    }

    @GET
    @Path("/alignmentboard/{alignmentBoardId}")
    @Produces("application/json")
    public Response getAlignmentBoard(@PathParam("alignmentBoardId") Long alignmentBoardId, @Context UriInfo uriInfo) {
        String output = "Hello from";
        return Response.status(200).entity(output).build();
    }

    @PUT
    @Path("/alignmentboard")
    public void addAlignmentBoard(@Context UriInfo uriInfo) {

    }

    @POST
    @Path("/alignmentboard/{alignmentBoardId}")
    public void updateAlignmentBoard(@PathParam("alignmentBoardId") Long alignmentBoardId, @Context UriInfo uriInfo) {

    }
}