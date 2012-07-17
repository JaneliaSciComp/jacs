package org.janelia.it.jacs.compute.service.entity;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleAttributesService implements IService {

    protected Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	if (sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_CHANNELS)!=null || sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_CHANNEL)!=null) {
        		logger.warn("Sample has 'Signal Channels' and/or 'Reference Channel' attributes. Run the MCFO Data Upgrade to convert them into 'Channel Specification'.");
        	}
        	 
        	String chanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        	logger.info("Channel specification for sample "+sampleEntityId+" is "+chanSpec);
        	
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
        	
        	String tilingPattern = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN);

        	String types = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_TYPES);
        	if (types == null || "".equals(types)) {
        		types = "AUTO";
        	}
        	List<String> alignmentTypes = Arrays.asList(types.split(" "));
        	
        	logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
        	processData.putItem("SIGNAL_CHANNELS", signalChannels.toString());
        	logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
        	processData.putItem("REFERENCE_CHANNEL", referenceChannels.toString());
        	logger.info("Putting '"+tilingPattern+"' in TILING_PATTERN");
        	processData.putItem("TILING_PATTERN", tilingPattern);
        	logger.info("Putting '"+Task.csvStringFromCollection(alignmentTypes)+"' in ALIGNMENT_TYPE");
        	processData.putItem("ALIGNMENT_TYPE", alignmentTypes);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
