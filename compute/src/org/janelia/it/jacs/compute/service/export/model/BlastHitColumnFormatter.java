
package org.janelia.it.jacs.compute.service.export.model;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.export.util.CSVDataConversionHelper;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.shared.node.FastaUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 29, 2008
 * Time: 4:50:40 PM
 */
public class BlastHitColumnFormatter extends ColumnFormatter {
    static Logger _logger = Logger.getLogger(BlastHitColumnFormatter.class);
    public static final Map<BlastHitHeader, String> headerMap = new HashMap<BlastHitHeader, String>();

    public static enum BlastHitHeader {
        QUERY,
        PROGRAM,
        SUBJNAME,
        NCBI_GI,
        ORGANISM,
        TAXON_ID,
        DEFLINE,
        ALIGNMENT_LENGTH,
        SUBJSEQ,
        SCORE,
        BITS,
        EXPECT,
        HSPRANK,
        NUMHSPS,
        CONSERVED,
        IDENTICAL,
        PERCIDENT,
        GAPS,
        FRAME,
        QUERYMATCH,
        QUERYSTART,
        HOMOLOGY,
        SUBJMATCH,
        SUBJSTART
    }

    static {
        headerMap.put(BlastHitHeader.QUERY, "Query Name");
        headerMap.put(BlastHitHeader.PROGRAM, "Program");
        headerMap.put(BlastHitHeader.SUBJNAME, "Subject Name");
        headerMap.put(BlastHitHeader.NCBI_GI, "NCBI gi");
        headerMap.put(BlastHitHeader.ORGANISM, "Organism");
        headerMap.put(BlastHitHeader.TAXON_ID, "Taxon ID");
        headerMap.put(BlastHitHeader.DEFLINE, "Defline");
        headerMap.put(BlastHitHeader.SUBJSEQ, "Subj Seq");
        headerMap.put(BlastHitHeader.SCORE, "Score");
        headerMap.put(BlastHitHeader.BITS, "Bits");
        headerMap.put(BlastHitHeader.EXPECT, "E-Value");
        headerMap.put(BlastHitHeader.HSPRANK, "Hsp Rank");
        headerMap.put(BlastHitHeader.NUMHSPS, "Num Hsps");
        headerMap.put(BlastHitHeader.ALIGNMENT_LENGTH, "Alignment Length");
        headerMap.put(BlastHitHeader.CONSERVED, "Conserved");
        headerMap.put(BlastHitHeader.IDENTICAL, "Identical");
        headerMap.put(BlastHitHeader.PERCIDENT, "Perc Ident");
        headerMap.put(BlastHitHeader.GAPS, "Gaps");
        headerMap.put(BlastHitHeader.FRAME, "Frame");
        headerMap.put(BlastHitHeader.QUERYMATCH, "Query Match");
        headerMap.put(BlastHitHeader.QUERYSTART, "Query Start");
        headerMap.put(BlastHitHeader.HOMOLOGY, "Homology");
        headerMap.put(BlastHitHeader.SUBJMATCH, "Subj Match");
        headerMap.put(BlastHitHeader.SUBJSTART, "Subj Start");
    }

    public static List<String> getHeaderList() {
        List<String> headerList = new ArrayList<String>();
        for (BlastHitHeader h : BlastHitHeader.values()) {
            headerList.add(headerMap.get(h));
        }
        return headerList;
    }

    public static List<String> formatColumns(BlastHitResult bhr) {
        BlastHit bh = bhr.getBlastHit();
        BaseSequenceEntity bse = bh.getSubjectEntity();
        List<String> pl = new ArrayList<String>();
        add(pl, bhr.getQueryDefline());         //QUERY,
        add(pl, bh.getProgramUsed());           //PROGRAM,
        add(pl, bh.getSubjectAcc());            //SUBJNAME,
        add(pl, bse.getNcbiGINumber() + "");            //NCBI_GI,
        add(pl, bse.getOrganism());                     //ORGANISM,
        add(pl, bse.getTaxonId() + "");                 //TAXON_ID,
        add(pl, bse.getDefline().replaceAll(",", " ")); //DEFLINE,      
        add(pl, bh.getLengthAlignment() + "");    //ALIGNMENT_LENGTH,
        add(pl, getSubjectSequence(bse));       //SUBJSEQ,
        add(pl, bh.getHspScore() + "");           //SCORE,
        add(pl, bh.getBitScore() + "");           //BITS,
        add(pl, bh.getExpectScore() + "");        //EXPECT,
        add(pl, bhr.getHspRank() + "");           //HSPRANK,
        add(pl, bhr.getNhsps() + "");             //NUMHSPS,
        add(pl, bh.getNumberSimilar() + "");      //CONSERVED,
        add(pl, bh.getNumberIdentical() + "");    //IDENTICAL,
        add(pl, (100F * bh.getNumberIdentical() / bh.getLengthAlignment()) + ""); //PERCIDENT,
        add(pl, (bh.getSubjectGaps() + bh.getQueryGaps()) + ""); //GAPS,
        add(pl, (bh.getSubjectFrame() + " ; " + bh.getQueryFrame()) + ""); //FRAME,
        add(pl, bh.getQueryAlignString());      //QUERYMATCH,
        add(pl, (bh.getQueryBegin() + 1) + "");   //QUERYSTART,
        add(pl, bh.getMidline());               //HOMOLOGY,
        add(pl, bh.getSubjectAlignString());    //SUBJMATCH,
        add(pl, (bh.getSubjectBegin() + 1) + ""); //SUBJSTART
        return pl;
    }

    protected static String getSubjectSequence(BaseSequenceEntity subjectSeq) {
        if (subjectSeq != null) {
            String fastaSequence;
            if (subjectSeq.getSequenceLength() <= CSVDataConversionHelper.DEFAULT_MAX_SEQ_LENGTH_IN_CSV) {
                try {
                    fastaSequence = FastaUtil.formatFasta(subjectSeq.getDescription(),
                            subjectSeq.getBioSequence().getSequence(), 80 /* fasta width */);
                }
                catch (Exception ex) {
                    _logger.error("Error retrieving sequence for sequenceAcc=" + subjectSeq.getAccession());
                    return "";
                }
                fastaSequence = fastaSequence.trim();  // Otherwise Excell will be confused.
                //fastaSequence = CSVDataConversionHelper.escapeSpecialExcelChars(fastaSequence);
            }
            else {
                fastaSequence = "sequence too large - see accession=" + subjectSeq.getAccession();
            }
            return fastaSequence;
        }
        else {
            return "";
        }
    }

}
