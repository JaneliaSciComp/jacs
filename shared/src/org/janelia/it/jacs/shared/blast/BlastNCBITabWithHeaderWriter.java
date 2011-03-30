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

/**
 * User: aresnick
 * Date: May 18, 2009
 * Time: 1:12:41 PM
 * <p/>
 * <p/>
 * Description:
 * Writes blast output in tab delimited output format with header lines (blast -m9 format)
 * as described here
 * http://books.google.com/books?id=xvcnhDG9fNUC&pg=PA291&lpg=PA291&dq=ncbi+blast+tabular&source=bl&ots=WJpbpIGaBl&sig=w9IpxXzW4aJtpp3PdvkOlX-IYRQ&hl=en&ei=W-YNStqtKcaFtgeS3rCBCA&sa=X&oi=book_result&ct=result&resnum=5#PPA296,M1
 */
public class BlastNCBITabWithHeaderWriter extends BlastNCBITabWriter {

    /**
     * write blast output header lines; sample output shown below
     * <p/>
     * # BLASTP 2.2.5 [Nov-16-2002]
     * # Query:
     * # Database:
     * # Fields: Query id, Subject id, % identity, alignment length, mismatches, gap openings, q.start, q.end. s.start, s.end, e-value, bit score
     *
     * @throws Exception
     */
    protected void writeHeader(ParsedBlastResult pbr) throws Exception {
        // blast program header line
        // # BLAST Program [analysis date]
        bufferedWriter.println("# " + pbr.getBlastVersion());

        // blast query header line
        // # Query: {query string}
        bufferedWriter.println("# Query: " + pbr.getQueryId());

        // balst database header line
        // omitted if no database provided or can be clearly identified
        // # Database: {database}
        // bufferedWriter.println("# Database: " + UNSPECIFIED_STR);

        // blast output fields header line
        // # Fields: Query id, Subject id, % identity, alignment length, mismatches, gap openings, q. start, q. end. s. start, s. end, e-value, bit score
        bufferedWriter.println("# Fields: Query id, Subject id, % identity, alignment length, mismatches, gap openings, q. start, q. end. s. start, s. end, e-value, bit score");
    }
}
