
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 5, 2009
 * Time: 1:58:13 PM
 */
public class PriamTask extends Task {

    transient public static final String rpsblast_options_DEFAULT = "";
    transient public static final String max_eval_DEFAULT = "-10";

    transient public static final String PARAM_input_fasta_node_id = "input fasta node";
    transient public static final String PARAM_rpsblast_options = "rps blast options";
    transient public static final String PARAM_max_eval = "max evalue";

    public PriamTask() {
        super();
        setTaskName("PriamTask");
        setParameter(PARAM_input_fasta_node_id, "");
        setParameter(PARAM_rpsblast_options, rpsblast_options_DEFAULT);
        setParameter(PARAM_max_eval, max_eval_DEFAULT);
    }

    public String getDisplayName() {
        return "PriamTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_rpsblast_options))
            return new TextParameterVO(value);
        if (key.equals(PARAM_max_eval))
            return new LongParameterVO(new Long("-100"), new Long("3"), new Long(value));
        // no match
        return null;
    }

}
