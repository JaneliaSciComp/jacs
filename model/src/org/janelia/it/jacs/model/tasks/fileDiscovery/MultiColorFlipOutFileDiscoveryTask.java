package org.janelia.it.jacs.model.tasks.fileDiscovery;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:27 PM
 */
public class MultiColorFlipOutFileDiscoveryTask extends Task {

    transient public static final String TASK_NAME = "multiColorFlipOutFileDiscovery";
    transient public static final String DISPLAY_NAME = "MultiColor FlipOut File Discovery";

    // Parameter Keys
    transient public static final String PARAM_inputDirectoryList = "list of input directories";
    transient public static final String PARAM_topLevelFolderName = "top level folder name";
    transient public static final String PARAM_linkingDirectoryName = "linking folder name";
    transient public static final String PARAM_refresh = "regenerate results";

    // Default values - default overrides

    public MultiColorFlipOutFileDiscoveryTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String inputDirList, String topLevelFolderName, 
    		String linkingDirName, boolean refresh) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_inputDirectoryList, inputDirList);
        setParameter(PARAM_topLevelFolderName, topLevelFolderName);
        setParameter(PARAM_linkingDirectoryName, linkingDirName);
        setParameter(PARAM_refresh, Boolean.toString(refresh));
    }

    public MultiColorFlipOutFileDiscoveryTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputDirectoryList, "");
        setParameter(PARAM_topLevelFolderName, "");
        setParameter(PARAM_linkingDirectoryName, "");
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
        if (key.equals(PARAM_topLevelFolderName)) {
            return new TextParameterVO(value, 4000);
        }
        if (key.equals(PARAM_linkingDirectoryName)) {
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
