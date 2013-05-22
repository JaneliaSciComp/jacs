package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Common utility functions for generating command lines to call the Neuron Separator and its related utilities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparatorHelper {

    protected static final String SEPARATOR_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("Separator.ScriptPath");

    protected static final String WARPED_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("Warped.ScriptPath");

    protected static final String FAST_LOAD_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("FastLoad.ScriptPath");

    protected static final String MASK_CHAN_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("MaskChan.ScriptPath");
    
    protected static final String MIP_CREATOR_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("MipCreator.ScriptPath");
    
	public static String getNeuronSeparationCommands(int numThreads, boolean isWarped) throws ServiceException {
	    StringBuilder script = new StringBuilder();
        if (isWarped) {
            script.append("EXT=${CONSOLIDATED_LABEL#*.}\n");
            script.append("cp $CONSOLIDATED_LABEL $OUTPUT_DIR/ConsolidatedLabel.$EXT\n");
        }
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+(isWarped?WARPED_SCRIPT:SEPARATOR_SCRIPT)+" $OUTPUT_DIR neuronSeparatorPipeline $INPUT_FILE \"$SIGNAL_CHAN\" \"$REF_CHAN\" $PREVIOUS_OUTPUT");
        return script.toString();
	}

	public static String getFastLoadCommands(int numThreads) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+FAST_LOAD_SCRIPT+" $INPUT_DIR");
        return script.toString();
	}

    public static String getMaskChanCommands(int numThreads) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+MASK_CHAN_SCRIPT+" $INPUT_DIR");
        return script.toString();
    }
    
	public static String getMipCreatorCommands(int numThreads) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+MIP_CREATOR_SCRIPT+" $OUTPUT_DIR png $INPUT_FILE \"$SIGNAL_CHAN\" \"$REF_CHAN\"");
        return script.toString();
	}
}
