package org.janelia.it.jacs.compute.service.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;

import com.google.common.collect.Ordering;

/**
 * Synchronizes Qi scores (for 20x JBA alignments) into the SAGE database. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageQiScoreSyncService extends AbstractDomainService {

    public transient static final String PARAM_testRun = "is test run";
    
    private static final String QI_SCORE_TERM_NAME = "qi";
    private static final String QM_SCORE_TERM_NAME = "qm";

	private boolean isDebug = false;

	private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;
    private SampleAlignmentResult alignment;
    private SageDAO sage;
    private CvTerm qiScoreTerm;
    private CvTerm qmScoreTerm;
    
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

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
        Long alignmentId = data.getItemAsLong("ALIGNMENT_ID");
        if (alignmentId==null) {
            throw new IllegalArgumentException("ALIGNMENT_ID cannot be null");
        }
        
        this.sage = new SageDAO(logger);
        this.qiScoreTerm = getCvTermByName("light_imagery",QI_SCORE_TERM_NAME);
        this.qmScoreTerm = getCvTermByName("light_imagery",QM_SCORE_TERM_NAME);
        List<SampleAlignmentResult> results = run.getResultsById(SampleAlignmentResult.class, alignmentId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Could not find alignment "+alignmentId+" in sample "+sample.getId());
        }
        this.alignment = results.get(0); // We can take any instance, since they're all the same
        
    	processAlignment();

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
    
    public void processAlignment() throws Exception {
    	
        logger.info("Updating LSM scores using alignment "+alignment.getId()+" with area "+alignment.getAnatomicalArea());
        
        String qiScore = alignment.getScores().get(AlignmentScoreType.Qi);
        String qmScore = alignment.getScores().get(AlignmentScoreType.ModelViolation);
        
    	for(SampleTile tile : objectiveSample.getTiles()) {
    	    for(LSMImage lsm : domainDao.getDomainObjectsAs(tile.getLsmReferences(), LSMImage.class)) {
    	        
    	        if (!lsm.getAnatomicalArea().equals(alignment.getAnatomicalArea())) {
    	            logger.info("Skipping LSM which has area "+lsm.getAnatomicalArea());
    	            continue;
    	        }
    	        
    	        Image sageImage = sage.getImage(lsm.getSageId());
                if (sageImage==null) {
                    logger.warn("Could not find SAGE image "+lsm.getSageId());
                    continue;
                }
                
                boolean lsmDirty = false;

                if (qiScore!=null) {
                    setImageProperty(sageImage, qiScoreTerm, qiScore);
                    if (!qiScore.equals(lsm.getQiScore())) {
                        lsm.setQiScore(qiScore);
                        lsmDirty = true;
                    }
                }
                if (qmScore!=null) {
                    setImageProperty(sageImage, qmScoreTerm, qmScore);
                    if (!qmScore.equals(lsm.getQmScore())) {
                        lsm.setQmScore(qmScore);
                        lsmDirty = true;
                    }
                }
                
                if (lsmDirty) {
                    logger.info("Saving scores in LSM "+lsm.getId());
                    sampleHelper.saveLsm(lsm);
                }
    	    }
    	}
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
