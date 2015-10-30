
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 4, 2010
 * Time: 1:49:26 PM
 */
public class OverlapAnalysisTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "overlapAnalysisTask";
    transient public static final String DISPLAY_NAME = "Overlap Analysis";

    // Parameters
    // Record if the data processed is Non-TIGR
    transient public static final String PARAM_IS_NT_DATA = "isNTData";

    public OverlapAnalysisTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public OverlapAnalysisTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        this.taskName = TASK_NAME;
        setParameter(PARAM_IS_NT_DATA, null);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_IS_NT_DATA)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}