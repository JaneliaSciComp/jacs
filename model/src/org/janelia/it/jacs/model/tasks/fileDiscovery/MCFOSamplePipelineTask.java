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
 * Process a single sample through the entire MCFO pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFOSamplePipelineTask extends Task {

    transient public static final String TASK_NAME = "mcfoSamplePipeline";
    transient public static final String DISPLAY_NAME = "MCFO Sample Pipeline";

    // Parameter Keys
    transient public static final String PARAM_sampleEntityId = "sample entity id";

    // Default values - default overrides

    public MCFOSamplePipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String sampleEntityId) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_sampleEntityId, sampleEntityId);
    }

    public MCFOSamplePipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }
    
    public MCFOSamplePipelineTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_sampleEntityId, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_sampleEntityId)) {
            return new TextParameterVO(value, 4000);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
