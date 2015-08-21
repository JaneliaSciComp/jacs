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

public class SemanticsWebService {
    @Context
    SecurityContext securityContext;

    @GET
    @Path("ontology/{ontologyId}")
    @Produces("application/json")
    public Response getOntology(@PathParam("ontologyId") Long ontologyId, @Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @PUT
    @Path("ontology")
    public void addOntology(@Context UriInfo uriInfo) {

    }

    @POST
    @Path("ontology/term")
    public void addTermToOntology(@Context UriInfo uriInfo) {

    }

    @GET
    @Path("annotation")
    public Response getAnnotation(@Context UriInfo uriInfo) {
        Response response = null;

        return response;
    }

    @PUT
    @Path("annotation")
    public void addAnnotation(@Context UriInfo uriInfo) {

    }
}