
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
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class CopyFileTask extends Task {

    public static final String DISPLAY_NAME = "Copy Directory Task";
    public static final String PARAM_SOURCE = "source";
    public static final String PARAM_DESTINATION = "destination";

    public CopyFileTask() {
        super();
    }

    public CopyFileTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                        String sourcePath, String destinationPath) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE, sourcePath);
        setParameter(PARAM_DESTINATION, destinationPath);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE) || key.equals(PARAM_DESTINATION)) {
            return new TextParameterVO(value, 400);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getPathToOriginal() {
        return getParameter(PARAM_SOURCE);
    }

    public String getPathToDestination() {
        return getParameter(PARAM_DESTINATION);
    }
}