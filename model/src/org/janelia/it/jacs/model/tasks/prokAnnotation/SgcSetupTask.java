
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
 * Time: 2:32:56 PM
 */
public class SgcSetupTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "sgcSetupTask";
    transient public static final String DISPLAY_NAME = "SGC Setup";

    // Parameters
    // annotation toggle, set stage to pre-annotation and run_sgc toggle to 1 in common..genomes.
    transient public static final String PARAM_TOGGLE_ANNOTATION = "toggleAnnotation";

    public SgcSetupTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public SgcSetupTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        setParameter(PARAM_TOGGLE_ANNOTATION, "false");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_TOGGLE_ANNOTATION)) {
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