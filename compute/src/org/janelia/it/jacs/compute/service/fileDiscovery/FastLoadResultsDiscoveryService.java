package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for a list of fast load results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadResultsDiscoveryService implements IService {

    protected Logger logger;
    protected IProcessData processData;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected FileDiscoveryHelper helper;
    
	@Override
    public void execute(IProcessData processData) throws ServiceException {

		try {
	    	this.processData=processData;
	        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
	        entityBean = EJBFactory.getLocalEntityBean();
	        computeBean = EJBFactory.getLocalComputeBean();
	        String ownerKey = ProcessDataHelper.getTask(processData).getOwner();
	        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey);
	        
	        List<Entity> entityList = (List<Entity>)processData.getItem("SEPARATION_LIST");
	        if (entityList==null) {
	        	Entity entity = (Entity)processData.getItem("SEPARATION");
	        	if (entity==null) {
	        		String entityId = (String)processData.getItem("SEPARATION_ID");
	        		if (entityId==null) {
	        			throw new ServiceException("Both SEPARATION/SEPARATION_ID and SEPARATION_LIST may not be null");	
	        		}
	        		entity = entityBean.getEntityById(entityId);
	        	}
	        	entityList = new ArrayList<Entity>();
	        	entityList.add(entity);
	        }
	        
	        for(Entity entity : entityList) {
	        	
	        	if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
	        		logger.info("Not a neuron separation result: "+entity.getId());
	        		continue;
	        	}
	    	
	    		entityBean.loadLazyEntity(entity, true);
	    		
	        	String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	        	logger.info("Processing "+entity.getId()+" with path "+filepath);
	        	
	        	File resultDir = new File(filepath);
	        	File fastloadDir = new File(filepath, "fastLoad");
	        	
	        	if (resultDir.exists() && fastloadDir.exists()) {
	        		Entity filesFolder = EntityUtils.getSupportingData(entity);
	        		Entity fastLoadFolder = helper.createOrVerifyFolderEntity(filesFolder, "Fast Load", fastloadDir, 0);
	        		processFastLoadFolder(fastLoadFolder);
	        	}
	        }
	    } 
	    catch (Exception e) {
	        throw new ServiceException(e);
	    }
    }

    protected void processFastLoadFolder(Entity folder) throws Exception {
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

		helper.addFilesInDirToFolder(folder, dir);
    }
}
