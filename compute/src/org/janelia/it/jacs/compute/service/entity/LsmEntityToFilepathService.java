package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Extracts and outputs the two filepaths from an LsmPair entity. The parameter must be included in the ProcessData:
 *   LSMPAIR_ENTITY_ID
 * 
 * Output is produced in ProcessData as:
 *   LSM_FILENAME_1
 *   LSM_FILENAME_2
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LsmEntityToFilepathService implements IService {

    protected Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
        	Long lsmPairEntityId = (Long)processData.getItem("LSMPAIR_ENTITY_ID");
        	if (lsmPairEntityId == null) {
        		throw new IllegalArgumentException("LSMPAIR_ENTITY_ID may not be null");
        	}
        	
        	Entity lsmPairEntity = EJBFactory.getLocalEntityBean().getEntityTree(lsmPairEntityId);
        	
        	if (lsmPairEntity == null) {
        		throw new IllegalArgumentException("LsmPair entity not found with id="+lsmPairEntityId);
        	}
        	
        	boolean gotFirst = false;
        	for(EntityData ed : lsmPairEntity.getOrderedEntityData()) {
        		Entity lsmStack = ed.getChildEntity();
        		if (lsmStack != null && lsmStack.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
        			String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        			if (gotFirst) {
                    	processData.putItem("LSM_FILENAME_2", filepath);	
        			}
        			else {
                    	processData.putItem("LSM_FILENAME_1", filepath);
                    	gotFirst = true;
        			}	
        		}
        	}
        	
        	logger.info("Got LSM filenames: "+
        			processData.getItem("LSM_FILENAME_1")+","+
        			processData.getItem("LSM_FILENAME_2"));
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
