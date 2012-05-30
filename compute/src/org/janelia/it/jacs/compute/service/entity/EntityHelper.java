package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
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
    protected boolean isDebug;
    
    public EntityHelper() {
    	this(false);
	}
    
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

	/**
	 * Remove the default 2d and 3d image references. Does NOT delete the images that are pointed to. 
	 * @param entity
	 * @throws ComputeException
	 */
	public void removeDefaultImages(Entity entity) throws ComputeException {
    	logger.info("Removing default images for id="+entity.getId()+" name="+entity.getName());
        if (isDebug) return;
        EntityData ed1 = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (ed1!=null) {
        	// Update database
        	entityBean.deleteEntityData(ed1);
        	// Update in-memory model
        	entity.getEntityData().remove(ed1);
        }
        EntityData ed2 = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (ed2!=null) {
        	// Update database
        	entityBean.deleteEntityData(ed2);
        	// Update in-memory model
        	entity.getEntityData().remove(ed2);
        }
    }
    
	/**
	 * Add the default 3d image, and the default 3d image's default 2d image as the default 2d image. Hah!
	 * @param entity
	 * @param default3dImage
	 * @throws ComputeException
	 */
	public void addDefaultImages(Entity entity, Entity default3dImage) throws ComputeException {
        addImage(entity, default3dImage, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        EntityData ed = default3dImage.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (ed!=null) {
        	if (!EntityUtils.isInitialized(ed.getChildEntity())) {
        		ed.setChildEntity(entityBean.getEntityById(""+ed.getChildEntity().getId()));
        	}
        	addImage(entity, ed.getChildEntity(), EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);	
        }
        else {
        	// TODO: this shouldn't be necessary in the future
        	EntityData oldEd = default3dImage.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        	if (oldEd!=null) {
        		addDefault2dImage(entity, oldEd.getValue());		
        	}
        }
    }

	/**
	 * Add the given image as the default 2d image for the given entity.
	 * @param entity
	 * @param defaultImage
	 * @throws ComputeException
	 */
	public void addDefault2dImage(Entity entity, Entity defaultImage) throws ComputeException {
        addImage(entity, defaultImage, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }

	/**
	 * This is not recommended. It's only for when you don't have an entity to point to, and only a filepath. It will
	 * automatically create a 2d image entity with the filename as the entity name, and point to that. 
	 */
	public void addDefault2dImage(Entity entity, String defaultImageFilepath) throws ComputeException {
        logger.info("Adding default 2d image to id="+entity.getId()+" name="+entity.getName());
        Entity default2dImage = create2dImage(entity.getUser().getUserLogin(), defaultImageFilepath);
        addDefault2dImage(entity, default2dImage);
    }
    
	/**
	 * Adds the given image as an image property to the given entity.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void addImage(Entity entity, Entity image, String attributeName) throws ComputeException {
        logger.info("Adding "+attributeName+" to id="+entity.getId()+" name="+entity.getName());
        if (image != null) {
	    	// Update in-memory model
	    	String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	    	EntityData ed = entity.addChildEntity(image, attributeName);
	    	ed.setValue(filepath);
	    	// Update database
	    	if (!isDebug) {
	    		EntityData savedEd = entityBean.saveOrUpdateEntityData(ed);
	    		EntityUtils.replaceEntityData(entity, ed, savedEd);
	    	}
        }
        else {
            logger.info("  No "+attributeName+" available");
        }
    }

	public void removeMIPs(Entity entity) throws ComputeException {
    	logger.info("Removing MIP images for id="+entity.getId()+" name="+entity.getName());
        EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
    	if (isDebug) return;
        if (ed!=null) {
        	// Update database
        	entityBean.deleteEntityData(ed);
        	// Update in-memory model
        	entity.getEntityData().remove(ed);
        }
        ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
        if (ed!=null) {
        	// Update database
        	entityBean.deleteEntityData(ed);
        	// Update in-memory model
        	entity.getEntityData().remove(ed);
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
            logger.info("  No signal MIP available");
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
            logger.info("  No reference MIP available");
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

	public Entity create2dImage(String username, String filepath) throws ComputeException {
		
		File file = new File(filepath);
		
        Entity entity = new Entity();
        entity.setUser(computeBean.getUserByName(username));
        entity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D));
        Date createDate = new Date();
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setName(file.getName());
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());

    	String filename = file.getName();
    	String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
    	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
        
    	// Update database
        if (!isDebug) entity = entityBean.saveOrUpdateEntity(entity);
        logger.info("Saved new 2d image as "+entity.getId());
    	// Return the new object so that we can update in-memory model
        return entity;
    }
	
}
