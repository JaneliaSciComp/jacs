/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
