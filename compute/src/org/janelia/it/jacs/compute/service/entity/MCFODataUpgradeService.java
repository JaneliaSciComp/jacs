package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
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
    protected ComputeBeanLocal computeBean;
    
    protected int numEntities;
    protected int numChanges;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            username = task.getOwner();
            isDebug = Boolean.parseBoolean(task.getParameter(PARAM_testRun));
            
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
			logger.info("Processed "+numEntities+" entities. Made "+numChanges+" changes.");
        }
    }

    public void processCommonRootFolders() throws ComputeException {
        List<Entity> entities=annotationBean.getCommonRootEntitiesByTypeName(username, EntityConstants.TYPE_FOLDER);
        for(Entity topEntity : entities) {
            logger.info("Found top-level entity name="+topEntity.getName());
            Entity tree = annotationBean.getEntityTree(topEntity.getId());
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
        		supportingFiles = createSupportingFilesFolder(username);
    			EntityData ed = sample.addChildEntity(supportingFiles, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	        if (!isDebug) {
                	// Update database
    	        	annotationBean.saveOrUpdateEntityData(ed);
    	        }
        	}

    		for(EntityData ed : toMove) {
                logger.info("    Moving ed="+ed.getId()+" to new parent "+supportingFiles.getId());
                if (!isDebug) {
                	// Update database
                	ed.setParentEntity(supportingFiles);
                	annotationBean.saveOrUpdateEntityData(ed);
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
	            	
	            	removeDefaultImage(fileEntity);
	            	removeDefaultImageFilePath(fileEntity);
	            }

	            Entity fragments = EntityUtils.findChildWithName(result, "Neuron Fragments");
	            for(Entity fragmentEntity : fragments.getOrderedChildren()) {
	            	removeDefaultImage(fragmentEntity);
	            	removeDefaultImageFilePath(fragmentEntity);
	            	String fragNumStr = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER); 
	            	Entity fragMip = fragImages.get(fragNumStr);
	            	if (fragMip==null) {
	            		logger.warn("Could not find MIP image for "+fragmentEntity.getName());
	            	}
	            	else {
	            		addDefaultImage(fragmentEntity, fragMip);
	            	}	
	            }
	            
    			if (signalMIP!=null) logger.info("Found signalMIP, id="+signalMIP.getId());
    			if (referenceMIP!=null) logger.info("Found referenceMIP, id="+referenceMIP.getId());
    			
	        	removeMIPs(result);
            	removeDefaultImage(result);
            	removeDefaultImageFilePath(result);
            	
            	addMIPs(result, signalMIP, referenceMIP);
            	addDefaultImage(result, signalMIP);
        		
	        	latestSignalMIP = signalMIP;
	        	latestReferenceMIP = referenceMIP;
    		}
    	}
    	
    	if (latestSignalMIP!=null) logger.info("Latest signalMIP, id="+latestSignalMIP.getId());
		if (latestReferenceMIP!=null) logger.info("Latest referenceMIP, id="+latestReferenceMIP.getId());
		
    	removeMIPs(sample);
    	removeDefaultImage(sample);
    	removeDefaultImageFilePath(sample);
    	
    	addMIPs(sample, latestSignalMIP, latestReferenceMIP);
		addDefaultImage(sample, latestSignalMIP);
    }

    protected String getIndex(String filename) {
    	if (!filename.contains("neuronSeparatorPipeline.PR.neuron")) return null;
    	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));
    	if (mipNum.startsWith(".")) mipNum = mipNum.substring(1); 
		return mipNum;
    }
    
    protected void removeDefaultImageFilePath(Entity entity) throws ComputeException {
    	EntityData filepathEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
    	if (filepathEd != null) {
            logger.info("Removing default 2d image filepath for id="+entity.getId()+" name="+entity.getName());
            if (!isDebug) {
            	// Update database
            	annotationBean.deleteEntityData(filepathEd);
            	// Update in-memory model
            	entity.getEntityData().remove(filepathEd);
            }
            numChanges++;
    	}
    }

    protected void removeDefaultImage(Entity entity) throws ComputeException {
    	logger.info("Removing MIP images for id="+entity.getId()+" name="+entity.getName());
        EntityData ed1 = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (ed1!=null) {
	        if (!isDebug) {
	        	// Update database
	        	annotationBean.deleteEntityData(ed1);
	        	// Update in-memory model
	        	entity.getEntityData().remove(ed1);
	        }
	        numChanges++;
        }
    }
    
    protected void removeMIPs(Entity entity) throws ComputeException {
    	logger.info("Removing MIP images for id="+entity.getId()+" name="+entity.getName());
        EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
        if (ed!=null) {
        	if (!isDebug) {
            	// Update database
	        	annotationBean.deleteEntityData(ed);
            	// Update in-memory model
	        	entity.getEntityData().remove(ed);
        	}
        	numChanges++;
        }
        ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
        if (ed!=null) {
        	if (!isDebug) {
	        	// Update database
	        	annotationBean.deleteEntityData(ed);
	        	// Update in-memory model
	        	entity.getEntityData().remove(ed);
        	}
	        numChanges++;
        }
        
    }

    protected void addDefaultImage(Entity entity, Entity defaultImage) throws ComputeException {
        logger.info("Adding default image to id="+entity.getId()+" name="+entity.getName());
        if (defaultImage != null) {
	    	// Update in-memory model
	    	String filepath = defaultImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	    	EntityData ed = entity.addChildEntity(defaultImage, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
	    	ed.setValue(filepath);
	    	// Update database
	    	if (!isDebug) {
	    		EntityData savedEd = annotationBean.saveOrUpdateEntityData(ed);
	    		EntityUtils.replaceEntityData(entity, ed, savedEd);
	    	}
	        numChanges++;
        }
        else {
            logger.info("No default image available!");
        }
    }
    
    protected void addMIPs(Entity entity, Entity signalMIP, Entity referenceMIP) throws ComputeException {
        logger.info("Adding MIPs to id="+entity.getId()+" name="+entity.getName());
        if (signalMIP != null) {
        	// Update in-memory model
        	String signalMIPfilepath = signalMIP.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	EntityData ed = entity.addChildEntity(signalMIP, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
        	ed.setValue(signalMIPfilepath);
        	// Update database
        	if (!isDebug) {
        		EntityData savedEd = annotationBean.saveOrUpdateEntityData(ed);
        		EntityUtils.replaceEntityData(entity, ed, savedEd);
        	}
	        numChanges++;
        }
        else {
            logger.info("  No signal MIP available!");
        }
        if (referenceMIP != null) {
        	// Update in-memory model
        	String referenceMIPfilepath = referenceMIP.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	EntityData ed = entity.addChildEntity(referenceMIP, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
        	ed.setValue(referenceMIPfilepath);
        	// Update database
        	if (!isDebug) {
        		EntityData savedEd = annotationBean.saveOrUpdateEntityData(ed);
        		EntityUtils.replaceEntityData(entity, ed, savedEd);
        	}
	        numChanges++;
        }
        else {
            logger.info("  No reference MIP available!");
        }
    }
    
    protected Entity createSupportingFilesFolder(String username) throws ComputeException {
        Entity filesFolder = new Entity();
        filesFolder.setUser(computeBean.getUserByName(username));
        filesFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        Date createDate = new Date();
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
    	// Update database
        if (!isDebug) filesFolder = annotationBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        numChanges++;
    	// Return the new object so that we can update in-memory model
        return filesFolder;
    }
    
}
