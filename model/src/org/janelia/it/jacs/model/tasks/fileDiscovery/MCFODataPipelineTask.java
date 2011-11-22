package org.janelia.it.jacs.model.tasks.fileDiscovery;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Discover files in a set of input directories and create corresponding entities in the database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFODataPipelineTask extends FileDiscoveryTask {

    transient public static final String TASK_NAME = "mcfoDataPipeline";
    transient public static final String DISPLAY_NAME = "MCFO Data Pipeline";
    transient public static final String PARAM_sampleViewName = "sample view name";
    
    public MCFODataPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String inputDirList, String topLevelFolderName, String sampleViewName, boolean refresh) {
        super(inputNodes, owner, events, taskParameterSet, inputDirList, topLevelFolderName, refresh);
        setDefaultValues();
        setParameter(PARAM_sampleViewName, sampleViewName);
    }

    public MCFODataPipelineTask() {
    	super();
        setDefaultValues();
        this.taskName = TASK_NAME;
    }

    private void setDefaultValues() {
        setParameter(PARAM_refresh, "true");
        setParameter(PARAM_sampleViewName, "");
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        String value = getParameter(key);
        if (key.equals(PARAM_refresh) || key.equals(PARAM_sampleViewName)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        }
        return super.getParameterVO(key);
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
