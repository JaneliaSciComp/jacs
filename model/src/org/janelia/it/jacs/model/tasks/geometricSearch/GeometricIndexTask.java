/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.tasks.geometricSearch;

import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 *
 * @author murphys
 */
public class GeometricIndexTask extends Task {
    public static final String DISPLAY_NAME = "GeometricIndex";
    
    public GeometricIndexTask() {
        super();
    }
    
    public GeometricIndexTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        return null;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
    
    @Override
    public String getTaskName() {
        return "GeometricIndexTask";
    }
    
}
