package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Decides which types of MCFO processing will be run for a Sample based on user preferences and 
 * what artifacts are available.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseAlignWholeBrainSampleStepsService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;
    protected IProcessData processData;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            this.processData = processData;
            
        	boolean refreshAlignment = getBoolean("REFRESH_ALIGNMENT");
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}

    		if (!canSkipProcessing(processData, sampleEntity)) {
    			logger.info("No stitched result found, skipping Sample "+sampleEntity.getName());
    			processData.putItem("IS_ALIGNABLE", new Boolean(false));
    			processData.putItem("RUN_ALIGNMENT", new Boolean(false));
    			processData.putItem("RUN_ALIGNED_SEPARATION", new Boolean(false));
    			return;
    		}
    		
    		String strTilingPattern = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN);
    		TilingPattern pattern = TilingPattern.valueOf(strTilingPattern);

    		boolean isAlignable = pattern.isAlignable();
    		boolean runAlignment = isAlignable && (pattern == TilingPattern.WHOLE_BRAIN) && (refreshAlignment || !canSkipWholeBrainAlignment(processData, sampleEntity));
    		boolean runAlignedSeparation = runAlignment || (isAlignable && !canSkipWholeBrainAlignedSeparation(processData, sampleEntity));
    		
    		logger.info("Sample "+sampleEntity.getName()+" (id="+sampleEntityId+") has tiling pattern "+pattern.getName()+" (alignable="+isAlignable+")");
    		
    		processData.putItem("IS_ALIGNABLE", new Boolean(isAlignable));
    		processData.putItem("RUN_ALIGNMENT", new Boolean(runAlignment));
    		processData.putItem("RUN_ALIGNED_SEPARATION", new Boolean(runAlignedSeparation));
        	
        	logger.info("Pipeline steps to execute for Sample "+sampleEntity.getName()+":");
        	logger.info("    Alignment = "+processData.getItem("RUN_ALIGNMENT"));
        	logger.info("    Aligned Separation = "+processData.getItem("RUN_ALIGNED_SEPARATION"));
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

	public boolean canSkipProcessing(IProcessData processData, Entity sampleEntity) {

		Entity sampleProcessing = sampleEntity.getLatestChildOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
		if (sampleProcessing == null) {
			logger.warn("Cannot find existing sample processing result for Sample with id="+sampleEntity.getId());
			return false;
		}


		int numTiles = 0;
		Entity sampleSupportingFiles = sampleEntity.getLatestChildOfType(EntityConstants.TYPE_SUPPORTING_DATA);
    	if (sampleSupportingFiles!=null) {
    		numTiles = sampleSupportingFiles.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR, true).size();
    	}
    	
		Entity stitchedFile = null;
		Entity mergedFile = null;
		int numMergedFiles = 0;
		Entity supportingFiles = sampleProcessing.getLatestChildOfType(EntityConstants.TYPE_SUPPORTING_DATA);
		if (supportingFiles != null) {
    		for(Entity child : supportingFiles.getChildren()) {
    			if (child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
    				if (child.getName().startsWith("stitched-")) {
        				stitchedFile = child;
        			}	
    				else if (child.getName().startsWith("merged-")) {
        				mergedFile = child;
        				numMergedFiles++;
        			}	
    			}
    		}
		}
		
		if (stitchedFile == null) {
			if (numTiles == 1) {
				// We should have a single merged tile
				if (mergedFile==null) {
					logger.warn("Cannot find existing merged result for Sample with id="+sampleEntity.getId());
					return false;	
				}
				processData.putItem("STITCHED_FILENAME", mergedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
		    	logger.info("Putting '"+processData.getItem("STITCHED_FILENAME")+"' in STITCHED_FILENAME");
			}
			else {
				// We should have a stitched file but we don't
				logger.warn("Cannot find existing stitched result for Sample with id="+sampleEntity.getId());
				return false;
			}
		}
		else {
			processData.putItem("STITCHED_FILENAME", stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
	    	logger.info("Putting '"+processData.getItem("STITCHED_FILENAME")+"' in STITCHED_FILENAME");	
		}	

		return true;
    }
	
    public boolean canSkipWholeBrainAlignment(IProcessData processData, Entity sampleEntity) {

		List<Entity> sampleAlignments = sampleEntity.getChildrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT);
		if (sampleAlignments == null || sampleAlignments.isEmpty()) {
			logger.warn("Cannot find existing alignment result for Sample with id="+sampleEntity.getId());
			return false;
		}

    	Collections.reverse(sampleAlignments);
    	Entity sampleAlignment = null;
    	for (Entity entity : sampleAlignments) {
    		if (entity.getName().contains("63x")) {
    			sampleAlignment = entity;
    		}
    	}
    	
    	if (sampleAlignment==null) {
			logger.warn("Cannot find existing whole brain alignment result for Sample with id="+sampleEntity.getId());
    	}
    	
		Entity alignedFile = null;
		Entity supportingFiles = sampleAlignment.getLatestChildOfType(EntityConstants.TYPE_SUPPORTING_DATA);
		if (supportingFiles != null) {
    		for(Entity child : supportingFiles.getChildren()) {
    			if (child.getName().startsWith("Aligned") && child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
    				alignedFile = child;
    			}
    		}
		}

		if (alignedFile == null) {
			logger.warn("Cannot find existing aligned image for Sample with id="+sampleEntity.getId());
			return false;
		}
		
		processData.putItem("ALIGNED_FILENAME", alignedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));	
		logger.info("Putting '"+processData.getItem("ALIGNED_FILENAME")+"' in ALIGNED_FILENAME");	
		
		return true;
    }
    
    public boolean canSkipWholeBrainAlignedSeparation(IProcessData processData, Entity sampleEntity) {

		List<Entity> separations = sampleEntity.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	for(Entity separation : separations) {
    		if (separation.getName().startsWith("Aligned 63x ")) return true;
    	}

		logger.warn("Cannot find existing aligned 63x separation result for Sample with id="+sampleEntity.getId());
		return false;
    }
    
    private boolean getBoolean(String key) {
        String boolStr = (String)processData.getItem(key);
    	if (boolStr == null) {
    		boolStr = "false";
    	}
    	return Boolean.parseBoolean(boolStr);
    }
}
