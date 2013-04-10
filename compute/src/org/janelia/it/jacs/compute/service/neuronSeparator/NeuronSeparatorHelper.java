package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/18/11
 * Time: 10:19 AM
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
    
	public static String getNeuronSeparationCommands(boolean warped) throws ServiceException {
        return "sh "+(warped?WARPED_SCRIPT:SEPARATOR_SCRIPT)+" $OUTPUT_DIR neuronSeparatorPipeline $INPUT_FILE \"$SIGNAL_CHAN\" \"$REF_CHAN\" $PREVIOUS_OUTPUT";
	}

	public static String getFastLoadCommands() {
		return "sh "+FAST_LOAD_SCRIPT+" $INPUT_DIR";
	}

    public static String getMaskChanCommands() {
        return "sh "+MASK_CHAN_SCRIPT+" $INPUT_DIR";
    }
    
	public static String getMipCreatorCommands() {
		return "sh "+MIP_CREATOR_SCRIPT+" $OUTPUT_DIR png $INPUT_FILE \"$SIGNAL_CHAN\" \"$REF_CHAN\"";
	}
}
