/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.tasks.tiledMicroscope;

import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Demarcate a process for updating neurons to ProtoBuf serialization.
 *
 * @author fosterl
 */
public class TmNeuronPBUpdateTask extends Task {

    transient public static final String PROCESS_NAME = "TmNeuronPB_Update";
    transient public static final String DEFAULT_TASK_NAME = "tmNeuronPBUpdateTask";
    transient public static final String DISPLAY_NAME = "Tiled Microscope Neuron PB Update Task";
    
    // Parameter Keys
    transient public static final String PARAM_workspaceId = "workspace id";
    
    public TmNeuronPBUpdateTask() {
        super();
        setDefaultValues();
    }
    
    public TmNeuronPBUpdateTask(Set<Node> inputNodes, String owner, List<Event> events,
                          Set<TaskParameter> taskParameterSet, String taskName, String displayName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setTaskName(taskName);
        setJobName(displayName);
    }
    
    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null) {
            return null;
        }
        if (key.equals(PARAM_workspaceId)) {
            return new LongParameterVO(Long.parseLong(value));
        }
        // No match
        return null;
    }
    
    @Override
    public String getDisplayName() {
        return super.getJobName();
    }

    private void setDefaultValues() {
        setTaskName(DEFAULT_TASK_NAME);
        setJobName(DISPLAY_NAME);
    }
}
