package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;

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
    
    protected static final String SUMMARY_CREATOR_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("SummaryCreator.ScriptPath");
    
    protected static final String NEURON_MAPPING_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("NeuronMapping.ScriptPath");

    protected static final String NEURON_WEIGHTS_SCRIPT =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("NeuronWeights.ScriptPath");
    
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

    public static String getNeuronMappingCommands(String outputDir, String inputFile1, String inputFile2) {
        StringBuilder script = new StringBuilder();
        script.append("sh "+NEURON_MAPPING_SCRIPT+" \""+outputDir+"\" \""+inputFile1+"\" \""+inputFile2+"\"");
        return script.toString();
    }

    public static String getNeuronWeightsCommands(int numThreads) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+NEURON_WEIGHTS_SCRIPT+ " $CONFIG_FILE $OUTPUT_DIR");
        return script.toString();
    }
    
	public static String getFastLoadCommands(int numThreads, String inputFile) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+FAST_LOAD_SCRIPT+" "+inputFile);
        return script.toString();
	}

    public static String getMaskChanCommands(int numThreads, String inputFile) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+MASK_CHAN_SCRIPT+" "+inputFile);
        return script.toString();
    }
    
	public static String getMipCreatorCommands(int numThreads) {
        StringBuilder script = new StringBuilder();
        script.append("export NFE_MAX_THREAD_COUNT="+numThreads+"\n");
        script.append("sh "+MIP_CREATOR_SCRIPT+" $OUTPUT_DIR png $INPUT_FILE \"$SIGNAL_CHAN\" \"$REF_CHAN\"");
        return script.toString();
	}

	
	public static Entity getSeparationResult(Entity supportingFiles) {
        for(Entity file : supportingFiles.getChildren()) {
            if ("SeparationResultUnmapped.nsp".equals(file.getName()) || "SeparationResult.nsp".equals(file.getName())) {
                return file;
            }
        }
        return null;
	}
}
