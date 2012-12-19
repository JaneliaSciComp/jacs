package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A helper class for dealing with common entities such as default images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityHelper {

	private static final boolean DEBUG = false;
	
    protected Logger logger = Logger.getLogger(EntityHelper.class);
    
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
   
	public EntityHelper(String ownerKey) {
		this(EJBFactory.getLocalEntityBean(), EJBFactory.getLocalComputeBean(), ownerKey);
	}
	
    public EntityHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, String username) {
        this.entityBean = entityBean;
        this.computeBean  = computeBean;
        this.ownerKey = ownerKey;
    }
	
	/**
	 * Remove the old-style default 2d image file path. 
	 * @param entity
	 * @throws ComputeException
	 */
	public void removeDefaultImageFilePath(Entity entity) throws ComputeException {
		removeEntityDataForAttributeName(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
    }

	/**
	 * Add the default 3d image, and the default 3d image's shortcut images as the entity's shortcut images.
	 * @param entity
	 * @param default3dImage
	 * @throws ComputeException
	 */
	public void setDefault3dImage(Entity entity, Entity default3dImage) throws ComputeException {
		if (entity==null || default3dImage==null) return;
        setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, default3dImage);
        
        if (!EntityUtils.areLoaded(default3dImage.getEntityData())) {
        	entityBean.loadLazyEntity(default3dImage, false);
        }
        
        Entity default2dImage = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    	if (default2dImage!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
    	}
    	else {
        	// TODO: this shouldn't be necessary in the future
        	EntityData oldEd = default3dImage.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        	if (oldEd!=null) {
        		setDefault2dImage(entity, oldEd.getValue());	
        		removeDefaultImageFilePath(default3dImage);
        	}
    	}

    	Entity signalMip = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
    	if (signalMip!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
    	}

    	Entity refMip = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
    	if (refMip!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);	
    	}
		
    }

	/**
	 * Sets the given image as the default 2d image for the given entity.
	 * @param entity
	 * @param default2dImage
	 * @throws ComputeException
	 */
	public void setDefault2dImage(Entity entity, Entity default2dImage) throws ComputeException {
        setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
    }

	/**
	 * Sets the default 2d image for the given entity. This creates a new entity with the given filename as the name.
	 * @param entity
	 * @param defaultImageFilepath
	 * @throws ComputeException
	 */
	public Entity setDefault2dImage(Entity entity, String defaultImageFilepath) throws ComputeException {
        logger.debug("Adding Default 2D Image to "+entity.getName()+" (id="+entity.getId()+")");
        Entity default2dImage = create2dImage(defaultImageFilepath);
        setDefault2dImage(entity, default2dImage);
        return default2dImage;
    }

	/**
	 * Sets the given image as an image property to the given entity. Removes any existing images for that property.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void setImageIfNecessary(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null) return;
    	EntityData currImage = entity.getEntityDataByAttributeName(attributeName);
    	if (currImage==null || currImage.getChildEntity()==null || !currImage.getId().equals(image.getId())) {
    		setImage(entity, attributeName, image);	
    	}
	}
	
	/**
	 * Sets the given image as an image property to the given entity. Removes any existing images for that property.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void setImage(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null) return;
		removeEntityDataForAttributeName(entity, attributeName);
		addImage(entity, attributeName, image);
	}
	
	/**
	 * Adds the given image as an image property to the given entity.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void addImage(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null) return;
        logger.debug("Adding "+attributeName+" ("+image.getName()+") to "+entity.getName()+" (id="+entity.getId()+")");
    	if (DEBUG) return;
    	// Update in-memory model
    	String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	EntityData ed = entity.addChildEntity(image, attributeName);
    	ed.setValue(filepath);
    	// Update database
		EntityData savedEd=entityBean.saveOrUpdateEntityData(ed);
		EntityUtils.replaceEntityData(entity, ed, savedEd);
    }

	/**
	 * Remove the given image property. Does NOT delete the image that is pointed to. 
	 * @param entity
	 * @throws ComputeException
	 */
	public void removeEntityDataForAttributeName(Entity entity, String attributeName) throws ComputeException {
    	logger.debug("Removing "+attributeName+" for "+entity.getName()+" (id="+entity.getId()+")");
        if (DEBUG) return;
        
        Set<EntityData> toDelete = new HashSet<EntityData>();
        for(EntityData ed : entity.getEntityData()) {
        	if (ed.getEntityAttribute().getName().equals(attributeName)) {
        		toDelete.add(ed);
        	}
        }
        
        for (EntityData ed : toDelete) {
        	// Update database
            entityBean.deleteEntityData(ed);
        	// Update in-memory model
        	entity.getEntityData().remove(ed);
        }
    }
	
	/**
	 * Create and save a new 2d image entity with the given owner and filepath. 
	 * @param ownerKey
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create2dImage(String filepath) throws ComputeException {
		File file = new File(filepath);
		return createImage(EntityConstants.TYPE_IMAGE_2D, filepath, file.getName());
	}
	
	/**
	 * Create and save a new 2d image entity with the given owner and filepath. 
	 * @param ownerKey
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create2dImage(String filepath, String name) throws ComputeException {
		return createImage(EntityConstants.TYPE_IMAGE_2D, filepath, name);
	}

	/**
	 * Create and save a new 3d image entity with the given owner and filepath. 
	 * @param ownerKey
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create3dImage(String filepath) throws ComputeException {
		File file = new File(filepath);
		return createImage(EntityConstants.TYPE_IMAGE_3D, filepath, file.getName());
	}
	
	/**
	 * Create and save a new 3d image entity with the given owner and filepath. 
	 * @param ownerKey
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create3dImage(String filepath, String name) throws ComputeException {
		return createImage(EntityConstants.TYPE_IMAGE_3D, filepath, name);
	}
	
	public Entity createImage(String entityTypeName, String filepath, String name) throws ComputeException {
		
        Entity entity = new Entity();
        entity.setOwnerKey(ownerKey);
        entity.setEntityType(entityBean.getEntityTypeByName(entityTypeName));
        Date createDate = new Date();
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setName(name);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, filepath);

		File file = new File(filepath);
    	String filename = file.getName();
    	String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
    	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
        
    	// Update database
        if (!DEBUG) entity = entityBean.saveOrUpdateEntity(entity);
        logger.debug("Saved new "+entityTypeName+" as "+entity.getId());
    	// Return the new object so that we can update in-memory model
        return entity;
    }
	
}
