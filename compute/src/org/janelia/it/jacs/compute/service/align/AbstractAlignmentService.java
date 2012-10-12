package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run the original central brain aligner from Hanchuan. Also serves as the base class for future alignment algorithms.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractAlignmentService extends SubmitDrmaaJobService {

	protected static final long LARGE_FILE_SIZE_THRESHOLD_UNCOMPRESSED = (long)(4.0*1024*1024*1024);
	protected static final long LARGE_FILE_SIZE_THRESHOLD_COMPRESSED = (long)(2.0*1024*1024*1024);
	
	protected static final String CONFIG_PREFIX = "alignConfiguration.";
	protected static final int TIMEOUT_SECONDS = 3600;  // 60 minutes

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");

    protected FileNode outputFileNode;
    protected FileNode alignFileNode;
    protected String inputFilename;
    protected String opticalResolution;
    
    @Override
    protected String getGridServicePrefixName() {
        return "align";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	outputFileNode = resultFileNode;
        }

        alignFileNode = (FileNode)processData.getItem("ALIGN_RESULT_FILE_NODE");
        if (alignFileNode==null) {
        	alignFileNode = resultFileNode;
        }

        inputFilename = (String)processData.getItem("INPUT_FILENAME");
        if (inputFilename==null) {
        	throw new ServiceException("Input parameter INPUT_FILENAME may not be null");
        }

        opticalResolution = (String)processData.getItem("OPTICAL_RESOLUTION");
        if (opticalResolution==null) {
        	throw new ServiceException("Input parameter OPTICAL_RESOLUTION may not be null");
        }
        
        File outputFile = new File(outputFileNode.getDirectoryPath(),"Aligned.v3draw");
        processData.putItem("ALIGNED_FILENAME", outputFile.getAbsolutePath());
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the alignment pipeline.");
        }
        createShellScript(writer);
        setJobIncrementStop(1);
    }

    protected abstract void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException;

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
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
	            return name.equals("Aligned.v3dpbd");
			}
		});

    	if (alignedFiles.length < 1) {
    		throw new MissingDataException("Expected Aligned.v3dpbd - not found for "+alignFileNode.getDirectoryPath());
    	}
	}
}
