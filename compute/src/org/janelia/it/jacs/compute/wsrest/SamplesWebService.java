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
import javax.ws.rs.core.*;

public class SamplesWebService {
    @Context
    SecurityContext securityContext;

    @GET
    @Path("sample/{sampleId}/lsms/")
    @Produces("application/json")
    public Response getSampleLsms(@PathParam("sampleId") Long sampleId, @Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @GET
    @Path("sample/{sampleId}/fragments/")
    @Produces("application/json")
    public Response getSampleFragments(@PathParam("sampleId") Long sampleId, @Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @POST
    @Path("sample/{sampleId}/reprocess")
    public void reprocessSample(@PathParam("sampleId") Long sampleId, @Context UriInfo uriInfo) {

    }

    @DELETE
    @Path("sample/{sampleId}")
    public void deleteSample(@PathParam("sampleId") Long sampleId, @Context UriInfo uriInfo) {

    }

    @GET
    @Path("file")
    @Produces("application/json")
    public Response getFile(@Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @PUT
    @Path("folder")
    public void addFolder(@Context UriInfo uriInfo) {

    }

    @POST
    @Path("folder")
    public void updateFolder(@Context UriInfo uriInfo) {

    }

    @DELETE
    @Path("folder")
    public void deleteFolder(@Context UriInfo uriInfo) {

    }
}