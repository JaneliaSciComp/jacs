package org.janelia.it.jacs.model.tasks.fileDiscovery;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Discover files in a set of input directories and create corresponding entities in the database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFODataPipelineTask extends FileDiscoveryTask {

    transient public static final String TASK_NAME = "mcfoDataPipeline";
    transient public static final String DISPLAY_NAME = "MCFO Data Pipeline";
    
    public MCFODataPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String inputDirList, String topLevelFolderName, Boolean refresh) {
        super(inputNodes, owner, events, taskParameterSet, inputDirList, topLevelFolderName, refresh);
        setDefaultValues();
    }

    public MCFODataPipelineTask() {
    	super();
        setDefaultValues();
        this.taskName = TASK_NAME;
    }

    private void setDefaultValues() {
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
