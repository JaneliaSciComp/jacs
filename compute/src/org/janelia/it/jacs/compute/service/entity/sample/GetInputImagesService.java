package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.align.ImageStack;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Convert a list of image stacks to InputImages which can be used as parameters to other services. 
 * Required input parameters:
 *     IMAGE_STACKS - List<ImageStack>
 * or
 *     STACK_FILENAMES - List<String>
 *     CHANNEL_SPEC - channel specification
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetInputImagesService extends AbstractDomainService {

    public void execute() throws Exception {

        List<InputImage> inputImages = new ArrayList<>();
        
        Object stacksParamObj = data.getItem("IMAGE_STACKS");
        if (stacksParamObj==null) {
            
            Object stackFilenamesParamObj = data.getItem("STACK_FILENAMES");
            if (stackFilenamesParamObj==null) {
                throw new IllegalArgumentException("Input parameters IMAGE_STACKS and STACK_FILENAMES cannot both be null");
            }
            if (!(stackFilenamesParamObj instanceof List)) {
                throw new IllegalArgumentException("Input parameter STACK_FILENAMES must be a List");
            }
            String chanspec = data.getRequiredItemAsString("CHANNEL_SPEC");
            
            List<String> stackFilenames = (List<String>)stackFilenamesParamObj;
            for(String stackFilename : stackFilenames) {
                inputImages.add(getInputImage(stackFilename, chanspec));
            }
        }
        else {
            if (!(stacksParamObj instanceof List)) {
                throw new IllegalArgumentException("Input parameter STACK_FILENAMES must be a List");
            }
            List<ImageStack> stacks = (List<ImageStack>)stacksParamObj;
            for(ImageStack stack : stacks) {
                inputImages.add(getInputImage(stack));
            }
        }
        
        contextLogger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
        processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(ImageStack stack) throws ComputeException {
        String filepath = stack.getFilepath();
        String chanSpec = stack.getChannelSpec();
        return getInputImage(filepath, chanSpec);
    }
    
    private InputImage getInputImage(String filepath, String chanSpec) throws ComputeException {

        String prefix = FileUtils.getFilePrefix(filepath);
        String colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
        
        contextLogger.info("Input file: "+filepath);
        contextLogger.info("  Channel specification: "+chanSpec);
        contextLogger.info("  Color specification: "+colorspec);
        contextLogger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec("");
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
