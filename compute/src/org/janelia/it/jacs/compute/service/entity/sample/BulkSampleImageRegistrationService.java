package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkSampleImageRegistrationService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(BulkSampleImageRegistrationService.class);

    public transient static final String PARAM_testRun = "is test run";
	
    protected int numSamples;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    private ResultImageRegistrationService resultImageRegService;
    
    public void execute() throws Exception {
        
        this.resultImageRegService = new ResultImageRegistrationService();
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);
        
        if (isDebug) {
        	logger.info("This is a test run. No entities will be moved or deleted.");
        }
        else {
        	logger.info("This is the real thing. Entities will be moved and/or deleted!");
        }
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {
        	List<Entity> samples = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE);
        	logger.info("Processing "+samples.size()+" samples");
            for(Entity sample : samples) {
                processSample(sample);
            }    		
    	}
    	else {
    		logger.info("Processing single sample: "+sampleEntityId);
    		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
            processSample(sampleEntity);
    	}

		logger.info("Processed "+numSamples+" samples.");
    }
    
    public void processSample(Entity sample) throws Exception {
    	
    	if (!sample.getOwnerKey().equals(ownerKey)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
		numSamples++;
		
		if (deleteSampleIfUnreferencedByOwner(sample)) {
			return;
		}

		upgradeSample(entityBean.getEntityTree(sample.getId()));
    }
    
    private boolean deleteSampleIfUnreferencedByOwner(Entity sample) throws ComputeException {
    	
    	for(EntityData ed : entityBean.getParentEntityDatas(sample.getId())) {
    		if (ed.getOwnerKey().equals(sample.getOwnerKey())) {
    			return false;
    		}
    	}
    	
    	logger.info("  Sample is not referenced by owner: "+sample.getName()+" (id="+sample.getId()+")");

    	long numAnnotated = annotationBean.getNumDescendantsAnnotated(sample.getId());
    	if (numAnnotated>0) {
    		logger.warn("  Cannnot delete sample because "+numAnnotated+" descendants are annotated");
    		return false;
    	}
    	
    	logger.info("  Removing unreferenced sample entirely: "+sample.getId());
    	
    	if (!isDebug) {
    		entityBean.deleteSmallEntityTree(sample.getOwnerKey(), sample.getId(), true);
    	}
        
    	return true;
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void upgradeSample(Entity sample) throws Exception {
        fixShortcutImages(sample);
          
    	for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN)) {
    	    fixShortcutImages(pipelineRun);
    	    
    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
    			if (!pred.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
    				continue;
    			}
    			Entity result = pred.getChildEntity();

    			logger.info("  Processing result: "+result.getName()+" (id="+result.getId()+")");
    			fixShortcutImages(result);

			    String defaultImageFilename = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                logger.info("  Running result image registration with "+new File(defaultImageFilename).getName());
                
                if (!isDebug) {
                    resultImageRegService.execute(processData, result, defaultImageFilename);
                }
    		}
    	}
    	
//    	logger.info("Registered Images for Sample:");
//    	printSample(sample);
    }
    
    private void fixShortcutImages(Entity parent) throws Exception {
        for(EntityData ed : EntityUtils.getOrderedEntityDataForAttribute(parent, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)) {
            fixShortcutImageEd(ed);
        }
    }
    
    private void fixShortcutImageEd(EntityData imageEd) throws Exception {
        Entity child = imageEd.getChildEntity();
        String filepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (!filepath.equals(imageEd.getValue())) {
            logger.info("  Fixed shortcut for image "+filepath);
            imageEd.setValue(filepath);
            entityBean.saveOrUpdateEntityData(imageEd);
        }   
    }
    
    private void printSample(Entity sample) {

    	logger.info(""+sample);
    	printImages("  ",sample);
    	
    	for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN)) {
    		
    		logger.info("    "+pipelineRun.getName());
    		printImages("      ",pipelineRun);
    		
    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
    			if (!pred.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
    				continue;
    			}
    			Entity result = pred.getChildEntity();
        		logger.info("        "+result.getName());
        		printImages("          ",result);
    		}
    	}
    }

    private void printImages(String indent, Entity entity) {

    	EntityData default3dImage = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	if (default3dImage!=null) {
    		logger.info(indent+"Default3d: "+new File(default3dImage.getValue()).getName());
    		
        	EntityData fast3dImage = default3dImage.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
        	if (fast3dImage!=null) {
        		logger.info(indent+"Fast3d: "+new File(fast3dImage.getValue()).getName());
        	}
    	}
    	
    	EntityData default2dImage = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    	if (default2dImage!=null) {
    		logger.info(indent+"Default2d: "+new File(default2dImage.getValue()).getName());
    	}
    	
    	EntityData signalMip = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
    	if (signalMip!=null) {
    		logger.info(indent+"SignalMip: "+new File(signalMip.getValue()).getName());
    	}
    	
    	EntityData refMip = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
    	if (refMip!=null) {
    		logger.info(indent+"ReferenceMip: "+new File(refMip.getValue()).getName());
    	}
    }
    
}
