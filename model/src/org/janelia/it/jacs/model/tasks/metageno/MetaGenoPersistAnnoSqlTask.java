
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 3, 2009
 * Time: 3:19:20 PM
 */
public class MetaGenoPersistAnnoSqlTask extends Task {
    transient public static final String PARAM_input_anno_result_node_id = "input annotation result node";

    public MetaGenoPersistAnnoSqlTask() {
        super();
        setTaskName("MetaGenoPersistAnnoSqlTask");
        setParameter(PARAM_input_anno_result_node_id, "");
    }

    public String getDisplayName() {
        return "MetaGenoPersistAnnoSqlTask";
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
