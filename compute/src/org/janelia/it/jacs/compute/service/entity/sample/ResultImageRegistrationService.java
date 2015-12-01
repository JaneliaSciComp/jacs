package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.entity.cv.SampleImageType;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

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
	
    private List<InputImage> inputImages;
    
	private Map<Long,Entity> images3d = new HashMap<Long,Entity>();
	private Map<String,Entity> allMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> signalMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> refMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> moviePrefixMap = new HashMap<String,Entity>();
	
	public void execute() throws Exception {

        this.inputImages = (List<InputImage>)data.getItem("INPUT_IMAGES");
        
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
        this.contextLogger = new ContextLogger(logger);
        this.task = ProcessDataHelper.getTask(processData);
        this.contextLogger.appendToLogContext(task);
        this.processData = processData;
        this.data = new ProcessDataAccessor(processData, contextLogger);
        this.entityBean = EJBFactory.getLocalEntityBean();
        this.computeBean = EJBFactory.getLocalComputeBean();
        this.annotationBean = EJBFactory.getLocalAnnotationBean();
        this.solrBean = EJBFactory.getLocalSolrBean();
        
        String ownerName = ProcessDataHelper.getTask(processData).getOwner();
        Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
        this.ownerKey = subject.getKey();
        
        this.entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger, contextLogger);
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
	    
	    contextLogger.info("Registering images for result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
	    if (default3dImage!=null) {
	        contextLogger.info("Using default image: "+default3dImage.getName()+" (id="+default3dImage.getId()+")");
	    }
	    else if (defaultImageFilename!=null) {
	        contextLogger.info("Will find default image: "+defaultImageFilename);
	    }
	    
    	// Find all the 2d and 3d images in this result tree, and populate all of the lookup maps and lists
    	
    	findArtifacts(resultEntity);
    	contextLogger.info("Found "+images3d.size()+" 3d images");
    	contextLogger.info("Found "+allMipPrefixMap.size()+" all MIPs");
    	contextLogger.info("Found "+signalMipPrefixMap.size()+" signal MIPs");
    	contextLogger.info("Found "+refMipPrefixMap.size()+" ref MIPs");
    	contextLogger.info("Found "+moviePrefixMap.size()+" movies");
    	
    	// Ensure all 3d images have their shortcut images correctly set. At the same time, find which of these
    	// 3d images is the default image for this result.

    	String defaultImageCanonicalFilename = defaultImageFilename == null ? null : new File(defaultImageFilename).getCanonicalPath();
    	
    	for(Entity image3d : images3d.values()) {
    	    populateChildren(image3d);
    	    logger.trace("  Processing "+image3d.getName()+" (id="+image3d.getId()+")");
			
			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			if (default3dImage==null && (filepath.equals(defaultImageFilename) || filepath.equals(defaultImageCanonicalFilename))) {
			    logger.trace("    Found default 3d image");
				default3dImage = image3d;
			}
				
			if (image3d.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
			
			    File file = new File(filepath);
				Pattern p = Pattern.compile("^(.*?)\\.(\\w+?)$");
				Matcher m = p.matcher(file.getName());
				
				if (m.matches()) {
					String prefix = m.group(1);

					contextLogger.debug("Get artifacts with prefix: '"+prefix+"'");
                    
					Entity allMip = allMipPrefixMap.get(prefix);
					Entity signalMip = signalMipPrefixMap.get(prefix);
					Entity refMip = refMipPrefixMap.get(prefix);
                    Entity movie = moviePrefixMap.get(prefix);
	
                    contextLogger.debug("  allMip="+allMip);
                    contextLogger.debug("  signalMip="+signalMip);
                    contextLogger.debug("  refMip="+refMip);
                    contextLogger.debug("  movie="+movie);
					
                    setMIPs(image3d, allMip, signalMip, refMip, movie); 
				}	
			}
    	}

        Entity topLevelSample = sampleEntity;
        if (sampleEntity.getName().contains("~")) {
            Entity parentSample = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
            if (parentSample==null) {
                // Already at top level sample
                logger.warn("Sub-sample "+sampleEntity.getName()+" has no ancestor sample");
            }
            else {
                topLevelSample = parentSample;
            }
        }

        SampleImageType sampleImageType = SampleImageType.Latest;
        String dataSetIdentifier = topLevelSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);
        if (dataSet!=null) {
            String sampleImageTypeName = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);
            contextLogger.debug("Sample image type is: "+sampleImageTypeName);
            if (sampleImageTypeName!=null) {
                sampleImageType = SampleImageType.valueOf(sampleImageTypeName);
            }
        }
        
    	if (default3dImage!=null) {
    	    contextLogger.info("Applying default 3d image to the result ("+resultEntity.getId()+")");
        	entityHelper.setDefault3dImage(resultEntity, default3dImage);
        	contextLogger.info("Applying default 3d image to the pipeline run ("+pipelineRunEntity.getId()+")");
        	entityHelper.setDefault3dImage(pipelineRunEntity, default3dImage);

            // Set the image on the sub-sample
        	contextLogger.info("Applying default 3d image to the sub-sample ("+sampleEntity.getName()+")");
            entityHelper.setDefault3dImage(sampleEntity, default3dImage);
            
            // Set the top level sample, if this image matches the user's preference for the sample's data set
            if (sampleEntity!=topLevelSample && sampleShouldUseResultImage(sampleEntity, sampleImageType, default3dImage)) {
                contextLogger.debug("Applying default 3d image to the top-level sample ("+topLevelSample.getName()+")");
                entityHelper.setDefault3dImage(topLevelSample, default3dImage);  
            }
            
        	// Find and apply fast 3d image, if available
    		Entity separation = findSeparation(resultEntity, default3dImage);
    		if (separation!=null) {
            	Entity fast3dImage = findFast3dImage(separation);
            	entityHelper.setDefault3dImage(separation, default3dImage);
            	if (fast3dImage!=null) {
            	    contextLogger.info("Found default fast 3d image in separation "+separation.getId()+", applying to "+default3dImage.getName());
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

    		if (allMip!=null || signalMip!=null || refMip!=null) {
                Entity defaultMip = allMip==null?signalMip:allMip;
    		    contextLogger.info("Applying 2d montages to the result ("+resultEntity.getId()+")");
	        	entityHelper.set2dImages(resultEntity, defaultMip, allMip, signalMip, refMip);
	        	contextLogger.info("Applying 2d montages to the pipeline run ("+pipelineRunEntity.getId()+")");
	        	entityHelper.set2dImages(pipelineRunEntity, defaultMip, allMip, signalMip, refMip);
    		}
    		else {
    		    // No montage found, we need to pick an image, so let's pick the first one alphabetically (this works well for Brain/VNC at least)

    		    List<String> keys = new ArrayList<>(allMipPrefixMap.keySet());
    		    if (!keys.isEmpty()) {
    		        Collections.sort(keys, new Comparator<String>() {
    		            @Override
    		            public int compare(String o1, String o2) {
    		                return ComparisonChain.start()
    		                        .compare(!o1.contains("stitched"), !o2.contains("stitched"), Ordering.natural()) // stitched first
    		                        .compare(!o1.contains("brain"), !o2.contains("brain"), Ordering.natural()) // Brain before VNC
    		                        .compare(!o1.contains("ventral_nerve_cord"), !o2.contains("ventral_nerve_cord"), Ordering.natural()) // Brain before VNC
    		                        .compare(o1, o2, Ordering.natural().nullsLast()).result(); // Order by filename
    		            }
    		        });
        	        
        		    String defaultKey = keys.get(0);

                    allMip = allMipPrefixMap.get(defaultKey);
                    signalMip = signalMipPrefixMap.get(defaultKey);
                    refMip = refMipPrefixMap.get(defaultKey);
                    Entity defaultMip = allMip==null?signalMip:allMip;
                    
                    if (allMip!=null || signalMip!=null || refMip!=null) {
                        contextLogger.info("Applying first 2d image to the result ("+resultEntity.getId()+")");
                        entityHelper.set2dImages(resultEntity, defaultMip, allMip, signalMip, refMip);
                        contextLogger.info("Applying first 2d image to the pipeline run ("+pipelineRunEntity.getId()+")");
                        entityHelper.set2dImages(pipelineRunEntity, defaultMip, allMip, signalMip, refMip);
                    }
                    
                    // Apply 2d images to sample
                    if (inputImages!=null) {
                        
                        contextLogger.info("Searching for input image with output prefix of "+defaultKey);
                        for(InputImage inputImage : inputImages) {
                            contextLogger.info("Considering "+inputImage.getOutputPrefix());
                            if (inputImage.getOutputPrefix().equals(defaultKey)) {
                                List<Entity> matches = entityBean.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_FILE_PATH, inputImage.getFilepath());
                                if (!matches.isEmpty()) {
                                    default3dImage = matches.get(0);
                                }
                                contextLogger.info("Found id="+default3dImage.getId());
                            }
                        }

                        // Set the top level sample, if this image matches the user's preference for the sample's data set
                        if (default3dImage!=null && sampleShouldUseResultImage(sampleEntity, sampleImageType, default3dImage)) {

                            // Set the images on the sub-sample
                            contextLogger.info("Applying first 2d image to the sub-sample ("+sampleEntity.getId()+")");
                            entityHelper.set2dImages(sampleEntity, defaultMip, allMip, signalMip, refMip);
                            
                            // Set the top level sample, if this image matches the user's preference for the sample's data set
                            if (sampleEntity!=topLevelSample && sampleShouldUseResultImage(sampleEntity, sampleImageType, default3dImage)) {
                                contextLogger.info("Applying first 2d image to the top-level sample ("+topLevelSample.getId()+")");
                                entityHelper.set2dImages(topLevelSample, defaultMip, allMip, signalMip, refMip);
                            }
                        }
                        
                    }
    		    }
    		}
    	}
    	
    	// Finally, set the images on the tiles and LSMs, if they are available in this result
    	
    	populateChildren(sampleEntity);
    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles!=null) {
    		populateChildren(supportingFiles);
    	
            for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE)) {
            	selectAndSetMIPs(imageTile, "(merged|tile)-"+imageTile.getId());
            	
        		populateChildren(imageTile);
                for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                	String name = ArchiveUtils.getDecompressedFilepath(lsmStack.getName());
                	String imageName = FileUtils.getFilePrefix(name);
                	selectAndSetMIPs(lsmStack, imageName);
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
	    if (allMip==null && signalMip==null && refMip==null && movie==null) return;
	    contextLogger.info("Applying MIP and movies on "+entity.getName()+" (id="+entity.getId()+")");
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

            contextLogger.debug("    Considering " + filename);
            contextLogger.debug("      filepath: " + filepath);
            if (fileDefault2dImage != null) {
                contextLogger.debug("      default 2d image: " + fileDefault2dImage);
            }

            if (resultDefault2dImage != null && resultDefault2dImage.equals(fileDefault2dImage) && priority < 20) {
                defaultImage = file;
                priority = 20;
                contextLogger.debug("      Using as default image");
            }
            if (filename.matches("Aligned.v3d(raw|pbd)") && priority < 10) {
                defaultImage = file;
                priority = 10;
                contextLogger.debug("      Using as default image");
            } else if (filename.matches("stitched-(\\w+?).v3d(raw|pbd)") && priority < 9) {
                defaultImage = file;
                priority = 9;
                contextLogger.debug("      Using as default image");
            } else if (filename.matches("tile-(\\w+?).v3d(raw|pbd)") && priority < 8) {
                defaultImage = file;
                priority = 8;
                contextLogger.debug("      Using as default image");
            } else if (filename.matches("merged-(\\w+?).v3d(raw|pbd)") && priority < 7) {
                defaultImage = file;
                priority = 7;
                contextLogger.debug("      Using as default image");
            }
        }
        
        if (defaultImage!=null) {
            contextLogger.info("  Inferred default image: "+defaultImage.getName());
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
        
	    contextLogger.info("sampleShouldUseResultImage? sampleImageType: "+sampleImageType);
	    
	    if (sampleImageType==null || sampleImageType==SampleImageType.Latest) {
	        // Use any image, if the user wants the latest
	        return true;
	    }

        String objectiveName = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        contextLogger.info("sampleShouldUseResultImage? objectiveName: "+objectiveName);
        
	    if (objectiveName==null) {
	        // Image has no objective, and user has specified an objective
	        return false;
	    }
	    
	    String alignmentSpace = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
	    contextLogger.info("sampleShouldUseResultImage? alignmentSpace: "+alignmentSpace);
	    
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
	
	private void findArtifacts(Entity entity) throws Exception {
		
		String entityType = entity.getEntityTypeName();
		if (entityType.equals(EntityConstants.TYPE_IMAGE_2D) || entityType.equals(EntityConstants.TYPE_MOVIE)) {
			String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			
			File file = new File(filepath);
			Pattern p = Pattern.compile("^(.*)(_.*)\\.(\\w+)$");
			Matcher m = p.matcher(file.getName());
			
			if (m.matches()) {
				String prefix = m.group(1);
				String type = m.group(2);
				String ext = m.group(3);
				
				contextLogger.debug("Found artifact: prefix="+prefix+", type="+type+", ext="+ext);
				
				if ("png".equals(ext)) {
					if ("_all".equals(type) || "".equals(type)) {
						allMipPrefixMap.put(prefix, entity);
					}
					else if ("_signal".equals(type) || "_sig".equals(type)) {
						signalMipPrefixMap.put(prefix, entity);
					}
					else if ("_reference".equals(type) || "_ref".equals(type)) {
						refMipPrefixMap.put(prefix, entity);
					}
				}
				else if ("mp4".equals(ext)) {
					if ("_movie".equals(type) || "_signal".equals(type) || "".equals(type)) {
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
				findArtifacts(child);
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
