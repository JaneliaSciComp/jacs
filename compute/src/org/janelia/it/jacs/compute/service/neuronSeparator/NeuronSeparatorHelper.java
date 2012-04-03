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

	public static String getNeuronSeparationCommands() throws ServiceException {
        return "sh "+SEPARATOR_SCRIPT+" $OUTPUT_DIR neuronSeparatorPipeline $INPUT_FILE $PREVIOUS_OUTPUT";
	}
    
}
