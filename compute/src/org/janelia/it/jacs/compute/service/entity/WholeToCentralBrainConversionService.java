package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Creates a central brain sample for every whole brain sample, and returns a list of the new central brains creatd.
 * Parameters must be provided in the ProcessData:
 *   ROOT_ENTITY_NAME Folder in which to create central brains
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WholeToCentralBrainConversionService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            
            createDate = new Date();

            String topLevelFolderName;
            Entity topLevelFolder;
            if (processData.getItem("ROOT_ENTITY_NAME") != null) {
            	topLevelFolderName = (String)processData.getItem("ROOT_ENTITY_NAME");
            	topLevelFolder = createOrVerifyRootEntityButDontLoadTree(topLevelFolderName);
            }
            else {
            	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
            	if (rootEntityId==null) {
            		throw new IllegalArgumentException("Both ROOT_ENTITY_NAME and ROOT_ENTITY_ID may not be null");
            	}
            	topLevelFolder = entityBean.getEntityById(rootEntityId);
            }
            
        	if (topLevelFolder==null) {
        		throw new IllegalArgumentException("Both ROOT_ENTITY_NAME and ROOT_ENTITY_ID may not be null");
        	}
        	
            logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
            
            boolean outputObjects = false;
        	
        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar == null) {
            	outvar = (String)processData.getItem("OUTVAR_ENTITY");
            	outputObjects = true;
            	if (outvar == null) {
            		throw new IllegalArgumentException("Both OUTVAR_ENTITY_ID and OUTVAR_ENTITY may not be null");
            	}
        	}
        	
        	List<Entity> wholeBrains = entityBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_TILING_PATTERN, TilingPattern.WHOLE_BRAIN.toString());
        	
    		logger.info("Found "+wholeBrains.size()+" whole brains. Filtering by owner, and creating central brains...");
    		
    		EntityUtils.replaceChildNodes(topLevelFolder, entityBean.getChildEntities(topLevelFolder.getId()));
    		Entity centralBrainFolder = verifyOrCreateChildFolder(topLevelFolder, "Central Brains");
    		EntityUtils.replaceChildNodes(centralBrainFolder, entityBean.getChildEntities(centralBrainFolder.getId()));
    		
    		List outObjects = new ArrayList();
        	for(Entity entity : wholeBrains) {
        		if (entity.getOwnerKey().equals(ownerKey)) {
        			Entity sample = ensureCorrespondingCentralBrainSampleExists(entity, centralBrainFolder);
        			if (sample!=null) {
        				outObjects.add(outputObjects ? sample : sample.getId());	
        			}
        		}
        	}

    		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
        	processData.putItem(outvar, outObjects);
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private Entity ensureCorrespondingCentralBrainSampleExists(Entity sample, Entity folder) throws Exception {
    	String centralName = sample.getName()+"_Central_Brain_Tiles";
    	logger.info("Ensure that central brain sample "+centralName+" exists in " +folder.getName());
    	
    	for(Entity child : folder.getChildren()) {
    		if (!EntityConstants.TYPE_SAMPLE.equals(child.getEntityType().getName())) continue;
    		if (child.getName().equals(centralName)) {
    			return child;
    		}
    	}
    	
        Entity newSample = createSample(centralName, TilingPattern.CENTRAL_BRAIN);
        addToParent(folder, newSample, null, EntityConstants.ATTRIBUTE_ENTITY);
    
        Entity newSupportingFiles = createSupportingFilesFolder();
		addToParent(newSample, newSupportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
     
		// Copy the LSMs that we want
		Entity supportingFiles = entityBean.getEntityTree(sample.getChildByAttributeName(EntityConstants.ATTRIBUTE_SUPPORTING_FILES).getId());
		
		for(Entity lsmStackPair : supportingFiles.getChildren()) {
			String name = lsmStackPair.getName();
			if ("Left Dorsal Brain".equals(name) || "Right Dorsal Brain".equals(name) || "Ventral Brain".equals(name)) {
				Entity newLsmStackPair = cloneLsmStackPair(lsmStackPair);
		    	addToParent(newSupportingFiles, newLsmStackPair, null, EntityConstants.ATTRIBUTE_ENTITY);
			}
		}
		
        return newSample;
	}

    protected Entity createSupportingFilesFolder() throws Exception {
    	Entity filesFolder = newEntity("Supporting Files", EntityConstants.TYPE_SUPPORTING_DATA);
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    private Entity cloneLsmStackPair(Entity lsmStackPair) throws Exception {
    	Entity newLsmStackPair = newEntity(lsmStackPair.getName(), EntityConstants.TYPE_IMAGE_TILE);
		newLsmStackPair = entityBean.saveOrUpdateEntity(newLsmStackPair);
        logger.info("Saved LSM stack pair for '"+newLsmStackPair.getName()+"' as "+newLsmStackPair.getId());
    	
		Entity lsmEntity1 = lsmStackPair.getChildByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_1);
		logger.info("Adding LSM file to sample: "+lsmEntity1.getName());
		Entity newLsmEntity1 = cloneLsmStack(lsmEntity1);
        addToParent(newLsmStackPair, newLsmEntity1, 0, EntityConstants.ATTRIBUTE_LSM_STACK_1);

        Entity lsmEntity2 = lsmStackPair.getChildByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_2);
		logger.info("Adding LSM file to sample: "+lsmEntity2.getName());
		Entity newLsmEntity2 = cloneLsmStack(lsmEntity2);
        addToParent(newLsmStackPair, newLsmEntity2, 1, EntityConstants.ATTRIBUTE_LSM_STACK_2);
        
        return newLsmStackPair;
    }
    
    private Entity cloneLsmStack(Entity lsmStack) throws Exception {
    	Entity newLsmStack = newEntity(lsmStack.getName(), EntityConstants.TYPE_LSM_STACK);
        newLsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        newLsmStack = entityBean.saveOrUpdateEntity(newLsmStack);
        logger.info("Saved LSM stack as "+newLsmStack.getId());
        return newLsmStack;
    }
    
    protected Entity createSample(String name, TilingPattern tiling) throws Exception {
        Entity sample = newEntity(name, EntityConstants.TYPE_SAMPLE);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        return sample;
    }

    protected Entity createOrVerifyRootEntityButDontLoadTree(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, ownerKey, createDate, logger, true /* create if necessary */, false /* load tree */);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, ownerKey, createDate, logger, true /* create if necessary */, true);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName, String ownerKey, Date createDate, org.apache.log4j.Logger logger, boolean createIfNecessary, boolean loadTree) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getOwnerKey().equals(ownerKey)
                        && entity.getEntityType().getName().equals(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER).getName())
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    // This is the folder we want, now load the entire folder hierarchy
                    if (loadTree) {
                        topLevelFolder = entityBean.getEntityTree(entity.getId());
                    } else {
                        topLevelFolder = entity;
                    }
                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                    break;
                }
            }
        }

        if (topLevelFolder == null) {
            if (createIfNecessary) {
                logger.info("Creating new topLevelFolder with name=" + topLevelFolderName);
                topLevelFolder = newEntity(topLevelFolderName, EntityConstants.TYPE_FOLDER);
                EntityUtils.addAttributeAsTag(topLevelFolder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
                topLevelFolder = entityBean.saveOrUpdateEntity(topLevelFolder);
                logger.info("Saved top level folder as " + topLevelFolder.getId());
            } else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
    }
    
    protected Entity verifyOrCreateChildFolder(Entity parentFolder, String childName) throws Exception {

        Entity folder = null;
        
        for (Entity child : parentFolder.getChildren()) {
            if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                if (child.getName().equals(childName)) {
                    if (folder != null) {
                    	logger.warn("Unexpectedly found multiple child folders with name=" + childName+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                    	folder = child;	
                    }
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            folder = newEntity(childName, EntityConstants.TYPE_FOLDER);
            folder = entityBean.saveOrUpdateEntity(folder);
            addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
    
    protected Entity newEntity(String name, String type) throws ComputeException {
        Entity e = new Entity();
        e.setCreationDate(createDate);
        e.setUpdatedDate(createDate);
        e.setOwnerKey(ownerKey);
        e.setName(name);
        e.setEntityType(entityBean.getEntityTypeByName(type));
        return e;
    }

}
