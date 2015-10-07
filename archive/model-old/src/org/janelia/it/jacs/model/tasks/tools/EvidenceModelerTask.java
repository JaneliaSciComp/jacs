
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 9, 2010
 * Time: 9:45:00 AM
 */
public class EvidenceModelerTask extends Task {

    /*  evidence_modeler.pl helpinfo:
    (Note:  The VICS service will hijack --exec_dir, pointing it* to the EvidenceModelerResultNode)

        ################# Evidence Modeler ##############################
        #
        #  parameters:
        #
        #  Required:
        #
        #  --genome              genome sequence in fasta format
        #  --weights              weights for evidence types file
        #  --gene_predictions     gene predictions gff3 file
        #
        #  Optional but recommended:
        #  --protein_alignments   protein alignments gff3 file
        #  --transcript_alignments       transcript alignments gff3 file
        #
        #  Optional and miscellaneous
        #
        #  --repeats               gff3 file with repeats masked from genome file
        #
        #
        #  --terminalExons         supplementary file of additional terminal exons to consider (from PASA long-orfs)
        #
        #  --stitch_ends             file listing source types to apply end stitching
        #                                 into existing exons (ie. 'genewise,alignAssembly')
        #  --extend_to_terminal      file listing source types to extend terminal alignment segment into a terminal exon (ie. 'genewise')
        #
        #  --stop_codons             list of stop codons (default: TAA,TGA,TAG)
        #                               *for Tetrahymena, set --stop_codons TGA
        #  --min_intron_length       minimum length for an intron (default 20 bp)
        #  --INTERGENIC_SCORE_ADJUST_FACTOR    value <= 1 applied to the calculated intergenic scores  (default 1)
        #  --exec_dir                directory that EVM cd's to before running.
        #
        # flags:
        #
        #  --forwardStrandOnly   runs only on the forward strand
        #  --reverseStrandOnly   runs only on the reverse strand
        #
        #  -S                    verbose flag
        #  --debug               debug mode, writes lots of extra files.
        #  --report_ELM          report the eliminated EVM preds too.
        #
        #  --NO_RECURSE          when set, only performs a single DP scan across the sequence.
        #
        #################################################################

    */

    // Required:
    transient public static final String PARAM_fastaInputNodeId = "fasta input node id";
    transient public static final String PARAM_weights = "weights";
    transient public static final String PARAM_gene_predictions = "gene predictions";
    // Optional but recommended:
    transient public static final String PARAM_protein_alignments = "protein alignments";
    transient public static final String PARAM_transcript_alignments = "transcript alignments";
    // Optional and miscellaneous:
    transient public static final String PARAM_repeats = "repeats";
    transient public static final String PARAM_terminalExons = "terminalExons";
    transient public static final String PARAM_stitch_ends = "stitch ends";
    transient public static final String PARAM_extend_to_terminal = "extend to terminal";
    transient public static final String PARAM_stop_codons = "stop codons";
    transient public static final String PARAM_min_intron_length = "min intron length";
    transient public static final String PARAM_INTERGENIC_SCORE_ADJUST_FACTOR = "intergenic score adjust factor";
    transient public static final String PARAM_exec_dir = "exec dir";
    // flags:
    transient public static final String PARAM_forwardStrandOnly = "forwardStrandOnly";
    transient public static final String PARAM_reverseStrandOnly = "reverseStrandOnly";
    transient public static final String PARAM_verbose = "verbose";
    transient public static final String PARAM_debug = "debug";
    transient public static final String PARAM_report_ELM = "report ELM";
    transient public static final String PARAM_RECURSE = "recurse";
    // Hidden params:
    transient public static final String PARAM_limit_range_lend = "limit range lend";
    transient public static final String PARAM_limit_range_rend = "limit range rend";
    //Partitioning params:
    transient public static final String PARAM_segmentSize = "segment size";
    transient public static final String PARAM_overlapSize = "overlap size";

    // Defaults:
    transient public static final String segmentSize_DEFAULT = "100000";
    transient public static final String overlapSize_DEFAULT = "10000";


    public EvidenceModelerTask() {
        super();
        setTaskName("EvidenceModelerTask");
        setParameter(PARAM_fastaInputNodeId, "");
        setParameter(PARAM_segmentSize, segmentSize_DEFAULT);
        setParameter(PARAM_overlapSize, overlapSize_DEFAULT);
    }

    public String getDisplayName() {
        return "EvidenceModelerTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateEvidenceModelerCommandOptions(String outputDirectory) throws ParameterException {
        StringBuffer sb = new StringBuffer();
        // Required:
        addRequiredCommandParameter(sb, " --weights", PARAM_weights);
        addRequiredCommandParameter(sb, " --gene_predictions", PARAM_gene_predictions);
        // Optional but recommended:
        addCommandParameter(sb, " --protein_alignments", PARAM_protein_alignments);
        addCommandParameter(sb, " --transcript_alignments", PARAM_transcript_alignments);
        // Optional and miscellaneous:
        addCommandParameter(sb, " --repeats", PARAM_repeats);
        addCommandParameter(sb, " --terminalExons", PARAM_terminalExons);
        addCommandParameter(sb, " --stitch_ends", PARAM_stitch_ends);
        addCommandParameter(sb, " --extend_to_terminal", PARAM_extend_to_terminal);
        addCommandParameter(sb, " --stop_codons", PARAM_stop_codons);
        addCommandParameter(sb, " --min_intron_length", PARAM_min_intron_length);
        addCommandParameter(sb, " --INTERGENIC_SCORE_ADJUST_FACTOR", PARAM_INTERGENIC_SCORE_ADJUST_FACTOR);
        // flags:
        addCommandParameterFlag(sb, "--forwardStrandOnly", PARAM_forwardStrandOnly);
        addCommandParameterFlag(sb, "--reverseStrandOnly", PARAM_reverseStrandOnly);
        addCommandParameterFlag(sb, "-S", PARAM_verbose);
        addCommandParameterFlag(sb, "--debug", PARAM_debug);
        addCommandParameterFlag(sb, "--report_ELM", PARAM_report_ELM);
        addCommandParameterFlag(sb, "--RECURSE", PARAM_RECURSE);
        // Hidden params:
        addCommandParameter(sb, " --limit_range_lend", PARAM_limit_range_lend);
        addCommandParameter(sb, " --limit_range_rend", PARAM_limit_range_rend);

        if (outputDirectory != null && outputDirectory.trim().length() > 0) {
            sb.append(" --exec_dir ").append(outputDirectory);     // *Toldja.
        }
        return sb.toString();
    }

    public String generateWriteCmdOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        // Required:
        addRequiredCommandParameter(sb, "--weights", PARAM_weights);
        addRequiredCommandParameter(sb, "--gene_predictions", PARAM_gene_predictions);
        // Optional but recommended:
        addCommandParameter(sb, " --protein_alignments", PARAM_protein_alignments);
        addCommandParameter(sb, " --transcript_alignments", PARAM_transcript_alignments);
        // Optional and miscellaneous:
        addCommandParameter(sb, " --repeats", PARAM_repeats);
        addCommandParameter(sb, " --terminalExons", PARAM_terminalExons);
        addCommandParameter(sb, " --stitch_ends", PARAM_stitch_ends);
        addCommandParameter(sb, " --extend_to_terminal", PARAM_extend_to_terminal);
        addCommandParameter(sb, " --stop_codons", PARAM_stop_codons);
        addCommandParameter(sb, " --min_intron_length", PARAM_min_intron_length);
        // flags:
        addCommandParameterFlag(sb, "--forwardStrandOnly", PARAM_forwardStrandOnly);
        addCommandParameterFlag(sb, "--reverseStrandOnly", PARAM_reverseStrandOnly);
        addCommandParameterFlag(sb, "-S", PARAM_verbose);
        addCommandParameterFlag(sb, "--debug", PARAM_debug);
        addCommandParameterFlag(sb, "--RECURSE", PARAM_RECURSE);
        return sb.toString();
    }

    public String generatePartitionCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        //Required:
        addRequiredCommandParameter(sb, "--gene_predictions", PARAM_gene_predictions);
        addRequiredCommandParameter(sb, "--segmentSize", PARAM_segmentSize);
        addRequiredCommandParameter(sb, "--overlapSize", PARAM_overlapSize);
        // Optional but recommended:
        addCommandParameter(sb, " --protein_alignments", PARAM_protein_alignments);
        addCommandParameter(sb, " --transcript_alignments", PARAM_transcript_alignments);
        addCommandParameter(sb, " --pasaTerminalExons", PARAM_terminalExons);
        addCommandParameter(sb, " --repeats", PARAM_repeats);
        return sb.toString();

    }
}