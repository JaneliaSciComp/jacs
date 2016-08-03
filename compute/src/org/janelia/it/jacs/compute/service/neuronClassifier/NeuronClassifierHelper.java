package org.janelia.it.jacs.compute.service.neuronClassifier;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Common utility functions for generating command lines to call the Training and Prediction aspects of the LineageClassifier utilities.
 * 
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronClassifierHelper {

    protected static final String TRAINING_SCRIPT =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("LineageClassifierTraining.ScriptPath");

    protected static final String PREDICTION_SCRIPT =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("LineageClassifierPrediction.ScriptPath");
    
	public static String getTrainingCommands(int numThreads) throws ServiceException {
	    StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+TRAINING_SCRIPT+" $CONFIG_FILE $OUTPUTDIR");
        return script.toString();
	}

    public static String getPredictionCommands(int numThreads) throws ServiceException {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+PREDICTION_SCRIPT+" $CONFIG_FILE $OUTPUTDIR");
        return script.toString();
    }
}
