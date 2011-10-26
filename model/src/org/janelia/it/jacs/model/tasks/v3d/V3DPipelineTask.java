package org.janelia.it.jacs.model.tasks.v3d;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

public class V3DPipelineTask extends Task {
    transient public static final String TASK_NAME      = "v3dPipelineTask";
    transient public static final String DISPLAY_NAME   = "V3D Task";

    // Parameter Keys
    transient public static final String PARAM_RUN_MERGE    = "Run V3D Merge";
    transient public static final String PARAM_RUN_STITCH   = "Run V3D Stitch";
    transient public static final String PARAM_RUN_BLEND    = "Run V3D Blend";
    // The value of this can be a directory or a comma-separated list of paired files
    transient public static final String PARAM_INPUT_FILE_PATHS    = "V3D Input File Paths";


    public V3DPipelineTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                           boolean runMergeService, boolean runStitchService, boolean runBlendService, String inputFilePaths) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_RUN_MERGE, Boolean.toString(runMergeService));
        setParameter(PARAM_RUN_STITCH, Boolean.toString(runStitchService));
        setParameter(PARAM_RUN_BLEND, Boolean.toString(runBlendService));
        setParameter(PARAM_INPUT_FILE_PATHS, inputFilePaths);
    }

    protected void setDefaultValues() {
        this.taskName = TASK_NAME;
    }

    public V3DPipelineTask() {
        super();
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_INPUT_FILE_PATHS)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_RUN_MERGE)||key.equals(PARAM_RUN_STITCH)||key.equals(PARAM_RUN_BLEND)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}