package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.jersey.server.model.Resource;
import javax.ws.rs.core.*;

@Path("/")
public class DataViewsWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;


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

    // filters
    @GET
    @Path("/filter")
    @Produces("application/json")
    public Response getFilter(@Context UriInfo uriInfo) {
        return Response.status(200).entity("foo foo").build();
    }

    @PUT
    @Path("/filter")
    public void setFilter(@PathParam("filterId") Long filterId, @Context UriInfo uriInfo) {

    }

    @POST
    @Path("/filter")
    public void updateFilter(@PathParam("filterId") Long filterId, @Context UriInfo uriInfo) {

    }

    @DELETE
    @Path("/filter")
    public void deleteFilter(@PathParam("filterId") Long filterId, @Context UriInfo uriInfo) {

    }

    // objectset
    @GET
    @Path("/objectset/{objectsetId}")
    @Produces("application/json")
    public Response getObjectSet(@PathParam("objectsetId") Long objectsetId, @Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @PUT
    @Path("/objectset")
    public void addObjectSet(@Context UriInfo uriInfo) {

    }

    @POST
    @Path("/objectset/reorder")
    public void updateObjectSetOrder(@PathParam("objectsetId") Long objectsetId, @Context UriInfo uriInfo) {

    }

    @PUT
    @Path("/objectset/object")
    public void addObjectToObjectSet(@PathParam("objectsetId") Long objectsetId, @Context UriInfo uriInfo) {

    }

    @DELETE
    @Path("/objectset/object/{objectId}")
    public void deleteObjectFromObjectSet(@PathParam("objectId") Long objectsetId, @Context UriInfo uriInfo) {

    }

    @GET
    @Path("/alignmentboard/{alignmentBoardId}")
    @Produces("application/json")
    public Response getAlignmentBoard(@PathParam("alignmentBoardId") Long alignmentBoardId, @Context UriInfo uriInfo) {
        System.out.println ("AAAAA");
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