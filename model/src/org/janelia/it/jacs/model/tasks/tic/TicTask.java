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
public abstract class TicTask extends Task {
    transient public static final String TASK_NAME = "tic";
    transient protected String BASE_DISPLAY_NAME = "Transcription Imaging Consortium";

    // Parameter Keys
    transient public static final String PARAM_inputFilePrefix              = "input file prefix";
    transient public static final String PARAM_borderValue                  = "border crop value";
    transient public static final String PARAM_transformationFile           = "transformation file";
    transient public static final String PARAM_intensityCorrectionFactorFile= "intensity correction file";
    transient public static final String PARAM_microscopeSettingsFile       = "microscope settings file";
    transient public static final String PARAM_avgReadNoise                 = "average read noise";
    transient public static final String PARAM_avgDark                      = "average dark";
    transient public static final String PARAM_runApplyCalibrationToFrame   = "run apply calibration to frame";
    transient public static final String PARAM_runIlluminationCorrection    = "run illumination correction";
    transient public static final String PARAM_runFQBatch                   = "run FISH QUANT V4";
    transient public static final String PARAM_spotDataOnly                 = "save spot data only";
    transient public static final String PARAM_positionXMin                 = "position x min";
    transient public static final String PARAM_positionXMax                 = "position x max";
    transient public static final String PARAM_positionYMin                 = "position y min";
    transient public static final String PARAM_positionYMax                 = "position y max";
    transient public static final String PARAM_positionZMin                 = "position z min";
    transient public static final String PARAM_positionZMax                 = "position z max";
    transient public static final String PARAM_sigmaXMin                    = "sigma x min";
    transient public static final String PARAM_sigmaXMax                    = "sigma x max";
    transient public static final String PARAM_sigmaYMin                    = "sigma y min";
    transient public static final String PARAM_sigmaYMax                    = "sigma y max";
    transient public static final String PARAM_sigmaZMin                    = "sigma z min";
    transient public static final String PARAM_sigmaZMax                    = "sigma z max";
    transient public static final String PARAM_finalOutputLocation          = "finalOutputLocation";


    public TicTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public TicTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputFilePrefix, "");
        setParameter(PARAM_borderValue, "");
        setParameter(PARAM_transformationFile, "");
        setParameter(PARAM_intensityCorrectionFactorFile, "");
        setParameter(PARAM_microscopeSettingsFile, "");
        setParameter(PARAM_avgReadNoise, "");
        setParameter(PARAM_avgDark, "");
        setParameter(PARAM_runApplyCalibrationToFrame, Boolean.FALSE.toString());
        setParameter(PARAM_runIlluminationCorrection, Boolean.FALSE.toString());
        setParameter(PARAM_runFQBatch, Boolean.FALSE.toString());
        setParameter(PARAM_spotDataOnly, Boolean.FALSE.toString());
        setParameter(PARAM_positionXMin, "");
        setParameter(PARAM_positionXMax, "");
        setParameter(PARAM_positionYMin, "");
        setParameter(PARAM_positionYMax, "");
        setParameter(PARAM_positionZMin, "");
        setParameter(PARAM_positionZMax, "");
        setParameter(PARAM_sigmaXMin, "");
        setParameter(PARAM_sigmaXMax, "");
        setParameter(PARAM_sigmaYMin, "");
        setParameter(PARAM_sigmaYMax, "");
        setParameter(PARAM_sigmaZMin, "");
        setParameter(PARAM_sigmaZMax, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_intensityCorrectionFactorFile) || key.equals(PARAM_transformationFile) ||
            key.equals(PARAM_microscopeSettingsFile) || key.equals(PARAM_avgReadNoise) || key.equals(PARAM_avgDark)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_inputFilePrefix)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_borderValue) || key.equals(PARAM_positionXMax)|| key.equals(PARAM_positionXMin)|| key.equals(PARAM_positionYMax)
                || key.equals(PARAM_positionYMin)|| key.equals(PARAM_positionZMax)|| key.equals(PARAM_positionZMin)|| 
                key.equals(PARAM_sigmaXMax)|| key.equals(PARAM_sigmaXMin)|| key.equals(PARAM_sigmaYMax)
                || key.equals(PARAM_sigmaYMin)|| key.equals(PARAM_sigmaZMax)|| key.equals(PARAM_sigmaZMin)) {
            return new DoubleParameterVO(Double.valueOf(value));
        }
        if (key.equals(PARAM_runApplyCalibrationToFrame) || key.equals(PARAM_runIlluminationCorrection) ||
            key.equals(PARAM_runFQBatch) || key.equals(PARAM_spotDataOnly)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return BASE_DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}