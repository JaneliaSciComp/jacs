package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
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
public class WholeBrain40xAlignmentService extends BrainAlignmentService {

	protected static final String WB40X_ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("WholeBrain40xAligner.ScriptPath");
	protected static final String WB40X_TEMPLATE_DIR = SystemConfigurationProperties.getString("WholeBrain40xAligner.TemplateDir");

    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Starting WholeBrain40xAlignmentService with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n " +
        	"sh " + EXECUTABLE_DIR + WB40X_ALIGNER_SCRIPT_CMD +
            " " +  EXECUTABLE_DIR + WB40X_TEMPLATE_DIR +
            " " + inputFilename + 
            " " + alignFileNode.getDirectoryPath()+"/Aligned.v3dpbd\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 8 slots on a node. This gives us 24 GB of memory. 
    	jt.setNativeSpecification("-pe batch 8");
    	return jt;
    }
}
