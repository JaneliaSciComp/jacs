
package org.janelia.it.jacs.model.tasks.sift;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;


/**
 * Created by IntelliJ IDEA.
 * User: zguan
 * Date: Jul 20, 2010
 * Time: 4:05:05 PM
 */
public class SiftProteinSubstitutionTask extends Task {

    transient public static final String PARAM_substitution_string = "substitution string";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    public SiftProteinSubstitutionTask() {
        super();
        setTaskName("SiftProteinSubstitutionTask");
        setParameter(PARAM_substitution_string, "");
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "SiftProteinSubstitutionTask";
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

}
