
package org.janelia.it.jacs.model.tasks.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2010
 * Time: 4:07:18 PM
 */
public class CufflinksTask extends Task {

    transient public static final String PARAM_sam_file_input_node_id = "sam file input node id";
    transient public static final String PARAM_inner_dist_mean = "expected inner mean distance between mate pairs";
    transient public static final String PARAM_inner_dist_std_dev = "standard dev for dist of inner distances between mate pairs";
    transient public static final String PARAM_collapse_thresh = "median depth of cov needed for preassembly collapse";
    transient public static final String PARAM_max_intron_length = "maximum intron length";
    transient public static final String PARAM_min_isoform_fraction = "minimum fraction vs most abundant isoform for alt isoform";
    transient public static final String PARAM_min_intron_fraction = "filter spliced alignments below this level";
    transient public static final String PARAM_junc_alpha = "alpha for junction binomial test filter";
    transient public static final String PARAM_small_anchor_fraction = "percent read overhang taken as suspiciously small";
    transient public static final String PARAM_pre_mrna_fraction = "local coverage fraction below which to ignore intron alignments";
    transient public static final String PARAM_min_mapqual = "ignore sam mapping quality below this value";
    transient public static final String PARAM_alt_label = "alternate label to use when reporting transfrags in GTF";
    transient public static final String PARAM_gtf_node_id = "use gtf reference annotation to constrain isoform estimation";

    // Default values
    transient public static final String inner_dist_mean_DEFAULT = "45";
    transient public static final String inner_dist_std_dev_DEFAULT = "20";
    transient public static final String collapse_thresh_DEFAULT = "10000";
    transient public static final String min_isoform_fraction_DEFAULT = "0.15";
    transient public static final String min_intron_fraction_DEFAULT = "0.05";
    transient public static final String junc_alpha_DEFAULT = "0.01";
    transient public static final String small_anchor_fraction_DEFAULT = "0.12";
    transient public static final String pre_mrna_fraction_DEFAULT = "0.05";
    transient public static final String max_intron_length_DEFAULT = "300000";
    transient public static final String min_mapqual_DEFAULT = "0";
    transient public static final String alt_label_DEFAULT = "CUFF";

    public CufflinksTask() {
        super();
        setTaskName("CufflinksTask");
        setParameter(PARAM_sam_file_input_node_id, "");
        setParameter(PARAM_gtf_node_id, "");
    }

    public String getDisplayName() {
        return "CufflinksTask";
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

    public String generateCommandOptions(String gtfOptionalFilePath) {
        StringBuffer sb = new StringBuffer();
        addCommandParameter(sb, "-m", PARAM_inner_dist_mean);
        addCommandParameter(sb, "-s", PARAM_inner_dist_std_dev);
        addCommandParameter(sb, "-c", PARAM_collapse_thresh);
        addCommandParameter(sb, "-I", PARAM_max_intron_length);
        addCommandParameter(sb, "-F", PARAM_min_isoform_fraction);
        addCommandParameter(sb, "-f", PARAM_min_intron_fraction);
        addCommandParameter(sb, "--junc-alpha", PARAM_junc_alpha);
        addCommandParameter(sb, "--small-anchor-fraction", PARAM_small_anchor_fraction);
        addCommandParameter(sb, "-j", PARAM_pre_mrna_fraction);
        addCommandParameter(sb, "-Q", PARAM_min_mapqual);
        addCommandParameter(sb, "-L", PARAM_alt_label);
        if (gtfOptionalFilePath != null && gtfOptionalFilePath.trim().length() > 0) {
            sb.append(" -G ").append(gtfOptionalFilePath);
        }
        return sb.toString();
    }

}
