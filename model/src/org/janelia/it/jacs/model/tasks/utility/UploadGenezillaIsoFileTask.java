
package org.janelia.it.jacs.model.tasks.utility;

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
 * User: jinman
 * Date: Jun 29, 2010
 * Time: 10:00:33 AM
 */
public class UploadGenezillaIsoFileTask extends Task {

    public static final String DISPLAY_NAME = "Upload GenezillaIso File Task";
    public static final String PARAM_SOURCE_FILE = "sourceFile";

    public UploadGenezillaIsoFileTask() {
        super();
        commonConstructor();
    }

    public UploadGenezillaIsoFileTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                      String pathToSourceFile) {
        super(inputNodes, owner, events, taskParameterSet);
        commonConstructor();
        setParameter(PARAM_SOURCE_FILE, pathToSourceFile);
    }

    protected void commonConstructor() {
        this.taskName = DISPLAY_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE_FILE)) {
            return new TextParameterVO(value, 400);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getPathToSourceFile() {
        return getParameter(PARAM_SOURCE_FILE);
    }

}