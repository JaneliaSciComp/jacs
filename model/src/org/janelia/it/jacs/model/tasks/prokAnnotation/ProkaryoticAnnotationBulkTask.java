
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class ProkaryoticAnnotationBulkTask extends ProkaryoticAnnotationTask {
    transient public static final String TASK_NAME = "prokAnnotationBulkTask";
    transient public static final String DISPLAY_NAME = "Prokaryotic Annotation Bulk Pipeline";
    transient public static final String PARAM_genomeListFile = "genomeListFile";

    public ProkaryoticAnnotationBulkTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public ProkaryoticAnnotationBulkTask() {
        super();
        setDefaultValues();
    }

}