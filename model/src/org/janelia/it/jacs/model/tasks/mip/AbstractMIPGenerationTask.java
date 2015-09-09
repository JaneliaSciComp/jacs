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
import java.util.List;
import java.util.Set;

/**
 * Created by goinac on 9/9/15.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractMIPGenerationTask extends Task {
    transient public static final String DISPLAY_NAME = "MIP Generator";

    // Parameter Keys
    transient public static final String PARAM_inputTifFilePath = "input tif file path";
    transient public static final String PARAM_inputLsmFilePathList = "input lsm file path list";

    public AbstractMIPGenerationTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public AbstractMIPGenerationTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputTifFilePath, "");
        setParameter(PARAM_inputLsmFilePathList, "");
        setTaskName(getDefaultTaskName());
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputTifFilePath)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_inputLsmFilePathList)) {
            return new TextParameterVO(value, 1000);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @XmlElement
    public String getInputTifFilePath() {
        return getParameter(PARAM_inputTifFilePath);
    }

    public void setInputTifFilePath(String inputTifFilePath) {
        setParameter(PARAM_inputTifFilePath, inputTifFilePath);
    }

    abstract protected String getDefaultTaskName();

}
