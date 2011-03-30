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

package org.janelia.it.jacs.shared.blast;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 30, 2008
 * Time: 1:18:17 PM
 */
public class ParsedBlastHSP implements Serializable, Cloneable, Comparable {

    private Long hspOrd;
    private Double expectScore;
    private Float bitScore;
    private Float hspScore;
    private Float entropy;
    private Integer lengthAlignment;
    private Integer numberIdentical;
    private Integer numberSimilar;
    private String midline;

    // Subject specific values
    private Integer subjectBegin;
    private Integer subjectEnd;
    private Integer subjectOrientation;
    private Integer subjectGaps;
    private Integer subjectGapRuns;
    private Integer subjectStops;
    private Integer subjectNumberUnalignable;
    private String subjectAlignString;
    private Integer subjectFrame;

    // Query specific values
    private Integer queryBegin;
    private Integer queryEnd;
    private Integer queryOrientation;
    private Integer queryGaps;
    private Integer queryGapRuns;
    private Integer queryStops;
    private Integer queryNumberUnalignable;
    private String queryAlignString;
    private Integer queryFrame;

    public ParsedBlastHSP() {
    }

    public int compareScore(Object o) {
        ParsedBlastHSP hsp2 = (ParsedBlastHSP) o;

        int result = this.expectScore.compareTo(hsp2.getExpectScore());
        if (result != 0) return result;

        return -(this.bitScore.compareTo(hsp2.getBitScore()));
    }

    public int compareTo(Object o) {
        ParsedBlastHSP hsp2 = (ParsedBlastHSP) o;

        int result = this.expectScore.compareTo(hsp2.getExpectScore());
        if (result != 0) return result;

        result = -(this.bitScore.compareTo(hsp2.getBitScore()));
        if (result != 0) return result;

        result = -(this.hspScore.compareTo(hsp2.getHspScore()));
        if (result != 0) return result;

        result = -(this.numberIdentical.compareTo(hsp2.getNumberIdentical()));
        if (result != 0) return result;

        result = -(this.numberSimilar.compareTo(hsp2.getNumberSimilar()));
        if (result != 0) return result;

        result = (this.lengthAlignment).compareTo(hsp2.getLengthAlignment());
        if (result != 0) return result;

        result = (this.queryBegin.compareTo(hsp2.getQueryBegin()));
        if (result != 0) return result;

        result = (this.subjectBegin.compareTo(hsp2.getSubjectBegin()));
        if (result != 0) return result;

        result = (this.queryEnd.compareTo(hsp2.getQueryEnd()));
        if (result != 0) return result;

        result = (this.subjectEnd.compareTo(hsp2.getSubjectEnd()));
        if (result != 0) return result;

        result = (this.queryOrientation.compareTo(hsp2.getQueryOrientation()));
        if (result != 0) return result;

        return (this.subjectOrientation.compareTo(hsp2.getSubjectOrientation()));
    }

    public Long getHspOrd() {
        return hspOrd;
    }

    public void setHspOrd(Long hspOrd) {
        this.hspOrd = hspOrd;
    }

    public Double getExpectScore() {
        return expectScore;
    }

    public void setExpectScore(Double expectScore) {
        this.expectScore = expectScore;
    }

    public Float getBitScore() {
        return bitScore;
    }

    public void setBitScore(Float bitScore) {
        this.bitScore = bitScore;
    }

    public Float getHspScore() {
        return hspScore;
    }

    public void setHspScore(Float hspScore) {
        this.hspScore = hspScore;
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

    public String getMidline() {
        return midline;
    }

    public void setMidline(String midline) {
        this.midline = midline;
    }

    public Integer getSubjectBegin() {
        return subjectBegin;
    }

    public void setSubjectBegin(Integer subjectBegin) {
        this.subjectBegin = subjectBegin;
    }

    public Integer getSubjectEnd() {
        return subjectEnd;
    }

    public void setSubjectEnd(Integer subjectEnd) {
        this.subjectEnd = subjectEnd;
    }

    public Integer getSubjectOrientation() {
        return subjectOrientation;
    }

    public void setSubjectOrientation(Integer subjectOrientation) {
        this.subjectOrientation = subjectOrientation;
    }

    public Integer getSubjectGaps() {
        return subjectGaps;
    }

    public void setSubjectGaps(Integer subjectGaps) {
        this.subjectGaps = subjectGaps;
    }

    public Integer getSubjectGapRuns() {
        return subjectGapRuns;
    }

    public void setSubjectGapRuns(Integer subjectGapRuns) {
        this.subjectGapRuns = subjectGapRuns;
    }

    public Integer getSubjectStops() {
        return subjectStops;
    }

    public void setSubjectStops(Integer subjectStops) {
        this.subjectStops = subjectStops;
    }

    public Integer getSubjectNumberUnalignable() {
        return subjectNumberUnalignable;
    }

    public void setSubjectNumberUnalignable(Integer subjectNumberUnalignable) {
        this.subjectNumberUnalignable = subjectNumberUnalignable;
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

    public Integer getQueryBegin() {
        return queryBegin;
    }

    public void setQueryBegin(Integer queryBegin) {
        this.queryBegin = queryBegin;
    }

    public Integer getQueryEnd() {
        return queryEnd;
    }

    public void setQueryEnd(Integer queryEnd) {
        this.queryEnd = queryEnd;
    }

    public Integer getQueryOrientation() {
        return queryOrientation;
    }

    public void setQueryOrientation(Integer queryOrientation) {
        this.queryOrientation = queryOrientation;
    }

    public Integer getQueryGaps() {
        return queryGaps;
    }

    public void setQueryGaps(Integer queryGaps) {
        this.queryGaps = queryGaps;
    }

    public Integer getQueryGapRuns() {
        return queryGapRuns;
    }

    public void setQueryGapRuns(Integer queryGapRuns) {
        this.queryGapRuns = queryGapRuns;
    }

    public Integer getQueryStops() {
        return queryStops;
    }

    public void setQueryStops(Integer queryStops) {
        this.queryStops = queryStops;
    }

    public Integer getQueryNumberUnalignable() {
        return queryNumberUnalignable;
    }

    public void setQueryNumberUnalignable(Integer queryNumberUnalignable) {
        this.queryNumberUnalignable = queryNumberUnalignable;
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

    public Float getEntropy() {
        return entropy;
    }

    public void setEntropy(Float entropy) {
        this.entropy = entropy;
    }
}
