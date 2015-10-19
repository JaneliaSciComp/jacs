package org.janelia.it.jacs.model.tasks.mip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * Created by goinac on 9/9/15.
 */
@XmlRootElement(name = "mipGeneration")
@XmlAccessorType(XmlAccessType.NONE)
public class MIPGenerationTask extends Task {
    transient public static final String TASK_NAME = "mipGenerator";
    transient public static final String DISPLAY_NAME = "MIP Generator";

    // Parameter Keys
    transient public static final String PARAM_inputImages = "input images";
    transient public static final String PARAM_normalizeToFirst = "normalize to first";
    transient public static final String PARAM_outputs = "outputs";

    private List<MIPInputImageData> inputImages;

    public MIPGenerationTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MIPGenerationTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameterAsBoolean(PARAM_normalizeToFirst, false);
        setTaskName(TASK_NAME);
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        switch (key) {
            case PARAM_normalizeToFirst:
                new BooleanParameterVO(Boolean.valueOf(value));
            case PARAM_outputs:
                new TextParameterVO(value);
            default:
                // No match
                return null;
        }
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @XmlElement
    public List<MIPInputImageData> getInputImages() {
        return inputImages;
    }

    public void setInputImages(List<MIPInputImageData> inputImages) throws Exception {
        this.inputImages = inputImages;
        if (inputImages != null) {
            ObjectMapper mapper = new ObjectMapper();
            setParameter(PARAM_inputImages, mapper.writeValueAsString(inputImages));
        }
    }

    @XmlElement(name = "normalizeToFirst")
    public Boolean getNormalizeToFirst() {
        return getParameterAsBoolean(PARAM_normalizeToFirst);
    }

    public void setNormalizeToFirst(Boolean normalizeToFirst) {
        setParameterAsBoolean(PARAM_normalizeToFirst, normalizeToFirst);
    }

    @XmlElement(name = "outputs")
    public String getOutputs() {
        return getParameter(PARAM_outputs);
    }

    public void setOutputs(String outputs) {
        setParameter(PARAM_outputs, outputs);
    }

}
