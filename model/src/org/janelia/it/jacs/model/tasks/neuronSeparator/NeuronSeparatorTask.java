
package org.janelia.it.jacs.model.tasks.neuronSeparator;

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
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class NeuronSeparatorTask extends Task {
    transient public static final String TASK_NAME = "neuronSeparator";
    transient public static final String DISPLAY_NAME = "Neuron Separation";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_inputTifFilePath = "input tif file path";
    transient public static final String PARAM_inputLsmFilePathList = "input lsm file path list";

    // Default values - default overrides

    public NeuronSeparatorTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public NeuronSeparatorTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputTifFilePath, "");
        setParameter(PARAM_inputLsmFilePathList, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputTifFilePath)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_inputLsmFilePathList)) {
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