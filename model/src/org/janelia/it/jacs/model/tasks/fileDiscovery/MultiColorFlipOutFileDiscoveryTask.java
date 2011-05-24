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
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiColorFlipOutFileDiscoveryTask extends Task {

    transient public static final String TASK_NAME = "multiColorFlipOutFileDiscovery";
    transient public static final String DISPLAY_NAME = "MultiColor FlipOut File Discovery";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_inputDirectoryList = "list of input directories";

    // Default values - default overrides

    public MultiColorFlipOutFileDiscoveryTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MultiColorFlipOutFileDiscoveryTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputDirectoryList, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputDirectoryList)) {
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
