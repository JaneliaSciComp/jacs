package org.janelia.it.jacs.model.tasks.fileDiscovery;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
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
    transient public static final String PARAM_filesUploadedFlag = "files uploaded";
    transient public static final String PARAM_topLevelFolderId = "top level folder id";

    public FileTreeLoaderPipelineTask(Set<Node> inputNodes,
                                      String owner,
                                      List<Event> events,
                                      Set<TaskParameter> taskParameterSet,
                                      String rootDirectoryPath,
                                      String topLevelFolderName,
                                      boolean filesUploadedFlag,
                                      Long topLevelFolderId) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = TASK_NAME;
        setParameter(PARAM_rootDirectoryPath, rootDirectoryPath);
        setParameter(PARAM_topLevelFolderName, topLevelFolderName);
        setParameter(PARAM_filesUploadedFlag, String.valueOf(filesUploadedFlag));
        setParameter(PARAM_topLevelFolderId, String.valueOf(topLevelFolderId));
    }

    public FileTreeLoaderPipelineTask(Set<Node> inputNodes,
                                      String owner,
                                      List<Event> events,
                                      Set<TaskParameter> taskParameterSet,
                                      String rootDirectoryPath,
                                      String topLevelFolderName) {
        this(inputNodes, owner, events, taskParameterSet, rootDirectoryPath, topLevelFolderName, false, null);
    }

    // no-arg constructor required by pipeline framework
    @SuppressWarnings("UnusedDeclaration")
    public FileTreeLoaderPipelineTask() {
        super();
        setDefaultValues();
    }

    private void setDefaultValues() {
        this.taskName = TASK_NAME;
        setParameter(PARAM_rootDirectoryPath, "");
        setParameter(PARAM_topLevelFolderName, getDefaultTopLevelFolderName());
        setParameter(PARAM_filesUploadedFlag, String.valueOf(false));
        setParameter(PARAM_topLevelFolderId, "null");
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
        ParameterVO valueObject = null;
        final String value = getParameter(key);
        if (value != null) {
            if (PARAM_rootDirectoryPath.equals(key) ||
                PARAM_topLevelFolderName.equals(key) ||
                PARAM_topLevelFolderId.equals(key)) {
                valueObject = new TextParameterVO(value, 4000);
            } else if (PARAM_filesUploadedFlag.equals(key)) {
                valueObject = new BooleanParameterVO(Boolean.parseBoolean(value));
            }
        }
        return valueObject;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
