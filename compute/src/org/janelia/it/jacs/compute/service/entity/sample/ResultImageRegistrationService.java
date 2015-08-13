package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.entity.cv.SampleImageType;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Start at some result entity within a Pipeline run, and look for 2d images corresponding to 3d images. These 
 * should be set as attributes of the Result, the Pipeline Run, and the Sample.
 * 
 * Parameters:
 *   RESULT_ENTITY_ID - the id of the root entity to look for 2d images within
 *   PIPELINE_RUN_ENTITY_ID - the id of the pipeline run entity to register with 
 *                            (needed because a result may be reused across multiple pipeline runs)
 *   DEFAULT_IMAGE_FILENAME - the file to use as the "Default 2D Image" for the root entity
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultImageRegistrationService extends AbstractEntityService {
	
	private Map<Long,Entity> images3d = new HashMap<Long,Entity>();
	private Map<String,Entity> allMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> signalMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> refMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> moviePrefixMap = new HashMap<String,Entity>();
	
	public void execute() throws Exception {

        String defaultImageFilename = data.getItemAsString("DEFAULT_IMAGE_FILENAME");
    	
    	Long resultEntityId = data.getRequiredItemAsLong("RESULT_ENTITY_ID");
    	Entity resultEntity = entityBean.getEntityTree(resultEntityId);
    	if (resultEntity == null) {
    		throw new IllegalArgumentException("Entity not found with id="+resultEntityId);
    	}

        Long pipelineRunEntityId = data.getRequiredItemAsLong("PIPELINE_RUN_ENTITY_ID");
        Entity pipelineRunEntity = entityBean.getEntityTree(pipelineRunEntityId);
        if (pipelineRunEntity == null) {
            throw new IllegalArgumentException("Entity not found with id="+resultEntityId);
        }

    	registerImages(pipelineRunEntity, resultEntity, defaultImageFilename);
    }

	public void execute(IProcessData processData, Entity pipelineRunEntity, Entity resultEntity, String defaultImageFilename) throws Exception {

        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.task = ProcessDataHelper.getTask(processData);
        this.processData = processData;
        this.entityBean = EJBFactory.getLocalEntityBean();
        this.computeBean = EJBFactory.getLocalComputeBean();
        this.annotationBean = EJBFactory.getLocalAnnotationBean();
        String ownerName = ProcessDataHelper.getTask(processData).getOwner();
        Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
        this.ownerKey = subject.getKey();
        this.entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger);
        this.entityLoader = new EntityBeanEntityLoader(entityBean);
        
    	registerImages(pipelineRunEntity, resultEntity, defaultImageFilename);
    }
	
	private void registerImages(Entity pipelineRunEntity, Entity resultEntity, String defaultImageFilename) throws Exception {

	    populateChildren(resultEntity);
	    
	    Entity sampleEntity = entityBean.getAncestorWithType(resultEntity, EntityConstants.TYPE_SAMPLE);

        Entity default3dImage = null;
        
	    if (defaultImageFilename==null) {
	        default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
	        if (default3dImage==null) {
	            default3dImage = findDefaultImage(resultEntity);
	        }
	        if (default3dImage!=null) {
	        	defaultImageFilename = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	        }
	    }
	    
	    logger.info("Registering images for result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
	    if (default3dImage!=null) {
	        logger.info("Using default image: "+default3dImage.getName()+" (id="+default3dImage.getId()+")");
	    }
	    else if (defaultImageFilename!=null) {
	        logger.info("Will find default image: "+defaultImageFilename);
	    }
	    
    	// Find all the 2d and 3d images in this result tree, and populate all of the lookup maps and lists
    	
    	findImages(resultEntity);
    	logger.info("Found "+images3d.size()+" 3d images");
    	logger.info("Found "+allMipPrefixMap.size()+" all MIPs");
    	logger.info("Found "+signalMipPrefixMap.size()+" signal MIPs");
    	logger.info("Found "+refMipPrefixMap.size()+" ref MIPs");
    	logger.info("Found "+moviePrefixMap.size()+" movies");
    	
    	// Ensure all 3d images have their shortcut images correctly set. At the same time, find which of these
    	// 3d images is the default image for this result.

    	String defaultImageCanonicalFilename = defaultImageFilename == null ? null : new File(defaultImageFilename).getCanonicalPath();
    	
    	for(Entity image3d : images3d.values()) {
			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

	        populateChildren(image3d);
	        
			logger.trace("  Processing "+image3d.getName()+" (id="+image3d.getId()+")");
			
			if (default3dImage==null && (filepath.equals(defaultImageFilename) || filepath.equals(defaultImageCanonicalFilename))) {
			    logger.info("  Found default 3d image");
				default3dImage = image3d;
			}
				
			if (image3d.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
			
				Pattern p = Pattern.compile("^(.*?)\\.(\\w+?)$");
				Matcher m = p.matcher(filepath);
				
				if (m.matches()) {
					String prefix = m.group(1);
	
					Entity allMip = allMipPrefixMap.get(prefix);
					Entity signalMip = signalMipPrefixMap.get(prefix);
					Entity refMip = refMipPrefixMap.get(prefix);
	
					if (allMip!=null) {
				    	EntityData currAllMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE);
				    	if (currAllMip==null || currAllMip.getChildEntity()==null || !currAllMip.getId().equals(allMip.getId())) {
				    	    logger.info("    Setting all MIP on "+image3d.getName()+" to: "+allMip.getName());
				    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE, allMip);
				    	}
					}
					
					if (signalMip!=null) {
				    	EntityData currDefault2dImage = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
				    	if (currDefault2dImage==null || currDefault2dImage.getChildEntity()==null || !currDefault2dImage.getId().equals(signalMip.getId())) {
				    	    logger.info("    Setting default 2d MIP on "+image3d.getName()+" to: "+signalMip.getName());
				    		entityHelper.setDefault2dImage(image3d, signalMip);
				    	}
				    	EntityData currSignalMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
				    	if (currSignalMip==null || currSignalMip.getChildEntity()==null || !currSignalMip.getId().equals(signalMip.getId())) {
				    	    logger.info("    Setting signal MIP on "+image3d.getName()+" to: "+signalMip.getName());
				    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
				    	}
					}
					
					if (refMip!=null) {
				    	EntityData currRefMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
				    	if (currRefMip==null || currRefMip.getChildEntity()==null || !currRefMip.getId().equals(refMip.getId())) {
				    	    logger.info("    Setting reference MIP on "+image3d.getName()+" to: "+refMip.getName());
				    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
				    	}
				    	if (signalMip==null) {
				    	    // No signal MIP, use the reference as the default 
				    	    logger.info("    Setting default 2d MIP on "+image3d.getName()+" to: "+refMip.getName());
	                        entityHelper.setDefault2dImage(image3d, refMip);
				    	}
					}
				}	
			}
    	}
    	
    	if (default3dImage!=null) {
            logger.info("Applying default 3d image to the result ("+resultEntity.getId()+")");
        	entityHelper.setDefault3dImage(resultEntity, default3dImage);
            logger.info("Applying default 3d image to the pipeline run ("+pipelineRunEntity.getId()+")");
        	entityHelper.setDefault3dImage(pipelineRunEntity, default3dImage);

        	Entity topLevelSample = sampleEntity;
        	if (sampleEntity.getName().contains("~")) {
	            Entity parentSample = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
	            if (parentSample==null) {
	                // Already at top level sample
	            	logger.warn("Sub-sample "+sampleEntity.getName()+" has no ancestor sample");
	            }
	            else {
	                // Set the image on the sub-sample
	                logger.info("Applying default 3d image to the sub-sample ("+sampleEntity.getName()+")");
	                entityHelper.setDefault3dImage(sampleEntity, default3dImage);
	                topLevelSample = parentSample;
	            }
        	}
            
            // Set the top level sample, if this image matches the user's preference for the sample's data set
            
        	String dataSetIdentifier = topLevelSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);
            if (dataSet!=null) {
                String sampleImageTypeName = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);
                logger.debug("Sample image type is: "+sampleImageTypeName);
                if (sampleImageTypeName!=null) {
                    SampleImageType sampleImageType = SampleImageType.valueOf(sampleImageTypeName);
                    if (sampleShouldUseResultImage(sampleEntity, sampleImageType, default3dImage)) {
                        logger.debug("Applying default 3d image to the top-level sample ("+topLevelSample.getName()+")");
                        entityHelper.setDefault3dImage(topLevelSample, default3dImage);  
                    }
                }
                else {
                    // Default to Latest
                    logger.info("Applying default 3d image to the top-level sample ("+topLevelSample.getName()+")");
                    entityHelper.setDefault3dImage(topLevelSample, default3dImage);        
                }
            }
            
        	// Find and apply fast 3d image, if available
    		Entity separation = findSeparation(resultEntity, default3dImage);
    		if (separation!=null) {
            	Entity fast3dImage = findFast3dImage(separation);
            	entityHelper.setDefault3dImage(separation, default3dImage);
            	if (fast3dImage!=null) {
            		logger.info("Found default fast 3d image in separation "+separation.getId()+", applying to "+default3dImage.getName());
            		entityHelper.setImageIfNecessary(default3dImage, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fast3dImage);
        		}
    		}
            
            processData.putItem("DEFAULT_IMAGE_ID", default3dImage.getId().toString());
    	}		
    	else {
    		// Is there a montage?
    		Entity allMip = getMontage(allMipPrefixMap);
    		Entity signalMip = getMontage(signalMipPrefixMap);
    		Entity refMip = getMontage(refMipPrefixMap);

    		if (allMip!=null || signalMip!=null ||  refMip!=null) {
	            logger.info("Applying 2d montages to the result ("+resultEntity.getId()+")");
	        	entityHelper.set2dImages(resultEntity, signalMip, allMip, signalMip, refMip);
	            logger.info("Applying 2d montages to the pipeline run ("+pipelineRunEntity.getId()+")");
	        	entityHelper.set2dImages(pipelineRunEntity, signalMip, allMip, signalMip, refMip);
    		}
    	}
    	
    	// Finally, set the images on the tiles and LSMs, if they are available in this result
    	
    	populateChildren(sampleEntity);
    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles!=null) {
    		populateChildren(supportingFiles);
    	
            for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE)) {
            	selectAndSetMIPs(imageTile, "(.*?)/(merged|tile)-"+imageTile.getId());
            	
        		populateChildren(imageTile);
                for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                	String name = ArchiveUtils.getDecompressedFilepath(lsmStack.getName());
                	String imageName = name.substring(0, name.lastIndexOf('.'));
                	selectAndSetMIPs(lsmStack, "(.*?)/"+imageName);
                }
            	
            }
    	}
	}
	
	private Entity getMontage(Map<String,Entity> prefixMap) {
    	for(String key : prefixMap.keySet()) {
    		if (key.endsWith("montage")) {
    			return prefixMap.get(key);
    		}
    	}
    	return null;
	}
	
	private void selectAndSetMIPs(Entity imageTile, String keyPattern) throws ComputeException {
    	Entity allMip = findMatchingEntity(allMipPrefixMap, keyPattern);
    	Entity signalMip = findMatchingEntity(signalMipPrefixMap, keyPattern);
    	Entity refMip = findMatchingEntity(refMipPrefixMap, keyPattern);
    	Entity movie = findMatchingEntity(moviePrefixMap, keyPattern);
    	setMIPs(imageTile, allMip, signalMip, refMip, movie);
	}

	private Entity findMatchingEntity(Map<String,Entity> prefixMap, String keyPattern) {
    	Entity image = null;
    	for(String key : prefixMap.keySet()) {
    		if (key.matches(keyPattern)) {
    			if (image!=null) {
    				logger.warn("Multiple matches for "+keyPattern+" in prefix map");
    			}
    			image = prefixMap.get(key);
    		}
    	}
    	return image;
	}
	
	private void setMIPs(Entity entity, Entity allMip, Entity signalMip, Entity refMip, Entity movie) throws ComputeException {
        logger.info("Applying MIP and movies on "+entity.getName()+" (id="+entity.getId()+")");
        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMip==null?refMip:signalMip);
        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE, allMip);
        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, movie);
	}
	
	/**
	 * Find the corresponding neuron separation for the given 3d image. 
	 * @param resultEntity
	 * @param default3dImage
	 * @return
	 */
	private Entity findSeparation(Entity resultEntity, Entity default3dImage) {
		Entity foundEntity = null;
		for(Entity separation : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
			// We'll take the latest if we don't find one matching our input image, because not all entity types keep track of the Input Image attribute
			foundEntity = separation;
    		EntityData inputImageEd = foundEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_INPUT_IMAGE);
			if (inputImageEd!=null && inputImageEd.getChildEntity()!=null && inputImageEd.getChildEntity().getId().equals(default3dImage.getId())) {
				// Found it, so let's stop looking
				break;
			}
		}
		return foundEntity;
	}
	
    private Entity findDefaultImage(Entity result) throws Exception {
        
        Entity supportingFiles = EntityUtils.getSupportingData(result);
        Entity defaultImage = null;
        
        String resultDefault2dImage = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        int priority = 0;

        populateChildren(supportingFiles);
        for (Entity file : supportingFiles.getChildren()) {
            String filename = file.getName();
            String filepath = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            String fileDefault2dImage = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
            if (StringUtils.isEmpty(filepath))
                continue;

            logger.debug("    Considering " + filename);
            logger.debug("      filepath: " + filepath);
            if (fileDefault2dImage != null) {
                logger.debug("      default 2d image: " + fileDefault2dImage);
            }

            if (resultDefault2dImage != null && resultDefault2dImage.equals(fileDefault2dImage) && priority < 20) {
                defaultImage = file;
                priority = 20;
                logger.debug("      Using as default image");
            }
            if (filename.matches("Aligned.v3d(raw|pbd)") && priority < 10) {
                defaultImage = file;
                priority = 10;
                logger.debug("      Using as default image");
            } else if (filename.matches("stitched-(\\w+?).v3d(raw|pbd)") && priority < 9) {
                defaultImage = file;
                priority = 9;
                logger.debug("      Using as default image");
            } else if (filename.matches("tile-(\\w+?).v3d(raw|pbd)") && priority < 8) {
                defaultImage = file;
                priority = 8;
                logger.debug("      Using as default image");
            } else if (filename.matches("merged-(\\w+?).v3d(raw|pbd)") && priority < 7) {
                defaultImage = file;
                priority = 7;
                logger.debug("      Using as default image");
            }
        }
        
        if (defaultImage!=null) {
            logger.info("  Inferred default image: "+defaultImage.getName());
        }
        return defaultImage;
    }

    /**
	 * Returns true if the given 3d image should be used as a sample image, given the user's preferred sample image type.
	 * @param sampleImageType
	 * @param image3d
	 * @return
	 */
	public boolean sampleShouldUseResultImage(Entity sample, SampleImageType sampleImageType, Entity image3d) {
        
	    logger.info("sampleShouldUseResultImage? sampleImageType: "+sampleImageType);
	    
	    if (sampleImageType==null || sampleImageType==SampleImageType.Latest) {
	        // Use any image, if the user wants the latest
	        return true;
	    }

        String objectiveName = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        logger.info("sampleShouldUseResultImage? objectiveName: "+objectiveName);
        
	    if (objectiveName==null) {
	        // Image has no objective, and user has specified an objective
	        return false;
	    }
	    
	    String alignmentSpace = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
	    logger.info("sampleShouldUseResultImage? alignmentSpace: "+alignmentSpace);
	    
        switch (sampleImageType) {
        case Latest: return true;
        case Unaligned20x: return alignmentSpace==null && Objective.OBJECTIVE_20X.getName().equals(objectiveName);
        case Unaligned63x: return alignmentSpace==null && Objective.OBJECTIVE_63X.getName().equals(objectiveName);
		// This code walks the tree and the 63X will be visited last in both cases that match
		case LatestUnaligned: return alignmentSpace==null;
		case LatestAligned: return alignmentSpace!=null;
        case Aligned20x: return alignmentSpace!=null && Objective.OBJECTIVE_20X.getName().equals(objectiveName);
        case Aligned63x: return alignmentSpace!=null && Objective.OBJECTIVE_63X.getName().equals(objectiveName);
        }
        return false;
	}
	
	private void findImages(Entity entity) throws Exception {
		
		String entityType = entity.getEntityTypeName();
		if (entityType.equals(EntityConstants.TYPE_IMAGE_2D) || entityType.equals(EntityConstants.TYPE_MOVIE)) {
			String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			Pattern p = Pattern.compile("^(.*)(_.*)\\.(\\w+)$");
			Matcher m = p.matcher(filepath);
			
			if (m.matches()) {
				String prefix = m.group(1);
				String type = m.group(2);
				String ext = m.group(3);
				
				logger.debug("Found prefix="+prefix+", type="+type+", ext="+ext);
				
				if ("png".equals(ext)) {
					if ("_all".equals(type)) {
						allMipPrefixMap.put(prefix, entity);
					}
					else if ("_signal".equals(type)) {
						signalMipPrefixMap.put(prefix, entity);
					}
					else if ("_reference".equals(type)) {
						refMipPrefixMap.put(prefix, entity);
					}
				}
				else if ("mp4".equals(ext)) {
					if ("_movie".equals(type)) {
						moviePrefixMap.put(prefix, entity);
					}
				}
			}
		}
		else if (entityType.equals(EntityConstants.TYPE_IMAGE_3D) || entityType.equals(EntityConstants.TYPE_MOVIE)) {
			images3d.put(entity.getId(), entity);
		}
		else {
			populateChildren(entity);
			for(Entity child : entity.getChildren()) {
				findImages(child);
			}
		}
	}
	
	private Entity findFast3dImage(Entity separation) throws Exception {
		Entity supportingFiles = EntityUtils.getSupportingData(separation);
		if (supportingFiles==null) return null;
		
        // Should find it here
		Entity signalVolume = EntityUtils.findChildWithName(supportingFiles, "ConsolidatedSignal.v3dpbd");
		if (signalVolume!=null) {
		    Entity fast3dImage = signalVolume.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
            if (fast3dImage!=null) {
                return fast3dImage;
            }
		}
		
        // If not, try looking in the old location
    	Entity fastLoad = EntityUtils.findChildWithName(supportingFiles, "Fast Load");
		if (fastLoad==null) return null;

        populateChildren(fastLoad);
		return EntityUtils.findChildWithName(fastLoad, "ConsolidatedSignal2_25.mp4");
	}
}
