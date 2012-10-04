package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Discovers images in SAGE which are part of a particular image family and not part of a data set, and creates 
 * or updates Samples within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageImageFamilyDiscoveryService extends SageImageDiscoveryService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();

            String sageImageFamily = (String)processData.getItem("SAGE_IMAGE_FAMILY");
            if (sageImageFamily==null) {
        		throw new IllegalArgumentException("SAGE_IMAGE_FAMILY may not be null");
            }
            
			defaultChannelSpec = (String) processData.getItem("DEFAULT_CHANNEL_SPECIFICATION");
			if (defaultChannelSpec == null) {
				throw new IllegalArgumentException("DEFAULT_CHANNEL_SPECIFICATION may not be null");
			}
            
			if ("system".equals(user.getUserLogin())) {
	        	topLevelFolder = createOrVerifyRootEntityButDontLoadTree(PUBLIC_DATA_SET_FOLDER_NAME);
			}
			else {
	        	topLevelFolder = createOrVerifyRootEntityButDontLoadTree(PRIVATE_DATA_SET_FOLDER_NAME);
			}
            
            logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
            
            processSageDataSet(sageImageFamily, null);	
        
            fixOrderIndices();
            
            logger.info("Created "+numSamplesCreated+" samples, Added "+numSamplesAdded+" samples to their corresponding data set folders.");
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected ResultSetIterator getImageIterator(String criteria) throws Exception {
    	SageDAO sageDAO = new SageDAO(logger);
    	return sageDAO.getImagesByFamily(criteria);
    }
    
    @Override
    protected String getDataSetIdentifier(TilingPattern tiling) {

    	List<Entity> dataSets = entityBean.getUserEntitiesByNameAndTypeName(user.getUserLogin(), "FlyLight "+tiling.getName(), EntityConstants.TYPE_DATA_SET);
    	
    	if (dataSets.isEmpty()) {
    		throw new IllegalStateException("Could not find for tiling pattern "+tiling);
    	}
    	
    	if (dataSets.size()>1) {
    		logger.warn("Found more than one data set for tiling pattern "+tiling);
    	}
    	
    	return dataSets.get(0).getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    }
}
