package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 30, 2010
 * Time: 12:16:04 PM
 */
public class TrfTask extends Task {

    /*
        Trf helpinfo:
        Tandem Repeats Finder, Version 4.00
        Copyright (C) Dr. Gary Benson 1999-2004. All rights reserved.
        
        
        Please use: trf File Match Mismatch Delta PM PI Minscore MaxPeriod [options]
        
        Where: (all weights, penalties, and scores are positive)
          File = sequences input file
          Match  = matching weight
          Mismatch  = mismatching penalty
          Delta = indel penalty
          PM = match probability (whole number)
          PI = indel probability (whole number)
          Minscore = minimum alignment score to report
          MaxPeriod = maximum period size to report
          [options] = one or more of the following :
                       -m    masked sequence file
                       -f    flanking sequence
                       -d    data file
                       -h    suppress html output
        
        Note the sequence file should be in FASTA format:
        
        >Name of sequence
           aggaaacctg ccatggcctc ctggtgagct gtcctcatcc actgctcgct gcctctccag
           atactctgac ccatggatcc cctgggtgca gccaagccac aatggccatg gcgccgctgt
           actcccaccc gccccaccct cctgatcctg ctatggacat ggcctttcca catccctgtg
        
     */


    transient public static final String PARAM_matching_weight = "matching weight";
    transient public static final String PARAM_mismatching_penalty = "mismatching_penalty";
    transient public static final String PARAM_indel_penalty = "indel_penalty";
    transient public static final String PARAM_match_probability = "match_probability";
    transient public static final String PARAM_indel_probability = "indel_probability";
    transient public static final String PARAM_minscore = "minscore";
    transient public static final String PARAM_maxperiod = "maxperiod";
    transient public static final String PARAM_masked_sequence_file = "masked sequence file";
    transient public static final String PARAM_flanking_sequence = "flanking sequence";
    transient public static final String PARAM_data_file = "data file";
    transient public static final String PARAM_suppress_html_output = "suppress html output";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    transient public static final String suppress_html_output_DEFAULT = "1";

    public TrfTask() {
        super();
        setTaskName("TrfTask");
        setParameter(PARAM_suppress_html_output, suppress_html_output_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "TrfTask";
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

    public String getNameSuffix() {
        return (getParameter(PARAM_matching_weight) + "." +
                getParameter(PARAM_mismatching_penalty) + "." +
                getParameter(PARAM_indel_penalty) + "." +
                getParameter(PARAM_match_probability) + "." +
                getParameter(PARAM_indel_probability) + "." +
                getParameter(PARAM_minscore) + "." +
                getParameter(PARAM_maxperiod));
    }

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        addRequiredCommandParameterValue(sb, PARAM_matching_weight);
        addRequiredCommandParameterValue(sb, PARAM_mismatching_penalty);
        addRequiredCommandParameterValue(sb, PARAM_indel_penalty);
        addRequiredCommandParameterValue(sb, PARAM_match_probability);
        addRequiredCommandParameterValue(sb, PARAM_indel_probability);
        addRequiredCommandParameterValue(sb, PARAM_minscore);
        addRequiredCommandParameterValue(sb, PARAM_maxperiod);
        addCommandParameter(sb, "-m", PARAM_masked_sequence_file);
        addCommandParameter(sb, "-f", PARAM_flanking_sequence);
        addCommandParameter(sb, "-d", PARAM_data_file);
        addCommandParameterFlag(sb, "-h", PARAM_suppress_html_output);
        return sb.toString();
    }

}