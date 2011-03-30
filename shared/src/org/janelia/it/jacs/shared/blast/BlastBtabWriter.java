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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 24, 2009
 * Time: 5:42:48 PM
 * <p/>
 * Attempt to mimic the output of the JCVI BPbtab perl script (/usr/local/common/BPbtab);
 * this output format IS NOT the same as the NCBI btab tool
 */
public class BlastBtabWriter extends BlastWriter {
    private String dateOfAnalysis;

    public BlastBtabWriter() {
        SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy");
        dateOfAnalysis = df.format(new Date());
    }

    public void writeTopPortion() throws Exception {
        // no header for tab separated file
    }

    public void writeBottomPortion() throws Exception {
        // no footer for tab separated file
    }

    public void startQueryPortion() throws Exception {
        // nothing to write here
    }

    public void endQueryPortion() throws Exception {
        //nothing to write here
    }

    /**
     * For this format each row in a file is an HSP
     *
     * @param qID
     * @param pbrList
     * @param queryCounter
     * @throws Exception
     */
    public void writeSingleQueryPortion(String qID, List<ParsedBlastResult> pbrList, long queryCounter) throws Exception {

        for (ParsedBlastResult pbr : pbrList) {
            for (ParsedBlastHSP hsp : pbr.getHspList()) {
                // Column 1: Query Sequence name
                bufferedWriter.print(qID + "\t");
                // Column 2: Date of analisys - Blast does not return it, so will use system date
                bufferedWriter.print(dateOfAnalysis + "\t");
                // Column 3: length of query
                bufferedWriter.print(String.valueOf(pbr.getQueryLength()) + "\t");
                // Column 4: search method (program name)
                bufferedWriter.print(task.getTaskName() + "\t");
                // Column 5: DB file name
                bufferedWriter.print(getBlastDB() + "\t");
                // Column 6: Subject Sequence name
                bufferedWriter.print(pbr.getSubjectId() + "\t");
                HSPCoordinates hspCoordinates = new HSPCoordinates(hsp);
                // Column 7: start of query's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getQueryBeginCoordinate()) + "\t");
                // Column 8: end of query's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getQueryEndCoordinate()) + "\t");
                // Column 9: start of subjects's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getSubjectBeginCoordinate()) + "\t");
                // Column 10: end of subjects's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getSubjectEndCoordinate()) + "\t");
                // Column 11: percent identity
                bufferedWriter.print(String.valueOf(getPercentIdentity(hsp)) + "\t");
                // Column 12: percent similarity
                bufferedWriter.print(String.valueOf(getPercentSimilarity(hsp)) + "\t");
                // Column 13: Blast Score
                bufferedWriter.print(String.valueOf(hsp.getHspScore()) + "\t");
                // Column 14: Bit Score
                bufferedWriter.print(String.valueOf(hsp.getBitScore()) + "\t");
                // Column 15: ???
                bufferedWriter.print("\t");
                // Column 16: Comment / Description
                bufferedWriter.print(getSubjectComment(pbr) + "\t");
                // Column 17: Frame - will use query frame value
                bufferedWriter.print(String.valueOf(hsp.getQueryFrame()) + "\t");
                // Column 18: Query strand - use query orientation to determine
                bufferedWriter.print(getStrandIdentifier(hsp.getQueryOrientation()) + "\t");
                // Column 19: Length of subject sequence
                bufferedWriter.print(String.valueOf(pbr.getSubjectLength()) + "\t");
                // Column 20: E-value
                bufferedWriter.print(String.valueOf(hsp.getExpectScore()) + "\t");
                // Column 21: P-value: NCBI Blast does not produce it
                // also print new line
                bufferedWriter.println("");

            }
        }
    }

    private String getSubjectComment(ParsedBlastResult pbr) {
        String comment =
                (pbr.getSubjectDefline() != null) ? pbr.getSubjectDefline() : deflineMap.get(pbr.getSubjectId());

        // (if possible) remove the subject id from the defline
        // and any leading/trailing whitespace as seems to be done by BPbtab
        if (comment != null) {
            comment = comment.replace(pbr.getSubjectId(), "");
            comment = comment.trim();
        }

        return comment;
    }

    private String getStrandIdentifier(Integer orientation) {
        String strandIdentifier;

        if (BlastHit.ALGN_ORI_FORWARD.equals(orientation)) {
            strandIdentifier = "Plus";
        }
        else if (BlastHit.ALGN_ORI_REVERSE.equals(orientation)) {
            strandIdentifier = "Minus";
        }
        else {
            strandIdentifier = "null";
        }

        return strandIdentifier;
    }
}
