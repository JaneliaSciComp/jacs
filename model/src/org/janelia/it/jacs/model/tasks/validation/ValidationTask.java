package org.janelia.it.jacs.model.tasks.validation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Create a task like this, to demarcate a process as a validation run..
 * 
 * @author <a href="mailto:fosterl@janelia.hhmi.org">Les Foster</a>
 */
public class ValidationTask extends Task {

    transient public static final String DEFAULT_TASK_NAME = "validationTask";
    transient public static final String DISPLAY_NAME = "Validation Task";

    public ValidationTask(Set<Node> inputNodes, String owner, List<Event> events,
                          Set<TaskParameter> taskParameterSet, String taskName, String displayName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setTaskName(taskName);
        setJobName(displayName);
    }

    public ValidationTask() {
    	super();
        setDefaultValues();
    }

    private void setDefaultValues() {
    	setTaskName(DEFAULT_TASK_NAME);
    	setJobName(DISPLAY_NAME);
    }

    public String getDisplayName() {
        return getJobName();
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
    
    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
