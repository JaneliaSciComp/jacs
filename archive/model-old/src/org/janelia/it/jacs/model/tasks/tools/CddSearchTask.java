
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 28, 2010
 * Time: 12:16:04 PM
 */
public class CddSearchTask extends Task {

    /*
        This task uses rpsblast to search CDD for the sequence(s) in a given fasta file.
        rpsblast is described at http://www.ncbi.nlm.nih.gov/Structure/cdd/cdd_help.shtml#RPSBWhat

        Loosely based PriamTask.java
                       
     */

    transient public static final String rpsblast_options_DEFAULT = "";
    transient public static final String max_eval_DEFAULT = "1e-10";

    transient public static final String PARAM_input_fasta_node_id = "input fasta node";
    transient public static final String PARAM_rpsblast_options = "rps blast options";
    transient public static final String PARAM_max_eval = "max evalue";

    public CddSearchTask() {
        super();
        setTaskName("CddSearchTask");
        setParameter(PARAM_input_fasta_node_id, "");
        setParameter(PARAM_rpsblast_options, rpsblast_options_DEFAULT);
        setParameter(PARAM_max_eval, max_eval_DEFAULT);
    }

    public String getDisplayName() {
        return "CddSearchTask";
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