package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Gets all the tiles for the given sample as InputImages which can be used as parameters to other services. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetTileInputImagesService extends AbstractEntityService {

    public void execute() throws Exception {

        String chanspec = data.getRequiredItemAsString("CHANNEL_SPEC");
        
        Object stackFilenamesParamObj = data.getRequiredItem("STACK_FILENAMES");
        if (!(stackFilenamesParamObj instanceof List)) {
            throw new IllegalArgumentException("Input parameter STACK_FILENAMES must be a List");
        }
        
        List<String> stackFilenames = (List<String>)stackFilenamesParamObj;
        
        List<InputImage> inputImages = new ArrayList<>();
        
        for(String stackFilename : stackFilenames) {
            inputImages.add(getInputImage(stackFilename, chanspec));
        }
        
    	logger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(String filepath, String chanSpec) throws ComputeException {

        String prefix = FileUtils.getFilePrefix(filepath);
        String colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec);
        
        logger.info("Input file: "+filepath);
        logger.info("  Channel specification: "+chanSpec);
        logger.info("  Color specification: "+colorspec);
        logger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec("");
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
