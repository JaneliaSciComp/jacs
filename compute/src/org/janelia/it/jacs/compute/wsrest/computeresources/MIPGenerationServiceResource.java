package org.janelia.it.jacs.compute.wsrest.computeresources;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.mip.AbstractMIPGenerationTask;
import org.janelia.it.jacs.model.tasks.mip.BatchMIPGenerationTask;
import org.janelia.it.jacs.model.tasks.mip.SingleMIPGenerationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

/**
 * This is implements a RESTful service for MIP Generation.
 *
 * Created by goinac on 9/2/15.
 */
@Path("/mip")
public class MIPGenerationServiceResource extends AbstractAsyncComputationResource {
    private static final String APP_ID = "MIPGeneration";
    private static final Logger LOG = LoggerFactory.getLogger(MIPGenerationServiceResource.class);

    public MIPGenerationServiceResource() {
        super(APP_ID);
    }

    @POST
    @Path("/single")
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task post(SingleMIPGenerationTask mipGenerationTask, @Context Request req) throws ProcessingException {
        Task persistedTask = init(mipGenerationTask);
        // TODO
        System.out.println("!!!!!!!!! PROCESS DATA" + mipGenerationTask);
        LOG.debug("!!!!!!!!! PROCESS DATA", mipGenerationTask);
        return persistedTask;
    }

    @POST
    @Path("/batch")
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task post(BatchMIPGenerationTask mipGenerationTask, @Context Request req) throws ProcessingException {
        Task persistedTask = init(mipGenerationTask);
        // TODO
        System.out.println("!!!!!!!!! PROCESS DATA" + mipGenerationTask);
        LOG.debug("!!!!!!!!! PROCESS DATA", mipGenerationTask);
        return persistedTask;
    }

}
