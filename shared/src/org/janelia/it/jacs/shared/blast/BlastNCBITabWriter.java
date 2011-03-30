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

import java.util.List;


/**
 * User: aresnick
 * Date: May 18, 2009
 * Time: 1:05:29 PM
 * <p/>
 * <p/>
 * Description:
 * Writes blast output in headerless tab delimited output format (blast -m8 format)
 * as described here
 * http://books.google.com/books?id=xvcnhDG9fNUC&pg=PA291&lpg=PA291&dq=ncbi+blast+tabular&source=bl&ots=WJpbpIGaBl&sig=w9IpxXzW4aJtpp3PdvkOlX-IYRQ&hl=en&ei=W-YNStqtKcaFtgeS3rCBCA&sa=X&oi=book_result&ct=result&resnum=5#PPA296,M1
 */
public class BlastNCBITabWriter extends BlastWriter {


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
        // nothing to write here
    }

    /**
     * For this format each row in a file is an HSP
     *
     * @param qID          query ID
     * @param pbrList      parsed blast result list
     * @param queryCounter query counter
     * @throws Exception
     */
    public void writeSingleQueryPortion(String qID, List<ParsedBlastResult> pbrList, long queryCounter) throws Exception {
        for (ParsedBlastResult pbr : pbrList) {
            writeHeader(pbr);
            for (ParsedBlastHSP hsp : pbr.getHspList()) {
                // Column 1: Query id
                bufferedWriter.print(qID + "\t");
                // Column 2: Subject id
                bufferedWriter.print(pbr.getSubjectId() + "\t");
                // Column 3: percent identity
                bufferedWriter.print(getPercentIdentity(hsp) + "\t");
                // Column 4: alignment length
                bufferedWriter.print(String.valueOf(hsp.getLengthAlignment()) + "\t");
                // Column 5: mismatches
                bufferedWriter.print(String.valueOf(getMismatches(hsp)) + "\t");
                // Column 6: gap openings
                bufferedWriter.print(String.valueOf(getGapOpenings(hsp)) + "\t");
                HSPCoordinates hspCoordinates = new HSPCoordinates(hsp);
                // Column 7: start of query's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getQueryBeginCoordinate()) + "\t");
                // Column 8: end of query's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getQueryEndCoordinate()) + "\t");
                // Column 9: start of subject's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getSubjectBeginCoordinate()) + "\t");
                // Column 10: end of subject's match
                bufferedWriter.print(String.valueOf(hspCoordinates.getSubjectEndCoordinate()) + "\t");
                // Column 11: E-value
                bufferedWriter.print(String.valueOf(hsp.getExpectScore()) + "\t");
                // Column 12: bit score
                bufferedWriter.println(String.valueOf(hsp.getBitScore()));
            }
        }

    }

    protected void writeHeader(ParsedBlastResult pbr) throws Exception {
        // default implementation is to write no header information
    }

    private Integer getMismatches(ParsedBlastHSP hsp) {
        Integer mismatches = null;
        if (hsp.getLengthAlignment() != null
                && hsp.getNumberIdentical() != null
                && hsp.getNumberSimilar() != null) {
            mismatches = hsp.getLengthAlignment() - hsp.getNumberIdentical() - hsp.getNumberSimilar();
        }
        return mismatches;
    }

    private Integer getGapOpenings(ParsedBlastHSP hsp) {
        Integer gapOpenings = null;
        if (hsp.getQueryGaps() != null
                && hsp.getSubjectGaps() != null) {
            gapOpenings = hsp.getQueryGaps() + hsp.getSubjectGaps();
        }
        return gapOpenings;
    }
}
