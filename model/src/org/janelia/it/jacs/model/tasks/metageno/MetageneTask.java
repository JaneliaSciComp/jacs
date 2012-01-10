
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 6, 2009
 * Time: 11:36:43 AM
 */

public class MetageneTask extends Task {
    transient public static final String PARAM_input_fasta_node_id = "input fasta node";

    public MetageneTask() {
        super();
        setTaskName("MetageneTask");
        setParameter(PARAM_input_fasta_node_id, "");
    }

    public String getDisplayName() {
        return "MetageneTask";
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
