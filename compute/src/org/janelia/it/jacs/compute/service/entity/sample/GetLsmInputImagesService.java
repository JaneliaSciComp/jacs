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

        String areas = "";
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            
            InputImage inputImage1 = getInputImage(mergedLsmPair.getLsmEntityId1());
            areas += inputImage1.getArea();
            inputImages.add(inputImage1);
            
            if (mergedLsmPair.getLsmEntityId2() != null) {
                InputImage inputImage2 = getInputImage(mergedLsmPair.getLsmEntityId2());
                areas += inputImage2.getArea();
                inputImages.add(inputImage2);    
            }
        }

        boolean normalize = false;
        if ("Brain,VNC".equalsIgnoreCase(areas) || "VNC,Brain".equalsIgnoreCase(areas)) {
            normalize = true;
        }
        
    	logger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    	logger.info("Putting "+normalize+" into NORMALIZE_TO_FIRST_IMAGE");
    	processData.putItem("NORMALIZE_TO_FIRST_IMAGE", Boolean.valueOf(normalize));
    }
    
    private InputImage getInputImage(Long lsmId) throws ComputeException {

        Entity lsm = entityBean.getEntityById(lsmId);
        
        String filepath = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String chanSpec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        String chanColors = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS);
        String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        File file = new File(filepath);
        
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
            
            logger.warn("LSM "+lsmId+" has illegal color specification: "+chanColors+" (interpreted as "+colorspec+")");
            
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
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec(divspec);
        inputImage.setOutputPrefix(file.getName().replace(".lsm", ""));
        inputImage.setArea(area);
        
        return inputImage;
    }
    
}
