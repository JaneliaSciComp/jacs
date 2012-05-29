package org.janelia.it.jacs.model.tasks.tic;

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
 * Modified by naxelrod
 */
public class SingleTicTask extends TicTask {
    transient public static final String TASK_NAME = "singleTIC";
    transient protected String DISPLAY_NAME = "Transcription Imaging Consortium";

    public SingleTicTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
    }

    public SingleTicTask() {
        super();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}