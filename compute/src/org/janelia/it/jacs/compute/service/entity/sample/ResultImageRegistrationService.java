package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.entity.cv.SampleImageType;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Start at some result entity within a Pipeline run, and look for 2d images corresponding to 3d images. These 
 * should be set as attributes of the Result, the Pipeline Run, and the Sample.
 * 
 * Parameters:
 *   RESULT_ENTITY_ID - the id of the root entity to look for 2d images within
 *   DEFAULT_IMAGE_FILENAME - the file to use as the "Default 2D Image" for the root entity
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultImageRegistrationService extends AbstractEntityService {
	
	private Map<Long,Entity> images3d = new HashMap<Long,Entity>();
	private Map<String,Entity> signalMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> refMipPrefixMap = new HashMap<String,Entity>();
	
	public void execute() throws Exception {

        String defaultImageFilename = (String)processData.getItem("DEFAULT_IMAGE_FILENAME");
    	
    	String resultEntityId = (String)processData.getItem("RESULT_ENTITY_ID");
    	if (StringUtils.isEmpty(resultEntityId)) {
    		throw new IllegalArgumentException("RESULT_ENTITY_ID may not be null");
    	}
    	
    	Entity resultEntity = entityBean.getEntityTree(new Long(resultEntityId));
    	if (resultEntity == null) {
    		throw new IllegalArgumentException("Entity not found with id="+resultEntityId);
    	}
    	
    	registerImages(resultEntity, defaultImageFilename);
    }

	public void execute(IProcessData processData, Entity resultEntity, String defaultImageFilename) throws Exception {

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
    	registerImages(resultEntity, defaultImageFilename);
    }
	
	private void registerImages(Entity resultEntity, String defaultImageFilename) throws Exception {

	    Entity pipelineRunEntity = entityBean.getAncestorWithType(resultEntity, EntityConstants.TYPE_PIPELINE_RUN);
	    Entity sampleEntity = entityBean.getAncestorWithType(resultEntity, EntityConstants.TYPE_SAMPLE);

        Entity default3dImage = null;
        
	    if (defaultImageFilename==null) {
	        default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
	        if (default3dImage==null) {
	            default3dImage = findDefaultImage(resultEntity);
	            if (default3dImage==null) {
	                throw new IllegalArgumentException("Could not determine default image for result entity "+resultEntity.getId());
	            }
	        }
	        defaultImageFilename = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	    }
	    
	    logger.info("Registering images for result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
	    if (default3dImage!=null) {
	        logger.info("Using default image: "+default3dImage.getName()+" (id="+default3dImage.getId()+")");
	    }
	    else {
	        logger.info("Will find default image: "+defaultImageFilename);
	    }
	    
    	// Find all the 2d and 3d images in this result tree, and populate all of the lookup maps and lists
    	
    	findImages(resultEntity);
    	logger.info("Found "+images3d.size()+" 3d images");
    	
    	// Ensure all 3d images have their shortcut images correctly set. At the same time, find which of these
    	// 3d images is the default image for this result.

    	String defaultImageCanonicalFilename = new File(defaultImageFilename).getCanonicalPath();
    	
    	for(Entity image3d : images3d.values()) {
			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			
			logger.info("  Processing "+image3d.getName()+" (id="+image3d.getId()+")");
			
			if (default3dImage==null && (filepath.equals(defaultImageFilename) || filepath.equals(defaultImageCanonicalFilename))) {
			    logger.info("  Found default 3d image");
				default3dImage = image3d;
			}
				
			Pattern p = Pattern.compile("^(.*?)\\.(\\w+?)$");
			Matcher m = p.matcher(filepath);
			
			if (m.matches()) {
				String prefix = m.group(1);
				
				Entity signalMip = signalMipPrefixMap.get(prefix);
				Entity refMip = refMipPrefixMap.get(prefix);

				if (signalMip!=null) {
			    	EntityData currDefault2dImage = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
			    	if (currDefault2dImage==null || currDefault2dImage.getChildEntity()==null || !currDefault2dImage.getId().equals(signalMip.getId())) {
			    	    logger.info("    Setting default 2d MIP to: "+signalMip.getName());
			    		entityHelper.setDefault2dImage(image3d, signalMip);
			    	}
			    	EntityData currSignalMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
			    	if (currSignalMip==null || currSignalMip.getChildEntity()==null || !currSignalMip.getId().equals(signalMip.getId())) {
			    	    logger.info("    Setting signal MIP to: "+signalMip.getName());
			    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
			    	}
				}
				
				if (refMip!=null) {
			    	EntityData currRefMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
			    	if (currRefMip==null || currRefMip.getChildEntity()==null || !currRefMip.getId().equals(refMip.getId())) {
			    	    logger.info("    Setting reference MIP to: "+refMip.getName());
			    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
			    	}
			    	if (signalMip==null) {
			    	    // No signal MIP, use the reference as the default 
			    	    logger.info("    Setting default 2d MIP to: "+refMip.getName());
                        entityHelper.setDefault2dImage(image3d, refMip);
			    	}
				}
			}	
    	}
    	
    	if (default3dImage!=null) {
        	logger.info("Applying default 3d image to the Result, Pipeline Run, and Sample ("+sampleEntity.getName()+")");
        	entityHelper.setDefault3dImage(resultEntity, default3dImage);
        	entityHelper.setDefault3dImage(pipelineRunEntity, default3dImage);

        	Entity topLevelSample = sampleEntity;
            Entity parentSample = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
            if (parentSample==null) {
                // Already at top level sample
            }
            else {
                // Set the image on the subsample
                logger.info("Applying default 3d image to the sub-sample ("+parentSample.getName()+")");
                entityHelper.setDefault3dImage(sampleEntity, default3dImage);
                topLevelSample = parentSample;
            }
            
            // Set the top level sample, if this image matches the user's preference for the sample's data set
            
        	String dataSetIdentifier = topLevelSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);
            if (dataSet!=null) {
                String sampleImageTypeName = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);
                logger.info("Sample image type is: "+sampleImageTypeName);
                if (sampleImageTypeName!=null) {
                    SampleImageType sampleImageType = SampleImageType.valueOf(sampleImageTypeName);
                    if (sampleShouldUseResultImage(sampleEntity, sampleImageType, default3dImage)) {
                        logger.info("Applying default 3d image to the top-level sample ("+topLevelSample.getName()+")");
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
    		Entity separation = EntityUtils.getLatestChildOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    		if (separation!=null) {
            	Entity fast3dImage = findFast3dImage(separation);
            	entityHelper.setDefault3dImage(separation, default3dImage);
            	if (fast3dImage!=null) {
            		logger.info("Found default fast 3d image, applying to "+default3dImage.getName());
            		entityHelper.setImageIfNecessary(default3dImage, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fast3dImage);
        		}
    		}
    	}		
		
    	// Finally, set the images on the sample tiles 
    	
    	populateChildren(sampleEntity);
    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles!=null) {
    		populateChildren(supportingFiles);
    	
            for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE)) {
            	
            	Entity signalMip = null;
            	for(String key : signalMipPrefixMap.keySet()) {
            		if (key.matches("(.*?)/(merged|tile)-"+imageTile.getId())) {
            			signalMip = signalMipPrefixMap.get(key);
            		}
            	}
            	
            	Entity refMip = null;
            	for(String key : refMipPrefixMap.keySet()) {
            		if (key.matches("(.*?)/(merged|tile)-"+imageTile.getId())) {
            			refMip = refMipPrefixMap.get(key);
            		}
            	}
            	
            	setMIPs(imageTile, signalMip, refMip);
            }
    	}
	}

    private Entity findDefaultImage(Entity result) {
        
        logger.info("  Result's default 3d image is missing. Attempting to infer...");
        Entity supportingFiles = EntityUtils.getSupportingData(result);
        Entity defaultImage = null;
        
        String resultDefault2dImage = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        int priority = 0;

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
        case Aligned20x: return alignmentSpace!=null && Objective.OBJECTIVE_20X.getName().equals(objectiveName);
        case Aligned63x: return alignmentSpace!=null && Objective.OBJECTIVE_63X.getName().equals(objectiveName);
        }
        return false;
	}
	
	private void findImages(Entity entity) throws Exception {
		
		String entityType = entity.getEntityType().getName();
		if (entityType.equals(EntityConstants.TYPE_IMAGE_2D)) {
			String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			Pattern p = Pattern.compile("^(.*?)_(\\w+)\\.(\\w+?)$");
			Matcher m = p.matcher(filepath);
			
			if (m.matches()) {
				String prefix = m.group(1);
				String type = m.group(2);
				
				if ("signal".equals(type)) {
					signalMipPrefixMap.put(prefix, entity);
				}
				else if ("reference".equals(type)) {
					refMipPrefixMap.put(prefix, entity);
				}
			}
		}
		else if (entityType.equals(EntityConstants.TYPE_IMAGE_3D)) {
			images3d.put(entity.getId(), entity);
		}
		else {
			populateChildren(entity);
			for(Entity child : entity.getChildren()) {
				findImages(child);
			}
		}
	}
	
	private Entity findFast3dImage(Entity separation) {
		Entity supportingFiles = EntityUtils.getSupportingData(separation);
		if (supportingFiles==null) return null;
		
        // Should find it here
		Entity signalVolume = EntityUtils.findChildWithName(supportingFiles, "ConsolidatedSignal.v3dpbd");
		if (signalVolume!=null) {
		    Entity fast3dImage = separation.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
            if (fast3dImage!=null) {
                return fast3dImage;
            }
		}
		
        // If not, try looking in the old location
    	Entity fastLoad = EntityUtils.findChildWithName(supportingFiles, "Fast Load");
		if (fastLoad==null) return null;
		return EntityUtils.findChildWithName(fastLoad, "ConsolidatedSignal2_25.mp4");
	}
	
	private void setMIPs(Entity entity, Entity signalMip, Entity refMip) throws ComputeException {
	    if (signalMip!=null) {
	        logger.info("Applying signal/reference MIPs to tile "+entity.getName());
	        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMip);
	        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
	        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
	    }
	    else {
	        logger.info("Applying reference MIPs to tile "+entity.getName());
	        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, refMip);
	        entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
	    }
	}
}
