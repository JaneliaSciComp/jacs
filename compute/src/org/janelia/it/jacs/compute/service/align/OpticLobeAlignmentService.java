package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
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
public class OpticLobeAlignmentService extends AbstractAlignmentService {

	protected static final long LARGE_FILE_SIZE_THRESHOLD_UNCOMPRESSED = (long)(4.0*1024*1024*1024);
	protected static final long LARGE_FILE_SIZE_THRESHOLD_COMPRESSED = (long)(2.0*1024*1024*1024);
	
	protected static final String PERL_EXE = SystemConfigurationProperties.getString("Perl.Path");
	protected static final String ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("OpticLobeAligner.ScriptPath");
	protected static final String ALIGNER_EXE_PATH = SystemConfigurationProperties.getString("BrainAligner.ExePath");
	protected static final String TEMPLATE_DIR = SystemConfigurationProperties.getString("OpticLobeAligner.TemplateDir");

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

		logger.info("Starting "+getClass().getName()+" with taskId=" + task.getObjectId() + " resultNodeId="
				+ resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() + " workingDir="
				+ alignFileNode.getDirectoryPath() + " inputFilename=" + inputFilename);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\n ");
        script.append(PERL_EXE + " " + EXECUTABLE_DIR + ALIGNER_SCRIPT_CMD +
            " -v " +  Vaa3DHelper.getVaa3dExecutableCmd() +
            " -b " +  EXECUTABLE_DIR + ALIGNER_EXE_PATH +
            " -t " +  EXECUTABLE_DIR + TEMPLATE_DIR +
            " -w " +  alignFileNode.getDirectoryPath() +
            " -n \"" +  tileName + "\"" + 
            " -i \"" +  inputFilename + "\"" +
        	" -r \"" + opticalResolution + "\"" +
        	" -c " +  refChannel + "\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);

    	// Reserve all 8 slots on a node. 
    	String spec = "-pe batch 8";
    	File inputFile = new File(inputFilename);
    	long fileSize = inputFile.length();
    	
    	// For large input files, go ahead and reserve a 96GB node
    	if ((inputFilename.endsWith("raw") && fileSize>LARGE_FILE_SIZE_THRESHOLD_UNCOMPRESSED) || 
    			(inputFilename.endsWith("pbd") && fileSize>LARGE_FILE_SIZE_THRESHOLD_COMPRESSED)) {
    		logger.info("Input file size "+fileSize+" exceeds threshold. Will use a large memory node for processing.");
    		spec += " -l mem96=true -now n";	
    	}
    	
    	jt.setNativeSpecification(spec);
    	return jt;
    }
}
