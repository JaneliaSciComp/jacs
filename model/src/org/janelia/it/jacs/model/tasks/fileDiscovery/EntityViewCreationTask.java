package org.janelia.it.jacs.model.tasks.fileDiscovery;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Create a view from a source entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityViewCreationTask extends Task {

    transient public static final String TASK_NAME = "viewCreation";
    transient public static final String DISPLAY_NAME = "View Creation";
    transient public static final String PARAM_sourceEntityId = "source entity id";
    transient public static final String PARAM_targetEntityName = "target entity name";
    
    public EntityViewCreationTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String sourceEntityId, String targetEntityName) {
    	super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_sourceEntityId, sourceEntityId);
        setParameter(PARAM_targetEntityName, targetEntityName);
    }

    public EntityViewCreationTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_sourceEntityId, "");
        setParameter(PARAM_targetEntityName, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        String value = getParameter(key);
        if (key.equals(PARAM_sourceEntityId) || key.equals(PARAM_targetEntityName)) {
            return new TextParameterVO(value);
        }
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
