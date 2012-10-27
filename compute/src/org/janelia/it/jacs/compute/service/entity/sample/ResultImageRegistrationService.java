package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.*;
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
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Start at some result entity within a Pipeline run, and look for 2d images corresponding to 3d images. These 
 * should be set as attributes of the Result, the Pipeline Run, and the Sample.
 * 
 * Parameters:
 *   SAMPLE_ENTITY_ID - the id of the sample for which will update the 2d images, and 2d images for the Image Tiles
 *   ROOT_ENTITY_ID - the id of the root entity to look for 2d images within
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
    	
    	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
    	if (StringUtils.isEmpty(rootEntityId)) {
    		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
    	}

    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	Entity rootEntity = entityBean.getEntityTree(new Long(rootEntityId));
    	if (rootEntity == null) {
    		throw new IllegalArgumentException("Entity not found with id="+rootEntityId);
    	}
    
    	registerImages(rootEntity, sampleEntity, defaultImageFilename);
    }

	public void execute(IProcessData processData, Entity rootEntity, Entity sampleEntity, String defaultImageFilename) throws Exception {

        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.task = ProcessDataHelper.getTask(processData);
        this.processData = processData;
        this.entityBean = EJBFactory.getLocalEntityBean();
        this.computeBean = EJBFactory.getLocalComputeBean();
        this.annotationBean = EJBFactory.getLocalAnnotationBean();
        this.user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
        this.entityHelper = new EntityHelper(entityBean, computeBean, user);
        this.entityLoader = new EntityBeanEntityLoader(entityBean);
    	registerImages(rootEntity, sampleEntity, defaultImageFilename);
    }
	
	private void registerImages(Entity rootEntity, Entity sampleEntity, String defaultImageFilename) throws Exception {

    	logger.info("Finding images under "+rootEntity.getName());
    	logger.info("Looking for default image: "+defaultImageFilename);
    	
    	findImages(rootEntity);
    	
    	logger.info("Found "+images3d.size()+" 3d images and "+images2d.size()+" 2d images");
    	
    	Entity default3dImage = null;
    	Entity parentSignalMip = null;
    	Entity parentRefMIP = null;
    	
    	File defaultImage = new File(defaultImageFilename);
    	String defaultImageCanonicalFilename = defaultImage.getCanonicalPath();
    	
    	for(Entity image3d : images3d) {
			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			
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
					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMip);
					if (filepath.equals(defaultImageFilename)) {
						parentSignalMip = signalMip;
					}
				}
				
				if (refMip!=null) {
					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
					if (filepath.equals(defaultImageFilename)) {
						parentRefMIP = refMip;
					}
				}
			}	
    	}

    	// Set images on the result
    	
    	entityHelper.setDefault3dImage(rootEntity, default3dImage);
    	setMIPs(rootEntity, parentSignalMip, parentRefMIP);
    	
    	// Set images on the sample
    	
    	setMIPs(sampleEntity, parentSignalMip, parentRefMIP);
    	
    	// Set images on the pipeline run parent
    	
    	Entity pipelineRun = null;
    	Set<Entity> parents = entityBean.getParentEntities(rootEntity.getId());
    	if (parents != null) {
    		for(Entity parent : parents) {
    			if (parent.getEntityType().getName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
    				if (pipelineRun!=null) {
    					logger.error("Detected multiple parent runs for result "+rootEntity.getName()+" (id="+rootEntity.getId()+")");
    				}
    				pipelineRun = parent;
    			}
    		}
    		
    		if (pipelineRun != null) {
            	setMIPs(pipelineRun, parentSignalMip, parentRefMIP);
    		}
    	}
		
    	// Finally, set the images on the sample tiles 

        populateChildren(sampleEntity);
    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles!=null) {
            populateChildren(supportingFiles);
    	
            for(Entity imageTile : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_TILE)) {
            	
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
	
	private void findImages(Entity entity) {
		
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

	private void setMIPs(Entity entity, Entity signalMip, Entity refMip) throws ComputeException {
		entityHelper.setImage(entity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
		entityHelper.setDefault2dImage(entity, signalMip);
		entityHelper.setImage(entity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
	}
}
