package org.janelia.it.jacs.compute.wsrest.computeresources;


import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.mip.MIP3dMapTask;
import org.janelia.it.jacs.model.user_data.mip.MIP3dMapResultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.util.Map;

/**
 * This is implements a RESTful service for MIP Generation.
 *
 * Created by goinac on 9/2/15.
 */
@Path("/data")
public class MIP3dMapServiceResource extends AbstractComputationResource<MIP3dMapTask, MIP3dMapResultNode> {
    private static final Logger LOG = LoggerFactory.getLogger(MIP3dMapServiceResource.class);
    private static final String RESOURCE_NAME = "MIP3dMap";

    public MIP3dMapServiceResource() {
        super(RESOURCE_NAME);
    }

    @POST
    @Path("/{owner}/images/3d-mapping")
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task post(@PathParam("owner") String owner, MIP3dMapTask mip3dMapTask, @Context Request req) throws ProcessingException {
        LOG.info("3d mapping requested by {} with {}", owner, mip3dMapTask);
        mip3dMapTask.setOwner(owner);
        MIP3dMapTask persistedTask = init(mip3dMapTask);
        submitJob(persistedTask);
        return persistedTask;
    }

    @Override
    protected MIP3dMapResultNode createResultNode(MIP3dMapTask task, String visibility) {
        return new MIP3dMapResultNode(task.getOwner(),
                task,
                "MIPMapResultNode",
                "MIPMapResultNode for " + task.getObjectId(),
                visibility,
                null/*session*/);
    }

    @Override
    protected Map<String, Object> prepareProcessConfiguration(MIP3dMapTask task) throws ProcessingException {
        Map<String, Object> processConfig = super.prepareProcessConfiguration(task);
        processConfig.put("INPUT_DIRECTORY", task.getInputDirectory());
        return processConfig;
    }

}
