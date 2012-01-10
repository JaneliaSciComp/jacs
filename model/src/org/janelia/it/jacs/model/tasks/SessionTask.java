
package org.janelia.it.jacs.model.tasks;

import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 21, 2009
 * Time: 2:42:40 PM
 */
public class SessionTask extends Task {

    public static final String TASK_NAME = "sessionTask";

    public SessionTask() {
    }

    public SessionTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = TASK_NAME;
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Session Task";
    }
}
