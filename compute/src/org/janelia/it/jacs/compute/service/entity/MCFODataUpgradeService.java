package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.sample.ResultImageRegistrationService;
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
public class MCFODataUpgradeService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(MCFODataUpgradeService.class);

    public transient static final String PARAM_testRun = "is test run";
	
    protected int numSamples;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    private String username;
    private ResultImageRegistrationService resultImageRegService;
    
    public void execute() throws Exception {
            
    	this.username = user.getUserLogin();
        
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
        	List<Entity> samples = entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE);
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
    	
    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
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
    		if (ed.getUser().getUserLogin().equals(sample.getUser().getUserLogin())) {
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
    		entityBean.deleteSmallEntityTree(sample.getUser().getUserLogin(), sample.getId(), true);
    	}
        
    	return true;
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void upgradeSample(Entity sample) throws Exception {
    	
    	for(Entity pipelineRun : sample.getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
    			if (!pred.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
    				continue;
    			}
    			Entity result = pred.getChildEntity();
    			Entity supportingFiles = EntityUtils.getSupportingData(result);

    			logger.info("  Processing result: "+result.getName()+" (id="+result.getId()+")");
    			
    			String defaultImageFilename = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
		        
    			if (defaultImageFilename==null) {
    				logger.info("  Result's default 3d image is missing. Attempting to infer...");
    				
    				String resultDefault2dImage = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    				int priority = 0;
    				
					for (Entity file : supportingFiles.getChildren()) {
						String filename = file.getName();
						String filepath = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
						String fileDefault2dImage = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
						if (StringUtils.isEmpty(filepath)) continue;
						
						logger.debug("    Considering "+filename);
						logger.debug("      filepath: "+filepath);
						if (fileDefault2dImage!=null) {
							logger.debug("      default 2d image: "+fileDefault2dImage);
						}
						
						if (resultDefault2dImage!=null && resultDefault2dImage.equals(fileDefault2dImage) && priority < 20) {
							defaultImageFilename = filepath;
							priority = 20;
							logger.debug("      Using as default image");
						}
						if (filename.matches("Aligned.v3d(raw|pbd)") && priority < 10) {
							defaultImageFilename = filepath;
							priority = 10;
							logger.debug("      Using as default image");
						}
						else if (filename.matches("stitched-(\\w+?).v3d(raw|pbd)") && priority < 9) {
							defaultImageFilename = filepath;
							priority = 9;
							logger.debug("      Using as default image");
						}
						else if (filename.matches("tile-(\\w+?).v3d(raw|pbd)") && priority < 8) {
							defaultImageFilename = filepath;
							priority = 8;
							logger.debug("      Using as default image");
						}
						else if (filename.matches("merged-(\\w+?).v3d(raw|pbd)") && priority < 7) {
							defaultImageFilename = filepath;
							priority = 7;
							logger.debug("      Using as default image");
						}
					}
    			}
    			
				if (defaultImageFilename==null) {
					logger.warn("  Could not find default image for result "+result.getId());
				}
				else {
					logger.info("  Running result image registration with "+new File(defaultImageFilename).getName());
			    	if (!isDebug) {
				        resultImageRegService = new ResultImageRegistrationService();
						resultImageRegService.execute(processData, result, pipelineRun, sample, defaultImageFilename);
			    	}
				}
    		}
    	}
    	
    	logger.info("Upgraded Sample:");
    	printSample(sample);
    }
    
    private void printSample(Entity sample) {

    	logger.info(""+sample);
    	printImages("  ",sample);
    	
    	for(Entity pipelineRun : sample.getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
    		
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
