
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 4, 2010
 * Time: 4:54:37 PM
 */
public abstract class ProkPipelineBaseTask extends Task {
    transient public static final String PARAM_DB_USERNAME = "dbUsername";
    transient public static final String PARAM_DB_PASSWORD = "dbPassword";
    transient public static final String PARAM_DB_NAME = "dbName";
    transient public static final String PARAM_DIRECTORY = "directory";

    public ProkPipelineBaseTask() {
        super();
    }

    protected ProkPipelineBaseTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
    }

    protected void setDefaultValues() {
        setParameter(PARAM_DB_NAME, null);
        setParameter(PARAM_DB_USERNAME, null);
        setParameter(PARAM_DB_PASSWORD, null);
        setParameter(PARAM_DIRECTORY, null);
    }

}
