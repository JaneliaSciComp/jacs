package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FijiColor;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the LSMs for the given sample as InputImages which can be used as parameters to other services. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetLsmInputImagesService extends AbstractDomainService {

    public void execute() throws Exception {

        AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
        List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();
        
        List<InputImage> inputImages = new ArrayList<>();
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            inputImages.add(getInputImage(mergedLsmPair.getLsmEntityId1(), mergedLsmPair.getFilepath1()));
            if (mergedLsmPair.getLsmEntityId2() != null) {
                inputImages.add(getInputImage(mergedLsmPair.getLsmEntityId2(), mergedLsmPair.getFilepath2()));    
            }
        }
        
    	contextLogger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(Long lsmId, String filepath) throws ComputeException {

        LSMImage lsm = domainDao.getDomainObject(ownerKey, LSMImage.class, lsmId);

        String prefix = FileUtils.getFilePrefix(filepath);
        String chanSpec = lsm.getChanSpec();
        String chanColors = lsm.getChannelColors();
        String area = lsm.getAnatomicalArea();
        
        if (chanSpec==null) {
            throw new ComputeException("Channel specification attribute is null for LSM id="+lsmId);
        }
        
        // Attempt to use the colors stored in the LSM
        String colorspec = "";
        String divspec = "";
        if (chanColors!=null) {
            List<String> colors = Task.listOfStringsFromCsvString(chanColors);
            
            int i = 0;
            for(String hexColor : colors) {
                if (i>=chanSpec.length()) {
                    logger.warn("More colors ('"+chanColors+"') than channels ('"+chanSpec+"') in LSM id="+lsmId);
                    break;
                }
                char type = chanSpec.charAt(i);
                FijiColor fc = ChanSpecUtils.getColorCode(hexColor, type);
                colorspec += fc.getCode();
                divspec += fc.getDivisor();
                i++;
            }
        }
        
        // If there are any uncertainties, default to RGB1
        if (StringUtils.isEmpty(colorspec) || colorspec.contains("?")) {
            String invalidColorspec = colorspec;
            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
            divspec = chanSpec.replaceAll("r", "2").replaceAll("s", "1");
            logger.warn("LSM "+lsmId+" has illegal color specification "+chanColors+" (interpreted as "+invalidColorspec+"). Defaulting to "+colorspec);
        }
        
        contextLogger.info("Input file: "+filepath);
        contextLogger.info("  Area: "+area);
        contextLogger.info("  Channel specification: "+chanSpec);
        contextLogger.info("  Color specification: "+colorspec);
        contextLogger.info("  Divisor specification: "+divspec);
        contextLogger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setArea(area);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec(divspec);
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
