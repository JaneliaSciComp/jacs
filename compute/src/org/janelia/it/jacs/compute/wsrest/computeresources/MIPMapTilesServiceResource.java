package org.janelia.it.jacs.compute.wsrest.computeresources;


import org.janelia.it.jacs.compute.wsrest.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.mip.MIPMapTilesTask;
import org.janelia.it.jacs.model.user_data.mip.MIPMapTilesResultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * This is implements a RESTful service for MIP Generation.
 *
 * Created by goinac on 9/2/15.
 */
@Path("/data")
public class MIPMapTilesServiceResource extends AbstractComputationResource<MIPMapTilesTask, MIPMapTilesResultNode> {
    private static final Logger LOG = LoggerFactory.getLogger(MIPMapTilesServiceResource.class);
    private static final String RESOURCE_NAME = "MIPMapTiles";

    public MIPMapTilesServiceResource() {
        super(RESOURCE_NAME);
    }

    @POST
    @Path("/{owner}/images/mipmaps")
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Response post(@PathParam("owner") String owner, MIPMapTilesTask mipMapTilesTask, @Context Request req) throws ProcessingException {
        LOG.info("3d mapping requested by {} with {}", owner, mipMapTilesTask);
        mipMapTilesTask.setOwner(owner);
        MIPMapTilesTask persistedTask = init(mipMapTilesTask);
        submitJob(persistedTask);
        return Response
                .status(Response.Status.CREATED)
                .entity(new JsonTask(persistedTask))
                .build();
    }

    @Override
    protected MIPMapTilesResultNode createResultNode(MIPMapTilesTask task, String visibility) {
        return new MIPMapTilesResultNode(task.getOwner(),
                task,
                "MIPMapTilesResultNode",
                "MIPMapTilesResultNode for " + task.getObjectId(),
                visibility,
                null/*session*/);
    }

    @Override
    protected Map<String, Object> prepareProcessConfiguration(MIPMapTilesTask task) throws ProcessingException {
        Map<String, Object> processConfig = super.prepareProcessConfiguration(task);
        processConfig.put("IMAGE_WIDTH", task.getImageWidth());
        processConfig.put("IMAGE_HEIGHT", task.getImageHeight());
        processConfig.put("IMAGE_DEPTH", task.getImageDepth());
        processConfig.put("SOURCE_ROOT_URL", task.getSourceRootUrl());
        processConfig.put("SOURCE_STACK_FORMAT", task.getSourceStackFormat());
        processConfig.put("SOURCE_MAGNIFICATION_LEVEL", task.getSourceMagnificationLevel());
        processConfig.put("SOURCE_TILE_WIDTH", task.getSourceTileWidth());
        processConfig.put("SOURCE_TILE_HEIGHT", task.getSourceTileHeight());
        processConfig.put("SOURCE_XY_RESOLUTION", task.getSourceXYResolution());
        processConfig.put("SOURCE_Z_RESOLUTION", task.getSourceZResolution());
        processConfig.put("SOURCE_MIN_X", task.getSourceMinX());
        processConfig.put("SOURCE_MIN_Y", task.getSourceMinY());
        processConfig.put("SOURCE_MIN_Z", task.getSourceMinZ());
        processConfig.put("SOURCE_WIDTH", task.getSourceWidth());
        processConfig.put("SOURCE_HEIGHT", task.getSourceHeight());
        processConfig.put("SOURCE_DEPTH", task.getSourceDepth());
        processConfig.put("TARGET_ROOT_URL", task.getTargetRootUrl());
        processConfig.put("TARGET_STACK_FORMAT", task.getTargetStackFormat());
        processConfig.put("TARGET_TILE_WIDTH", task.getTargetTileWidth());
        processConfig.put("TARGET_TILE_HEIGHT", task.getTargetTileHeight());
        processConfig.put("TARGET_MIN_ROW", task.getTargetMinRow());
        processConfig.put("TARGET_MAX_ROW", task.getTargetMaxRow());
        processConfig.put("TARGET_MIN_COL", task.getTargetMinCol());
        processConfig.put("TARGET_MAX_COL", task.getTargetMaxCol());
        processConfig.put("TARGET_MIN_Z", task.getTargetMinZ());
        processConfig.put("TARGET_MAX_Z", task.getTargetMaxZ());
        processConfig.put("TARGET_QUALITY", task.getTargetQuality());
        processConfig.put("TARGET_TYPE", task.getTargetType());
        processConfig.put("TARGET_MEDIA_FORMAT", task.getTargetMediaFormat());
        processConfig.put("TARGET_SKIP_EMPTY_TILES", task.getSkipEmptyTiles());
        return processConfig;
    }

}
