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
public class WholeBrain63xAlignmentService extends BrainAlignmentService {

	protected static final String WB63X_ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("WholeBrain63xAligner.ScriptPath");
	protected static final String WB63X_TEMPLATE_DIR = SystemConfigurationProperties.getString("WholeBrain63xAligner.TemplateDir");

    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Starting WholeBrain63xAlignmentService with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n " +
        	"sh " + EXECUTABLE_DIR + WB63X_ALIGNER_SCRIPT_CMD +
            " " +  EXECUTABLE_DIR + WB63X_TEMPLATE_DIR +
            " " + inputFilename + 
            " " + alignFileNode.getDirectoryPath()+"/Aligned.v3dpbd\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 8 slots on 96 GB node (superclass already requests 8 nodes) 
    	jt.setNativeSpecification("-l mem96=true -now n");
    	return jt;
    }
}
