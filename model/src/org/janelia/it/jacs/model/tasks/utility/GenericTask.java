package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * A generic task that can be used for anything. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GenericTask extends Task {

    transient public static final String DEFAULT_TASK_NAME = "genericTask";
    transient public static final String DISPLAY_NAME = "Generic Task";
    
    private String displayName;
    
    public GenericTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String taskName, String displayName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        this.taskName = taskName;
        this.displayName = displayName;
        setJobName(displayName+" Task");
    }

    public GenericTask() {
    	super();
        setDefaultValues();
    }

    private void setDefaultValues() {
    	this.taskName = DEFAULT_TASK_NAME;
    	this.displayName = DISPLAY_NAME;
    }

    public String getDisplayName() {
        return displayName;
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
