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

import org.janelia.it.jacs.model.genomics.BlastHit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Jan 30, 2007
 * Time: 3:23:45 PM
 */
public class ParsedBlastResult implements Serializable, Cloneable, Comparable {

    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Alignment Values
    public String programUsed;
    public String blastVersion;
    public Double bestExpectScore;
    public String comment;
    public Float bestBitScore = 0F;

    // Subject sequence
    public String subjectId;
    public Integer subjectLength;
    private String subjectDefline;

    // Query sequence
    public String queryId;
    public Integer queryLength;
    private String queryDefline;

    // HSPs
    private List<ParsedBlastHSP> hspList = new ArrayList<ParsedBlastHSP>();

    public ParsedBlastResult() {
    }

    public ParsedBlastResult(BlastHit bh) {
        // Alignment Values
        programUsed = bh.getProgramUsed();
        blastVersion = bh.getBlastVersion();

        // subject sequence
        subjectId = bh.getSubjectAcc();
        subjectLength = bh.getSubjectLength();

        // query sequence
        queryId = bh.getQueryAcc();
        queryLength = bh.getQueryLength();

        // HSP
        List<ParsedBlastHSP> tmpList = new ArrayList<ParsedBlastHSP>();
        ParsedBlastHSP hsp = new ParsedBlastHSP();
        hsp.setBitScore(bh.getBitScore());
        hsp.setHspScore(bh.getHspScore());
        hsp.setLengthAlignment(bh.getLengthAlignment());
        hsp.setNumberIdentical(bh.getNumberIdentical());
        hsp.setNumberSimilar(bh.getNumberSimilar());
        hsp.setMidline(bh.getMidline());
        hsp.setSubjectBegin(bh.getSubjectBegin());
        hsp.setSubjectEnd(bh.getSubjectEnd());
        hsp.setSubjectOrientation(bh.getSubjectOrientation());
        hsp.setSubjectGaps(bh.getSubjectGaps());
        hsp.setSubjectGapRuns(bh.getSubjectGapRuns());
        hsp.setSubjectStops(bh.getSubjectStops());
        hsp.setSubjectNumberUnalignable(bh.getSubjectNumberUnalignable());
        hsp.setSubjectAlignString(bh.getSubjectAlignString());
        hsp.setSubjectFrame(bh.getSubjectFrame());
        hsp.setQueryBegin(bh.getQueryBegin());
        hsp.setQueryEnd(bh.getQueryEnd());
        hsp.setQueryOrientation(bh.getQueryOrientation());
        hsp.setQueryGaps(bh.getQueryGaps());
        hsp.setQueryGapRuns(bh.getQueryGapRuns());
        hsp.setQueryStops(bh.getQueryStops());
        hsp.setQueryNumberUnalignable(bh.getQueryNumberUnalignable());
        hsp.setQueryAlignString(bh.getQueryAlignString());
        hsp.setQueryFrame(bh.getQueryFrame());
        tmpList.add(hsp);
        setHspList(tmpList);
    }

    public String getProgramUsed() {
        return programUsed;
    }

    public void setProgramUsed(String programUsed) {
        this.programUsed = programUsed;
    }

    public String getBlastVersion() {
        return blastVersion;
    }

    public void setBlastVersion(String blastVersion) {
        this.blastVersion = blastVersion;
    }

    public Double getBestExpectScore() {
        return bestExpectScore;
    }

    public void setBestExpectScore(Double expectScore) {
        this.bestExpectScore = expectScore;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getSubjectLength() {
        return subjectLength;
    }

    public void setSubjectLength(Integer subjectLength) {
        this.subjectLength = subjectLength;
    }

    public String getSubjectDefline() {
        return subjectDefline;
    }

    public void setSubjectDefline(String subjectDefline) {
        this.subjectDefline = subjectDefline;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public Integer getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(Integer queryLength) {
        this.queryLength = queryLength;
    }

    public void setHspList(List<ParsedBlastHSP> hspList) {
        this.hspList = hspList;
        Collections.sort(this.hspList);

        bestExpectScore = null;
        bestBitScore = 0F;

        ParsedBlastHSP hsp = this.hspList.get(0);
        bestExpectScore = hsp.getExpectScore();
        bestBitScore = hsp.getBitScore();
    }

    public List<ParsedBlastHSP> getHspList() {
        return hspList;
    }

    public String getQueryDefline() {
        return queryDefline;
    }

    public void setQueryDefline(String queryDefline) {
        this.queryDefline = queryDefline;
    }

    public String toString() {
        return (
                "pgm: " + programUsed + "\t" +
                        "qry: " + queryId + "\t" +
                        "qlen: " + queryLength + "\t" +
                        "subj: " + subjectId + "\t" +
                        "slen: " + subjectLength + "\t" +
                        "eval: " + bestExpectScore + "\t" +
                        "#hsp: " + hspList.size() + "\t" +
                        "bbit: " + bestBitScore + "\t");
    }

    public String toFormattedString() {
//        String one = String.format("pgm: %1$-10.10s ", programUsed);
//        String two = String.format("qry: %1$-20.20s ", queryId);
//        String three = String.format("qlen: %1$-9d ", queryLength);
//        String four = String.format("subj: %1$-20.20s ", subjectId);
//        String five = String.format("slen: %1$-9d ", subjectLength);
//        String six = String.format("e-val: %1$-9e ", bestExpectScore);
//        String seven = String.format("bbit: %1$-8.2f ", bestBitScore);
//        String eight = String.format("#hsp: %1$-3d ", hspList.size());
//
//        String out = one + two + three + four + five + six + seven + eight;

        return String.format
                ("pgm: %1$-10.10s qry: %2$-20.20s qlen: %3$-9d subj: %4$-20.20s slen: %5$-9d e-val: %6$-10.8e bbit: %7$-8.2f #hsp: %8$-3d",
                        programUsed, queryId, queryLength, subjectId, subjectLength, bestExpectScore, bestBitScore, hspList.size());
    }

    // Stole this from BlastHit
    public int compareTo(Object o) {
//
// order by QUERY sequence
        ParsedBlastResult pr2 = (ParsedBlastResult) o;
        int result = this.queryId.compareTo(pr2.queryId);
        if (result != 0) return result;

//
// order by BEST hsp evalue
        result = this.bestExpectScore.compareTo(pr2.bestExpectScore);
        if (result != 0) return result;
//
// order by BEST hsp bit-score
        result = -this.bestBitScore.compareTo(pr2.bestBitScore);
        if (result != 0) return result;
//
// order by HSP e-value/bit-score
        Iterator thisIter = this.hspList.iterator();
        Iterator pr2Iter = pr2.getHspList().iterator();
        while (thisIter.hasNext() && pr2Iter.hasNext() && result == 0) {
            result = ((ParsedBlastHSP) thisIter.next()).compareScore(pr2Iter.next());
        }
        if (result != 0) return result;
        if (thisIter.hasNext()) return -1;
        if (pr2Iter.hasNext()) return 1;
//
// order by subject sequence
        result = this.subjectId.compareTo(pr2.subjectId);
        if (result != 0) return result;
//
// order by HSP content
        thisIter = this.hspList.iterator();
        pr2Iter = pr2.getHspList().iterator();
        while (thisIter.hasNext() && pr2Iter.hasNext() && result == 0) {
            result = ((ParsedBlastHSP) thisIter.next()).compareTo(pr2Iter.next());
        }
        if (result != 0) return result;
        if (thisIter.hasNext()) return -1;
        if (pr2Iter.hasNext()) return 1;
//
// hits are identical
        return 0;
    }

    public boolean matchAlignment(ParsedBlastResult pbr) {
        if (!this.queryId.equals(pbr.queryId) || !this.subjectId.equals(pbr.subjectId)) return false;

        if (this.getHspList().size() != pbr.getHspList().size()) return false;

        Iterator thisIter = this.getHspList().iterator();
        Iterator pbrIter = pbr.getHspList().iterator();
        while (thisIter.hasNext()) {
            ParsedBlastHSP thisHsp = (ParsedBlastHSP) thisIter.next();
            ParsedBlastHSP pbrHsp = (ParsedBlastHSP) pbrIter.next();
            if (!thisHsp.getQueryBegin().equals(pbrHsp.getQueryBegin()) ||
                    !thisHsp.getQueryEnd().equals(pbrHsp.getQueryEnd()) ||
                    !thisHsp.getQueryOrientation().equals(pbrHsp.getQueryOrientation()) ||
                    !thisHsp.getSubjectBegin().equals(pbrHsp.getSubjectBegin()) ||
                    !thisHsp.getSubjectEnd().equals(pbrHsp.getSubjectEnd()) ||
                    !thisHsp.getSubjectOrientation().equals(pbrHsp.getSubjectOrientation())) {
                return false;
            }
        }

        return true;
    }

    public boolean matchQuery(ParsedBlastResult pbr) {
        return this.queryId.equals(pbr.queryId);
    }

    public boolean matchSubject(ParsedBlastResult pbr) {
        return this.subjectId.equals(pbr.subjectId);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}