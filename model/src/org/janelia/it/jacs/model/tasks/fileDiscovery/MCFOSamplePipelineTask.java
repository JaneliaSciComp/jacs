package org.janelia.it.jacs.model.tasks.fileDiscovery;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Process a single sample through the entire MCFO pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Deprecated
public class MCFOSamplePipelineTask extends Task {

    transient public static final String TASK_NAME = "mcfoSamplePipeline";
    transient public static final String DISPLAY_NAME = "MCFO Sample Pipeline";

    // Parameter Keys
    transient public static final String PARAM_sampleEntityId = "sample entity id";
    transient public static final String PARAM_refreshProcessing = "refresh processing";
    transient public static final String PARAM_refreshAlignment = "refresh alignment";
    transient public static final String PARAM_refreshSeparation = "refresh separation";

    // Default values - default overrides

    public MCFOSamplePipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String sampleEntityId, 
    		Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_sampleEntityId, sampleEntityId);
        setParameter(PARAM_refreshProcessing, refreshProcessing==null?"false":refreshProcessing.toString());
        setParameter(PARAM_refreshAlignment, refreshAlignment==null?"false":refreshAlignment.toString());
        setParameter(PARAM_refreshSeparation, refreshSeparation==null?"false":refreshSeparation.toString());
    }

    public MCFOSamplePipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }
    
    public MCFOSamplePipelineTask() {
    	super();
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_sampleEntityId, "");
        setParameter(PARAM_refreshProcessing, "false");
        setParameter(PARAM_refreshAlignment, "false");
        setParameter(PARAM_refreshSeparation, "false");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_sampleEntityId)) {
            return new TextParameterVO(value, 4000);
        }
        if (key.equals(PARAM_refreshProcessing)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        }
        if (key.equals(PARAM_refreshAlignment)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        }
        if (key.equals(PARAM_refreshSeparation)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
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
