package org.janelia.it.jacs.model.tasks.fileDiscovery;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 8/29/11
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class MCFOStitchedFileDiscoveryTask extends Task {

    transient public static final String TASK_NAME = "mcfoStitchedFileDiscovery";
    transient public static final String DISPLAY_NAME = "MultiColor FlipOut Stitched File Discovery";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_inputDirectoryList = "list of input directories";
    transient public static final String PARAM_refresh = "regenerate results";

    // Default values - default overrides

    public MCFOStitchedFileDiscoveryTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, boolean refresh) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_refresh, Boolean.toString(refresh));
    }

    public MCFOStitchedFileDiscoveryTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputDirectoryList, "");
        setParameter(PARAM_refresh, "true");
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
        if (key.equals(PARAM_refresh)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
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
