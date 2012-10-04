package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleAttributesService extends AbstractEntityService {

	private static final String DEFAULT_CHANNEL_SPEC = "sssr";
	
    public void execute() throws Exception {
        	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	String chanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    	if (chanSpec==null) {
    		chanSpec = DEFAULT_CHANNEL_SPEC;
    		logger.warn("Channel specification for sample is not specified. Assuming "+chanSpec);
    	}
    	else {
    		logger.info("Channel specification for sample "+sampleEntityId+" is "+chanSpec);	
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

    	String dataSetStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    	
    	if (dataSetStr==null) {
    		logger.warn("Sample is not part of a dataset, id="+sampleEntityId);
    	}
    	else {
        	List<String> dataSetList = new ArrayList<String>();
        	for (String dataSetIdentifier : dataSetStr.split(",")) {
        		dataSetList.add(dataSetIdentifier);
        	}
        	
        	logger.info("Putting ("+Task.csvStringFromCollection(dataSetList)+") in DATA_SET_IDENTIFIER");
    		processData.putItem("DATA_SET_IDENTIFIER", dataSetList);
    	}
    	
    	logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
    	processData.putItem("SIGNAL_CHANNELS", signalChannels.toString());
    	logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
    	processData.putItem("REFERENCE_CHANNEL", referenceChannels.toString());
    }
}
