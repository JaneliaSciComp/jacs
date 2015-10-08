package org.janelia.it.jacs.compute.wsrest.computeresources;

import com.google.common.base.Joiner;
import org.apache.commons.httpclient.HttpStatus;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.mip.MIPGenerationTask;
import org.janelia.it.jacs.model.tasks.mip.MIPInputImageData;
import org.janelia.it.jacs.model.user_data.mip.MIPGenerationResultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * This is implements a RESTful service for MIP Generation.
 *
 * Created by goinac on 9/2/15.
 */
@Path("/data")
public class MIPGenerationServiceResource extends AbstractComputationResource<MIPGenerationTask, MIPGenerationResultNode> {
    private static final String RESOURCE_NAME = "MIPGeneration";
    private static final Logger LOG = LoggerFactory.getLogger(MIPGenerationServiceResource.class);

    public MIPGenerationServiceResource() {
        super(RESOURCE_NAME);
    }

    /**
     * Post request to trigger the Maximum Intensity Projection process.
     * @param owner
     * @param mipGenerationTask
     * @param req
     * @return
     * @throws ProcessingException
     */
    @POST
    @Path("/{owner}/images/mips")
    @Consumes({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task post(@PathParam("owner") String owner, MIPGenerationTask mipGenerationTask, @Context Request req) throws ProcessingException {
        LOG.info("MIP generation requested by {} with {}", owner, mipGenerationTask);
        mipGenerationTask.setOwner(owner);
        validateRequest(mipGenerationTask);
        MIPGenerationTask persistedTask = init(mipGenerationTask);
        submitJob(persistedTask);
        return persistedTask;
    }

    private void validateRequest(MIPGenerationTask mipGenerationTask) throws ProcessingException {
        List<MIPInputImageData> inputImages = mipGenerationTask.getInputImages();
        if (inputImages == null || inputImages.size() == 0) {
            throw new ProcessingException(HttpStatus.SC_BAD_REQUEST, "No input image");
        }
        List<String> validationMessages = new ArrayList<>();
        for (MIPInputImageData inputImage : inputImages) {
            if (!inputImage.hasFilepath()) {
                validationMessages.add("Input image entry has not filepath.\n");
            }
            if (!inputImage.hasColorSpec()) {
                validationMessages.add(String.format("Entry for %s must have a color spec.\n", inputImage.filepath));
            }
            if (!inputImage.hasChanSpec()) {
                validationMessages.add(String.format("Entry for %s must have a channel spec.\n", inputImage.filepath));
            }
        }
        if (!validationMessages.isEmpty()) {
            LOG.warn("MIP Generation validation errors: {}", validationMessages);
            throw new ProcessingException(HttpStatus.SC_BAD_REQUEST, Joiner.on('\n').join(validationMessages));
        }
    }

    @Override
    protected MIPGenerationResultNode createResultNode(MIPGenerationTask task, String visibility) {
        return new MIPGenerationResultNode(task.getOwner(),
                task,
                "MIPGenerationResultNode",
                "MIPResultNode for " + task.getObjectId(),
                visibility,
                null/*session*/);
    }

    @Override
    protected Map<String, Object> prepareProcessConfiguration(MIPGenerationTask task) throws ProcessingException {
        Map<String, Object> processConfig = super.prepareProcessConfiguration(task);
        processConfig.put("INPUT_IMAGES", extractInputImagesFromTask(task));
        processConfig.put("OUTPUT_FILE_NODE", processConfig.get(ProcessDataConstants.RESULT_FILE_NODE));
        processConfig.put("OUTPUTS", task.getOutputs());

        return processConfig;
    }

    private List<InputImage> extractInputImagesFromTask(MIPGenerationTask task) {
        List<InputImage> processInputImages = new ArrayList<>();
        if (task.getInputImages() != null) {
            for (MIPInputImageData mipInputImage : task.getInputImages()) {
                InputImage processInputImage = new InputImage();
                processInputImage.setFilepath(mipInputImage.filepath);
                processInputImage.setArea(mipInputImage.area);
                processInputImage.setOutputPrefix(mipInputImage.outputPrefix);
                processInputImage.setChanspec(mipInputImage.chanspec);
                processInputImage.setColorspec(mipInputImage.colorspec);
                processInputImage.setDivspec(mipInputImage.divspec);
                processInputImage.setLaser(mipInputImage.laser);
                processInputImage.setGain(mipInputImage.gain);
                processInputImage.setArea(mipInputImage.area);

                processInputImages.add(processInputImage);
            }
        }
        return processInputImages;
    }
}
