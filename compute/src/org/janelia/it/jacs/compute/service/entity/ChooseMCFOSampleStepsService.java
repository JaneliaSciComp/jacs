package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Decides which types of MCFO processing will be run for a Sample based on user preferences and 
 * what artifacts are available.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseMCFOSampleStepsService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;
    protected IProcessData processData;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            this.processData = processData;
            
        	boolean refreshProcessing = getBoolean("REFRESH_PROCESSING");
        	boolean refreshAlignment = getBoolean("REFRESH_ALIGNMENT");
        	boolean refreshSeparation = getBoolean("REFRESH_SEPARATION");
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
    		String strTilingPattern = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN);
    		TilingPattern pattern = null;
    		
    		// If the Sample has no tiling pattern then it was not created correctly. But we can fix it on-the-fly.
    		if (strTilingPattern==null) {
    			pattern = fixMissingTilingPattern(sampleEntity);
    		}
    		else {
    			pattern = TilingPattern.valueOf(strTilingPattern);
    		}
    		
    		boolean isAlignable = pattern.isAlignable();

    		// TODO: currently Left optic lobe alignments are not supported. This work-around should be removed in the future.
    		if (pattern == TilingPattern.OPTIC_TILE && sampleEntity.getName().contains("Left")) {
    			isAlignable = false;
    		}
    		
    		boolean runProcessing = refreshProcessing || !canSkipProcessing(processData, sampleEntity);
    		boolean runAlignment = isAlignable && (runProcessing || refreshAlignment || !canSkipAlignment(processData, sampleEntity));
    		boolean runSeparation = runProcessing || refreshSeparation || !canSkipPrealignedSeparation(processData, sampleEntity);
    		boolean runAlignedSeparation = runAlignment || refreshSeparation || (isAlignable && !canSkipAlignedSeparation(processData, sampleEntity));
    		
    		logger.info("Sample "+sampleEntity.getName()+" (id="+sampleEntityId+") has tiling pattern "+pattern.getName()+" (alignable="+isAlignable+")");
    		
    		processData.putItem("RUN_PROCESSING", new Boolean(runProcessing));
    		processData.putItem("RUN_ALIGNMENT", new Boolean(runAlignment));
    		processData.putItem("RUN_SEPARATION", new Boolean(runSeparation));
    		processData.putItem("RUN_ALIGNED_SEPARATION", new Boolean(runAlignedSeparation));
        	processData.putItem("IS_ALIGNABLE", new Boolean(isAlignable));

        	logger.info("Pipeline steps to execute for Sample "+sampleEntity.getName()+":");
        	logger.info("    Processing = "+processData.getItem("RUN_PROCESSING"));
        	logger.info("    Alignment = "+processData.getItem("RUN_ALIGNMENT"));
        	logger.info("    Prealigned Separation = "+processData.getItem("RUN_SEPARATION"));
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
		
		Entity stitchedFile = null;
		Entity supportingFiles = sampleProcessing.getLatestChildOfType(EntityConstants.TYPE_SUPPORTING_DATA);
		if (supportingFiles != null) {
    		for(Entity child : supportingFiles.getChildren()) {
    			if (child.getName().startsWith("stitched-") && child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
    				stitchedFile = child;
    			}
    		}
		}
		
		if (stitchedFile == null) {
			logger.warn("Cannot find existing stitched result for Sample with id="+sampleEntity.getId());
			return false;
		}
		
		processData.putItem("STITCHED_FILENAME", stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
    	logger.info("Putting '"+processData.getItem("STITCHED_FILENAME")+"' in STITCHED_FILENAME");	

		return true;
    }
    
    public boolean canSkipAlignment(IProcessData processData, Entity sampleEntity) {

		Entity sampleAlignment = sampleEntity.getLatestChildOfType(EntityConstants.TYPE_ALIGNMENT_RESULT);
		if (sampleAlignment == null) {
			logger.warn("Cannot find existing alignment result for Sample with id="+sampleEntity.getId());
			return false;
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
			logger.warn("Cannot find existing alignment for Sample with id="+sampleEntity.getId());
			return false;
		}
		
		processData.putItem("ALIGNED_FILENAME", alignedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));	
		logger.info("Putting '"+processData.getItem("ALIGNED_FILENAME")+"' in ALIGNED_FILENAME");	
		
		return true;
    }
    
    public boolean canSkipPrealignedSeparation(IProcessData processData, Entity sampleEntity) {

		List<Entity> separations = sampleEntity.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	for(Entity separation : separations) {
    		if (separation.getName().startsWith("Prealigned")) return true;
    	}

		logger.warn("Cannot find existing prealigned separation result for Sample with id="+sampleEntity.getId());
		return false;
    }
    
    public boolean canSkipAlignedSeparation(IProcessData processData, Entity sampleEntity) {

		List<Entity> separations = sampleEntity.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	for(Entity separation : separations) {
    		if (separation.getName().startsWith("Aligned")) return true;
    	}

		logger.warn("Cannot find existing aligned separation result for Sample with id="+sampleEntity.getId());
		return false;
    }
    
    private boolean getBoolean(String key) {
        String boolStr = (String)processData.getItem(key);
    	if (boolStr == null) {
    		boolStr = "false";
    	}
    	return Boolean.parseBoolean(boolStr);
    }
    
    private TilingPattern fixMissingTilingPattern(Entity sample) {

    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	
    	TilingPattern tiling = TilingPattern.OTHER;
    	if (supportingFiles!=null) {
        	List<String> tags = new ArrayList<String>();
        	for(Entity lsmPairEntity : supportingFiles.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR)) {
    			tags.add(lsmPairEntity.getName());
            }
            tiling = TilingPattern.getTilingPattern(tags);
    	}
        
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        
        try {
            annotationBean.saveOrUpdateEntity(sample);
            logger.info("Fixed sample "+sample.getName()+" by adding its tiling pattern: "+tiling.getName());
        }
        catch (ComputeException e) {
        	logger.warn("Unable to fix sample "+sample.getName()+" by adding its tiling pattern ("+tiling.getName()+") but proceeding anyway.",e);
        }
        
        return tiling;
	}
}
