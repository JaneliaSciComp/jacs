
package org.janelia.it.jacs.model.tasks.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 23, 2010
 * Time: 4:59:49 PM
 */
public class RnaSeqPipelineTask extends Task {

    // Top-level parameters
    transient public static final String PARAM_read_mapper = "Read mapper";
    transient public static final String PARAM_transcript_assembler = "Transcript assembler";
    transient public static final String PARAM_input_reads_fastQ_node_id = "input reads fastQ node id";
    transient public static final String PARAM_input_refgenome_fasta_node_id = "input reference genome fasta node id";
    transient public static final String PARAM_pasa_db_name = "PASA database name";

    // Tophat parameters
    transient public static final String TOPHAT = "Tophat";

    // Cufflinks parameters
    transient public static final String CUFFLINKS = "Cufflinks";
    transient public static final String PARAM_CUFFLINKS_inner_dist_mean = "expected inner mean distance between mate pairs";
    transient public static final String PARAM_CUFFLINKS_inner_dist_std_dev = "standard dev for dist of inner distances between mate pairs";
    transient public static final String PARAM_CUFFLINKS_collapse_thresh = "median depth of cov needed for preassembly collapse";
    transient public static final String PARAM_CUFFLINKS_max_intron_length = "maximum intron length";
    transient public static final String PARAM_CUFFLINKS_min_isoform_fraction = "minimum fraction vs most abundant isoform for alt isoform";
    transient public static final String PARAM_CUFFLINKS_min_intron_fraction = "filter spliced alignments below this level";
    transient public static final String PARAM_CUFFLINKS_junc_alpha = "alpha for junction binomial test filter";
    transient public static final String PARAM_CUFFLINKS_small_anchor_fraction = "percent read overhang taken as suspiciously small";
    transient public static final String PARAM_CUFFLINKS_pre_mrna_fraction = "local coverage fraction below which to ignore intron alignments";
    transient public static final String PARAM_CUFFLINKS_min_mapqual = "ignore sam mapping quality below this value";
    transient public static final String PARAM_CUFFLINKS_alt_label = "alternate label to use when reporting transfrags in GTF";
    transient public static final String PARAM_CUFFLINKS_gtf_node_id = "use gtf reference annotation to constrain isoform estimation";


    public RnaSeqPipelineTask() {
        super();
        setTaskName("RnaSeqPipelineTask");

        // Top-level
        setParameter(PARAM_read_mapper, TOPHAT);
        setParameter(PARAM_transcript_assembler, CUFFLINKS);
        setParameter(PARAM_input_reads_fastQ_node_id, "");
        setParameter(PARAM_input_refgenome_fasta_node_id, "");
        setParameter(PARAM_pasa_db_name, "");

        // Cufflinks
        setParameter(PARAM_CUFFLINKS_inner_dist_mean, CufflinksTask.inner_dist_mean_DEFAULT);
        setParameter(PARAM_CUFFLINKS_inner_dist_std_dev, CufflinksTask.inner_dist_std_dev_DEFAULT);
        setParameter(PARAM_CUFFLINKS_collapse_thresh, CufflinksTask.collapse_thresh_DEFAULT);
        setParameter(PARAM_CUFFLINKS_max_intron_length, CufflinksTask.max_intron_length_DEFAULT);
        setParameter(PARAM_CUFFLINKS_min_isoform_fraction, CufflinksTask.min_isoform_fraction_DEFAULT);
        setParameter(PARAM_CUFFLINKS_min_intron_fraction, CufflinksTask.min_intron_fraction_DEFAULT);
        setParameter(PARAM_CUFFLINKS_junc_alpha, CufflinksTask.junc_alpha_DEFAULT);
        setParameter(PARAM_CUFFLINKS_small_anchor_fraction, CufflinksTask.small_anchor_fraction_DEFAULT);
        setParameter(PARAM_CUFFLINKS_pre_mrna_fraction, CufflinksTask.pre_mrna_fraction_DEFAULT);
        setParameter(PARAM_CUFFLINKS_min_mapqual, CufflinksTask.min_mapqual_DEFAULT);

    }

    public String getDisplayName() {
        return "RnaSeq Pipeline Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        return new TextParameterVO(value);
    }

}
