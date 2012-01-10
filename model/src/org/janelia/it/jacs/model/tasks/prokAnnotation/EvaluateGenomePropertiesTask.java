
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 4, 2010
 * Time: 3:32:08 PM
 */
public class EvaluateGenomePropertiesTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "evaluateGenomePropertiesTask";
    transient public static final String DISPLAY_NAME = "Evaluate Genome Properties";

    public EvaluateGenomePropertiesTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public EvaluateGenomePropertiesTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
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
