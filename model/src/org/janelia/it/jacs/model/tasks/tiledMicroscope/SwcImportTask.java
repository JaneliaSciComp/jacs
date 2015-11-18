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
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Demarcate a process as a SWC import.
 *
 * @author fosterl
 */
public class SwcImportTask extends Task {

    transient public static final String PROCESS_NAME = "SWC_Import";
    transient public static final String DEFAULT_TASK_NAME = "swcImportTask";
    transient public static final String DISPLAY_NAME = "SWC Import Task";
    
    // Parameter Keys
    transient public static final String PARAM_topLevelFolderName = "top level folder name";
    transient public static final String PARAM_sampleId = "sample id";
    transient public static final String PARAM_userName = "user name";
    
    public SwcImportTask() {
        super();
        setDefaultValues();
    }
    
    public SwcImportTask(Set<Node> inputNodes, String owner, List<Event> events,
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
        if (key.equals(PARAM_topLevelFolderName)) {
            return new TextParameterVO(value, 4000);
        }
        if (key.equals(PARAM_sampleId)) {
            return new LongParameterVO(Long.parseLong(value));
        }
        if (key.equals(PARAM_userName)) {
            return new TextParameterVO(value, 100);
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
