package org.janelia.it.jacs.model.tasks.mip;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import javax.xml.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by goinac on 9/9/15.
 */
@XmlRootElement(name = "mipGeneration")
@XmlAccessorType(XmlAccessType.NONE)
public class MIPGenerationTask extends Task {
    transient public static final String TASK_NAME = "mipGenerator";
    transient public static final String DISPLAY_NAME = "MIP Generator";

    // Parameter Keys
    transient public static final String PARAM_inputFileList = "input file list";
    transient public static final String PARAM_signalChannels = "signal channels";
    transient public static final String PARAM_referenceChannel = "reference channel";
    transient public static final String PARAM_colorDepth = "color depth";

    public MIPGenerationTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MIPGenerationTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputFileList, "");
        setParameter(PARAM_signalChannels, "");
        setParameter(PARAM_referenceChannel, "");
        setParameter(PARAM_colorDepth, "");
        setTaskName(TASK_NAME);
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputFileList)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_signalChannels)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_referenceChannel)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_colorDepth)) {
            return new TextParameterVO(value, 400);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @XmlElement(name = "csInputFiles")
    public String getCsInputFiles() {
        return getParameter(PARAM_inputFileList);
    }

    public void setCsInputFiles(String csInputFiles) {
        setParameter(PARAM_inputFileList, csInputFiles);
    }

    public List<String> getInputFileList() {
        return Task.listOfStringsFromCsvString(getParameter(PARAM_inputFileList));
    }

    @XmlElement
    public String getSignalChannels() {
        return getParameter(PARAM_signalChannels);
    }

    public void setSignalChannels(String signalChannels) {
        setParameter(PARAM_signalChannels, signalChannels);
    }

    @XmlElement
    public String getReferenceChannel() {
        return getParameter(PARAM_referenceChannel);
    }

    public void setReferenceChannel(String referenceChannel) {
        setParameter(PARAM_referenceChannel, referenceChannel);
    }

    @XmlElement
    public String getColorDepth() {
        return getParameter(PARAM_colorDepth);
    }

    public void setColorDepth(String colorDepth) {
        setParameter(PARAM_colorDepth, colorDepth);
    }
}
