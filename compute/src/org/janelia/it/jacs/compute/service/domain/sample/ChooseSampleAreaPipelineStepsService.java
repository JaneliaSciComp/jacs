package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.exceptions.MetadataException;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides which types of processing will be run for a Sample Area.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseSampleAreaPipelineStepsService extends AbstractDomainService {

    public void execute() throws Exception {

        final Sample sample = domainHelper.getRequiredSample(data);
        final ObjectiveSample objectiveSample = domainHelper.getRequiredObjectiveSample(sample, data);
        
        AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");

        SampleHelperNG sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        
        List<SampleTile> tiles = sampleHelper.getTilesForArea(objectiveSample, sampleArea);
        
        Integer numImagesPerTile = null;
        Integer numTiles = tiles.size();
        for (SampleTile tile : tiles) {
            int numLsms = tile.getLsmReferences()==null?0:tile.getLsmReferences().size();
            if (numImagesPerTile != null && numImagesPerTile != numLsms) {
                throw new MetadataException("Sample " + sample.getName() + " (" + sample.getId() +
                                                ") has differing numbers of images per tile");
            }
            numImagesPerTile = numLsms;
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

        List<String> steps = new ArrayList<>();
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

        contextLogger.info("Processing pipeline for Sample "+sample.getName()+": "+Task.csvStringFromCollection(steps));
    }
}
