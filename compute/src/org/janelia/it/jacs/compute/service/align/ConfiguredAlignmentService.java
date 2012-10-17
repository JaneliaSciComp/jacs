package org.janelia.it.jacs.compute.service.align;

import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run a configured brain aligner script.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredAlignmentService extends AbstractAlignmentService {

	protected static final String ALIGNER_SCRIPT_CMD = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ScriptPath");
	protected static final String BRAIN_ALIGNER_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.BrainAlignerDir");
	protected static final String CONFIG_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ConfigDir");
	protected static final String TEMPLATE_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.TemplateDir");
	
	
    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

        String scriptFile = (String)processData.getItem("ALIGNMENT_SCRIPT_NAME");
        if (scriptFile==null) {
        	throw new ServiceException("Input parameter ALIGNMENT_SCRIPT_NAME may not be null");
        }

		logger.info("Running configured aligner "+ALIGNER_SCRIPT_CMD+" ("
				+ " resultNodeId=" + resultFileNode.getObjectId() 
				+ " outputDir=" + alignFileNode.getDirectoryPath() 
				+ " inputFilename=" + inputFilename
				+")");

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd() + "\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n");
		script.append("sh " + ALIGNER_SCRIPT_CMD);
		script.append(" " + BRAIN_ALIGNER_DIR + "/" + scriptFile);
		script.append(" " + CONFIG_DIR + "/systemvars.apconf");
		script.append(" " + TEMPLATE_DIR);
		script.append(" " + inputFilename);
		script.append(" " + alignFileNode.getDirectoryPath());
		script.append(" " + refChannel);
		script.append(" \"" + opticalResolution+"\"");
		
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}
}
