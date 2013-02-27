package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Discovers images in SAGE which are part of data sets defined in the workstation, and creates or updates Samples 
 * within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */ 
public class SageDataSetDiscoveryService extends AbstractEntityService {
   
	protected static final String PRIVATE_DATA_SET_FOLDER_NAME = "My Data Sets";
	protected static final String PUBLIC_DATA_SET_FOLDER_NAME = "Public Data Sets";
	
    protected Entity topLevelFolder;
    
    protected int sageRowsProcessed = 0;
    protected int numSamplesCreated = 0;
    protected int numSamplesUpdated = 0;
    protected int numSamplesAdded = 0;
    
    public void execute() throws Exception {
        
		if ("group:flylight".equals(ownerKey)) {
        	topLevelFolder = createOrVerifyRootEntity(PUBLIC_DATA_SET_FOLDER_NAME, true, false);
		}
		else {
        	topLevelFolder = createOrVerifyRootEntity(PRIVATE_DATA_SET_FOLDER_NAME, true, false);
		}
		
		logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
		
        List<Entity> dataSets = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_DATA_SET);
        if (dataSets.isEmpty()) {
        	logger.info("No data sets found for user: "+ownerKey);
        	return;
        }        

    	for(Entity dataSet : dataSets) {
    		if (dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)==null) {
    			logger.info("Skipping non-SAGE data set: "+dataSet.getName());
    		}
    		else {
        		logger.info("Processing data set: "+dataSet.getName());
            	processSageDataSet(null, dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
    		}	
    	}
    
        fixOrderIndices();
        
        logger.info("Processed "+sageRowsProcessed+" rows, created "+numSamplesCreated+
        		" samples, updated "+numSamplesUpdated+" samples, added "+numSamplesAdded+
        		" samples to their corresponding data set folders.");
    }
    
    /**
     * Provide either imageFamily or dataSetIdentifier. 
     */
    protected void processSageDataSet(String imageFamily, String dataSetIdentifier) throws Exception {
    	
    	SageDAO sageDAO = new SageDAO(logger);
    	ResultSetIterator iterator = null;
    	try {
    		List<SlideImage> slideGroup = null;
    		String currSlideCode = null;
    		
    		if (dataSetIdentifier==null) {
    			logger.info("Querying SAGE for image family: "+imageFamily);
    			iterator = sageDAO.getImagesByFamily(imageFamily);
    		}
    		else {
    			logger.info("Querying SAGE for data set: "+dataSetIdentifier);
    			iterator = sageDAO.getImagesByDataSet(dataSetIdentifier);
    		}
    		
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
				sageRowsProcessed++;
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

    protected SlideImage getSlideImage(Map<String,Object> row) {
        SlideImage slideImage = new SlideImage();
        slideImage.sageId = (Long)row.get("id");
		slideImage.slideCode = (String)row.get("slide_code");
		slideImage.imagePath = (String)row.get("path");
		slideImage.tileType = (String)row.get("tile");
		slideImage.line = (String)row.get("line");
		slideImage.channelSpec = (String)row.get("channel_spec");
		slideImage.gender = (String)row.get("gender");
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
        
        logger.info("Processing "+slideCode+", "+slideGroup.size()+" slide images");
        
        int tileNum = 0;
        for(SlideImage slideImage : slideGroup) {
        	
        	if (slideImage.file==null) {
        		logger.warn("Slide code "+slideImage.slideCode+" has an image with a null path, so it is not ready for synchronization.");
        		return;
        	}
        	
        	ImageTileGroup tileGroup = tileGroups.get(slideImage.tileType);
            if (tileGroup==null) {
            	
            	String tag = slideImage.tileType;
            	if (tag==null) {
            		tag = "Tile "+(tileNum+1);
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
        	
            tileNum++;
        }
        
    	List<ImageTileGroup> tileGroupList = new ArrayList<ImageTileGroup>(tileGroups.values());
    	
        // Sort the pairs by their tag name
        Collections.sort(tileGroupList, new Comparator<ImageTileGroup>() {
			@Override
			public int compare(ImageTileGroup o1, ImageTileGroup o2) {
				return o1.getTag().compareTo(o2.getTag());
			}
		});
        
        createOrUpdateSample(sampleIdentifier, dataSetIdentifier, tileGroupList);
    }
    
    protected Entity createOrUpdateSample(String sampleIdentifier, String dataSetIdentifier, 
    		List<ImageTileGroup> tileGroupList) throws Exception {

    	if (dataSetIdentifier==null) {
    		throw new IllegalStateException("Cannot create or update sample without a data set identifier");
    	}
    	
        // Figure out the number of channels that should be in the final merged/stitched sample
        int sampleNumSignals = -1;
        for(ImageTileGroup tileGroup : tileGroupList) {

        	int tileNumSignals = 0;
        	logger.debug("Calculating number of channels in tile "+tileGroup.getTag());
        	for(SlideImage slideImage : tileGroup.getImages()) {
            	if (slideImage.channelSpec!=null) {
            		for(int j=0; j<slideImage.channelSpec.length(); j++) {
            			if (slideImage.channelSpec.charAt(j)=='s') {
            				tileNumSignals++;
            			}
            		}
            	}
        	}
        	
        	if (tileNumSignals<1) {
        		logger.debug("Falling back on channel number");
        		// We didn't get the information from the channel spec, let's fall back on inference from numChannels
            	for(SlideImage slideImage : tileGroup.getImages()) {
                	if (slideImage.channels!=null) {
                		tileNumSignals += Integer.parseInt(slideImage.channels) - 1;
                	}
            	}
        	}
        	
        	logger.debug("Tile "+tileGroup.getTag()+" has "+tileNumSignals+" signal channels");
        	
        	if (sampleNumSignals<0) {
        		sampleNumSignals = tileNumSignals;
        	}
        	else if (sampleNumSignals != tileNumSignals) {
        		logger.warn("No consensus for number of signal channels per tile ("+sampleNumSignals+" != "+tileNumSignals+")");
        	}
        }
        
        String sampleChannelSpec = getChanSpec(sampleNumSignals);
        logger.debug("Sample has "+sampleNumSignals+" signal channels, and thus specification '"+sampleChannelSpec+"'");
        
    	// Find the sample, if it exists
        Entity sample = findExistingSample(sampleIdentifier);
        if (sample == null) {
        	logger.info("Did not find sample "+sampleIdentifier);
	        sample = createSample(sampleIdentifier, sampleChannelSpec, dataSetIdentifier);
	        numSamplesCreated++;
        }
        else {
        	logger.info("Found existing sample "+sampleIdentifier);
        	populateSampleAttributes(sample, sampleChannelSpec, dataSetIdentifier);
        	populateChildren(sample);
        }

    	// Add the tile groups to the Sample's Supporting Files folder
        for (ImageTileGroup tileGroup : tileGroupList) {
        	addTileToSample(sample, tileGroup);
        }

        // Remove from current data set folder, if we're changing         
		String currDataSet = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
		if (currDataSet!=null && !currDataSet.equals(dataSetIdentifier)) {
			logger.info("    Data set is changing from "+currDataSet+" to "+dataSetIdentifier);	
		
			Set<EntityData> toDelete = new HashSet<EntityData>();
			for(EntityData ed : entityBean.getParentEntityDatas(sample.getId())) {
				Entity parent = ed.getParentEntity();
				for(Entity grandparent : entityBean.getParentEntities(parent.getId())) {
					if (grandparent.getId().equals(topLevelFolder.getId())) {
						logger.info("    Removing from data set folder: "+parent.getName()+" (id="+parent.getId()+")");	
						toDelete.add(ed);
					}
				}
			}
			
			for(EntityData ed : toDelete) {
				entityBean.deleteEntityData(ed);
			}
		}
	
		// Add to correct data set folder
    	Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);
    	if (dataSet!=null) {
    		Entity dataSetFolder = verifyOrCreateChildFolder(topLevelFolder, dataSet.getName());
            
            boolean found = false;
            for(Entity entity : dataSetFolder.getChildren()) {
            	if (entity.getId().equals(sample.getId())) {
            		found = true;
            	}
            }
            
            if (!found) {
            	logger.info("    Adding to data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");	
    	        addToParent(dataSetFolder, sample, dataSetFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    	        numSamplesAdded++;
            }	
    	}
    	else {
    		logger.warn("Data set for sample (id="+sample.getId()+") does not exist: "+dataSetIdentifier);
    	}
    	
    	return sample;
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
				logger.info("  Tile '"+imageTile.getName()+"' (id="+imageTileEd.getId()+") has changed, will delete and recreate it.");
				entityBean.deleteSmallEntityTree(imageTile.getOwnerKey(), imageTile.getId());
				imageTile = null;
			}
			else {
				logger.info("  Tile '"+imageTile.getName()+"' exists (id="+imageTileEd.getId()+")");
				for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
					for(SlideImage image : tileGroup.images) {
						if (image.file.getName().equals(lsmStack.getName())) {
							populateLsmStackAttributes(lsmStack, image);
						}
					}
				}
			}
		}
		
		if (imageTile == null) {
			Date createDate = new Date();
			imageTile = new Entity();
            imageTile.setOwnerKey(ownerKey);
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
		List<Entity> currTiles = EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK);
		
		Set<String> newFilenames = new HashSet<String>();
		for(SlideImage image : images) {
			newFilenames.add(image.file.getName());
		}
		
		if (images.size() != currTiles.size()) {
			return false;
		}
		
		Set<String> currFilenames = new HashSet<String>();		
		for(Entity lsmStack : currTiles) {
			currFilenames.add(lsmStack.getName());
			if (!newFilenames.contains(lsmStack.getName())) {
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
    protected Entity findExistingSample(String sampleIdentifier) throws ComputeException {

    	List<Entity> matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(ownerKey, 
    			sampleIdentifier, EntityConstants.TYPE_SAMPLE);
    	
    	if (matchingSamples.isEmpty()) {

    		if ("group:flylight".equals(ownerKey)) {
    			// FlyLight cannot steal samples from others
    			return null;
    		}
    		
        	matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(null, 
        			sampleIdentifier, EntityConstants.TYPE_SAMPLE);
        	
        	if (matchingSamples.isEmpty()) {
	            // Could not find sample child entity
	    		return null;
        	}

        	Entity matchingSample = null;
        	for(Entity sample : matchingSamples) {
        		if ("group:flylight".equals(sample.getOwnerKey())) {
        			// Can only steal samples from the FlyLight user
        			matchingSample = sample;
        		}
        	}
        	
        	if (matchingSample != null) {
        		return entityBean.annexEntityTree(ownerKey, matchingSample.getId());	
        	}
    	}
    	
    	if (matchingSamples.size()>1) {
    		logger.warn("Multiple samples (count="+matchingSamples.size()+") found with sample identifier: "+sampleIdentifier);
    	}
    	
    	return matchingSamples.get(0);
    }

    protected Entity createSample(String name, String channelSpec, String dataSetIdentifier) throws Exception {
    	Date createDate = new Date();
        Entity sample = new Entity();
        sample.setOwnerKey(ownerKey);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        return sample;
    }

    protected Entity populateSampleAttributes(Entity sample, String channelSpec, String dataSetIdentifier) throws Exception {
		logger.info("    Setting properties: dataSet="+dataSetIdentifier+", spec="+channelSpec);
		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);	
		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);	
		sample = entityBean.saveOrUpdateEntity(sample);
		numSamplesUpdated++;
        return sample;
    }
    
    protected Entity createSupportingFilesFolder() throws Exception {
    	Date createDate = new Date();
        Entity filesFolder = new Entity();
        filesFolder.setOwnerKey(ownerKey);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    protected Entity createLsmStackFromFile(SlideImage image) throws Exception {
    	Date createDate = new Date();
        Entity lsmStack = new Entity();
        lsmStack.setOwnerKey(ownerKey);
        lsmStack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(image.file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, image.imagePath);
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, image.channels);
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, image.opticalRes);
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, image.channelSpec);
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
    
    protected Entity populateLsmStackAttributes(Entity lsmStack, SlideImage image) throws Exception {
		logger.info("    Setting properties: channels="+image.channels+", res="+image.opticalRes+", spec="+image.channelSpec);
		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, image.imagePath);
		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, image.channels);
		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, image.opticalRes);	
		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, image.channelSpec);	
		lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        return lsmStack;
    }
    
    protected void fixOrderIndices() throws Exception {
		logger.info("Fixing order indicies in children of "+topLevelFolder.getName());
		populateChildren(topLevelFolder);
		
    	for(Entity childFolder : EntityUtils.getChildrenOfType(topLevelFolder, EntityConstants.TYPE_FOLDER)) {
    		populateChildren(childFolder);
    		
			logger.info("Fixing sample order indicies in "+childFolder.getName()+" (id="+childFolder.getId()+")");

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
    
    protected Entity createOrVerifyRootEntity(String topLevelFolderName, boolean createIfNecessary, boolean loadTree) throws Exception {
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
                Date createDate = new Date();
                topLevelFolder = new Entity();
                topLevelFolder.setCreationDate(createDate);
                topLevelFolder.setUpdatedDate(createDate);
                topLevelFolder.setOwnerKey(ownerKey);
                topLevelFolder.setName(topLevelFolderName);
                topLevelFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
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

    	populateChildren(parentFolder);
    	
        Entity folder = null;
        for (Entity child : EntityUtils.getChildrenOfType(parentFolder, EntityConstants.TYPE_FOLDER)) {
            if (child.getName().equals(childName)) {
                if (folder != null) {
                	logger.warn("Unexpectedly found multiple child folders with name=" + childName+" for parent folder id="+parentFolder.getId());
                }
                else {
                	folder = child;
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
        	Date createDate = new Date();
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setOwnerKey(ownerKey);
            folder.setName(childName);
            folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder = entityBean.saveOrUpdateEntity(folder);
            addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }

	protected String getChanSpec(int numSignals) {
		StringBuilder buf = new StringBuilder();
		for(int j=0; j<numSignals; j++) {
			buf.append("s");
		}
		buf.append("r");
		return buf.toString();
	}
	
    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
    
    protected class SlideImage {
    	Long sageId;
    	String slideCode;
    	String imagePath;
    	String tileType;
    	String line;
    	String channelSpec;
    	String channels;
    	String opticalRes;
    	String gender;
    	File file;
    }
    
    protected class ImageTileGroup {
        private String tag;
        private List<SlideImage> images = new ArrayList<SlideImage>();

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
