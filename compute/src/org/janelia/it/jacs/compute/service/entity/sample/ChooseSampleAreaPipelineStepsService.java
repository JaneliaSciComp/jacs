package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
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

        final Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);
        AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");

    	List<Entity> tiles = entityBean.getEntitiesById(sampleArea.getTileIds());
        Integer numImagesPerTile = null;
        Integer numTiles = tiles.size();
        for (Entity tile : tiles) {
            populateChildren(tile);
            List<Entity> lsms = EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK);
            if (numImagesPerTile != null && numImagesPerTile != lsms.size()) {
                throw new IllegalStateException("Sample " + sampleEntity.getName() + " (" + sampleEntity.getId() +
                                                ") has differing numbers of images per tile");
            }
            numImagesPerTile = lsms.size();
        }

        final String mergeAlgorithms = data.getItemAsString("MERGE_ALGORITHMS");
        final String stitchAlgorithms = data.getItemAsString("STITCH_ALGORITHMS");
        final String analysisAlgorithms = data.getItemAsString("ANALYSIS_ALGORITHMS");

        data.putItem("MERGE_ALGORITHM", Task.listOfStringsFromCsvString(mergeAlgorithms));
        data.putItem("STITCH_ALGORITHM", Task.listOfStringsFromCsvString(stitchAlgorithms));
        data.putItem("ANALYSIS_ALGORITHM", Task.listOfStringsFromCsvString(analysisAlgorithms));

        final boolean hasMerge = !StringUtils.isEmpty(mergeAlgorithms);
        final boolean hasStitch = !StringUtils.isEmpty(stitchAlgorithms);

        final boolean runProcessing = true;
        final boolean runMerge = hasMerge && (numImagesPerTile != null) && (numImagesPerTile > 1);
        final boolean runStitch = hasStitch && numTiles>1;
        final boolean runAnalysis = !StringUtils.isEmpty(analysisAlgorithms);

        data.putItem("RUN_PROCESSING", runProcessing);
        data.putItem("RUN_MERGE", runMerge);
        data.putItem("RUN_STITCH", runStitch);
        data.putItem("RUN_ANALYSIS", runAnalysis);

        List<String> steps = new ArrayList<String>();
        steps.add("processing"); // runProcessing is always true

        if (runMerge) {
            steps.add("merge");
        }
        if (runStitch) {
            steps.add("stitch");
        }
        if (runAnalysis) {
            steps.add("analysis");
        }

        logger.info("Processing pipeline for Sample "+sampleEntity.getName()+": "+Task.csvStringFromCollection(steps));
    }
}
