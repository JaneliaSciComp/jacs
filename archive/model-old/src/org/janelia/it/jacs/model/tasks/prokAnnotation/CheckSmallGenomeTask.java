
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 9, 2010
 * Time: 9:23:10 AM
 */
public class CheckSmallGenomeTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "checkSmallGenomeTask";
    transient public static final String DISPLAY_NAME = "Check Small Genome";
    transient public static final String STAGE_LOAD = "load";
    transient public static final String STAGE_GIP = "gip";
    transient public static final String STAGE_SGC = "sgc";

    // Parameters
    transient public static final String PARAM_STAGE = "checkStage";
    transient public static final String PARAM_ANNOTATION_MODE = "annotationMode";

    public CheckSmallGenomeTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public CheckSmallGenomeTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        this.taskName = TASK_NAME;
        setParameter(PARAM_STAGE, null);
        setParameter(PARAM_ANNOTATION_MODE, null);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_STAGE) || key.equals(PARAM_ANNOTATION_MODE)) {
            return new TextParameterVO(value);
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
