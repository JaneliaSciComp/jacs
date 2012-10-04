package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Discovers images in SAGE which are part of data sets defined in the workstation, and creates or updates Samples 
 * within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageImageDiscoveryService implements IService {
   
	protected static final String PRIVATE_DATA_SET_FOLDER_NAME = "My Data Sets";
	protected static final String PUBLIC_DATA_SET_FOLDER_NAME = "Public Data Sets";
	
    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected IProcessData processData;
    protected String defaultChannelSpec;
    protected Entity topLevelFolder;
    
    protected int numSamplesCreated = 0;
    protected int numSamplesAdded = 0;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            
			defaultChannelSpec = (String) processData.getItem("DEFAULT_CHANNEL_SPECIFICATION");
			if (defaultChannelSpec == null) {
				throw new IllegalArgumentException("DEFAULT_CHANNEL_SPECIFICATION may not be null");
			}
            
			if ("system".equals(user.getUserLogin())) {
	        	topLevelFolder = createOrVerifyRootEntityButDontLoadTree(PUBLIC_DATA_SET_FOLDER_NAME);
			}
			else {
	        	topLevelFolder = createOrVerifyRootEntityButDontLoadTree(PRIVATE_DATA_SET_FOLDER_NAME);
			}
            
            logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
            
        	for(Entity dataSet : entityBean.getUserEntitiesByTypeName(user.getUserLogin(), EntityConstants.TYPE_DATA_SET)) {
        		logger.info("Processing data set: "+dataSet.getName());
            	processSageDataSet(null, dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));	
        	}
        
            fixOrderIndices();
            
            logger.info("Created "+numSamplesCreated+" samples, Added "+numSamplesAdded+" samples to their corresponding data set folders.");
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    protected void processSageDataSet(String imageFamily, String dataSetIdentifier) throws Exception {
    	
    	ResultSetIterator iterator = null;
    	try {
    		List<SlideImage> slideGroup = null;
    		String currSlideCode = null;
			iterator = getImageIterator(dataSetIdentifier==null?imageFamily:dataSetIdentifier);	
    		
        	while (iterator.hasNext()) {
        		Map<String,Object> row = iterator.next();
        		SlideImage slideImage = getSlideImage(row);
        		
				if (!slideImage.slideCode.equals(currSlideCode)) {
					// Process the current group
					if (slideGroup != null) {
		                processSlideGroup(dataSetIdentifier, currSlideCode, slideGroup);
					}
					// Start a new group
					currSlideCode = slideImage.slideCode;
					slideGroup = new ArrayList<SlideImage>();
				}
				
				slideGroup.add(slideImage);
			}

			// Process the last group
			if (slideGroup != null) {
                processSlideGroup(dataSetIdentifier, currSlideCode, slideGroup);
			}
            
    	}
    	catch (RuntimeException e) {
    		if (e.getCause() instanceof SQLException) {
    			throw new DaoException(e);
    		}
    		throw e;
    	}
        finally {
        	if (iterator!=null) iterator.close();
        }
    }
    
    protected ResultSetIterator getImageIterator(String criteria) throws Exception {
    	SageDAO sageDAO = new SageDAO(logger);
    	return sageDAO.getImagesByDataSet(criteria);
    }
    
    protected SlideImage getSlideImage(Map<String,Object> row) {
        SlideImage slideImage = new SlideImage();
		slideImage.slideCode = (String)row.get("slide_code");
		slideImage.imagePath = (String)row.get("path");
		slideImage.tileType = (String)row.get("tile");
		slideImage.line = (String)row.get("line");
		slideImage.channelSpec = (String)row.get("channel_spec");
		slideImage.channels = (String)row.get("channels");
		String voxelSizeX = (String)row.get("voxel_size_x");
		String voxelSizeY = (String)row.get("voxel_size_y");
		String voxelSizeZ = (String)row.get("voxel_size_z");
		slideImage.opticalRes = voxelSizeX+"x"+voxelSizeY+"x"+voxelSizeZ;
		slideImage.file = slideImage.imagePath!=null?new File(slideImage.imagePath):null;
		return slideImage;
    }
    
    protected void processSlideGroup(String dataSetIdentifier, String slideCode, List<SlideImage> slideGroup) throws Exception {
    	
        HashMap<String, ImageTileGroup> tileGroups = new HashMap<String, ImageTileGroup>();
        String sampleIdentifier = null;
        String sampleChannelSpec = null;

        logger.info("Processing "+slideCode+", "+slideGroup.size()+" tiles");
        
        int i = 0;
        for(SlideImage slideImage : slideGroup) {
        	ImageTileGroup tileGroup = tileGroups.get(slideImage.tileType);
            if (tileGroup==null) {
            	
            	String tag = slideImage.tileType;
            	if (tag==null) {
            		tag = "Tile "+(i+1);
            	}
            	
            	tileGroup = new ImageTileGroup(tag);
        		tileGroups.put(tileGroup.getTag(), tileGroup);
            }
            
        	tileGroup.addFile(slideImage);
    		if (!slideImage.file.exists()) {
    			logger.warn("File referenced by SAGE does not exist: "+slideImage.imagePath);
    			return;
    		}
    		
            if (sampleIdentifier==null) {
                sampleIdentifier = slideImage.line + "-" + slideCode;
            }
            if (sampleChannelSpec==null) {
            	if (slideImage.channelSpec!=null) {
            		sampleChannelSpec = slideImage.channelSpec;
            	}
            }
            else if (slideImage.channelSpec!=null && !sampleChannelSpec.equals(slideImage.channelSpec)) {
            	logger.warn("Inconsistent channel specification for "+sampleIdentifier+" ("+sampleChannelSpec+" vs "+slideImage.channelSpec+")");
            }
            
            i++;
        }
        
        if (sampleChannelSpec==null) {
        	sampleChannelSpec = defaultChannelSpec;
        }
        
    	List<ImageTileGroup> tileGroupList = new ArrayList<ImageTileGroup>(tileGroups.values());
    	
        // Sort the pairs by their tag name
        Collections.sort(tileGroupList, new Comparator<ImageTileGroup>() {
			@Override
			public int compare(ImageTileGroup o1, ImageTileGroup o2) {
				return o1.getTag().compareTo(o2.getTag());
			}
		});
        
        List<String> tags = new ArrayList<String>();
        for(ImageTileGroup filePair : tileGroupList) {
        	tags.add(filePair.getTag());
        }
        
        TilingPattern tiling = TilingPattern.getTilingPattern(tags);
        logger.info("Sample "+sampleIdentifier+" has tiling pattern: "+tiling.getName());
     
        if (dataSetIdentifier==null) {
        	dataSetIdentifier = getDataSetIdentifier(tiling);
        }
        
        createOrUpdateSample(sampleIdentifier, dataSetIdentifier, tiling, sampleChannelSpec, tileGroupList);
    }

    protected String getDataSetIdentifier(TilingPattern tiling) {
    	return null;
    }
    
    protected void createOrUpdateSample(String sampleIdentifier, String dataSetIdentifier, TilingPattern tiling, 
    		String sampleChannelSpec, List<ImageTileGroup> tileGroupList) throws Exception {

        Entity dataSetFolder = verifyOrCreateChildFolder(topLevelFolder, dataSetIdentifier);
        Entity sample = findExistingSample(sampleIdentifier);
        if (sample == null) {
	        sample = createSample(sampleIdentifier, tiling, sampleChannelSpec, dataSetIdentifier);
	        numSamplesCreated++;
        }
        else {
        	populateSampleAttributes(sample, tiling, sampleChannelSpec, dataSetIdentifier);
        	
        	if (!EntityUtils.areLoaded(sample.getEntityData())) {
        		entityBean.loadLazyEntity(sample, false);
        	}
        }
        
        boolean found = false;
        for(Entity entity : dataSetFolder.getChildren()) {
        	if (entity.getId().equals(sample.getId())) {
        		found = true;
        	}
        }
        
        if (!found) {
	        addToParent(dataSetFolder, sample, dataSetFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
	        numSamplesAdded++;
        }
        
    	// Add the tile groups to the Sample's Supporting Files folder
        for (ImageTileGroup tileGroup : tileGroupList) {
        	addTileToSample(sample, tileGroup);
        }
    }
    
    protected void addTileToSample(Entity sample, ImageTileGroup tileGroup) throws Exception {
    	
        // Get the existing Supporting Files, or create a new one
        Entity supportingFiles = EntityUtils.getSupportingData(sample);
        
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	else {
        	if (!EntityUtils.areLoaded(supportingFiles.getEntityData())) {
        		entityBean.loadLazyEntity(supportingFiles, true);
        	}
    	}
    	
		EntityData imageTileEd = EntityUtils.findChildEntityDataWithNameAndType(supportingFiles, tileGroup.getTag(), EntityConstants.TYPE_IMAGE_TILE);
		Entity imageTile = null;
		
		if (imageTileEd != null) {
			imageTile = imageTileEd.getChildEntity();
			if (!existingTileMatches(imageTile, tileGroup)) {
				logger.warn("Tile '"+imageTile.getName()+"' has changed, will delete and recreate it.");
				entityBean.deleteEntityById(imageTile.getId());
				imageTile = null;
			}
			else {
				logger.warn("Tile '"+imageTile.getName()+"' exists, updating...");
				for(Entity lsmStack : imageTile.getChildrenOfType(EntityConstants.TYPE_LSM_STACK)) {
					for(SlideImage image : tileGroup.images) {
						if (image.file.getName().equals(lsmStack.getName())) {
							populateLsmStackAttributes(lsmStack, image);
						}
					}
				}
			}
		}
		
		if (imageTile == null) {
			imageTile = new Entity();
            imageTile.setUser(user);
            imageTile.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_TILE));
            imageTile.setCreationDate(createDate);
            imageTile.setUpdatedDate(createDate);
            imageTile.setName(tileGroup.getTag());
            imageTile = entityBean.saveOrUpdateEntity(imageTile);
            logger.info("Saved image tile '"+imageTile.getName()+"' as "+imageTile.getId());
        	addToParent(supportingFiles, imageTile, null, EntityConstants.ATTRIBUTE_ENTITY);
        	
        	for(SlideImage image : tileGroup.getImages()) {
    			logger.info("Adding LSM file to sample: "+image.imagePath);
    			Entity lsmEntity = createLsmStackFromFile(image);
                addToParent(imageTile, lsmEntity, imageTile.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        	}
		}
    }
    
    protected boolean existingTileMatches(Entity imageTile, ImageTileGroup tileGroup) {

		List<SlideImage> images = tileGroup.getImages();
		List<Entity> currTiles = imageTile.getChildrenOfType(EntityConstants.TYPE_IMAGE_TILE);
		
		Set<String> newFilenames = new HashSet<String>();
		for(SlideImage image : images) {
			newFilenames.add(image.file.getName());
		}
		
		if (images.size() != currTiles.size()) {
			return false;
		}
		
		Set<String> currFilenames = new HashSet<String>();		
		for(Entity child : imageTile.getChildren()) {
			currFilenames.add(child.getName());
			if (!newFilenames.contains(child.getName())) {
				return false;
			}
		}
		
		for(SlideImage image : tileGroup.getImages()) {
			if (!currFilenames.contains(image.file.getName())) {
				return false;
			}
		}
		
		return true;
    }
    
    /**
     * Find and return the child Sample entity
     */
    protected Entity findExistingSample(String sampleIdentifier) {

    	List<Entity> matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(user.getUserLogin(), 
    			sampleIdentifier, EntityConstants.TYPE_SAMPLE);
    	
    	if (matchingSamples.isEmpty()) {
            // Could not find sample child entity
    		return null;
    	}
    	
    	if (matchingSamples.size()>1) {
    		logger.warn("Multiple samples (count="+matchingSamples.size()+") found with sample identifier: "+sampleIdentifier);
    	}
    	
    	return matchingSamples.get(0);
    }

    protected Entity createSample(String name, TilingPattern tiling, String channelSpec, String dataSetIdentifier) throws Exception {
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        return sample;
    }

    protected Entity populateSampleAttributes(Entity sample, TilingPattern tiling, String channelSpec, String dataSetIdentifier) throws Exception {

    	boolean save = false;
    	if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_TILING_PATTERN+"'="+tiling+" for id="+sample.getId());
    		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());	
    		save = true;
    	}
    	if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER+"'="+dataSetIdentifier+" for id="+sample.getId());
    		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);	
    		save = true;
    	}
    	if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION+"'="+channelSpec+" for id="+sample.getId());
    		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);	
    		save = true;
    	}
        
    	if (save) {
    		sample = entityBean.saveOrUpdateEntity(sample);
        	logger.info("Updated sample attributes for "+sample.getId());
    	}
        return sample;
    }
    
    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    protected Entity createLsmStackFromFile(SlideImage image) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(image.file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, image.imagePath);
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, image.channels);
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, image.opticalRes);
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
    
    protected Entity populateLsmStackAttributes(Entity lsmStack, SlideImage image) throws Exception {

    	boolean save = false;
    	if (lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_FILE_PATH+"'="+image.imagePath+" for id="+lsmStack.getId());
    		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, image.imagePath);	
    		save = true;
    	}
    	if (lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_NUM_CHANNELS+"'="+image.channels+" for id="+lsmStack.getId());
    		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, image.channels);	
    		save = true;
    	}
    	if (lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)==null) {
    		logger.info("Setting '"+EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION+"'="+image.opticalRes+" for id="+lsmStack.getId());
    		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, image.opticalRes);	
    		save = true;
    	}
        
    	if (save) {
    		lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        	logger.info("Updated LSM attributes for "+lsmStack.getId());
    	}
        return lsmStack;
    }
    
    protected void fixOrderIndices() throws Exception {
		logger.info("Fixing sample order indicies");
		
    	for(Entity childFolder : topLevelFolder.getChildrenOfType(EntityConstants.TYPE_FOLDER)) {
    			
			logger.info("Fixing order indicies in "+childFolder.getName());

	    	List<EntityData> orderedData = new ArrayList<EntityData>();
			for(EntityData ed : childFolder.getEntityData()) {
				if (ed.getChildEntity()!=null) {
					orderedData.add(ed);
				}
			}
	    	Collections.sort(orderedData, new Comparator<EntityData>() {
				@Override
				public int compare(EntityData o1, EntityData o2) {
					int c = o1.getChildEntity().getCreationDate().compareTo(o2.getChildEntity().getCreationDate());
					if (c==0) {
						return o1.getChildEntity().getId().compareTo(o2.getChildEntity().getId());
					}
					return c;
				}
			});
			
	    	int orderIndex = 0;
			for(EntityData ed : orderedData) {
				if (ed.getOrderIndex()==null || orderIndex!=ed.getOrderIndex()) {
					logger.info("  Updating link (id="+ed.getId()+") to "+ed.getChildEntity().getName()+" with order index "+orderIndex+" (was "+ed.getOrderIndex()+")");
					ed.setOrderIndex(orderIndex);
					entityBean.saveOrUpdateEntityData(ed);
				}
				orderIndex++;
			}
		}
    }
    
    protected Entity createOrVerifyRootEntityButDontLoadTree(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, false /* load tree */);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, true);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName, User user, Date createDate, org.apache.log4j.Logger logger, boolean createIfNecessary, boolean loadTree) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getUser().getUserLogin().equals(user.getUserLogin())
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
                topLevelFolder = new Entity();
                topLevelFolder.setCreationDate(createDate);
                topLevelFolder.setUpdatedDate(createDate);
                topLevelFolder.setUser(user);
                topLevelFolder.setName(topLevelFolderName);
                topLevelFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
                topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
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
            folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder = entityBean.saveOrUpdateEntity(folder);
            addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }
    
    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }

    protected class SlideImage {
    	String slideCode;
    	String imagePath;
    	String tileType;
    	String line;
    	String channelSpec;
    	String channels;
    	String opticalRes;
    	File file;
    }
    
    protected class ImageTileGroup {
        private String tag;
        private List<SlideImage> images;

        public ImageTileGroup(String tag) {
            this.tag = tag;
        }

        public String getTag() {
			return tag;
		}

		public List<SlideImage> getImages() {
			return images;
		}

		public void addFile(SlideImage image) {
			images.add(image);
		}
    }
}
