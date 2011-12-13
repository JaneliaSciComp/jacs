package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.v3d.V3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/9/11
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class BrainAlignmentService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "alignConfiguration.";
    private static final int TIMEOUT_SECONDS = 3600;  // 60 minutes

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String ALIGNER_SCRIPT_CMD = SystemConfigurationProperties.getString("BrainAligner.ScriptPath");
    protected static final String ALIGNER_EXE_PATH = SystemConfigurationProperties.getString("BrainAligner.ExePath");
    protected static final String LOBESEG_EXE_PATH = SystemConfigurationProperties.getString("BrainAligner.LobesegPath");
    protected static final String TEMPLATE_DIR = SystemConfigurationProperties.getString("BrainAligner.TemplateDir");
    protected static final String PERL_EXE = SystemConfigurationProperties.getString("Perl.Path");

    private FileNode outputFileNode;
    private FileNode alignFileNode;
    private String inputFilename;

    @Override
    protected String getGridServicePrefixName() {
        return "align";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	throw new ServiceException("Input parameter OUTPUT_FILE_NODE may not be null");
        }

        alignFileNode = (FileNode)processData.getItem("ALIGN_RESULT_FILE_NODE");
        if (alignFileNode==null) {
        	throw new ServiceException("Input parameter ALIGN_RESULT_FILE_NODE may not be null");
        }

        inputFilename = (String)processData.getItem("INPUT_FILENAME");
        if (inputFilename==null) {
        	throw new ServiceException("Input parameter INPUT_FILENAME may not be null");
        }
        
        File outputFile = new File(outputFileNode.getDirectoryPath(),"Aligned.v3draw");
        processData.putItem("ALIGNED_FILENAME", outputFile.getAbsolutePath());
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the Neuron Separation pipeline.");
        }
        createShellScript(writer);
        setJobIncrementStop(1);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {

        logger.info("Starting BrainAlignmentService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath() +
            " workingDir="+alignFileNode.getDirectoryPath() + " inputFilename="+inputFilename);

/*
sub usage {
    print STDERR $_[0] . "\n";
    die "Usage: -v <v3d exe path> -b <brain_aligner path> -l <lobeseg path> -t <template dir> -w <working dir> -i <input stack> \n";
}
  */

        StringBuffer script = new StringBuffer();
        script.append(V3DHelper.getV3dLibrarySetupCmd()+"\n");
        script.append("cd " + alignFileNode.getDirectoryPath() + "\nsh " +
                      EXECUTABLE_DIR + ALIGNER_SCRIPT_CMD +
            " -v " +  V3DHelper.getV3dExecutableCmd() +
            " -b " +  EXECUTABLE_DIR + ALIGNER_EXE_PATH +
            " -l " +  EXECUTABLE_DIR + LOBESEG_EXE_PATH +
            " -t " +  EXECUTABLE_DIR + TEMPLATE_DIR +
            " -w " +  alignFileNode.getDirectoryPath() +
            " -i " +  inputFilename + "\n");
        writer.write(script.toString());
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve 4 out of the 8 slots on a node. This gives us 12 GB of memory.
    	jt.setNativeSpecification("-pe batch 4");
    	return jt;
    }

    @Override
	public void postProcess() throws MissingDataException {

        File alignDir = new File(alignFileNode.getDirectoryPath());
    	File[] coreFiles = alignDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});

    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Brain alignment core dumped for "+alignFileNode.getDirectoryPath());
    	}

    	File[] alignedFiles = alignDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.equals("Aligned.v3draw");
			}
		});

    	if (alignedFiles.length < 1) {
    		throw new MissingDataException("Expected Aligned.v3draw - not found for "+alignFileNode.getDirectoryPath());
    	}
	}
}
