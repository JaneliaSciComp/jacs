
package org.janelia.it.jacs.web.gwt.common.client.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Client-side value object based on org.janelia.it.jacs.model.genomics.BlastHit.
 *
 * @author Michael Press
 */
public class BlastHit extends Alignment implements IsSerializable {
    public static final String SORT_BY_ALIGNMENT_LEN = "lengthAlignment";
    public static final String SORT_BY_BIT_SCORE = "bitScore";

    private Float bitScore = null;
    private String bitScoreFormatted = null;
    private Float hspScore = null;
    private Double expectScore = null;
    private String expectScoreFormatted = null;
    private String comment = null;
    private Integer lengthAlignment = null;
    private Float entropy = null;
    private Integer numberIdentical = null;
    private Integer numberSimilar = null;

    // Subject specific values
    private Integer subjectLength = null;
    private Integer subjectGaps = null;
    private Integer subjectGapRuns = null;
    private Integer subjectStops = null;
    private Integer subjectNumberUnalignable = null;
    private Integer subjectFrame = null;

    // Query specific values
    private Integer queryLength = null;
    private Integer queryGaps = null;
    private Integer queryGapRuns = null;
    private Integer queryStops = null;
    private Integer queryNumberUnalignable = null;
    private Integer queryFrame = null;

    // Alignment strings
    private String subjectAlignString = null;
    private String midline = null;
    private String queryAlignString = null;
    private String pairwiseAlignmentNarrow = null; // 40 chars wide
    private String pairwiseAlignmentWide = null;   // 80 chars wide

    public BlastHit() {
        super();
        this.setAlignmentType(AlignmentType.BLAST_HIT);
    }

    public BlastHit(
            Long blastHitId,
            // Alignment params
            Integer subjectBegin, Integer subjectEnd, Integer subjectOrientation, Integer queryBegin,
            Integer queryEnd, Integer queryOrientation,
            // BlastHit params
            Float bitScore, String bitScoreFormatted, Float hspScore, Double expectScore, String expectScoreFormatted, String comment, Integer lengthAlignment, Float entropy,
            Integer numberIdentical, Integer numberSimilar, Integer subjectLength, Integer subjectGaps,
            Integer subjectGapRuns, Integer subjectStops, Integer subjectNumberUnalignable, Integer subjectFrame,
            Integer queryLength, Integer queryGaps, Integer queryGapRuns, Integer queryStops, Integer queryNumberUnalignable,
            Integer queryFrame, String subjectAlignString, String midline, String queryAlignString, String pairwiseAlignmentNarrow,
            String pairwiseAlignmentWide, BaseSequenceEntity queryEntity, BaseSequenceEntity subjectEntity) {
        super(blastHitId, subjectBegin, subjectEnd, subjectOrientation, queryBegin, queryEnd, queryOrientation,
                queryEntity, subjectEntity);
        this.bitScore = bitScore;
        this.bitScoreFormatted = bitScoreFormatted;
        this.hspScore = hspScore;
        this.expectScore = expectScore;
        this.expectScoreFormatted = expectScoreFormatted;
        this.comment = comment;
        this.lengthAlignment = lengthAlignment;
        this.entropy = entropy;
        this.numberIdentical = numberIdentical;
        this.numberSimilar = numberSimilar;
        this.subjectLength = subjectLength;
        this.subjectGaps = subjectGaps;
        this.subjectGapRuns = subjectGapRuns;
        this.subjectStops = subjectStops;
        this.subjectNumberUnalignable = subjectNumberUnalignable;
        this.subjectFrame = subjectFrame;
        this.queryLength = queryLength;
        this.queryGaps = queryGaps;
        this.queryGapRuns = queryGapRuns;
        this.queryStops = queryStops;
        this.queryNumberUnalignable = queryNumberUnalignable;
        this.queryFrame = queryFrame;
        this.subjectAlignString = subjectAlignString;
        this.midline = midline;
        this.queryAlignString = queryAlignString;
        this.pairwiseAlignmentNarrow = pairwiseAlignmentNarrow;
        this.pairwiseAlignmentWide = pairwiseAlignmentWide;
    }

    public Float getBitScore() {
        return bitScore;
    }

    public String getBitScoreFormatted() {
        return bitScoreFormatted;
    }


    public void setBitScoreFormatted(String bitScoreFormatted) {
        this.bitScoreFormatted = bitScoreFormatted;
    }


    public void setBitScore(Float bitScore) {
        this.bitScore = bitScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Float getEntropy() {
        return entropy;
    }

    public void setEntropy(Float entropy) {
        this.entropy = entropy;
    }

    public Double getExpectScore() {
        return expectScore;
    }

    public void setExpectScore(Double expectScore) {
        this.expectScore = expectScore;
    }

    public Integer getLengthAlignment() {
        return lengthAlignment;
    }

    public void setLengthAlignment(Integer lengthAlignment) {
        this.lengthAlignment = lengthAlignment;
    }

    public Integer getNumberIdentical() {
        return numberIdentical;
    }

    public void setNumberIdentical(Integer numberIdentical) {
        this.numberIdentical = numberIdentical;
    }

    public Integer getNumberSimilar() {
        return numberSimilar;
    }

    public void setNumberSimilar(Integer numberSimilar) {
        this.numberSimilar = numberSimilar;
    }

    public Integer getQueryGapRuns() {
        return queryGapRuns;
    }

    public void setQueryGapRuns(Integer queryGapRuns) {
        this.queryGapRuns = queryGapRuns;
    }

    public Integer getQueryGaps() {
        return queryGaps;
    }

    public void setQueryGaps(Integer queryGaps) {
        this.queryGaps = queryGaps;
    }

    public Integer getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(Integer queryLength) {
        this.queryLength = queryLength;
    }

    public Integer getQueryNumberUnalignable() {
        return queryNumberUnalignable;
    }

    public void setQueryNumberUnalignable(Integer queryNumberUnalignable) {
        this.queryNumberUnalignable = queryNumberUnalignable;
    }

    public Integer getQueryStops() {
        return queryStops;
    }

    public void setQueryStops(Integer queryStops) {
        this.queryStops = queryStops;
    }

    public Integer getSubjectGapRuns() {
        return subjectGapRuns;
    }

    public void setSubjectGapRuns(Integer subjectGapRuns) {
        this.subjectGapRuns = subjectGapRuns;
    }

    public Integer getSubjectGaps() {
        return subjectGaps;
    }

    public void setSubjectGaps(Integer subjectGaps) {
        this.subjectGaps = subjectGaps;
    }

    public Integer getSubjectLength() {
        return subjectLength;
    }

    public void setSubjectLength(Integer subjectLength) {
        this.subjectLength = subjectLength;
    }

    public Integer getSubjectNumberUnalignable() {
        return subjectNumberUnalignable;
    }

    public void setSubjectNumberUnalignable(Integer subjectNumberUnalignable) {
        this.subjectNumberUnalignable = subjectNumberUnalignable;
    }

    public Integer getSubjectStops() {
        return subjectStops;
    }

    public void setSubjectStops(Integer subjectStops) {
        this.subjectStops = subjectStops;
    }

    public Float getHspScore() {
        return hspScore;
    }

    public void setHspScore(Float hspScore) {
        this.hspScore = hspScore;
    }

    public String getMidline() {
        return midline;
    }

    public void setMidline(String midline) {
        this.midline = midline;
    }

    public String getSubjectAlignString() {
        return subjectAlignString;
    }

    public void setSubjectAlignString(String subjectAlignString) {
        this.subjectAlignString = subjectAlignString;
    }

    public Integer getSubjectFrame() {
        return subjectFrame;
    }

    public void setSubjectFrame(Integer subjectFrame) {
        this.subjectFrame = subjectFrame;
    }

    public String getQueryAlignString() {
        return queryAlignString;
    }

    public void setQueryAlignString(String queryAlignString) {
        this.queryAlignString = queryAlignString;
    }

    public Integer getQueryFrame() {
        return queryFrame;
    }

    public void setQueryFrame(Integer queryFrame) {
        this.queryFrame = queryFrame;
    }

    public String getPairwiseAlignmentNarrow() {
        return pairwiseAlignmentNarrow;
    }

    public void setPairwiseAlignmentNarrow(String pairwiseAlignmentNarrow) {
        this.pairwiseAlignmentNarrow = pairwiseAlignmentNarrow;
    }

    public String getExpectScoreFormatted() {
        return expectScoreFormatted;
    }

    public void setExpectScoreFormatted(String expectScoreFormatted) {
        this.expectScoreFormatted = expectScoreFormatted;
    }

    public String getPairwiseAlignmentWide() {
        return pairwiseAlignmentWide;
    }

    public void setPairwiseAlignmentWide(String pairwiseAlignmentWide) {
        this.pairwiseAlignmentWide = pairwiseAlignmentWide;
    }
}
