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
 * Discovers images in SAGE which are part of a particular image family, and creates Samples within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageImageDiscoveryService implements IService {
   
    protected Map<TilingPattern,Integer> patterns = new EnumMap<TilingPattern,Integer>(TilingPattern.class);
    
    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected String confocalStacksDir;
    protected String sageImageFamily;
    protected IProcessData processData;
    protected String defaultChannelSpec;
    protected String alignmentTypes;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();

            confocalStacksDir = (String)processData.getItem("CONFOCAL_STACKS_DIR");
            if (confocalStacksDir==null) {
        		throw new IllegalArgumentException("SAGE_IMAGE_FAMILY may not be null");
            }
            
            sageImageFamily = (String)processData.getItem("SAGE_IMAGE_FAMILY");
            if (sageImageFamily==null) {
        		throw new IllegalArgumentException("SAGE_IMAGE_FAMILY may not be null");
            }

			defaultChannelSpec = (String) processData.getItem("DEFAULT CHANNEL SPECIFICATION");
			if (defaultChannelSpec == null) {
				throw new IllegalArgumentException("DEFAULT CHANNEL SPECIFICATION may not be null");
			}
            
            alignmentTypes = (String)processData.getItem("ALIGNMENT_TYPES");
            if (alignmentTypes==null) {
            	alignmentTypes = "AUTO";
            }
            
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
            	topLevelFolder = entityBean.getEntityById(rootEntityId);
            }
            
        	if (topLevelFolder==null) {
        		throw new IllegalArgumentException("Both ROOT_ENTITY_NAME and ROOT_ENTITY_ID may not be null");
        	}
        	
            logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
            
            processSageData(topLevelFolder);
            
        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar != null) {
        		logger.info("Putting "+topLevelFolder.getId()+" in "+outvar);
        		processData.putItem(outvar, topLevelFolder.getId());
        	}

        	logger.info("Tiling pattern statistics:");
            for(TilingPattern pattern : TilingPattern.values()) {
            	Integer count = patterns.get(pattern);
            	if (count==null) count = 0;
            	logger.info(pattern.getName()+": "+count);
            }
            
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    protected void processSageData(Entity topLevelFolder) throws Exception {

    	// Load all the tiling pattern folders
    	if (!EntityUtils.areLoaded(topLevelFolder.getEntityData())) {
    		entityBean.loadLazyEntity(topLevelFolder, false);
    	}
    	
    	// Load all the children (Samples) for each tiling pattern 
    	for(Entity tilingPatternFolder : topLevelFolder.getChildren()) {
    		if (tilingPatternFolder.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
    			entityBean.loadLazyEntity(tilingPatternFolder, false);
    		}
    	}
    	
    	fixOrderIndices(topLevelFolder);
    	
    	SageDAO sageDAO = new SageDAO(logger);
    		
    	ResultSetIterator iterator = null;
    	try {
    		List<SlideImage> slideGroup = null;
    		String currSlideCode = null;

            iterator = sageDAO.getImages(sageImageFamily);

        	while (iterator.hasNext()) {
        		Map<String,Object> row = iterator.next();
                SlideImage slideImage = new SlideImage();
				slideImage.slideCode = (String)row.get("slide_code");
				slideImage.imagePath = (String)row.get("name");
				slideImage.tileType = (String)row.get("tile");
				slideImage.age = (String)row.get("age");
				slideImage.gender = (String)row.get("gender");
				slideImage.effector = (String)row.get("effector");
				slideImage.line = (String)row.get("line");
				slideImage.channelSpec = (String)row.get("channel_spec");

				if (!slideImage.slideCode.equals(currSlideCode)) {

					// Process the current group
					if (slideGroup != null) {
		                processSlideGroup(topLevelFolder, currSlideCode, slideGroup);
					}
					
					// Start a new group
					currSlideCode = slideImage.slideCode;
					slideGroup = new ArrayList<SlideImage>();
				}
				
				slideGroup.add(slideImage);
			}

			// Process the last group
			if (slideGroup != null) {
                processSlideGroup(topLevelFolder, currSlideCode, slideGroup);
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
    
    protected void processSlideGroup(Entity topLevelFolder, String slideCode, List<SlideImage> slideGroup) throws Exception {
    	
        HashMap<String, FilePair> filePairings = new HashMap<String, FilePair>();
        String sampleIdentifier = null;
        String sampleChannelSpec = null;

        logger.info("Processing "+slideCode+", "+slideGroup.size()+" tiles");
        
        int i = 0;
        for(SlideImage slideImage : slideGroup) {
    	
            if (slideImage.tileType==null || !filePairings.containsKey(slideImage.tileType)) {
            	
            	String pairTag = slideImage.tileType;
            	if (pairTag==null) {
            		pairTag = "Tile "+(i+1);
            	}
            	
            	FilePair filePair = new FilePair(pairTag);
            	File lsmFile1 = new File(confocalStacksDir,slideImage.imagePath);
            	filePair.setFilename1(lsmFile1.getAbsolutePath());
        		if (!lsmFile1.exists()) {
        			logger.warn("File referenced by SAGE does not exist: "+filePair.getFilename1());
        			return;
        		}
        		
        		filePair.setFile1(lsmFile1);
        		filePairings.put(filePair.getPairTag(), filePair);
            }
            else {
            	FilePair filePair = filePairings.get(slideImage.tileType);
            	File lsmFile2 = new File(confocalStacksDir,slideImage.imagePath);
            	filePair.setFilename2(lsmFile2.getAbsolutePath());
        		if (!lsmFile2.exists()) {
        			logger.warn("File referenced by SAGE does not exist: "+filePair.getFilename2());
        			return;
        		}

        		filePair.setFile2(lsmFile2);
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
        
        // Get a list of complete pairs
    	List<FilePair> filePairs = new ArrayList<FilePair>();
        for (FilePair filePair : filePairings.values()) {
        	if (filePair.isPairingComplete()) {
        		filePairs.add(filePair);
        	}
        }

        if (filePairs.isEmpty()) {
        	if (!filePairings.isEmpty()) {
        		// No complete pairs, but some singles. That probably means these images are not meant to be paired.
        		filePairs.addAll(filePairings.values());
        	}
        	else {
        		return;
        	}
        }
    	
        // Sort the pairs by their tag name
        Collections.sort(filePairs, new Comparator<FilePair>() {
			@Override
			public int compare(FilePair o1, FilePair o2) {
				return o1.getPairTag().compareTo(o2.getPairTag());
			}
		});
        
        List<String> tags = new ArrayList<String>();
        for(FilePair filePair : filePairs) {
        	tags.add(filePair.getPairTag());
        	logger.info("    "+filePair.getPairTag()+" = "+filePair.getFile1().getName());
        }
        
        TilingPattern tiling = TilingPattern.getTilingPattern(tags);
        logger.info("Sample "+sampleIdentifier+" has tiling pattern: "+tiling.getName());
        
        if (tiling != null) {
        	Integer count = patterns.get(tiling);
        	if (count == null) {
        		count = 1;
        	}
        	else {
        		count++;
        	}
        	patterns.put(tiling, count);
        }
        
        if (tiling != null && tiling.isStitchable()) {
        	// This is a stitchable case
        	logger.info("Sample "+sampleIdentifier+" is stitchable");
            Entity sample = createOrVerifySample(topLevelFolder, sampleIdentifier, tiling, sampleChannelSpec);
        	// Add the LSM pairs to the Sample's Supporting Files folder
            for (FilePair filePair : filePairs) {
            	addLsmPairToSample(sample, filePair);
            }
        }
        else {
        	// In non-stitchable cases we just create a Sample for each LSM pair
        	logger.info("Sample "+sampleIdentifier+" is not stitchable");
            for (FilePair filePair : filePairs) {
            	String sampleName = sampleIdentifier+"-"+filePair.getPairTag().replaceAll(" ", "_");
                Entity sample = createOrVerifySample(topLevelFolder, sampleName, tiling, sampleChannelSpec);
            	addLsmPairToSample(sample, filePair);
            }
        }
    }
    
    private Entity createOrVerifySample(Entity topLevelFolder, String name, TilingPattern tiling, String channelSpec) throws Exception {
    	
    	Entity tilingPatternFolder = verifyOrCreateChildFolder(topLevelFolder, tiling.getName());
        Entity sample = findExistingSample(tilingPatternFolder, name);
        if (sample == null) {
	        sample = createSample(name, tiling, channelSpec);
	        addToParent(tilingPatternFolder, sample, tilingPatternFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        }
        else {
        	if (!EntityUtils.areLoaded(sample.getEntityData())) {
        		entityBean.loadLazyEntity(sample, false);
        	}
        }
        return sample;
    }

    private void addLsmPairToSample(Entity sample, FilePair filePair) throws Exception {
    	
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
    	
		EntityData lsmStackPairEd = EntityUtils.findChildEntityDataWithNameAndType(supportingFiles, filePair.getPairTag(), EntityConstants.TYPE_LSM_STACK_PAIR);
		Entity lsmStackPair = null;
		
		if (lsmStackPairEd != null) {
			lsmStackPair = lsmStackPairEd.getChildEntity();
			// The LSM pair already exists, check if the LSMs have changed
			Entity stack1 = lsmStackPair.getChildByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_1);
			Entity stack2 = lsmStackPair.getChildByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_2);
			if (stack1==null && stack2==null) {
				// Both pairs are missing, this stack pair is vestigial and can be trashed
				logger.warn("Stack pair exists, but has no stacks. Deleting it (id="+lsmStackPair.getId()+")");
				entityBean.deleteEntityById(lsmStackPair.getId());
				lsmStackPair = null;
			}
			else if ((stack1==null&&filePair.getFile1()!=null) || (stack1!=null && filePair.getFile1()!=null && !stack1.getName().equals(filePair.getFile1().getName())) || 
					(stack2==null&&filePair.getFile2()!=null) || (stack2!=null && filePair.getFile2()!=null && !stack2.getName().equals(filePair.getFile2().getName()))) {
				// One or more of the LSMs have changed, so move the old pair out of the way and then recreate it
				logger.warn("One or more of the LSMs have changed. Renaming the stack pair and recreating it.");
				lsmStackPair.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
				lsmStackPair.setName(lsmStackPair.getName()+" (old)");
				entityBean.saveOrUpdateEntity(lsmStackPair);
	            logger.info("Renamed old LSM stack pair to '"+lsmStackPair.getName()+"' (id="+lsmStackPair.getId()+")");
				lsmStackPair = null;
			}
		}
		
		if (lsmStackPair == null) {
			lsmStackPair = new Entity();
            lsmStackPair.setUser(user);
            lsmStackPair.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
            lsmStackPair.setCreationDate(createDate);
            lsmStackPair.setUpdatedDate(createDate);
            lsmStackPair.setName(filePair.getPairTag());
            lsmStackPair = entityBean.saveOrUpdateEntity(lsmStackPair);
            logger.info("Saved LSM stack pair for '"+filePair.getPairTag()+"' as "+lsmStackPair.getId());
        	addToParent(supportingFiles, lsmStackPair, null, EntityConstants.ATTRIBUTE_ENTITY);
        	
			logger.info("Adding LSM file to sample: "+filePair.getFile1().getAbsolutePath());
			Entity lsmEntity1 = createLsmStackFromFile(filePair.getFile1());
            addToParent(lsmStackPair, lsmEntity1, 0, EntityConstants.ATTRIBUTE_LSM_STACK_1);

            if (filePair.getFile2()!=null) {
				logger.info("Adding LSM file to sample: "+filePair.getFile2().getAbsolutePath());
				Entity lsmEntity2 = createLsmStackFromFile(filePair.getFile2());
	            addToParent(lsmStackPair, lsmEntity2, 1, EntityConstants.ATTRIBUTE_LSM_STACK_2);
            }
		}
    }
    
    /**
     * Find and return the child Sample entity
     */
    private Entity findExistingSample(Entity folder, String sampleIdentifier) {

    	for(EntityData ed : folder.getEntityData()) {
			Entity sample = ed.getChildEntity();
    		if (sample == null) continue;
    		if (!EntityConstants.TYPE_SAMPLE.equals(sample.getEntityType().getName())) continue;
    		if (!sample.getName().equals(sampleIdentifier)) continue;
    	    return sample;
    	}

        // Could not find sample child entity
    	return null;
    }

    protected Entity createSample(String name, TilingPattern tiling, String channelSpec) throws Exception {
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_TYPES, alignmentTypes);
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
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

    private Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }

    protected void fixOrderIndices(Entity topLevelFolder) throws Exception {
		logger.info("Fixing sample order indicies");
		
    	for(Entity tilingPatternFolder : topLevelFolder.getChildren()) {
    		if (tilingPatternFolder.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
    			
    			logger.info("Fixing order indicies in "+tilingPatternFolder.getName());

    	    	List<EntityData> orderedData = new ArrayList<EntityData>();
    			for(EntityData ed : tilingPatternFolder.getEntityData()) {
    				if (ed.getChildEntity()!=null) {
    					orderedData.add(ed);
    				}
    			}
    	    	Collections.sort(orderedData, new Comparator<EntityData>() {
    				@Override
    				public int compare(EntityData o1, EntityData o2) {
    					return o1.getChildEntity().getCreationDate().compareTo(o2.getChildEntity().getCreationDate());
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
        EJBFactory.getLocalEntityBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }

    
    private class SlideImage {
    	String slideCode;
    	String imagePath;
    	String tileType;
    	String age;
    	String gender;
    	String effector;
    	String line;
    	String channelSpec;
    }
    
    private class FilePair {
        private String pairTag;
        private String filename1;
        private String filename2;
        private File file1;
        private File file2;

        public FilePair(String pairTag) {
            this.pairTag = pairTag;
        }

        public String getPairTag() {
			return pairTag;
		}

		public String getFilename1() {
			return filename1;
		}

		public void setFilename1(String filename1) {
			this.filename1 = filename1;
		}

		public String getFilename2() {
			return filename2;
		}

		public void setFilename2(String filename2) {
			this.filename2 = filename2;
		}

		public File getFile1() {
			return file1;
		}

		public void setFile1(File file1) {
			this.file1 = file1;
		}

		public File getFile2() {
			return file2;
		}

		public void setFile2(File file2) {
			this.file2 = file2;
		}

		public boolean isPairingComplete() {
            return (null!=filename1&&!"".equals(filename1)) &&
                   (null!=filename2&&!"".equals(filename2));
        }
    }
}
