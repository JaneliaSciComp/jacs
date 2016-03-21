package org.janelia.it.jacs.compute.service.domain;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;


/**
 * A helper class for dealing with common entities such as default images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityHelperNG {

	private static final boolean DEBUG = false;
	
    protected Logger logger;
    protected ContextLogger contextLogger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected DomainDAO domainDao;
    protected String ownerKey;
   
    public EntityHelperNG(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, String ownerKey, Logger logger) {
        this(entityBean, computeBean, ownerKey, logger, null);
    }

    public EntityHelperNG(EntityBeanLocal entityBean, 
                        ComputeBeanLocal computeBean,
                        String ownerKey,
                        Logger logger,
                        ContextLogger contextLogger) {
        this.entityBean = entityBean;
        this.computeBean  = computeBean;
        this.ownerKey = ownerKey;
        this.domainDao = DomainDAOManager.getInstance().getDao();
        this.logger = logger;
        if (contextLogger == null) {
            this.contextLogger = new ContextLogger(logger);
        } else {
            this.contextLogger = contextLogger;
        }
    }

    /**
     * Create a child folder or verify it exists and return it.
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    public TreeNode verifyOrCreateChildFolder(TreeNode parentFolder, String childName) throws Exception {
        
        TreeNode folder = null;
        for(DomainObject domainObject : domainDao.getDomainObjects(ownerKey, parentFolder.getChildren())) {
            if (domainObject instanceof TreeNode && domainObject.getName().equals(childName)) {
                TreeNode child = (TreeNode)domainObject;
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
            folder = new TreeNode();
            folder.setName(childName);
            domainDao.save(folder);
            domainDao.addChildren(ownerKey, parentFolder, Arrays.asList(Reference.createFor(folder)));
        }

        logger.debug("Using childFolder with id=" + folder.getId());
        return folder;
    }
    
    /**
     * Create the given top level object set, or verify it exists and return it. 
     * @param topLevelFolderName
     * @param createIfNecessary
     * @param loadTree
     * @return
     * @throws Exception
     */
    public TreeNode createOrVerifyRootEntity(String topLevelFolderName, boolean createIfNecessary, boolean loadTree) throws Exception {
        TreeNode topLevelFolder = null;
        Workspace workspace = domainDao.getDefaultWorkspace(ownerKey);
        
        for(DomainObject domainObject : domainDao.getDomainObjects(ownerKey, workspace.getChildren())) {
            if (domainObject instanceof TreeNode && domainObject.getName().equals(topLevelFolderName)) {
                topLevelFolder = (TreeNode)domainObject;
                logger.debug("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                break;
            }
        }

        if (topLevelFolder == null) {
            if (createIfNecessary) {
                logger.debug("Creating new topLevelFolder with name=" + topLevelFolderName);
                topLevelFolder = new TreeNode();
                topLevelFolder.setName(topLevelFolderName);
                domainDao.save(topLevelFolder);
                domainDao.addChildren(ownerKey, workspace, Arrays.asList(Reference.createFor(topLevelFolder)));
                logger.debug("Saved top level folder as " + topLevelFolder.getId());
            } 
            else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.debug("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
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
		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, default3dImage);
        
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
                setDefault2dImage(entity, create2dImage(oldEd.getValue()));	
        		removeDefaultImageFilePath(default3dImage);
        	}
    	}
    	
    	Entity allMip = default3dImage.getChildByAttributeName(EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE);
    	if (allMip!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE, allMip);	
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

	public void set2dImages(Entity entity, Entity default2dImage, Entity allMip, Entity signalMip, Entity refMip) throws ComputeException {

    	if (default2dImage!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
    	}
    	
    	if (allMip!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE, allMip);	
    	}

    	if (signalMip!=null) {
    		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
    	}

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
	public boolean setDefault2dImage(Entity entity, Entity default2dImage) throws ComputeException {
        return setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
    }

	/**
	 * Sets the given image as an image property to the given entity. Removes any existing images for that property.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @return true if the image was necessary
	 * @throws ComputeException
	 */
	public boolean setImageIfNecessary(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null || entity==null) return false;
    	EntityData currImage = entity.getEntityDataByAttributeName(attributeName);
    	if (currImage==null || currImage.getChildEntity()==null || !currImage.getId().equals(image.getId())) {
    		setImage(entity, attributeName, image);	
    		return true;
    	}
    	return false;
	}
	
	/**
	 * Sets the given image as an image property to the given entity. Removes any existing images for that property.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void setImage(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null || entity==null) return;
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
        logger.debug("Adding " + attributeName + " (" + image.getName() + ") to " + entity.getName() + " (id=" + entity.getId() + ")");
    	if (DEBUG) return;
    	String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	entityBean.addEntityToParent(entity, image, null, attributeName, filepath);
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
        	if (ed.getEntityAttrName().equals(attributeName)) {
        		toDelete.add(ed);
        	}
        }
        
        for (EntityData ed : toDelete) {
            entityBean.deleteEntityData(ed);
        }
    }
	
	/**
	 * Create and save a new 2d image entity with the given owner and filepath. 
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
	 * @param filepath
	 * @return
	 * @throws ComputeException
	 */
	public Entity create2dImage(String filepath, String name) throws ComputeException {
		return createImage(EntityConstants.TYPE_IMAGE_2D, filepath, name);
	}

	/**
	 * Create and save a new 3d image entity with the given owner and filepath. 
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
        entity.setEntityTypeName(entityTypeName);
        Date createDate = new Date();
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setName(name);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, filepath);

		File file = new File(filepath);
    	String filename = file.getName();
    	String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
    	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
        
        if (!DEBUG) entity = entityBean.saveOrUpdateEntity(entity);
        logger.debug("Saved new "+entityTypeName+" as "+entity.getId());
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
    
    public void setInconsistencyScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE, value);
    }
    
    public void setInconsistencyScores(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORES, value);
    }

    public void setQiScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, value);
    }
    
    public void setQiScores(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORES, value);
    }
    
    public void setModelViolationScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE, value);
    }
    
    public void setNccScore(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_NCC_SCORE, value);
    }

    public void setOverlapCoeff(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_OVERLAP_COEFFICIENT, value);
    }

    public void setObjectPearsonCoeff(Entity entity, String value) throws Exception {
        setAttributeIfNecessary(entity, EntityConstants.ATTRIBUTE_ALIGNMENT_OBJECT_PEARSON_COEFFICIENT, value);
    }

    private void setAttributeIfNecessary(Entity entity, String attributeName, String value) throws Exception {
        if (entity==null || StringUtils.isEmpty(value)) return;
        EntityData currEd = entity.getEntityDataByAttributeName(attributeName);
        if (currEd==null || currEd.getValue()==null || !currEd.getValue().equals(value)) {
            entity.setValueByAttributeName(attributeName, value);
            entityBean.saveOrUpdateEntity(entity);
        }
    }

    private Sample getRequiredSampleEntity(String key, Long sampleEntityId) throws Exception {
        Sample sample = domainDao.getDomainObject(ownerKey, Sample.class, new Long(sampleEntityId));
        if (sample == null) {
            throw new IllegalArgumentException("Sample not found: " + sampleEntityId);
        }
        if (contextLogger.isInfoEnabled()) {
            contextLogger.info("Retrieved sample " + sample.getName() + " for " + key + " " + sampleEntityId);
        }
        contextLogger.appendToLogContext("sample " + sample.getName());

        return sample;
    }

    public Sample getRequiredSample(ProcessDataAccessor data) throws Exception {
        final String defaultKey = "SAMPLE_ENTITY_ID";
        final Object sampleEntityId = data.getRequiredItem(defaultKey);
        if (sampleEntityId instanceof Long) {
            return getRequiredSampleEntity(defaultKey, (Long)sampleEntityId);
        }
        else if (sampleEntityId instanceof String) {
            return getRequiredSampleEntity(defaultKey, new Long((String)sampleEntityId));
        }
        else {
            throw new IllegalArgumentException("Illegal type for SAMPLE_ENTITY_ID (must be Long or String)");
        }
    }
    
    public ObjectiveSample getRequiredObjectiveSample(Sample sample, ProcessDataAccessor data) throws Exception {
        final String objective = data.getRequiredItemAsString("OBJECTIVE");
        ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
        if (objectiveSample==null) {
            throw new IllegalArgumentException("Objective '"+objective+"' not found for sample " + sample.getId());
        }
        return objectiveSample;
    }
    
    public SamplePipelineRun getRequiredPipelineRun(Sample sample, ObjectiveSample objectiveSample, ProcessDataAccessor data) throws Exception {
        Long pipelineRunId = data.getRequiredItemAsLong("PIPELINE_RUN_ENTITY_ID");
        SamplePipelineRun run = objectiveSample.getRunById(pipelineRunId);
        if (run==null) {
            throw new IllegalArgumentException("Pipeline run "+pipelineRunId+" not found in sample "+sample.getId());
        }
        return run;
    }
}
