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
@Deprecated
public class MCFODataPipelineTask extends FileDiscoveryTask {

    transient public static final String TASK_NAME = "mcfoDataPipeline";
    transient public static final String DISPLAY_NAME = "MCFO Data Pipeline";

    // Parameter Keys
    transient public static final String PARAM_refreshProcessing = "refresh processing";
    transient public static final String PARAM_refreshAlignment = "refresh alignment";
    transient public static final String PARAM_refreshSeparation = "refresh separation";
    transient public static final String PARAM_sageFamily = "Sage image family";


    public MCFODataPipelineTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String inputDirList, String topLevelFolderName, 
    		Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation, String sageImageFamily) {
        super(inputNodes, owner, events, taskParameterSet, inputDirList, topLevelFolderName, false);
        setDefaultValues();
        setParameter(PARAM_refreshProcessing, refreshProcessing==null?"false":refreshProcessing.toString());
        setParameter(PARAM_refreshAlignment, refreshAlignment==null?"false":refreshAlignment.toString());
        setParameter(PARAM_refreshSeparation, refreshSeparation==null?"false":refreshSeparation.toString());
        setParameter(PARAM_sageFamily, sageImageFamily);
    }

    public MCFODataPipelineTask() {
    	super();
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_refreshProcessing, "false");
        setParameter(PARAM_refreshAlignment, "false");
        setParameter(PARAM_refreshSeparation, "false");
        setParameter(PARAM_sageFamily,"");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
    	ParameterVO sp = super.getParameterVO(key);
    	if (sp!=null) {
            return sp;
    	}
        String value = getParameter(key);
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
