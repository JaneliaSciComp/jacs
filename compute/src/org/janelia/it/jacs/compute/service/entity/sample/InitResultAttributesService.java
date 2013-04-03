package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitResultAttributesService extends AbstractEntityService {

    public void execute() throws Exception {
        	        
    	String resultEntityId = (String)processData.getItem("RESULT_ENTITY_ID");
    	if (resultEntityId == null || "".equals(resultEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity resultEntity = entityBean.getEntityById(resultEntityId);
    	if (resultEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+resultEntityId);
    	}
    	
    	if (!EntityConstants.TYPE_SAMPLE.equals(resultEntity.getEntityType().getName())) {
    		throw new IllegalArgumentException("Entity is not a sample: "+resultEntityId);
    	}
    	
    	logger.info("Retrieved result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
    	Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	logger.info("Retrieved image: "+default3dImage.getName()+" (id="+default3dImage.getId()+")");
    	
        String chanSpec = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (chanSpec==null) {
            throw new IllegalStateException("Channel specification is not specified for result's default 3d image: "+resultEntityId);
        }
        else {
            logger.info("Channel specification for sample "+resultEntityId+" is "+chanSpec);    
        }
        
        StringBuffer signalChannels = new StringBuffer();
        StringBuffer referenceChannels = new StringBuffer();
        
        for(int i=0; i<chanSpec.length(); i++) {
            char chanCode = chanSpec.charAt(i);
            switch (chanCode) {
            case 's':
                if (signalChannels.length()>0) signalChannels.append(" ");
                signalChannels.append(i+"");
                break;
            case 'r':
                if (referenceChannels.length()>0) referenceChannels.append(" ");
                referenceChannels.append(i+"");
                break;
            default:
                logger.warn("Unknown channel code: "+chanCode);
                break;
            }
        }

        String opticalRes = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pixelRes = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        
        logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
        processData.putItem("SIGNAL_CHANNELS", signalChannels.toString());
        logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
        processData.putItem("REFERENCE_CHANNEL", referenceChannels.toString());
        logger.info("Putting '"+opticalRes+"' in OPTICAL_RESOLUTION");
        processData.putItem("OPTICAL_RESOLUTION", opticalRes);
        logger.info("Putting '"+pixelRes+"' in PIXEL_RESOLUTION");
        processData.putItem("PIXEL_RESOLUTION", pixelRes);
    }
}
