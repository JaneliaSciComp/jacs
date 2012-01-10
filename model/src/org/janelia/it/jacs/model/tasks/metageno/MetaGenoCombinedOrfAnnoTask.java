
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 20, 2009
 * Time: 12:08:37 PM
 */

public class MetaGenoCombinedOrfAnnoTask extends Task {

    transient public static final String PARAM_input_node_id = "Query node id";
    transient public static final String PARAM_useClearRange = "Use clear range";

    transient public static final Boolean useClearRange_DEFAULT = Boolean.FALSE;


    public MetaGenoCombinedOrfAnnoTask() {
        super();
        setTaskName("MetaGenoCombinedOrfAnnoTask");
        setParameter(PARAM_input_node_id, "");
        setParameter(PARAM_useClearRange, useClearRange_DEFAULT.toString());
    }

    public MetaGenoCombinedOrfAnnoTask(Long queryNodeId, Boolean useClearRange) {
        setParameter(PARAM_input_node_id, queryNodeId.toString());
        setParameter(PARAM_useClearRange, useClearRange.toString());
    }

    public String getDisplayName() {
        return "Metagenomic Combined Orf-Caller and Annotation Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (key.equals(PARAM_useClearRange))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (value == null)
            return null;
        // no match
        return null;
    }

}
