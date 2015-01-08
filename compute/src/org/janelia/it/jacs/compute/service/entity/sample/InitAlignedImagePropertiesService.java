package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Reads an aligned image from the database and put its properties into the ProcessData as individual variables.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignedImagePropertiesService extends AbstractEntityService {
	
    public void execute() throws Exception {
    	
    	String imageId = (String)processData.getItem("IMAGE_ID");
    	if (StringUtils.isEmpty(imageId)) {
    		throw new IllegalArgumentException("RESULT_ID may not be null");
    	}
    	
    	Entity image = entityBean.getEntityById(imageId);
    	populateChildren(image);
    	
    	String filename = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	if (filename!=null) {
            logger.info("Putting '"+filename+"' in FILENAME");
            processData.putItem("FILENAME", filename);
    	}
        
    	String opticalRes = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
    	if (opticalRes!=null) {
            logger.info("Putting '"+opticalRes+"' in OPTICAL_RESOLUTION");
            processData.putItem("OPTICAL_RESOLUTION", opticalRes);
    	}
    	
    	String pixelRes = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        if (pixelRes!=null) {
            logger.info("Putting '"+pixelRes+"' in PIXEL_RESOLUTION");
            processData.putItem("PIXEL_RESOLUTION", pixelRes);
        }        
        
        String objective = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        if (objective!=null) {
            logger.info("Putting '"+objective+"' in OBJECTIVE");
            processData.putItem("OBJECTIVE", objective);
        }        

        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger, contextLogger);
        
        String chanSpec = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        final String signalChannels = sampleHelper.getSignalChannelIndexes(chanSpec);
        if (signalChannels!=null) {
            logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
            processData.putItem("SIGNAL_CHANNELS", signalChannels);
        }        
        final String referenceChannels = sampleHelper.getReferenceChannelIndexes(chanSpec);
        if (referenceChannels!=null) {
            logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
            processData.putItem("REFERENCE_CHANNEL", referenceChannels);
        }        
        
        Entity alignedLabel = image.getChildByAttributeName(EntityConstants.ATTRIBUTE_ALIGNED_CONSOLIDATED_LABEL);
        if (alignedLabel!=null) {
            String filepath = alignedLabel.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            logger.info("Putting '"+filepath+"' in ALIGNED_CONSOLIDATED_LABEL_FILEPATH");
            processData.putItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH", filepath);
        }        
    }
}
