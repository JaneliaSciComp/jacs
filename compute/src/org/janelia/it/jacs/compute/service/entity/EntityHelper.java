package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A helper class for dealing with common entities such as default images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityHelper {

    protected Logger logger = Logger.getLogger(EntityHelper.class);
    protected EntityBeanLocal entityBeanLocal;
    protected ComputeBeanLocal computeBeanLocal;
    protected EntityBeanRemote entityBeanRemote;
    protected ComputeBeanRemote computeBeanRemote;
    protected boolean isDebug;
    
    public EntityHelper() {
    	this(false);
	}
    
	public EntityHelper(boolean isDebug) {
        entityBeanLocal = EJBFactory.getLocalEntityBean();
        computeBeanLocal = EJBFactory.getLocalComputeBean();
        this.isDebug = isDebug;
	}
	
    public EntityHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean) {
		this(entityBean, computeBean, false);
	}

    public EntityHelper(EntityBeanRemote entityBean, ComputeBeanRemote computeBean) {
        this(entityBean, computeBean, false);
    }
    
	public EntityHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, boolean isDebug) {
		this.entityBeanLocal = entityBean;
        this.computeBeanLocal  = computeBean;
        this.isDebug = isDebug;
	}

    public EntityHelper(EntityBeanRemote entityBean, ComputeBeanRemote computeBean, boolean isDebug) {
        this.entityBeanRemote = entityBean;
        this.computeBeanRemote  = computeBean;
        this.isDebug = isDebug;
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
	 * Add the default 3d image, and the default 3d image's default 2d image as the default 2d image. Hah!
	 * @param entity
	 * @param default3dImage
	 * @throws ComputeException
	 */
	public void setDefault3dImage(Entity entity, Entity default3dImage) throws ComputeException {
        setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, default3dImage);
        EntityData ed = default3dImage.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (ed!=null) {
        	if (!EntityUtils.isInitialized(ed.getChildEntity())) {
                if (entityBeanLocal!=null) {
        		    ed.setChildEntity(entityBeanLocal.getEntityById(""+ed.getChildEntity().getId()));
                } else {
                    ed.setChildEntity(entityBeanRemote.getEntityById(""+ed.getChildEntity().getId()));
                }
        	}
        	setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, ed.getChildEntity());	
        }
        else {
        	// TODO: this shouldn't be necessary in the future
        	EntityData oldEd = default3dImage.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        	if (oldEd!=null) {
        		setDefault2dImage(entity, oldEd.getValue());		
        	}
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
        logger.info("Adding Default 2D Image to id="+entity.getId()+" name="+entity.getName());
        Entity default2dImage = create2dImage(entity.getUser().getUserLogin(), defaultImageFilepath);
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
	public void setImage(Entity entity, String attributeName, Entity image) throws ComputeException {
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
        logger.info("Adding "+attributeName+" ("+image.getName()+") to id="+entity.getId()+" name="+entity.getName());
    	if (isDebug) return;
        if (image != null) {
	    	// Update in-memory model
	    	String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	    	EntityData ed = entity.addChildEntity(image, attributeName);
	    	ed.setValue(filepath);
	    	// Update database
    		EntityData savedEd;
            if (entityBeanLocal!=null) {
                savedEd=entityBeanLocal.saveOrUpdateEntityData(ed);
            } else {
                savedEd=entityBeanRemote.saveOrUpdateEntityData(ed);
            }
    		EntityUtils.replaceEntityData(entity, ed, savedEd);
        }
    }

	/**
	 * Remove the given image property. Does NOT delete the image that is pointed to. 
	 * @param entity
	 * @throws ComputeException
	 */
	public void removeEntityDataForAttributeName(Entity entity, String attributeName) throws ComputeException {
    	logger.info("Removing "+attributeName+" for id="+entity.getId()+" name="+entity.getName());
        if (isDebug) return;
        
        Set<EntityData> toDelete = new HashSet<EntityData>();
        for(EntityData ed : entity.getEntityData()) {
        	if (ed.getEntityAttribute().getName().equals(attributeName)) {
        		toDelete.add(ed);
        	}
        }
        
        for (EntityData ed : toDelete) {
        	// Update database
            if (entityBeanLocal!=null) {
        	    entityBeanLocal.deleteEntityData(ed);
            } else {
                entityBeanRemote.deleteEntityData(ed);
            }
        	// Update in-memory model
        	entity.getEntityData().remove(ed);
        }
    }
	
	/**
	 * Tries to get the supporting data for an entity. If none exists, adds it and then returns it. 
	 * @param entity
	 * @return
	 * @throws ComputeException
	 */
	public Entity getOrCreateSupportingFilesFolder(Entity entity) throws ComputeException {
        Entity supportingFiles = EntityUtils.getSupportingData(entity);
    	if (supportingFiles == null) {
        	// Update in-memory model
    		supportingFiles = createSupportingFilesFolder(entity.getUser().getUserLogin());
			EntityData ed = entity.addChildEntity(supportingFiles, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
	        if (!isDebug) {
            	// Update database
                if (entityBeanLocal!=null) {
	        	    entityBeanLocal.saveOrUpdateEntityData(ed);
                } else {
                    entityBeanRemote.saveOrUpdateEntityData(ed);
                }
	        }
    	}
    	return supportingFiles;
	}
	
	/**
	 * Create and save a new supporting files folder with the given owner. 
	 * @param username
	 * @return
	 * @throws ComputeException
	 */
	public Entity createSupportingFilesFolder(String username) throws ComputeException {
        Entity filesFolder = new Entity();
        if (entityBeanLocal!=null) {
            filesFolder.setUser(computeBeanLocal.getUserByName(username));
        } else {
            try {
                filesFolder.setUser(computeBeanRemote.getUserByName(username));
            } catch (Exception ex) {
                throw new ComputeException(ex);
            }
        }
        if (entityBeanLocal!=null) {
            filesFolder.setEntityType(entityBeanLocal.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        } else {
            filesFolder.setEntityType(entityBeanRemote.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        }
        Date createDate = new Date();
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
    	// Update database
        if (!isDebug) {
            if (entityBeanLocal!=null) {
                filesFolder = entityBeanLocal.saveOrUpdateEntity(filesFolder);
            } else {
                filesFolder = entityBeanRemote.saveOrUpdateEntity(filesFolder);
            }
        }
        logger.info("Saved supporting files folder as "+filesFolder.getId());
    	// Return the new object so that we can update in-memory model
        return filesFolder;
    }
	
	/**
	 * Create and save a new 2d image entity with the given owner and filepath. 
	 * @param username
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create2dImage(String username, String filepath) throws ComputeException {
		File file = new File(filepath);
		return createImage(username, EntityConstants.TYPE_IMAGE_2D, filepath, file.getName());
	}
	
	/**
	 * Create and save a new 2d image entity with the given owner and filepath. 
	 * @param username
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create2dImage(String username, String filepath, String name) throws ComputeException {
		return createImage(username, EntityConstants.TYPE_IMAGE_2D, filepath, name);
	}

	/**
	 * Create and save a new 3d image entity with the given owner and filepath. 
	 * @param username
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create3dImage(String username, String filepath) throws ComputeException {
		File file = new File(filepath);
		return createImage(username, EntityConstants.TYPE_IMAGE_3D, filepath, file.getName());
	}
	
	/**
	 * Create and save a new 3d image entity with the given owner and filepath. 
	 * @param username
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create3dImage(String username, String filepath, String name) throws ComputeException {
		return createImage(username, EntityConstants.TYPE_IMAGE_3D, filepath, name);
	}
	
	public Entity createImage(String username, String entityTypeName, String filepath, String name) throws ComputeException {
		
        Entity entity = new Entity();
        if (entityBeanLocal!=null) {
            entity.setUser(computeBeanLocal.getUserByName(username));
        } else {
            try {
                entity.setUser(computeBeanRemote.getUserByName(username));
            } catch (Exception ex) {
                throw new ComputeException(ex);
            }
        }
        if (entityBeanLocal!=null) {
            entity.setEntityType(entityBeanLocal.getEntityTypeByName(entityTypeName));
        } else {
            entity.setEntityType(entityBeanRemote.getEntityTypeByName(entityTypeName));
        }
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
        if (!isDebug) {
            if (entityBeanLocal!=null) {
                entity = entityBeanLocal.saveOrUpdateEntity(entity);
            } else {
                entity = entityBeanRemote.saveOrUpdateEntity(entity);
            }
        }
        logger.info("Saved new "+entityTypeName+" as "+entity.getId());
    	// Return the new object so that we can update in-memory model
        return entity;
    }
	
}
