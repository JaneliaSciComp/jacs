package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Compute similarity between a target stack and a set of other stacks. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DSimilarityService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "simConfiguration.";
    
    private File targetStackFile;
    private File inputListFile;
    private File outputListFile;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "sim";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        String targetStackFilepath = (String)processData.getItem("TARGET_STACK_FILEPATH");
        if (targetStackFilepath==null) {
        	throw new ServiceException("Input parameter TARGET_STACK_FILEPATH may not be null");	
        }
        targetStackFile = new File(targetStackFilepath);
        
        String inputListFilepath = (String)processData.getItem("INPUT_LIST_FILEPATH");
        if (inputListFilepath==null) {
        	throw new ServiceException("Input parameter INPUT_LIST_FILEPATH may not be null");	
        }
        inputListFile = new File(inputListFilepath);

        String outputListFilepath = (String)processData.getItem("OUTPUT_LIST_FILEPATH");
        if (outputListFilepath==null) {
        	throw new ServiceException("Input parameter OUTPUT_LIST_FILEPATH may not be null");	
        }
        outputListFile = new File(outputListFilepath);
        
        writeInstanceFiles();
        setJobIncrementStop(1);
        
    	createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }

    /**
     * Write the shell script that runs the grouper on the merged files.
     * @param writer
     * @param mergedFilepath
     * @param groupedFilepath
     * @throws Exception
     */
    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedSimilarityCommand(targetStackFile.getAbsolutePath(), inputListFile.getAbsolutePath(), outputListFile.getAbsolutePath()));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve 8 slots on a short node. This gives us 24 GB of memory. 
    	jt.setNativeSpecification("-pe batch 8 -l short=true");
    	return jt;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Grouping core dumped for "+resultFileNode.getDirectoryPath());
    	}
    	
    	if (!outputListFile.exists()) {
    		throw new MissingDataException("Output file not found at "+outputListFile.getAbsolutePath());
    	}
    }
}
