package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
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
    protected FileDiscoveryHelper helper;
    
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
            helper = new FileDiscoveryHelper(entityBean, computeBean, username);
            
            final String serverVersion = computeBean.getAppVersion();
            logger.info("Updating data model to latest version: "+serverVersion);
            
            if (isDebug) {
            	logger.info("This is a test run. No entities will be moved or deleted.");
            }
            else {
            	logger.info("This is the real thing. Entities will be moved and/or deleted!");
            }
            
            processSamples();
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running MCFODataUpgradeService", e);
        }
        finally {
			logger.info("Processed "+numEntities+" entities.");
        }
    }

    public void processSamples() throws ComputeException {
    	List<Entity> samples = entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE);
        for(Entity sample : samples) {
            processSample(sample);
        }
		logger.info("The surgery was a success.");
    }
    
    public void processSample(Entity sample) throws ComputeException {
    	
    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
		numEntities++;
		
		entityBean.loadLazyEntity(sample, false);
		
		migrateSampleLevelLsmPairs(sample);
//        	migrateDefault2dImages(entity);
    	migrateSignalSpecs(sample);
    }
    
    /**
     * Check for old-style signal specs, and convert them to the new single-attribute format.
     * @param sample
     * @throws ComputeException
     */
    private void migrateSignalSpecs(Entity sample) throws ComputeException {
    	
    	EntityData refEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_CHANNEL);
    	EntityData sigEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_CHANNELS);
    	EntityData specEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    	
    	if (refEd!=null) {
        	logger.info("  Found old-style ref spec "+refEd.getValue()+" (id="+refEd.getId()+")");
        	sample.getEntityData().remove(refEd);
        	
        	if (sigEd!=null) {
	        	logger.info("  Found old-style signal spec "+sigEd.getValue()+" (id="+sigEd.getId()+")");
	        	sample.getEntityData().remove(sigEd);
        	}
        	
        	if (specEd!=null) {
        		logger.info("  Found new-style spec "+specEd.getValue()+" (id="+specEd.getId()+")");
	        	sample.getEntityData().remove(specEd);
        	}
        	
        	String spec = null;
        	if ("2".equals(refEd.getValue())) {
        		spec = "ssr";
        	}
        	else if ("3".equals(refEd.getValue())) {
        		spec = "sssr";
        	}
        	else {
        		logger.warn("  Unknown reference channel "+refEd.getValue()+" for sample "+sample.getId());
        	}
        	
        	if (spec!=null) {
            	logger.info("  Adding new signal spec '"+spec+"' and removing old-style specs.");
            	sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, spec);
                if (!isDebug) {
                	entityBean.saveOrUpdateEntity(sample);
                }
                numChanges++;
        	}
    	}
    }
    
    /**
     * Check for Samples with LSM pairs as direct children, and move the LSMs into Supporting Data.
     */
    private void migrateSampleLevelLsmPairs(Entity sample) throws ComputeException {
    	
    	List<EntityData> toMove = new ArrayList<EntityData>();
		for(EntityData ed : sample.getEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null && child.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK_PAIR)) {
				toMove.add(ed);
			}
		}
		
		if (!toMove.isEmpty()) {
			logger.info("Found old-style sample, id="+sample.getId()+" name="+sample.getName());
            Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(sample);

        	if (!EntityUtils.areLoaded(supportingFiles.getEntityData())) {
        		entityBean.loadLazyEntity(supportingFiles, false);
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
	            
	        	if (!EntityUtils.areLoaded(supportingFiles.getEntityData())) {
	        		entityBean.loadLazyEntity(supportingFiles, false);
	        	}
	            
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
	            	helper.removeDefaultImageFilePath(fragmentEntity);
	            	String fragNumStr = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER); 
	            	Entity fragMip = fragImages.get(fragNumStr);
	            	if (fragMip==null) {
	            		logger.warn("Could not find MIP image for "+fragmentEntity.getName());
	            	}
	            	else {
	            		helper.setDefault2dImage(fragmentEntity, fragMip);
	            	}	
	            }
	            
    			if (signalMIP!=null) logger.info("Found signalMIP, id="+signalMIP.getId());
    			if (referenceMIP!=null) logger.info("Found referenceMIP, id="+referenceMIP.getId());
    			
    			helper.removeDefaultImageFilePath(result);    			
    			helper.setImage(result, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
    			helper.setImage(result, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
    			helper.setDefault2dImage(result, signalMIP);
        		
	        	latestSignalMIP = signalMIP;
	        	latestReferenceMIP = referenceMIP;
    		}
    	}
    	
    	if (latestSignalMIP!=null) logger.info("Latest signalMIP, id="+latestSignalMIP.getId());
		if (latestReferenceMIP!=null) logger.info("Latest referenceMIP, id="+latestReferenceMIP.getId());
		
		helper.removeDefaultImageFilePath(sample);
		helper.setImage(sample, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, latestSignalMIP);
		helper.setImage(sample, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, latestReferenceMIP);
		helper.setDefault2dImage(sample, latestSignalMIP);
    }

    protected String getIndex(String filename) {
    	if (!filename.contains("neuronSeparatorPipeline.PR.neuron")) return null;
    	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));
    	if (mipNum.startsWith(".")) mipNum = mipNum.substring(1); 
		return mipNum;
    }
    
    
}
