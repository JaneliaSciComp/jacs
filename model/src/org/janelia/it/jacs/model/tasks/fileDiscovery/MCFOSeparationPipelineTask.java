package org.janelia.it.jacs.model.tasks.fileDiscovery;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Separate neurons in a single input file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFOSeparationPipelineTask extends Task {

    transient public static final String TASK_NAME = "mcfoSeparationPipeline";
    transient public static final String DISPLAY_NAME = "MCFO Separation Pipeline";

    // Parameter Keys
    transient public static final String PARAM_sampleEntityId = "sample entity id";
    transient public static final String PARAM_inputFilename = "input filename";
    transient public static final String PARAM_resultEntityName = "result entity name";
    
    // Default values - default overrides

    public MCFOSeparationPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String sampleEntityId, String inputFilename, String resultEntityName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_sampleEntityId, sampleEntityId);
        setParameter(PARAM_inputFilename, inputFilename);
        setParameter(PARAM_resultEntityName, resultEntityName);
    }

    public MCFOSeparationPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }
    
    public MCFOSeparationPipelineTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_sampleEntityId, "");
        setParameter(PARAM_inputFilename, "");
        setParameter(PARAM_resultEntityName, "Neuron Separation");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_sampleEntityId)||key.equals(PARAM_inputFilename)||key.equals(PARAM_resultEntityName)) {
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
