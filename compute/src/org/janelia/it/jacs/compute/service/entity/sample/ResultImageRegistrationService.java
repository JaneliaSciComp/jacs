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
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Start at some result entity within a Pipeline run, and look for 2d images corresponding to 3d images. These 
 * should be set as attributes of the Result, the Pipeline Run, and the Sample.
 * 
 * Parameters:
 *   SAMPLE_ENTITY_ID - the id of the sample for which will update the 2d images, and 2d images for the Image Tiles
 *   PIPELINE_RUN_ENTITY_ID - the id of the pipeline run containing the result entity id
 *   RESULT_ENTITY_ID - the id of the root entity to look for 2d images within
 *   DEFAULT_IMAGE_FILENAME - the file to use as the "Default 2D Image" for the root entity
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultImageRegistrationService extends AbstractEntityService {
	
	private List<Entity> images2d = new ArrayList<Entity>();
	private List<Entity> images3d = new ArrayList<Entity>();
	private Map<String,Entity> signalMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> refMipPrefixMap = new HashMap<String,Entity>();
	
	public void execute() throws Exception {

        String defaultImageFilename = (String)processData.getItem("DEFAULT_IMAGE_FILENAME");
    	if (defaultImageFilename == null || "".equals(defaultImageFilename)) {
    		throw new IllegalArgumentException("DEFAULT_IMAGE_FILENAME may not be null");
    	}

    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	String pipelineRunEntityId = (String)processData.getItem("PIPELINE_RUN_ENTITY_ID");
    	if (StringUtils.isEmpty(pipelineRunEntityId)) {
    		throw new IllegalArgumentException("PIPELINE_RUN_ENTITY_ID may not be null");
    	}
    	
    	String resultEntityId = (String)processData.getItem("RESULT_ENTITY_ID");
    	if (StringUtils.isEmpty(resultEntityId)) {
    		throw new IllegalArgumentException("RESULT_ENTITY_ID may not be null");
    	}

    	Entity sampleEntity = entityBean.getEntityAndChildren(new Long(sampleEntityId));
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

    	Entity pipelineRunEntity = entityBean.getEntityAndChildren(new Long(pipelineRunEntityId));
    	if (pipelineRunEntity == null) {
    		throw new IllegalArgumentException("Pipeline run entity not found with id="+pipelineRunEntity);
    	}
    	
    	Entity resultEntity = entityBean.getEntityTree(new Long(resultEntityId));
    	if (resultEntity == null) {
    		throw new IllegalArgumentException("Entity not found with id="+resultEntityId);
    	}
    	
    	registerImages(resultEntity, pipelineRunEntity, sampleEntity, defaultImageFilename);
    }

	public void execute(IProcessData processData, Entity resultEntity, Entity pipelineRunEntity, Entity sampleEntity, String defaultImageFilename) throws Exception {

        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.task = ProcessDataHelper.getTask(processData);
        this.processData = processData;
        this.entityBean = EJBFactory.getLocalEntityBean();
        this.computeBean = EJBFactory.getLocalComputeBean();
        this.annotationBean = EJBFactory.getLocalAnnotationBean();
        String ownerName = ProcessDataHelper.getTask(processData).getOwner();
        Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
        this.ownerKey = subject.getKey();
        this.entityHelper = new EntityHelper(entityBean, computeBean, ownerKey);
        this.entityLoader = new EntityBeanEntityLoader(entityBean);
    	registerImages(resultEntity, pipelineRunEntity, sampleEntity, defaultImageFilename);
    }
	
	private void registerImages(Entity resultEntity, Entity pipelineRunEntity, Entity sampleEntity, String defaultImageFilename) throws Exception {

    	logger.info("Finding images under "+resultEntity.getName());
    	
    	// Find all the 2d and 3d images in this result tree, and populate all of the lookup maps and lists
    	
    	findImages(resultEntity);
    	logger.info("Found "+images3d.size()+" 3d images and "+images2d.size()+" 2d images");
    	
    	// Ensure all 3d images have their shortcut images correctly set. At the same time, find which of these
    	// 3d images is the default image for this result.

    	String defaultImageCanonicalFilename = new File(defaultImageFilename).getCanonicalPath();
    	Entity default3dImage = null;
    	
    	logger.debug("Looking for default image: "+defaultImageFilename);
    	logger.debug("         (canonical path): "+defaultImageCanonicalFilename);
    	
    	for(Entity image3d : images3d) {
			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			
			logger.debug("  Considering "+filepath);
			
			if (filepath.equals(defaultImageFilename) || filepath.equals(defaultImageCanonicalFilename)) {
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
			    		entityHelper.setDefault2dImage(image3d, signalMip);
			    	}
			    	EntityData currSignalMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
			    	if (currSignalMip==null || currSignalMip.getChildEntity()==null || !currSignalMip.getId().equals(signalMip.getId())) {
			    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
			    	}
				}
				
				if (refMip!=null) {
			    	EntityData currRefMip = image3d.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
			    	if (currRefMip==null || currRefMip.getChildEntity()==null || !currRefMip.getId().equals(refMip.getId())) {
			    		entityHelper.setImageIfNecessary(image3d, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
			    	}
				}
			}	
    	}
    	
    	if (default3dImage!=null) {
        	logger.info("Found default 3d image, applying to the Result, Pipeline Run, and Sample");
        	entityHelper.setDefault3dImage(resultEntity, default3dImage);
        	entityHelper.setDefault3dImage(pipelineRunEntity, default3dImage);
        	entityHelper.setDefault3dImage(sampleEntity, default3dImage);
        	
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
    	
    	logger.info("Applying MIPs to sample tiles");
    	
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
			
			images2d.add(entity);
		}
		else if (entityType.equals(EntityConstants.TYPE_IMAGE_3D)) {
			images3d.add(entity);
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
    	Entity fastLoad = EntityUtils.findChildWithName(supportingFiles, "Fast Load");
		if (fastLoad==null) return null;
		return EntityUtils.findChildWithName(fastLoad, "ConsolidatedSignal2_25.mp4");
	}
	
	private void setMIPs(Entity entity, Entity signalMip, Entity refMip) throws ComputeException {
		entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMip);
		entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
		entityHelper.setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
	}
}
