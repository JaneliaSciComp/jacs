package org.janelia.it.jacs.model.tasks.mip;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

/**
 * Task for mapping a stack of MIPs to a 3D image.
 */
@XmlRootElement(name = "mapMIPStack")
@XmlAccessorType(XmlAccessType.NONE)
public class MIP3dMapTask extends Task {
    transient public static final String TASK_NAME = "map2dTo3d";
    transient public static final String DISPLAY_NAME = "Map 2d stack to 3d";

    // Parameter Keys
    transient public static final String PARAM_inputDirectory = "directory containing the MIPs";

    public MIP3dMapTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MIP3dMapTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputDirectory, "");
        setTaskName(TASK_NAME);
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputDirectory)) {
            return new TextParameterVO(value, 400);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @XmlElement(name = "inputDirectory")
    public String getInputDirectory() {
        return getParameter(PARAM_inputDirectory);
    }

    public void setInputDirectory(String inputDirectory) {
        setParameter(PARAM_inputDirectory, inputDirectory);
    }

}
