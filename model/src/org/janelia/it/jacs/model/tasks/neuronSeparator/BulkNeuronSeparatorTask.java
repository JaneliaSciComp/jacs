
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
public class BulkNeuronSeparatorTask extends Task {
    transient public static final String TASK_NAME = "bulkNeuronSeparator";
    transient public static final String DISPLAY_NAME = "Bulk Neuron Separation";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_inputDirectoryList = "list of input directories";
    transient public static final String PARAM_topLevelFolderName = "top level folder name";

    // Default values - default overrides

    public BulkNeuronSeparatorTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public BulkNeuronSeparatorTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputDirectoryList, "");
        setParameter(PARAM_topLevelFolderName, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputDirectoryList)||key.equals(PARAM_topLevelFolderName)) {
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