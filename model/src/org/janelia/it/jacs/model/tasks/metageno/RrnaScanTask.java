
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 3, 2009
 * Time: 9:32:14 AM
 */
public class RrnaScanTask extends Task {

    transient public static final String initial_blast_options_DEFAULT = "-e 0.1 -F T -b 1 -v 1 -z 3000000000 -W 9";
    transient public static final String second_blast_options_DEFAULT = "-e 1e-4 -F m L -b 1500 -v 1500 -q 5 -r 4 -X 1500 -z 3000000000 -W 9 -U T";
    transient public static final String min_5S_length_DEFAULT = "25";
    transient public static final String min_16S_length_DEFAULT = "250";
    transient public static final String min_18S_length_DEFAULT = "250";
    transient public static final String min_23S_length_DEFAULT = "250";

    transient public static final String PARAM_input_fasta_node_id = "input fasta node";
    transient public static final String PARAM_initial_blast_options = "initial_blast_options";
    transient public static final String PARAM_second_blast_options = "second_blast_options";
    transient public static final String PARAM_min_5S_length = "min 5S length";
    transient public static final String PARAM_min_16S_length = "min 16S length";
    transient public static final String PARAM_min_18S_length = "min 18S length";
    transient public static final String PARAM_min_23S_length = "min 23S length";

    public RrnaScanTask() {
        super();
        setTaskName("RrnaScanTask");
        setParameter(PARAM_input_fasta_node_id, "");
        setParameter(PARAM_initial_blast_options, initial_blast_options_DEFAULT);
        setParameter(PARAM_second_blast_options, second_blast_options_DEFAULT);
        setParameter(PARAM_min_5S_length, min_5S_length_DEFAULT);
        setParameter(PARAM_min_16S_length, min_16S_length_DEFAULT);
        setParameter(PARAM_min_18S_length, min_18S_length_DEFAULT);
        setParameter(PARAM_min_23S_length, min_23S_length_DEFAULT);
    }

    public String getDisplayName() {
        return "RrnaScanTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_initial_blast_options))
            return new TextParameterVO(value);
        if (key.equals(PARAM_second_blast_options))
            return new TextParameterVO(value);
        if (key.equals(PARAM_min_5S_length))
            return new LongParameterVO(0L, 1000000L, new Long(value));
        if (key.equals(PARAM_min_16S_length))
            return new LongParameterVO(0L, 1000000L, new Long(value));
        if (key.equals(PARAM_min_18S_length))
            return new LongParameterVO(0L, 1000000L, new Long(value));
        if (key.equals(PARAM_min_23S_length))
            return new LongParameterVO(0L, 1000000L, new Long(value));
        // no match
        return null;
    }

}
