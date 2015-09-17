package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FijiColor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the LSMs for the given sample as InputImages which can be used as parameters to other services. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetLsmInputImagesService extends AbstractEntityService {

    public void execute() throws Exception {

        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
            throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (!(bulkMergeParamObj instanceof List)) {
            throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
        
        List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        
        List<InputImage> inputImages = new ArrayList<>();
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            inputImages.add(getInputImage(mergedLsmPair.getLsmEntityId1(), mergedLsmPair.getFilepath1()));
            if (mergedLsmPair.getLsmEntityId2() != null) {
                inputImages.add(getInputImage(mergedLsmPair.getLsmEntityId2(), mergedLsmPair.getFilepath2()));    
            }
        }
        
    	logger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(Long lsmId, String tempFilepath) throws ComputeException {

        Entity lsm = entityBean.getEntityById(lsmId);

        File file = new File(tempFilepath);
        String prefix = file.getName().replace(".lsm", "");
        String chanSpec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        String chanColors = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS);
        String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        
        // Attempt to use the colors stored in the LSM
        String colorspec = "";
        String divspec = "";
        if (chanColors!=null) {
            List<String> colors = Task.listOfStringsFromCsvString(chanColors);
            
            int i = 0;
            for(String hexColor : colors) {
                char type = chanSpec.charAt(i);
                FijiColor fc = ChanSpecUtils.getColorCode(hexColor, type);
                colorspec += fc.getCode();
                divspec += fc.getDivisor();
                i++;
            }
        }
        
        // If there are any uncertainties, default to RGB1
        if (StringUtils.isEmpty(colorspec) || colorspec.contains("?")) {
            
            logger.warn("LSM "+lsmId+" has illegal color specification "+chanColors+" (interpreted as "+colorspec+"). Defaulting to RGB.");
            
            List<String> tags = new ArrayList<String>();
            tags.add("R");
            tags.add("G");
            tags.add("B");
            StringBuilder csb = new StringBuilder();
            StringBuilder dsb = new StringBuilder();
            
            for(int i=0; i<chanSpec.length(); i++) {
                char type = chanSpec.charAt(i);
                if (type=='r') {
                    csb.append('1');
                    dsb.append('2');
                }
                else {
                    csb.append(tags.remove(0));
                    dsb.append('1');
                }
            }
            
            colorspec = csb.toString();
            divspec = dsb.toString();
        }
        
        logger.info("Input file: "+tempFilepath);
        logger.info("  Area: "+area);
        logger.info("  Channel specification: "+chanSpec);
        logger.info("  Color specification: "+colorspec);
        logger.info("  Divisor specification: "+divspec);
        logger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(tempFilepath);
        inputImage.setArea(area);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec(divspec);
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
