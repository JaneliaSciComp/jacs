package org.janelia.it.jacs.compute.api.support;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;

/**
 * A helper class for dealing with common entities such as default images.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityMethods {

    protected Logger logger;
    protected EntityBeanRemote entityBean;
    protected ComputeBeanRemote computeBean;
    protected String ownerKey;
    protected AbstractEntityLoader entityLoader;

    public EntityMethods(EntityBeanRemote entityBean, ComputeBeanRemote computeBean, AbstractEntityLoader entityLoader,
            String ownerKey, Logger logger) {
        this.entityBean = entityBean;
        this.computeBean = computeBean;
        this.entityLoader = entityLoader;
        this.ownerKey = ownerKey;
        this.logger = logger;
    }
    
    /**
     * Create a child folder or verify it exists and return it.
     * 
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    public Entity verifyOrCreateChildFolder(Entity parentFolder, String childName) throws Exception {

        entityLoader.populateChildren(parentFolder);
        Entity folder = EntityUtils.findChildWithName(parentFolder, childName);
        if (folder == null) {
            // We need to create a new folder
            Date createDate = new Date();
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setOwnerKey(ownerKey);
            folder.setName(childName);
            folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder = entityBean.saveOrUpdateEntity(ownerKey, folder);
            addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }

        return folder;
    }

    /**
     * Create the given top level folder, or verify it exists and return it.
     * 
     * @param topLevelFolderName
     * @param createIfNecessary
     * @param loadTree
     * @return
     * @throws Exception
     */
    public Entity createOrVerifyRootEntity(String topLevelFolderName, boolean createIfNecessary, boolean loadTree)
            throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(ownerKey, topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getOwnerKey().equals(ownerKey)
                        && entity.getEntityType().getName()
                                .equals(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER).getName())
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    // This is the folder we want, now load the entire folder
                    // hierarchy
                    if (loadTree) {
                        topLevelFolder = entityBean.getEntityTree(ownerKey, entity.getId());
                    }
                    else {
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
                Date createDate = new Date();
                topLevelFolder = new Entity();
                topLevelFolder.setCreationDate(createDate);
                topLevelFolder.setUpdatedDate(createDate);
                topLevelFolder.setOwnerKey(ownerKey);
                topLevelFolder.setName(topLevelFolderName);
                topLevelFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
                EntityUtils.addAttributeAsTag(topLevelFolder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
                topLevelFolder = entityBean.saveOrUpdateEntity(ownerKey, topLevelFolder);
                logger.info("Saved top level folder as " + topLevelFolder.getId());
            }
            else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
    }

    /**
     * Add the default 3d image, and the default 3d image's shortcut images as
     * the entity's shortcut images.
     * 
     * @param entity
     * @param default3dImage
     * @throws ComputeException
     */
    public void setDefault3dImage(Entity entity, Entity default3dImage) throws Exception {
        if (entity == null || default3dImage == null) return;
        setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, default3dImage);

        if (!EntityUtils.areLoaded(default3dImage.getEntityData())) {
            entityLoader.populateChildren(default3dImage);
        }

        Entity default2dImage = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if (default2dImage != null) {
            setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
        }

        Entity signalMip = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
        if (signalMip != null) {
            setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
        }

        Entity refMip = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
        if (refMip != null) {
            setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
        }

    }

    /**
     * Sets the given image as the default 2d image for the given entity.
     * 
     * @param entity
     * @param default2dImage
     * @throws ComputeException
     */
    public void setDefault2dImage(Entity entity, Entity default2dImage) throws ComputeException {
        setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
    }

    /**
     * Sets the default 2d image for the given entity. This creates a new entity
     * with the given filename as the name.
     * 
     * @param entity
     * @param defaultImageFilepath
     * @throws ComputeException
     */
    public Entity setDefault2dImage(Entity entity, String defaultImageFilepath) throws ComputeException {
        logger.debug("Adding Default 2D Image to " + entity.getName() + " (id=" + entity.getId() + ")");
        Entity default2dImage = create2dImage(defaultImageFilepath);
        setDefault2dImage(entity, default2dImage);
        return default2dImage;
    }

    /**
     * Sets the given image as an image property to the given entity. Removes
     * any existing images for that property.
     * 
     * @param entity
     * @param image
     * @param attributeName
     * @throws ComputeException
     */
    public void setImageIfNecessary(Entity entity, String attributeName, Entity image) throws ComputeException {
        if (image == null || entity == null) return;
        EntityData currImage = entity.getEntityDataByAttributeName(attributeName);
        if (currImage == null || currImage.getChildEntity() == null || !currImage.getId().equals(image.getId())) {
            setImage(entity, attributeName, image);
        }
    }

    /**
     * Sets the given image as an image property to the given entity. Removes
     * any existing images for that property.
     * 
     * @param entity
     * @param image
     * @param attributeName
     * @throws ComputeException
     */
    public void setImage(Entity entity, String attributeName, Entity image) throws ComputeException {
        if (image == null || entity == null) return;
        removeEntityDataForAttributeName(entity, attributeName);
        addImage(entity, attributeName, image);
    }

    /**
     * Adds the given image as an image property to the given entity.
     * 
     * @param entity
     * @param image
     * @param attributeName
     * @throws ComputeException
     */
    public void addImage(Entity entity, String attributeName, Entity image) throws ComputeException {
        if (image == null) return;
        logger.debug("Adding " + attributeName + " (" + image.getName() + ") to " + entity.getName() + " (id="
                + entity.getId() + ")");
        // Update in-memory model
        String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        EntityData ed = entity.addChildEntity(image, attributeName);
        ed.setValue(filepath);
        // Update database
        EntityData savedEd = entityBean.saveOrUpdateEntityData(ownerKey, ed);
        EntityUtils.replaceEntityData(entity, ed, savedEd);
    }

    /**
     * Remove the given image property. Does NOT delete the image that is
     * pointed to.
     * 
     * @param entity
     * @throws ComputeException
     */
    public void removeEntityDataForAttributeName(Entity entity, String attributeName) throws ComputeException {
        logger.debug("Removing " + attributeName + " for " + entity.getName() + " (id=" + entity.getId() + ")");
        Set<EntityData> toDelete = new HashSet<EntityData>();
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttribute().getName().equals(attributeName)) {
                toDelete.add(ed);
            }
        }

        for (EntityData ed : toDelete) {
            // Update in-memory model
            entity.getEntityData().remove(ed);
            ed.setParentEntity(null);
            // Update database
            entityBean.deleteEntityData(ownerKey, ed.getId());
        }
    }

    /**
     * Create and save a new 2d image entity with the given owner and filepath.
     * 
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
     * 
     * @param filepath
     * @return
     * @throws ComputeException
     */
    public Entity create2dImage(String filepath, String name) throws ComputeException {
        return createImage(EntityConstants.TYPE_IMAGE_2D, filepath, name);
    }

    /**
     * Create and save a new 3d image entity with the given owner and filepath.
     * 
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
     * 
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
        String fileFormat = filename.substring(filename.lastIndexOf('.') + 1);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);

        // Update database
        entity = entityBean.saveOrUpdateEntity(ownerKey, entity);
        logger.debug("Saved new " + entityTypeName + " as " + entity.getId());
        // Return the new object so that we can update in-memory model
        return entity;
    }

    public void setAlignmentSpace(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE, value);
    }

    public void setOpticalResolution(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, value);
    }

    public void setPixelResolution(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, value);
    }

    public void setObjective(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_OBJECTIVE, value);
    }

    public void setBoundingBox(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_BOUNDING_BOX, value);
    }

    public void setChannelSpec(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, value);
    }

    public void setChannelColors(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_CHANNEL_COLORS, value);
    }

    public void setQiScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, value);
    }

    public void setQmScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE, value);
    }

    public void setNccScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_NCC_SCORE, value);
    }

    private void setAttributeIfNecessary(Entity entity, String attributeName, String value) throws Exception {
        if (entity == null || StringUtils.isEmpty(value)) return;
        EntityData currEd = entity.getEntityDataByAttributeName(attributeName);
        if (currEd == null || !currEd.getValue().equals(value)) {
            entity.setValueByAttributeName(attributeName, value);
            entityBean.saveOrUpdateEntity(ownerKey, entity);
        }
    }

    public void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ownerKey, ed);
        logger.trace("Added " + entity.getName() + " (" + entity.getEntityType().getName() + "#" + entity.getId()
                + ") as child of " + parent.getName() + " (" + parent.getEntityType().getName() + "#" + parent.getId()
                + ")");
    }

}
