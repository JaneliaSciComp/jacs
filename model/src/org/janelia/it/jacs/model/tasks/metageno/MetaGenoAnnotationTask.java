
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 12:30:38 PM
 */
public class MetaGenoAnnotationTask extends Task {

    transient public static final String PARAM_input_node_id = "Query node id";


    public MetaGenoAnnotationTask() {
        super();
        setTaskName("MetaGenoAnnotationTask");
        setParameter(PARAM_input_node_id, "");
    }

    public MetaGenoAnnotationTask(Long queryNodeId) {
        setParameter(PARAM_input_node_id, queryNodeId.toString());
    }

    public String getDisplayName() {
        return "Metagenomic Annotation Task";
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