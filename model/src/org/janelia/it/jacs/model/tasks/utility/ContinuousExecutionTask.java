package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class ContinuousExecutionTask extends Task {

    public static final String DISPLAY_NAME = "Continuous Execution Task";
    public static final String PARAM_ENABLED_STATE  = "enabled state";
    public static final String PARAM_LOOP_TIMER_MINS     = "loop timer";  // Loop timer in minutes
    public static final String PARAM_SUBTASK_ID     = "subtask id";  // used as the primer
    public static final String PARAM_SUBTASK_PROCESS= "subtask process";
    // How many seconds between checking for completion
    public static final String PARAM_SUBTASK_STATUS_TIMER_SECS = "subtask status timer interval";

    public ContinuousExecutionTask() {
        super();
    }

    public ContinuousExecutionTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                   int loopTimerInMinutes, boolean isEnabled, long subtaskId, String subtaskProcess,
                                   int statusCheckDelayInSeconds) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_ENABLED_STATE, Boolean.toString(isEnabled));
        setParameter(PARAM_LOOP_TIMER_MINS, Integer.toString(loopTimerInMinutes));
        setParameter(PARAM_SUBTASK_ID, Long.toString(subtaskId));
        setParameter(PARAM_SUBTASK_PROCESS, subtaskProcess);
        setParameter(PARAM_SUBTASK_STATUS_TIMER_SECS, Integer.toString(statusCheckDelayInSeconds));
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_LOOP_TIMER_MINS)||key.equals(PARAM_SUBTASK_ID)||key.equals(PARAM_SUBTASK_STATUS_TIMER_SECS)) {
            return new LongParameterVO(Long.valueOf(value));
        }
        else if (key.equals(PARAM_ENABLED_STATE)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        else if (key.equals(PARAM_SUBTASK_PROCESS)) {
            return new TextParameterVO(value);
        }

        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isStillEnabled() {
        return Boolean.valueOf(getParameter(ContinuousExecutionTask.PARAM_ENABLED_STATE));
    }
    
    public void setEnabled(boolean isEnabled) {
        setParameter(ContinuousExecutionTask.PARAM_ENABLED_STATE, Boolean.toString(isEnabled));
    }
}