package org.janelia.it.jacs.compute.service.align;

import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run the 63x whole brain alignment.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WholeBrain63xAlignmentService extends LegacyAlignmentService {

	protected static final String ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("WholeBrain63xAligner.ScriptPath");
	protected static final String TEMPLATE_DIR = SystemConfigurationProperties.getString("WholeBrain63xAligner.TemplateDir");

    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Starting "+getClass().getName()+" with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ resultFileNode.getDirectoryPath() + " inputFilename=" + input1.getFilepath());

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + resultFileNode.getDirectoryPath() + "\n ");
		script.append("sh " + EXECUTABLE_DIR + ALIGNER_SCRIPT_CMD +
            " " +  EXECUTABLE_DIR + TEMPLATE_DIR +
            " " + input1.getFilepath() + 
            " " + resultFileNode.getDirectoryPath()+"/Aligned.v3draw" +
            " \"" + input1.getOpticalResolution() + "\"\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}
}
