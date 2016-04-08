package org.janelia.it.jacs.compute.wsrest.data;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.wsrest.WebServiceContext;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;


@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class SampleWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SampleWebService.class);

    @Context
    SecurityContext securityContext;

    public SampleWebService() {
        register(JacksonFeature.class);
    }

    @GET
    @Path("/sample/lsms")
    @ApiOperation(value = "Gets a list of LSMImages for a Sample",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<LSMImage> getLsmsForSample(@QueryParam("subjectKey") final String subjectKey,
                                           @QueryParam("sampleId") final Long sampleId) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            Collection<LSMImage> lsms = dao.getLsmsBySampleId(subjectKey, sampleId);
            return new ArrayList<LSMImage>(lsms);
        } catch (Exception e) {
            log.error("Error occurred getting lsms for sample",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/sample")
    @ApiOperation(value = "Gets Samples given Id and Name",
            notes = "")
    @ApiResponses(value = {

    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List getSamples(@QueryParam("subjectKey") final String subjectKey,
                                     @QueryParam("sampleId") final Long sampleId,
                                     @QueryParam("sampleName") final String sampleName) {
        DomainDAO dao = WebServiceContext.getDomainManager();
        try {
            if (sampleId!=null) {
                return dao.getDomainObjectsByName(subjectKey, Sample.class, sampleName);
            } else {
                Reference ref = new Reference(Sample.class.getCanonicalName(), sampleId);
                List<Reference> refList = new ArrayList<>();
                refList.add(ref);
                return dao.getDomainObjects(subjectKey, refList);
            }
        } catch (Exception e) {
            log.error("Error occurred getting lsms for sample",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}