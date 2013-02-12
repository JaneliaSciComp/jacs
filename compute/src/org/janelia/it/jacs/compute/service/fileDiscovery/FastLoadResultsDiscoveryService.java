package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for a list of fast load results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadResultsDiscoveryService extends AbstractEntityService {

    protected FileDiscoveryHelper helper;
    
	@Override
    public void execute() throws Exception {

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
        	try {
        		processSeparation(entity);
        	}
        	catch (Exception e) {
        		if (entityList.size()==1) {
        			throw e;
        		}
        		else {
        			logger.error("Results discovery failed for separation id="+entity.getId());	
        		}
        	}
        }
    }
	
	protected void processSeparation(Entity entity) throws Exception {

    	if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		logger.info("Not a neuron separation result: "+entity.getId());
    		return;
    	}
	
		entityBean.loadLazyEntity(entity, true);
		
    	String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	logger.info("Processing "+entity.getId()+" with path "+filepath);
    	
    	File resultDir = new File(filepath);
    	File fastloadDir = new File(filepath, "fastLoad");
    	
    	if (resultDir.exists() && fastloadDir.exists()) {
    		Entity filesFolder = EntityUtils.getSupportingData(entity);
    		
    		// Delete existing fast load results, if any 
    		Entity fastLoadFolder = EntityUtils.findChildWithName(filesFolder, "Fast Load");
    		if (fastLoadFolder!=null) {
    			entityBean.deleteEntityTree(ownerKey, fastLoadFolder.getId());
    		}
    		
    		fastLoadFolder = helper.createOrVerifyFolderEntity(filesFolder, "Fast Load", fastloadDir, 0);
    		processFastLoadFolder(fastLoadFolder);
    	}
	}

    protected void processFastLoadFolder(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        helper.addFileExclusion("*.metadata");
		helper.addFilesInDirToFolder(folder, dir);
    }
}
