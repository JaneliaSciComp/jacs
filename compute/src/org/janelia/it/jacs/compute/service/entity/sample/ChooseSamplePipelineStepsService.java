package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides which types of processing will be run for a Sample based on its assigned data set.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseSamplePipelineStepsService extends AbstractEntityService {

    public void execute() throws Exception {

    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	int numTiles = 0;
        populateChildren(sampleEntity);
    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles!=null) {
            populateChildren(supportingFiles);
            numTiles = supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_TILE).size();
    	}
    	
    	String mergeAlgorithms = (String)processData.getItem("MERGE_ALGORITHMS");
    	String stitchAlgorithms = (String)processData.getItem("STITCH_ALGORITHMS");
    	String alignAlgorithms = (String)processData.getItem("ALIGNMENT_ALGORITHMS");
    	String analysisAlgorithms = (String)processData.getItem("ANALYSIS_ALGORITHMS");
    	
    	processData.putItem("MERGE_ALGORITHM", Task.listOfStringsFromCsvString(mergeAlgorithms));
		processData.putItem("STITCH_ALGORITHM", Task.listOfStringsFromCsvString(stitchAlgorithms));
		processData.putItem("ALIGNMENT_ALGORITHM", Task.listOfStringsFromCsvString(alignAlgorithms));
		processData.putItem("ANALYSIS_ALGORITHM", Task.listOfStringsFromCsvString(analysisAlgorithms));
		
        boolean hasMerge = !StringUtils.isEmpty(mergeAlgorithms);
        boolean hasStitch = !StringUtils.isEmpty(stitchAlgorithms);
        boolean hasAlignment = !StringUtils.isEmpty(alignAlgorithms);
        boolean hasAnalysis = !StringUtils.isEmpty(analysisAlgorithms);
        
        boolean runMerge = hasMerge;
        boolean runStitch = hasStitch && numTiles>1;
		boolean runAlignment = hasAlignment;
		boolean runAnalysis = hasAnalysis;
		
		processData.putItem("RUN_MERGE", new Boolean(runMerge));
		processData.putItem("RUN_STITCH", new Boolean(runStitch));
		processData.putItem("RUN_ALIGNMENT", new Boolean(runAlignment));
		processData.putItem("RUN_ANALYSIS", new Boolean(runAnalysis));

    	logger.info("Pipeline steps to execute for Sample "+sampleEntity.getName()+":");
    	logger.info("    Merge = "+runMerge);
    	logger.info("    Stitch = "+runStitch +((!runStitch&&hasStitch)?" (configured but not necessary for a single tile)":""));
    	logger.info("    Alignment = "+runAlignment);
    	logger.info("    Analysis = "+runAnalysis);
    }
}
