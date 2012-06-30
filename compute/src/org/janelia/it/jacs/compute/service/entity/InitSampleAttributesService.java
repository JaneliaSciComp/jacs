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
        	
        	processData.putItem("TILING_PATTERN", sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN));
        	processData.putItem("SIGNAL_CHANNELS", sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_CHANNELS));
        	processData.putItem("REFERENCE_CHANNEL", sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_CHANNEL));
        	
        	String types = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_TYPES);
        	if (types == null || "".equals(types)) {
        		types = "AUTO";
        	}
        	
        	List<String> alignmentTypes = Arrays.asList(types.split(" "));
        	processData.putItem("ALIGNMENT_TYPE", alignmentTypes);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
