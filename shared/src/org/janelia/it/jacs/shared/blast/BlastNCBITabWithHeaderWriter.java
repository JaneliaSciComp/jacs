
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
