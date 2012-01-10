
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 3, 2009
 * Time: 3:19:35 PM
 */
public class MetaGenoPersistOrfSqlTask extends Task {
    transient public static final String PARAM_input_orf_result_node_id = "input orf result node";

    public MetaGenoPersistOrfSqlTask() {
        super();
        setTaskName("MetaGenoPersistOrfSqlTask");
        setParameter(PARAM_input_orf_result_node_id, "");
    }

    public String getDisplayName() {
        return "MetaGenoPersistOrfSqlTask";
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
