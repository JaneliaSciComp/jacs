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
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Demarcate a process for updating the known set of common Large Volume
 * Sample entities.
 *
 * @author fosterl
 */
public class LargeVolumeDiscoveryTask extends Task {

    transient public static final String PROCESS_NAME = "LargeVolumeSampleDiscovery";
    transient public static final String DEFAULT_TASK_NAME = "lvsdTask";
    transient public static final String DISPLAY_NAME = "Large Volume Sample Discovery Task";
    
    // Parameter Keys
    // NONE: placeholder comment.
    
    public LargeVolumeDiscoveryTask() {
        super();
        setDefaultValues();
    }
    
    public LargeVolumeDiscoveryTask(Set<Node> inputNodes, String owner, List<Event> events,
                          Set<TaskParameter> taskParameterSet, String taskName, String displayName) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setTaskName(taskName);
        setJobName(displayName);
    }
    
    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        // Nothing to do here.
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
