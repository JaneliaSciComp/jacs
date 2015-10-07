package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import com.google.common.collect.Ordering;

/**
 * Synchronizes Qi scores (for 20x JBA alignments) into the SAGE database. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageQiScoreSyncService extends AbstractEntityService {

    public transient static final String PARAM_testRun = "is test run";
    
	private static final int BATCH_SIZE = 1000;
    private static final String ANATOMICAL_AREA = "Brain";
    private static final String QI_SCORE_TERM_NAME = "qi";
    private static final String QM_SCORE_TERM_NAME = "qm";

	private boolean isDebug = false;
	
    private SageDAO sage;
    private CvTerm qiScoreTerm;
    private CvTerm qmScoreTerm;
    
    private Map<Long,String> qiScoreBatch = new HashMap<Long,String>();
    private Map<Long,String> inScoreBatch = new HashMap<Long,String>();
    private Map<Long,String> qmScoreBatch = new HashMap<Long,String>();
    
    private int numAlignments = 0;
    private Map<String,Integer> numUpdated = new HashMap<String,Integer>();
    private Map<String,Integer> numInserted = new HashMap<String,Integer>();
    
    /**
     * Process all alignments.
     */
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }            

        contextLogger.info("Running Qi Score Synchronization (isDebug="+isDebug+")");

        Long alignmentId = data.getItemAsLong("ALIGNMENT_ID");
        
    	try {
	        if (alignmentId!=null) {
	                this.sage = new SageDAO(logger);
	                this.qiScoreTerm = getCvTermByName("light_imagery",QI_SCORE_TERM_NAME);
	                this.qmScoreTerm = getCvTermByName("light_imagery",QM_SCORE_TERM_NAME);
	        		processAlignment(alignmentId);
	        }
	        else {
	        	processAllAlignments();
	        }
    	}
    	catch (Exception e) {
    		logger.warn("Problem synchronizing Qi/Qm scores to SAGE. The pipeline will continue.",e);
    	}

        contextLogger.info("Processed "+numAlignments+" JBA Alignments");
        
        if (numUpdated.isEmpty()) {
            contextLogger.info("No Qi Scores updated in SAGE"+(alignmentId==null?"":" for "+alignmentId));
        }
        else {
            contextLogger.info("Completed Qi Score Synchronization"+(alignmentId==null?"":" for "+alignmentId));
            for(String term : Ordering.natural().sortedCopy(numUpdated.keySet())) {
            	contextLogger.info("  Property "+term);
    	        contextLogger.info("    Num updated: "+numUpdated.get(term));
    	        contextLogger.info("    Num inserted: "+numInserted.get(term));
            }
        }
    }
    
    private void processAllAlignments() throws Exception {

		contextLogger.info("Synchronizing all JBA Alignments to SAGE by loading their Qi/Qm scores");
		
        for(Entity jbaAlignment : entityBean.getEntitiesByName("JBA Alignment")) {
        	
        	Entity pipelineRun = entityBean.getAncestorWithType(jbaAlignment, EntityConstants.TYPE_PIPELINE_RUN);
        	if (pipelineRun==null) {
        		logger.error("Alignment run has no ancestor pipeline run: "+jbaAlignment.getId());
        		continue;
        	}
        	Entity sample = entityBean.getAncestorWithType(pipelineRun, EntityConstants.TYPE_SAMPLE);
        	if (sample==null) {
        		logger.error("Pipeline run has no ancestor sample: "+pipelineRun.getId());
        		continue;
        	}
        	if (sample.getName().contains("~")) {
        		// Check parent sample for retirement
	        	Entity parentSample = entityBean.getAncestorWithType(sample, EntityConstants.TYPE_SAMPLE);
	        	if (parentSample!=null) {
	            	if (parentSample.getName().endsWith("-Retired")) {
	            		logger.warn("Alignment is part of retired sample: "+jbaAlignment.getId());
	            		continue;
	            	}	
	        		continue;
	        	}
        	}
        	else {
        		// Check sample for retirement
            	if (sample.getName().endsWith("-Retired")) {
            		logger.warn("Alignment is part of retired sample: "+jbaAlignment.getId());
            		continue;
            	}
        	}
        	String process = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        	if (process==null) {
        		logger.error("Pipeline run has no pipeline process: "+pipelineRun.getId());
        		continue;
        	}
        	populateChildren(sample);
        	List<Entity> children = sample.getOrderedChildren();
        	Collections.reverse(children);
            Entity lastGoodRun = null;
        	for(Entity run : children) {
        		if (!run.getEntityTypeName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
        			continue;
        		}
        		if (!process.equals(run.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS))) {
        			// This is a different pipeline, ignore it.
        			continue;
        		}
        		populateChildren(run);
	            Entity error = EntityUtils.getLatestChildOfType(run, EntityConstants.TYPE_ERROR);
	            if (error==null) {
	            	lastGoodRun = run;
	            	break;
	            }
        	}
        	if (lastGoodRun==null) {
        		logger.warn("Alignment is not part of any good pipeline runs: "+jbaAlignment.getId());
        		continue;
        	}
        	if (!lastGoodRun.getId().equals(pipelineRun.getId())) {
        		logger.warn("Alignment is not part of the last good pipeline run: "+jbaAlignment.getId());
        		continue;
        	}
        	
        	addQiQmScores(jbaAlignment);
        	jbaAlignment.setEntityData(null);
        	if (qiScoreBatch.size()>=BATCH_SIZE) {
        		processQiScoreBatch();
                qiScoreBatch.clear();
                inScoreBatch.clear();
                qmScoreBatch.clear();
        	}        
        	numAlignments++;
        }
        processQiScoreBatch();
    }

    public void processAlignment(Long alignmentId) throws Exception {
    	Entity alignment = entityBean.getEntityById(alignmentId);
    	addQiQmScores(alignment);
        processQiScoreBatch();
    }
    
    private void addQiQmScores(Entity alignment) throws Exception {
    	populateChildren(alignment);
    	Entity alignedImage = alignment.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	if (alignedImage==null) {
    		logger.warn("Alignment has no default 3d image: "+alignment.getId());
    		return;
    	}
    	String qiScore = alignedImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
    	if (qiScore!=null) {
        	qiScoreBatch.put(alignment.getId(), qiScore);
    	}
    	String inScore = alignedImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
    	if (inScore!=null) {
        	inScoreBatch.put(alignment.getId(), inScore);
    	}
    	String qmScore = alignedImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE);
    	if (qmScore!=null) {
        	qmScoreBatch.put(alignment.getId(), qmScore);
    	}
    }
    
    private void processQiScoreBatch() throws Exception {

    	if (qiScoreBatch.isEmpty()) return;

		contextLogger.info("Processing "+qiScoreBatch.size()+" Qi/Qm scores");
		
    	Map<Long,Long> lsmToAlignment = getLsmToAlignmentMap(qiScoreBatch.keySet());
    	Map<Long,Integer> lsmIdToSageId = getLsmToSageMap(lsmToAlignment.keySet());

        Map<Integer,Image> sageImages = new HashMap<Integer,Image>();
    	for(Image image : sage.getImages(lsmIdToSageId.values())) {
    		sageImages.put(image.getId(), image);
    	}
    	
        for(Long lsmId : lsmIdToSageId.keySet()) {
        	Integer sageId = lsmIdToSageId.get(lsmId);
        	if (sageId==null) continue;
        	
        	Image sageImage = sageImages.get(sageId);
        	if (sageImage==null) {
        		logger.warn("Could not find SAGE image "+sageId);
        	}
        	
        	Long alignmentId = lsmToAlignment.get(lsmId);
        	
    		String qiScore = qiScoreBatch.get(alignmentId);
    		if (qiScore!=null) {
    			if (sageImage!=null) {
    				setImageProperty(sageImage, qiScoreTerm, qiScore);
    			}
    			contextLogger.info("Updating LSM "+lsmId+" with Qi score "+qiScore);
    			if (!isDebug) {
	    			// FW-2763: Also denormalize the scores directly onto the LSM entity, for ease of searching/browsing
	        		entityBean.setOrUpdateValue(null, lsmId, EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, qiScore);
	        		String inScore = inScoreBatch.get(alignmentId);
	        		if (inScore != null) {
	        			entityBean.setOrUpdateValue(null, lsmId, EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE, inScore);
	        		}
    			}
    		}

    		String qmScore = qmScoreBatch.get(alignmentId);
    		if (qmScore!=null) {
    			if (sageImage!=null) {
    				setImageProperty(sageImage, qmScoreTerm, qmScore);
    			}
    		}	
        }
        
        if (!isDebug) {
	        sage.getCurrentSession().flush();
        }
    }

    private Map<Long,Long> getLsmToAlignmentMap(Set<Long> alignmentIds) throws Exception {
    	
    	Map<Long,Long> lsmToAlignment = new HashMap<Long,Long>();
    	
        List<String> upMapping = new ArrayList<String>();
        upMapping.add(EntityConstants.TYPE_PIPELINE_RUN);
        upMapping.add(EntityConstants.TYPE_SAMPLE);
        List<String> downMapping = new ArrayList<String>();
        downMapping.add(EntityConstants.TYPE_SUPPORTING_DATA);
        downMapping.add(EntityConstants.TYPE_IMAGE_TILE);
        downMapping.add(EntityConstants.TYPE_LSM_STACK);
        
        List<Long> entityIds = new ArrayList<Long>();
        entityIds.addAll(alignmentIds);        
         
        for(MappedId mappedId : entityBean.getProjectedResults(null, entityIds, upMapping, downMapping)) {
        	lsmToAlignment.put(mappedId.getMappedId(),mappedId.getOriginalId());
        }
        return lsmToAlignment;
    }
    
    private Map<Long,Integer> getLsmToSageMap(Set<Long> lsmIds) throws Exception {

        Map<Long,Integer> lsmIdToSageId = new HashMap<Long,Integer>();
        
        for(Entity lsm : entityBean.getEntitiesById(new ArrayList<Long>(lsmIds))) {
            if (ANATOMICAL_AREA.equals(lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA))) {
	        	String sageIdStr = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
	        	if (sageIdStr==null) {
	        		logger.warn("LSM has no SAGE identifier: "+lsm.getId());
	        		continue;
	        	}
	        	Integer sageId = null;
	        	try {
	        		sageId = Integer.parseInt(sageIdStr);
	        		lsmIdToSageId.put(lsm.getId(), sageId);
	        	}
	        	catch (NumberFormatException e) {
	        		logger.error("Cannot parse SAGE identifier "+sageIdStr+" for LSM "+lsm.getId());
	        	}
            }
        }
        return lsmIdToSageId;
    }


	private void setImageProperty(Image image, CvTerm type, String value) throws Exception {
		
		Set<ImageProperty> toDelete = new HashSet<ImageProperty>();
		boolean found = false;
    	for(ImageProperty property : image.getImageProperties()) {
    		if (property.getType().equals(type)) {
    			if (found) {
    				toDelete.add(property);
    			}
    			if (!property.getValue().equals(value)) {
	    			// Update existing property value
	    			contextLogger.info("Overwriting existing "+type.getName()+" value ("+property.getValue()+") with new value ("+value+") for image "+image.getId());
	    			property.setValue(value);
	    			
	    			Integer numUpdatedCount = numUpdated.get(type.getName());
	    			if (numUpdatedCount==null) {
	    				numUpdated.put(type.getName(),1);
	    			}
	    			else {
	    				numUpdated.put(type.getName(),numUpdatedCount+1);
	    			}
	
	    	        if (!isDebug) sage.saveImageProperty(property);
    			}
    			else {
    				// Already has the correct value
    			}
    			found = true;
    		}
    	}
    	
    	image.getImageProperties().removeAll(toDelete);
    	for(ImageProperty imageProperty : toDelete) {
    		contextLogger.info("Deleting redundant image property "+imageProperty.getType().getName()+" for image "+image.getId());
    		sage.deleteImageProperty(imageProperty);
    	}
    	
    	if (!found) {
	    	// Set new property
			contextLogger.info("Setting new "+type.getName()+" value ("+value+") for image "+image.getId()+")");
	        ImageProperty prop = new ImageProperty(type, image, value, new Date());
	        image.getImageProperties().add(prop);
	        if (!isDebug) sage.saveImageProperty(prop);
	
			Integer numInsertedCount = numInserted.get(type.getName());
			if (numInsertedCount==null) {
				numInserted.put(type.getName(),1);
			}
			else {
				numInserted.put(type.getName(),numInsertedCount+1);
			}
    	}
    }
	
    private CvTerm getCvTermByName(String cvName, String termName) throws DaoException {
        CvTerm term = sage.getCvTermByName(cvName, termName);
        if (term==null) {
            throw new IllegalStateException("No such term: "+termName+" in CV "+cvName);
        }
        return term;
    }
}
