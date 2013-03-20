package org.janelia.it.jacs.model.tasks.maskSearch;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
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
public class MaskSearchTask extends SearchTask {
    transient public static final String TASK_NAME      = "maskSearch";
    transient public static final String DISPLAY_NAME   = "Mask Search";
    transient public static final String PARAM_inputFilePath    = "inputFilePath";
    transient public static final String PARAM_resultsFolderName    = "resultsFolderName";

    public MaskSearchTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                          String inputFilePath, String resultsFolderName) {
        super(inputNodes, owner, events, taskParameterSet, DISPLAY_NAME + " Task");
        setParameter(PARAM_inputFilePath, inputFilePath);
        setParameter(PARAM_resultsFolderName, resultsFolderName);
    }

    public MaskSearchTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (key.equals(PARAM_resultsFolderName) || key.equals(PARAM_inputFilePath)) {
            return new TextParameterVO(value);
        }
        if (value == null)
            return null;
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
