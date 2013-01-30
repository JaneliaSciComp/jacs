package org.janelia.it.jacs.model.tasks.fileDiscovery;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 2/16/12
 * Time: 9:37 AM
 */
public class FileTreeLoaderPipelineTask extends Task {

    transient public static final String TASK_NAME = "fileTreeLoaderPipeline";
    transient public static final String DISPLAY_NAME = "File Tree Loader Pipeline";

    // Parameter Keys
    transient public static final String PARAM_rootDirectoryPath = "root directory path";
    transient public static final String PARAM_topLevelFolderName = "top level folder name";

    // Default values - default overrides
    public FileTreeLoaderPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
                                      Set<TaskParameter> taskParameterSet, String rootDirectoryPath, String topLevelFolderName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_rootDirectoryPath, rootDirectoryPath);
        setParameter(PARAM_topLevelFolderName, topLevelFolderName);
    }

    public FileTreeLoaderPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
                                      Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public FileTreeLoaderPipelineTask() {
        super();
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_rootDirectoryPath, "");
        setParameter(PARAM_topLevelFolderName, getDefaultTopLevelFolderName());
        this.taskName = TASK_NAME;
    }

    private String getDefaultTopLevelFolderName() {
        String topLevelFolderName=getParameter(PARAM_topLevelFolderName);
        if (topLevelFolderName!=null) {
            return topLevelFolderName;
        } else {
            return "FileTreeLoaderPipelineTask "+new Date().toString();
        }
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_rootDirectoryPath) || key.equals(PARAM_topLevelFolderName)) {
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
