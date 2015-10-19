package org.janelia.it.jacs.model.tasks.zlatic;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * Modified by naxelrod
 */
public class ZlaticLabTask extends Task {
    transient public static final String TASK_NAME = "zlaticLab";
    transient public static final String DISPLAY_NAME = "Zlatic Lab";

    // Parameter Keys
    transient public static final String PARAM_inputFile = "input files";
    transient public static final String PARAM_inputFileType = "inputFileType";


    public ZlaticLabTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public ZlaticLabTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputFile, "");
        setParameter(PARAM_inputFileType, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputFile)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}