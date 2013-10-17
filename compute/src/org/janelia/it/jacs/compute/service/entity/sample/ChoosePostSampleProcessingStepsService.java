package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides which types of processing will be run for a Sample after the initial processing is done.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChoosePostSampleProcessingStepsService extends AbstractEntityService {

    public void execute() throws Exception {

        final List<String> alignAlgorithms = data.getCsvStringItem("ALIGNMENT_ALGORITHMS");
        final List<String> alignAlgorithmParams = data.getCsvStringItem("ALIGNMENT_ALGORITHM_PARAMS");
        final List<String> alignAlgorithmResultNames = data.getCsvStringItem("ALIGNMENT_ALGORITHM_RESULT_NAMES");
        final List<String> analysisAlgorithms = data.getCsvStringItem("ANALYSIS_ALGORITHMS");

        final int numberOfAlignAlgorithms = alignAlgorithms.size();
        final int numberOfAlignAlgorithmParams = alignAlgorithmParams.size();
        final int numberOfAlignAlgorithmResultNames = alignAlgorithmResultNames.size();

        List<ParameterizedAlignmentAlgorithm> parameterizedAlignmentAlgorithms =
                new ArrayList<ParameterizedAlignmentAlgorithm>();

        AlignmentAlgorithm aa;
        String p;
        String n;
        for (int i = 0; i < numberOfAlignAlgorithms; i++) {

            aa = AlignmentAlgorithm.valueOf(alignAlgorithms.get(i));

            if (i < numberOfAlignAlgorithmParams) {
                p = alignAlgorithmParams.get(i);
            } else {
                p = null;
                contextLogger.info("Alignment algorithm " + aa + " specified with no parameter");
            }

            if (i < numberOfAlignAlgorithmResultNames) {
                n = alignAlgorithmResultNames.get(i);
            } else {
                n = "Brain Alignment";
                contextLogger.info("Alignment algorithm " + aa + " specified with default name: " + n);
            }

            parameterizedAlignmentAlgorithms.add(new ParameterizedAlignmentAlgorithm(aa, p, n));
        }

        data.putItem("PARAMETERIZED_ALIGNMENT_ALGORITHM", parameterizedAlignmentAlgorithms);
        data.putItem("ANALYSIS_ALGORITHM", analysisAlgorithms);

        final boolean hasAlignment = numberOfAlignAlgorithms > 0;
        final boolean runAlignment = hasAlignment && (! skipAlignment());
        final boolean runAnalysis = analysisAlgorithms.size() > 0;

        data.putItem("RUN_ALIGNMENT", runAlignment);
        data.putItem("RUN_ANALYSIS", runAnalysis);
    }

    /**
     * @return true if a skip alignment tile filter has been set and this sample contains at least one matching tile.
     */
    private boolean skipAlignment() {

        boolean skipAlignment = false;

        final String skipAlignmentTileFilter = data.getStringItem("SKIP_ALIGNMENT_TILE_FILTER");
        if (! StringUtils.isEmpty(skipAlignmentTileFilter)) {
            final Pattern skipTileNamePattern = Pattern.compile(skipAlignmentTileFilter);

            @SuppressWarnings("unchecked")
            final List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getItem("SAMPLE_AREA");
            if (sampleAreas != null) {
                List<Entity> tiles;
                String tileName;
                for (AnatomicalArea area : sampleAreas) {
                    tiles = area.getTiles();
                    for (Entity tile: tiles) {
                        tileName = tile.getName();
                        final Matcher m = skipTileNamePattern.matcher(tileName);
                        if (m.matches()) {
                            skipAlignment = true;
                            contextLogger.info("skipping alignment because SAMPLE_AREA contains tile '" +
                                               tileName + "'");
                            break;
                        }
                    }
                    if (skipAlignment) {
                        break;
                    }
                }
            }
        }

        return skipAlignment;
    }
}
