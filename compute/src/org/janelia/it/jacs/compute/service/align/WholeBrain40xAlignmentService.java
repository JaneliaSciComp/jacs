package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run the 40x whole brain alignment.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WholeBrain40xAlignmentService extends AbstractAlignmentService {

	protected static final String ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("WholeBrain40xAligner.ScriptPath");
	protected static final String TEMPLATE_DIR = SystemConfigurationProperties.getString("WholeBrain40xAligner.TemplateDir");

    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {
    	
		logger.info("Starting "+getClass().getName()+" with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n ");
		script.append("sh " + EXECUTABLE_DIR + ALIGNER_SCRIPT_CMD +
            " " +  EXECUTABLE_DIR + TEMPLATE_DIR +
            " " + inputFilename + 
            " " + alignFileNode.getDirectoryPath()+"/Aligned.v3draw" +
            " \"" + opticalResolution + "\"\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 8 slots on a 96 gig node. 
    	jt.setNativeSpecification("-pe batch 8 -l mem96=true -now n");
    	return jt;
    }
}
