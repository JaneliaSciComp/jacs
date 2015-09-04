package org.janelia.it.jacs.compute.wsrest.computeresources;

import org.hibernate.Session;
import org.janelia.it.jacs.compute.service.neuronSeparator.MIPGenerationService;
import org.janelia.it.jacs.compute.engine.data.ProcessData;

import org.janelia.it.jacs.model.tasks.SessionTask;
import org.janelia.it.jacs.model.tasks.Task;
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
@Path("/mips-service")
public class MIPGenerationServiceResource extends AbstractAsyncComputationResource {
    private static final String APP_ID = "MIPGenerator";
    private static final Logger LOG = LoggerFactory.getLogger(MIPGenerationServiceResource.class);

    public MIPGenerationServiceResource() {
        super(APP_ID, new MIPGenerationService());
    }

    @POST
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task post(ProcessData processData, @Context Request req) throws ProcessingException {
        Task task = init(processData);
        // TODO
        System.out.println("!!!!!!!!! PROCESS DATA" + processData);
        LOG.debug("!!!!!!!!! PROCESS DATA", processData);
        return task;
    }

}
