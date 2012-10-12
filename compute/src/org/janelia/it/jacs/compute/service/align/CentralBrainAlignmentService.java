package org.janelia.it.jacs.compute.service.align;

import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run the Peng central brain aligner.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CentralBrainAlignmentService extends AbstractAlignmentService {

	protected static final String PERL_EXE = SystemConfigurationProperties.getString("Perl.Path");
    protected static final String ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("BrainAligner.ScriptPath");
    protected static final String ALIGNER_EXE_PATH = SystemConfigurationProperties.getString("BrainAligner.ExePath");
    protected static final String LOBESEG_EXE_PATH = SystemConfigurationProperties.getString("BrainAligner.LobesegPath");
    protected static final String TEMPLATE_DIR = SystemConfigurationProperties.getString("BrainAligner.TemplateDir");
    
    @Override
    protected void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {

        logger.info("Starting "+getClass().getName()+" with taskId=" + task.getObjectId() + 
        		" resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() +
        		" workingDir="+alignFileNode.getDirectoryPath() + " inputFilename="+inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n " + 
        	PERL_EXE + " " + EXECUTABLE_DIR + ALIGNER_SCRIPT_CMD +
            " -v " +  Vaa3DHelper.getVaa3dExecutableCmd() +
            " -b " +  EXECUTABLE_DIR + ALIGNER_EXE_PATH +
            " -l " +  EXECUTABLE_DIR + LOBESEG_EXE_PATH +
            " -t " +  EXECUTABLE_DIR + TEMPLATE_DIR +
            " -w " +  alignFileNode.getDirectoryPath() +
            " -i " +  inputFilename + 
        	" -r \"" + opticalResolution.replaceAll("x", " ") + "\"\n");
        writer.write(script.toString());
    }
}
