package org.janelia.it.jacs.model.tasks.neuron;

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
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 7/6/12
 * Time: 3:26 PM
 */
public class NeuronMergeTask extends Task {
    transient public static final String TASK_NAME = "neuronMerge";
    transient public static final String DISPLAY_NAME = "Neuron Merge";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_separationEntityId = "separationEntityId";
    transient public static final String PARAM_commaSeparatedNeuronFragmentList = "neuronFragments";


    // Default values - default overrides

    public NeuronMergeTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public NeuronMergeTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_separationEntityId, "");
        setParameter(PARAM_commaSeparatedNeuronFragmentList, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_separationEntityId)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_commaSeparatedNeuronFragmentList)) {
            return new TextParameterVO(value, 1000);
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
