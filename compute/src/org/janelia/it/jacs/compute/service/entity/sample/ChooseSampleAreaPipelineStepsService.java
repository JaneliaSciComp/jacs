package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides which types of processing will be run for a Sample Area.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseSampleAreaPipelineStepsService extends AbstractEntityService {

    public void execute() throws Exception {

    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
            throw new IllegalArgumentException("SAMPLE_AREA may not be null");
        }
        
        List<Entity> tiles = sampleArea.getTiles();
    	Integer numImagesPerTile = null;    
    	Integer numTiles = tiles.size();
        for(Entity tile : tiles) {
            populateChildren(tile);
            List<Entity> lsms = EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK);
            if (numImagesPerTile!=null && numImagesPerTile!=lsms.size()) {
                throw new IllegalStateException("Sample has differing numbers of images per tile: "+sampleEntityId);
            }
            numImagesPerTile = lsms.size();
        }
    	
    	String mergeAlgorithms = (String)processData.getItem("MERGE_ALGORITHMS");
    	String stitchAlgorithms = (String)processData.getItem("STITCH_ALGORITHMS");
    	String analysisAlgorithms = (String)processData.getItem("ANALYSIS_ALGORITHMS");
    	
    	processData.putItem("MERGE_ALGORITHM", Task.listOfStringsFromCsvString(mergeAlgorithms));
		processData.putItem("STITCH_ALGORITHM", Task.listOfStringsFromCsvString(stitchAlgorithms));
		processData.putItem("ANALYSIS_ALGORITHM", Task.listOfStringsFromCsvString(analysisAlgorithms));
		
        boolean hasMerge = !StringUtils.isEmpty(mergeAlgorithms);
        boolean hasStitch = !StringUtils.isEmpty(stitchAlgorithms);
        boolean hasAnalysis = !StringUtils.isEmpty(analysisAlgorithms);
        
        boolean runProcessing = true;
        boolean runMerge = hasMerge && numImagesPerTile>1;
        boolean runStitch = hasStitch && numTiles>1;
		boolean runAnalysis = hasAnalysis;

        processData.putItem("RUN_PROCESSING", new Boolean(runProcessing));
		processData.putItem("RUN_MERGE", new Boolean(runMerge));
		processData.putItem("RUN_STITCH", new Boolean(runStitch));
		processData.putItem("RUN_ANALYSIS", new Boolean(runAnalysis));

    	logger.info("Pipeline steps to execute for Sample "+sampleEntity.getName()+":");
    	logger.info("    Processing = "+runProcessing);
    	logger.info("    Merge = "+runMerge);
    	logger.info("    Stitch = "+runStitch +((!runStitch&&hasStitch)?" (configured but not necessary for a single tile)":""));
    	logger.info("    Analysis = "+runAnalysis);
    }
}
