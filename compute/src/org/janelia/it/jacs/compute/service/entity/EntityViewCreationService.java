package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.mortbay.log.Log;

/**
 * Creates or updates a view of the given entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityViewCreationService implements IService {

    protected Logger logger;
    protected Task task;
    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            
        	String viewEntityName = (String)processData.getItem("VIEW_ENTITY_NAME");
        	if (viewEntityName == null) {
        		throw new IllegalArgumentException("VIEW_ENTITY_NAME may not be null");
        	}

        	List<Long> entityIdList = (List<Long>)processData.getItem("ENTITY_ID_LIST");
        	if (entityIdList == null) {
        		throw new IllegalArgumentException("ENTITY_ID_LIST may not be null");
        	}

        	Entity rootEntity = createOrVerifyRootEntity(viewEntityName);
        	if (rootEntity == null) {
        		throw new IllegalArgumentException("Root entity not found with id="+viewEntityName);
        	}
        	
        	List<Entity> entities = annotationBean.getEntitiesById(entityIdList);
        	if (entities.isEmpty()) {
        		logger.warn("No entities to add to view '"+viewEntityName+"'");
        	}
        	else {
        		logger.warn("Adding "+entities.size()+" entities to view '"+viewEntityName+"'");
        	}
        	
        	Collections.sort(entities, new Comparator<Entity>() {
				@Override
				public int compare(Entity o1, Entity o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});
        	
        	Set<Long> existingChildrenIds = new HashSet<Long>();
        	for(Entity child : rootEntity.getChildren()) {
        		existingChildrenIds.add(child.getId());
        	}
        		
        	for(Entity entity : entities) {
        		if (!existingChildrenIds.contains(entity.getId())) {
        			addToParent(rootEntity, entity, rootEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        		}
        	}
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
    	
        List<Entity> topLevelFolders = annotationBean.getCommonRootEntitiesByTypeName(user.getUserLogin(), EntityConstants.TYPE_FOLDER);
    	Entity topLevelFolder = null;
        if (topLevelFolders!=null) {
	        for(Entity entity : topLevelFolders) {
	        	if (entity.getName().equals(topLevelFolder)) {
	        		if (topLevelFolder!=null) {
	        			logger.warn("More than one top level folder with name "+topLevelFolderName+
	        					" found for user "+user.getUserLogin()+". Proceeding by picking the first.");
	        			break;
	        		}
	                // This is the folder we want, now load the entire folder hierarchy
	                topLevelFolder = annotationBean.getFolderTree(entity.getId());
	                logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
	        	}
	        }
        }
         
        if (topLevelFolder == null) {
            logger.info("Creating new topLevelFolder with name="+topLevelFolderName);
            topLevelFolder = new Entity();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            topLevelFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Saved top level view as "+topLevelFolder.getId());
        }
        
        logger.info("Using topLevelFolder with id="+topLevelFolder.getId());
        return topLevelFolder;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
}
