package org.janelia.it.jacs.compute.service.entity;

import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

public class EntityHelper {

    protected Logger logger = Logger.getLogger(EntityHelper.class);
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected boolean isDebug = true;
    
	public EntityHelper(boolean isDebug) {
        entityBean = EJBFactory.getLocalEntityBean();
        computeBean = EJBFactory.getLocalComputeBean();
        this.isDebug = isDebug;
	}
    
	public void removeDefaultImageFilePath(Entity entity) throws ComputeException {
    	EntityData filepathEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
    	if (filepathEd != null) {
            logger.info("Removing default 2d image filepath for id="+entity.getId()+" name="+entity.getName());
            if (!isDebug) {
            	// Update database
            	entityBean.deleteEntityData(filepathEd);
            	// Update in-memory model
            	entity.getEntityData().remove(filepathEd);
            }
    	}
    }

	public void removeDefaultImage(Entity entity) throws ComputeException {
    	logger.info("Removing default image for id="+entity.getId()+" name="+entity.getName());
        EntityData ed1 = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (ed1!=null) {
	        if (!isDebug) {
	        	// Update database
	        	entityBean.deleteEntityData(ed1);
	        	// Update in-memory model
	        	entity.getEntityData().remove(ed1);
	        }
        }
    }
    
	public void removeMIPs(Entity entity) throws ComputeException {
    	logger.info("Removing MIP images for id="+entity.getId()+" name="+entity.getName());
        EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
        if (ed!=null) {
        	if (!isDebug) {
            	// Update database
	        	entityBean.deleteEntityData(ed);
            	// Update in-memory model
	        	entity.getEntityData().remove(ed);
        	}
        }
        ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
        if (ed!=null) {
        	if (!isDebug) {
	        	// Update database
	        	entityBean.deleteEntityData(ed);
	        	// Update in-memory model
	        	entity.getEntityData().remove(ed);
        	}
        }
        
    }

	public void addDefaultImage(Entity entity, Entity defaultImage) throws ComputeException {
        logger.info("Adding default image to id="+entity.getId()+" name="+entity.getName());
        if (defaultImage != null) {
	    	// Update in-memory model
	    	String filepath = defaultImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	    	EntityData ed = entity.addChildEntity(defaultImage, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
	    	ed.setValue(filepath);
	    	// Update database
	    	if (!isDebug) {
	    		EntityData savedEd = entityBean.saveOrUpdateEntityData(ed);
	    		EntityUtils.replaceEntityData(entity, ed, savedEd);
	    	}
        }
        else {
            logger.info("No default image available!");
        }
    }
    
	public void addMIPs(Entity entity, Entity signalMIP, Entity referenceMIP) throws ComputeException {
        logger.info("Adding MIPs to id="+entity.getId()+" name="+entity.getName());
        if (signalMIP != null) {
        	// Update in-memory model
        	String signalMIPfilepath = signalMIP.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	EntityData ed = entity.addChildEntity(signalMIP, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
        	ed.setValue(signalMIPfilepath);
        	// Update database
        	if (!isDebug) {
        		EntityData savedEd = entityBean.saveOrUpdateEntityData(ed);
        		EntityUtils.replaceEntityData(entity, ed, savedEd);
        	}
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
        		EntityData savedEd = entityBean.saveOrUpdateEntityData(ed);
        		EntityUtils.replaceEntityData(entity, ed, savedEd);
        	}
        }
        else {
            logger.info("  No reference MIP available!");
        }
    }
    
	public Entity createSupportingFilesFolder(String username) throws ComputeException {
        Entity filesFolder = new Entity();
        filesFolder.setUser(computeBean.getUserByName(username));
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        Date createDate = new Date();
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
    	// Update database
        if (!isDebug) filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
    	// Return the new object so that we can update in-memory model
        return filesFolder;
    }
	
}
