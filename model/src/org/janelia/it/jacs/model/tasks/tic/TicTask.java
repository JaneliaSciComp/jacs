package org.janelia.it.jacs.model.tasks.tic;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * Modified by naxelrod
 */
public class TicTask extends Task {
    transient public static final String TASK_NAME = "tic";
    transient public static final String DISPLAY_NAME = "Transcription Imaging Consortium";

    // Parameter Keys
    transient public static final String PARAM_inputFile            = "input files";
    transient public static final String PARAM_borderValue          = "border value";
    transient public static final String PARAM_calibrationFile      = "calibration file";
    transient public static final String PARAM_correctionFactorFile = "correction factor file";

    public TicTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public TicTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputFile, "");
        setParameter(PARAM_borderValue, "");
        setParameter(PARAM_calibrationFile, "");
        setParameter(PARAM_correctionFactorFile, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_calibrationFile) || key.equals(PARAM_correctionFactorFile)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_inputFile)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_borderValue)) {
            return new DoubleParameterVO(Double.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}