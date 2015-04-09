package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Extracts stuff about the result from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitResultAttributesService extends AbstractEntityService {

    public void execute() throws Exception {
        	        
    	String resultEntityId = (String)processData.getItem("RESULT_ENTITY_ID");
    	if (resultEntityId == null || "".equals(resultEntityId)) {
    		throw new IllegalArgumentException("RESULT_ENTITY_ID may not be null");
    	}
    	
    	Entity resultEntity = entityBean.getEntityById(resultEntityId);
    	if (resultEntity == null) {
    		throw new IllegalArgumentException("Result entity not found with id="+resultEntityId);
    	}
    	
    	logger.info("Retrieved result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
    	
    	populateChildren(resultEntity);
    	Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	
    	if (default3dImage==null) {
    	    throw new IllegalStateException("Result entity has no default 3d image: "+resultEntityId);
    	}
    	
    	logger.info("Retrieved image: "+default3dImage.getName()+" (id="+default3dImage.getId()+")");
    	
    	logger.info("Putting '"+default3dImage.getId()+"' in DEFAULT_IMAGE_ID");
        processData.putItem("DEFAULT_IMAGE_ID", default3dImage.getId().toString());
    	
        String chanSpec = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (chanSpec==null) {
            logger.warn("No channel specification for result (id="+resultEntityId+")");    
        }
        else {
            logger.info("Channel specification for result (id="+resultEntityId+") is "+chanSpec);    
            String signalChannels = ChanSpecUtils.getSignalChannelIndexes(chanSpec);
            logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
            processData.putItem("SIGNAL_CHANNELS", signalChannels);
            String referenceChannels = ChanSpecUtils.getReferenceChannelIndexes(chanSpec);
            logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
            processData.putItem("REFERENCE_CHANNEL", referenceChannels);            
        }

        String opticalRes = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pixelRes = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        logger.info("Putting '"+opticalRes+"' in OPTICAL_RESOLUTION");
        processData.putItem("OPTICAL_RESOLUTION", opticalRes);
        logger.info("Putting '"+pixelRes+"' in PIXEL_RESOLUTION");
        processData.putItem("PIXEL_RESOLUTION", pixelRes);
    }
}
