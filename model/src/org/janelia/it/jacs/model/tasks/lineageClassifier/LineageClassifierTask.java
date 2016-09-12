
package org.janelia.it.jacs.model.tasks.lineageClassifier;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * Time: 3:03:13 PM
 */
public class LineageClassifierTask extends Task {
    transient public static final String TASK_NAME = "LineageClassifier";
    transient public static final String DISPLAY_NAME = "Lineage Classifier";

    public static final String PARAM_SAMPLE = "sample id list";

    // Default values - default overrides

    public LineageClassifierTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public LineageClassifierTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // No match
        return null;
    }

    public String getSampleIdList() {
        return getParameter(PARAM_SAMPLE);
    }

    public void setSampleIdList(String sampleIdList) {
        this.setParameter(PARAM_SAMPLE, sampleIdList);
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}