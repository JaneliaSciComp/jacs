package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFODataUpgradeService implements IService {

    public transient static final String PARAM_testRun = "is test run";
	
    protected Logger logger;
    protected Task task;
    protected String username;
    protected AnnotationBeanLocal annotationBean;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected EntityHelper entityHelper;
    
    protected int numEntities;
    protected int numChanges;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            username = task.getOwner();
            isDebug = Boolean.parseBoolean(task.getParameter(PARAM_testRun));
            entityHelper = new EntityHelper(isDebug);
            
            final String serverVersion = computeBean.getAppVersion();
            logger.info("Updating data model to latest version: "+serverVersion);
            
            if (isDebug) {
            	logger.info("This is a test run. No entities will be moved or deleted.");
            }
            else {
            	logger.info("This is the real thing. Entities will be moved and/or deleted!");
            }
            
            processCommonRootFolders();
    	}
        catch (Exception e) {
			logger.info("Encountered an exception. Before dying, we...");
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running MCFODataUpgradeService", e);
        }
        finally {
			logger.info("Processed "+numEntities+" entities.");
        }
    }

    public void processCommonRootFolders() throws ComputeException {
        List<Entity> entities=annotationBean.getCommonRootEntitiesByTypeName(username, EntityConstants.TYPE_FOLDER);
        for(Entity topEntity : entities) {
            logger.info("Found top-level entity name="+topEntity.getName());
            Entity tree = entityBean.getEntityTree(topEntity.getId());
            processEntityTree(tree);
        }
		logger.info("The surgery was a success.");
    }
    
    public void processEntityTree(Entity entity) throws ComputeException {
    	
    	if (visited.contains(entity.getId())) return;
    	visited.add(entity.getId());
    	
    	if (!entity.getUser().getUserLogin().equals(username)) return;

		logger.info("Processing "+entity.getName()+" (id="+entity.getId()+")");
		numEntities++;

    	String entityTypeName = entity.getEntityType().getName();
    	if (entityTypeName.equals(EntityConstants.TYPE_SAMPLE)) {
    		migrateSampleLevelLsmPairs(entity);
        	migrateDefault2dImages(entity);
    	}
    	
		for(Entity child : entity.getChildren()) {
			processEntityTree(child);
		}
    }
    
    /**
     * Check for Samples with LSM pairs as direct children, and move the LSMs into Supporting Data.
     */
    private void migrateSampleLevelLsmPairs(Entity sample) throws ComputeException {

    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
    	logger.info("migrateSampleLevelLsmPairs for sample "+sample.getName());
    	
    	List<EntityData> toMove = new ArrayList<EntityData>();
		for(EntityData ed : sample.getEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null && child.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK_PAIR)) {
				toMove.add(ed);
			}
		}
		
		if (!toMove.isEmpty()) {
			logger.info("Found old-style sample, id="+sample.getId()+" name="+sample.getName());
			
            Entity supportingFiles = EntityUtils.getSupportingData(sample);
        	if (supportingFiles == null) {
            	// Update in-memory model
        		supportingFiles = entityHelper.createSupportingFilesFolder(username);
    			EntityData ed = sample.addChildEntity(supportingFiles, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	        if (!isDebug) {
                	// Update database
    	        	entityBean.saveOrUpdateEntityData(ed);
    	        }
        	}

    		for(EntityData ed : toMove) {
                logger.info("    Moving ed="+ed.getId()+" to new parent "+supportingFiles.getId());
                if (!isDebug) {
                	// Update database
                	ed.setParentEntity(supportingFiles);
                	entityBean.saveOrUpdateEntityData(ed);
                	// Update in-memory model
                	supportingFiles.getEntityData().add(ed);
                	sample.getEntityData().remove(ed);
                }
                numChanges++;
    		}
		}
    }
    
    /**
     * Check for Default 2d Image Filepaths and instead link directly to the correct image entity.
     */
    protected void migrateDefault2dImages(Entity sample) throws ComputeException {

    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
    	logger.info("migrateDefault2dImages for sample "+sample.getName());
    	
        Entity latestSignalMIP = null;
        Entity latestReferenceMIP = null;
    	
    	for(Entity result : sample.getOrderedChildren()) {
        	String resultTypeName = result.getEntityType().getName();	
    		if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(resultTypeName)) {
    			logger.info("Processing result "+result.getName());
    			
    			Map<String,Entity> fragImages = new HashMap<String,Entity>();
    			
    	        Entity signalMIP = null;
    	        Entity referenceMIP = null; 	    		
	            Entity supportingFiles = EntityUtils.getSupportingData(result);
	            for(Entity fileEntity : supportingFiles.getOrderedChildren()) {
	            	if ("ConsolidatedSignalMIP.png".equals(fileEntity.getName())) {
	            		signalMIP = fileEntity;
	            	}
	            	else if ("ReferenceMIP.png".equals(fileEntity.getName())) {
	            		referenceMIP = fileEntity;
	            	}

	            	String fragNum = getIndex(fileEntity.getName());
	            	if (fragNum!=null) {
	            		if (fragImages.containsKey(fragNum)) {
	            			logger.warn("We already saw a fragment MIP for "+fragNum);
	            		}
	            		fragImages.put(fragNum, fileEntity);
	            	}
	            }

	            Entity fragments = EntityUtils.findChildWithName(result, "Neuron Fragments");
	            for(Entity fragmentEntity : fragments.getOrderedChildren()) {
	            	entityHelper.removeDefaultImageFilePath(fragmentEntity);
	            	String fragNumStr = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER); 
	            	Entity fragMip = fragImages.get(fragNumStr);
	            	if (fragMip==null) {
	            		logger.warn("Could not find MIP image for "+fragmentEntity.getName());
	            	}
	            	else {
	            		entityHelper.setDefault2dImage(fragmentEntity, fragMip);
	            	}	
	            }
	            
    			if (signalMIP!=null) logger.info("Found signalMIP, id="+signalMIP.getId());
    			if (referenceMIP!=null) logger.info("Found referenceMIP, id="+referenceMIP.getId());
    			
    			entityHelper.removeDefaultImageFilePath(result);    			
    			entityHelper.setImage(result, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
    			entityHelper.setImage(result, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
    			entityHelper.setDefault2dImage(result, signalMIP);
        		
	        	latestSignalMIP = signalMIP;
	        	latestReferenceMIP = referenceMIP;
    		}
    	}
    	
    	if (latestSignalMIP!=null) logger.info("Latest signalMIP, id="+latestSignalMIP.getId());
		if (latestReferenceMIP!=null) logger.info("Latest referenceMIP, id="+latestReferenceMIP.getId());
		
		entityHelper.removeDefaultImageFilePath(sample);
		entityHelper.setImage(sample, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, latestSignalMIP);
		entityHelper.setImage(sample, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, latestReferenceMIP);
		entityHelper.setDefault2dImage(sample, latestSignalMIP);
    }

    protected String getIndex(String filename) {
    	if (!filename.contains("neuronSeparatorPipeline.PR.neuron")) return null;
    	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));
    	if (mipNum.startsWith(".")) mipNum = mipNum.substring(1); 
		return mipNum;
    }
    
    
}
