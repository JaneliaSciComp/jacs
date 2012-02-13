package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Align an optic lobe globally and locally.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpticLobeAlignmentService extends BrainAlignmentService {

	protected static final String OPTIC_ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("OpticLobeAligner.ScriptPath");
	protected static final String OPTIC_TEMPLATE_DIR = SystemConfigurationProperties.getString("OpticLobeAligner.TemplateDir");

    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Starting OpticLobeAlignmentService with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n " +
        	PERL_EXE + " " + EXECUTABLE_DIR + OPTIC_ALIGNER_SCRIPT_CMD +
            " -v " +  Vaa3DHelper.getVaa3dExecutableCmd() +
            " -b " +  EXECUTABLE_DIR + ALIGNER_EXE_PATH +
            " -t " +  EXECUTABLE_DIR + OPTIC_TEMPLATE_DIR +
            " -w " +  alignFileNode.getDirectoryPath() +
            " -i " +  inputFilename + "\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}
}
