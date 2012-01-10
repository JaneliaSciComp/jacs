
package org.janelia.it.jacs.web.gwt.common.client.model.genomics;

import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;

/**
 * @author Cristian Goina
 */
public class BlastHitWithSample extends BlastHit {
    public static final String SORT_BY_LOCATION = "sample_location";
    public static final String SORT_BY_SAMPLE_ACC = "sampleAcc";
    public static final String SORT_BY_SAMPLE_NAME = "sampleName";

    private Integer clearRangeBegin;
    private Integer clearRangeEnd;
    private Sample sample;

    public BlastHitWithSample() {
        super();
    }

    public BlastHitWithSample(Long blastHitId, Integer subjectBegin, Integer subjectEnd, Integer subjectOrientation,
                              Integer queryBegin, Integer queryEnd, Integer queryOrientation,
                              Float bitScore, String bitScoreFormatted, Float hspScore, Double expectScore, String expectScoreFormatted,
                              String comment, Integer lengthAlignment, Float entropy, Integer numberIdentical, Integer numberSimilar,
                              Integer subjectLength, Integer subjectGaps, Integer subjectGapRuns, Integer subjectStops,
                              Integer subjectNumberUnalignable, Integer subjectFrame,
                              Integer queryLength, Integer queryGaps, Integer queryGapRuns, Integer queryStops,
                              Integer queryNumberUnalignable, Integer queryFrame,
                              String subjectAlignString, String midline, String queryAlignString,
                              String pairwiseAlignmentNarrow, String pairwiseAlignmentWide,
                              BaseSequenceEntity queryEntity,
                              BaseSequenceEntity subjectEntity,
                              Sample sample) {
        super(blastHitId,
                subjectBegin, subjectEnd, subjectOrientation,
                queryBegin, queryEnd, queryOrientation,
                bitScore, bitScoreFormatted,
                hspScore, expectScore, expectScoreFormatted, comment, lengthAlignment, entropy, numberIdentical, numberSimilar,
                subjectLength, subjectGaps, subjectGapRuns, subjectStops, subjectNumberUnalignable, subjectFrame,
                queryLength, queryGaps, queryGapRuns, queryStops, queryNumberUnalignable, queryFrame,
                subjectAlignString, midline, queryAlignString, pairwiseAlignmentNarrow, pairwiseAlignmentWide,
                queryEntity,
                subjectEntity);
        this.sample = sample;
    }

    public org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample getSample() {
        return sample;
    }

    public Integer getClearRangeBegin() {
        return clearRangeBegin;
    }

    public void setClearRangeBegin(Integer clearRangeBegin) {
        this.clearRangeBegin = clearRangeBegin;
    }

    public Integer getClearRangeEnd() {
        return clearRangeEnd;
    }

    public void setClearRangeEnd(Integer clearRangeEnd) {
        this.clearRangeEnd = clearRangeEnd;
    }

    public void setSample(org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample sample) {
        this.sample = sample;
    }

    public Integer getClearRangeBegin_oneResCoords() {
        return clearRangeBegin != null ? new Integer(clearRangeBegin.intValue() + 1) : null;
    }

    public Integer getClearRangeEnd_oneResCoords() {
        return clearRangeEnd;
    }

}
