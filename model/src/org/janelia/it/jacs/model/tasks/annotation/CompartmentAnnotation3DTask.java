package org.janelia.it.jacs.model.tasks.annotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 9/10/13
 * Time: 11:05 AM
 * To change this template use File | Settings | File Templates.
 */

public class CompartmentAnnotation3DTask extends Task {

    transient public static final String TASK_NAME = "CompartmentAnnotation3D";
    transient public static final String DISPLAY_NAME = "Compartment 3D Annotation";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_configurationName = "configuration name";
    transient public static final String PARAM_sampleIdListPath = "sample id list path";
    transient public static final String PARAM_patternChannel = "pattern channel";

    // Default values - default overrides

    public CompartmentAnnotation3DTask(Set<Node> inputNodes,
                                       String owner,
                                       List<Event> events,
                                       Set<TaskParameter> taskParameterSet,
                                       String configurationName,
                                       String sampleIdListPath,
                                       Integer patternChannel) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_configurationName, configurationName);
        setParameter(PARAM_sampleIdListPath, sampleIdListPath);
        setParameter(PARAM_patternChannel, patternChannel.toString());
    }

    public CompartmentAnnotation3DTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_configurationName, "");
        setParameter(PARAM_sampleIdListPath, "");
        setParameter(PARAM_patternChannel, "0");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_configurationName)) {
            return new TextParameterVO(value, 4000);
        } else if (key.equals(PARAM_sampleIdListPath)) {
            return new TextParameterVO(value, 4000);
        } else if (key.equals(PARAM_patternChannel)) {
            return new LongParameterVO(new Long(value));
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

