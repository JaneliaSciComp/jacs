package org.janelia.it.jacs.compute.service.align;

import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run the optic lobe aligner.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpticLobeAlignmentService extends BrainAlignmentService {

	protected static final String OPTIC_ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("OpticLobeAligner.ScriptPath");
	protected static final String OPTIC_TEMPLATE_DIR = SystemConfigurationProperties.getString("OpticLobeAligner.TemplateDir");

	private String tileName;
	
    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	tileName = (String)processData.getItem("ALIGNMENT_TILE_NAME");
        if (tileName==null) {
        	throw new ServiceException("Input parameter ALIGNMENT_TILE_NAME may not be null");
        }
    }
    
    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Starting OpticLobeAlignmentService with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n ");
        script.append(PERL_EXE + " " + EXECUTABLE_DIR + OPTIC_ALIGNER_SCRIPT_CMD +
            " -v " +  Vaa3DHelper.getVaa3dExecutableCmd() +
            " -b " +  EXECUTABLE_DIR + ALIGNER_EXE_PATH +
            " -t " +  EXECUTABLE_DIR + OPTIC_TEMPLATE_DIR +
            " -w " +  alignFileNode.getDirectoryPath() +
            " -n \"" +  tileName + "\"" + 
            " -i \"" +  inputFilename + "\"" +
        	" -r \"" + opticalResolution.replaceAll("x", " ") + "\"\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}
}
