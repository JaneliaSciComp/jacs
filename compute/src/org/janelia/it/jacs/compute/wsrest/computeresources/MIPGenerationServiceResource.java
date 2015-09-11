package org.janelia.it.jacs.compute.wsrest.computeresources;


import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.mip.AbstractMIPGenerationTask;
import org.janelia.it.jacs.model.tasks.mip.SingleMIPGenerationTask;
import org.janelia.it.jacs.model.user_data.mip.MIPGenerationResultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.util.Map;

/**
 * This is implements a RESTful service for MIP Generation.
 *
 * Created by goinac on 9/2/15.
 */
@Path("/mip")
public class MIPGenerationServiceResource extends AbstractComputationResource<AbstractMIPGenerationTask, MIPGenerationResultNode> {
    private static final String RESOURCE_NAME = "MIPGeneration";
    private static final Logger LOG = LoggerFactory.getLogger(MIPGenerationServiceResource.class);

    public MIPGenerationServiceResource() {
        super(RESOURCE_NAME);
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
        LOG.info("post single MIP task " + mipGenerationTask.getClass());
        AbstractMIPGenerationTask persistedTask = init(mipGenerationTask);
        submitJob(persistedTask);
        return persistedTask;
    }

    @Override
    protected MIPGenerationResultNode createResultNode(AbstractMIPGenerationTask task, String visibility) {
        return new MIPGenerationResultNode(task.getOwner(),
                task,
                "MIPGenerationResultNode",
                "MIPResultNode for " + task.getObjectId(),
                visibility,
                null/*session*/);
    }

    @Override
    protected Map<String, Object> prepareProcessConfiguration(AbstractMIPGenerationTask task) throws ProcessingException {
        Map<String, Object> processConfig = super.prepareProcessConfiguration(task);
        processConfig.put("INPUT_FILENAMES", task.getInputFileList());
        processConfig.put("SIGNAL_CHANNELS", task.getSignalChannels());
        processConfig.put("REFERENCE_CHANNEL", task.getReferenceChannel());
        return processConfig;
    }

}
