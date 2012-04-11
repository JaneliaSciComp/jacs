package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.User;
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
    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected IProcessData processData;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
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
            	topLevelFolder = annotationBean.getEntityById(rootEntityId);
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
        	
        	List<Entity> wholeBrains = annotationBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_TILING_PATTERN, TilingPattern.WHOLE_BRAIN.toString());
        	
    		logger.info("Found "+wholeBrains.size()+" whole brains. Filtering by owner, and creating central brains...");
    		
    		EntityUtils.replaceChildNodes(topLevelFolder, annotationBean.getChildEntities(topLevelFolder.getId()));
    		Entity centralBrainFolder = verifyOrCreateChildFolder(topLevelFolder, "Central Brains");
    		EntityUtils.replaceChildNodes(centralBrainFolder, annotationBean.getChildEntities(centralBrainFolder.getId()));
    		
    		List outObjects = new ArrayList();
        	for(Entity entity : wholeBrains) {
        		if (entity.getUser().getUserLogin().equals(user.getUserLogin())) {
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
		Entity supportingFiles = annotationBean.getEntityTree(sample.getChildByAttributeName(EntityConstants.ATTRIBUTE_SUPPORTING_FILES).getId());
		
		for(Entity lsmStackPair : supportingFiles.getChildren()) {
			String name = lsmStackPair.getName();
			if ("Left Dorsal Brain".equals(name) || "Right Dorsal Brain".equals(name) || "Ventral Brain".equals(name)) {
				Entity newLsmStackPair = cloneLsmStackPair(lsmStackPair);
		    	addToParent(newSupportingFiles, newLsmStackPair, null, EntityConstants.ATTRIBUTE_ENTITY);
			}
		}
		
        return sample;
	}

    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = annotationBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    private Entity cloneLsmStackPair(Entity lsmStackPair) throws Exception {

		Entity newLsmStackPair = new Entity();
		newLsmStackPair.setUser(user);
		newLsmStackPair.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
		newLsmStackPair.setCreationDate(createDate);
		newLsmStackPair.setUpdatedDate(createDate);
		newLsmStackPair.setName(lsmStackPair.getName());
		newLsmStackPair = annotationBean.saveOrUpdateEntity(newLsmStackPair);
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
        Entity newLsmStack = new Entity();
        newLsmStack.setUser(user);
        newLsmStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        newLsmStack.setCreationDate(createDate);
        newLsmStack.setUpdatedDate(createDate);
        newLsmStack.setName(lsmStack.getName());
        newLsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        newLsmStack = annotationBean.saveOrUpdateEntity(newLsmStack);
        logger.info("Saved LSM stack as "+newLsmStack.getId());
        return newLsmStack;
    }
    
    protected Entity createSample(String name, TilingPattern tiling) throws Exception {
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        sample = annotationBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        return sample;
    }

    protected Entity createOrVerifyRootEntityButDontLoadTree(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, false /* load tree */);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, true);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName, User user, Date createDate, org.apache.log4j.Logger logger, boolean createIfNecessary, boolean loadTree) throws Exception {
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getUser().getUserLogin().equals(user.getUserLogin())
                        && entity.getEntityType().getName().equals(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER).getName())
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    // This is the folder we want, now load the entire folder hierarchy
                    if (loadTree) {
                        topLevelFolder = annotationBean.getFolderTree(entity.getId());
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
                topLevelFolder = new Entity();
                topLevelFolder.setCreationDate(createDate);
                topLevelFolder.setUpdatedDate(createDate);
                topLevelFolder.setUser(user);
                topLevelFolder.setName(topLevelFolderName);
                topLevelFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
                topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
                topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
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
        
        for (EntityData ed : parentFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
            
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                if (child.getName().equals(childName)) {
                    if (folder != null) {
                    	logger.warn("Unexpectedly found multiple child folders with name=" + childName+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                    	folder = ed.getChildEntity();	
                    }
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setUser(user);
            folder.setName(childName);
            folder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder = annotationBean.saveOrUpdateEntity(folder);
            addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }

}
