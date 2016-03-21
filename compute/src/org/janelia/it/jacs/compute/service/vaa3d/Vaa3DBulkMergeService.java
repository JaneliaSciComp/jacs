package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Merge paired LSMs into v3draw files. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   SAMPLE_AREA - object containing a list of MergedLsmPair
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DBulkMergeService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "mergeConfiguration.";
    
    private int randomPort;
    private String multiscanblendVersion = "";
    private AnatomicalArea sampleArea;

    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
        this.sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
    }

    @Override
    protected String getGridServicePrefixName() {
        return "merge";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        
        String mergeAlgorithm = (String)processData.getItem("MERGE_ALGORITHM");
        
        logger.info("Using merge algorithm: "+mergeAlgorithm);
        
        if ("FLYLIGHT_ORDERED".equals(mergeAlgorithm)) {
            multiscanblendVersion = "2";
        }
        
        logger.info("Using multiscanblendVersion: "+multiscanblendVersion);
        
        int configIndex = 1;
        randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        for(MergedLsmPair mergedLsmPair : sampleArea.getMergedLsmPairs()) {
        	writeInstanceFiles(mergedLsmPair, configIndex++);
        }
        
    	createShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(MergedLsmPair mergedLsmPair, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(mergedLsmPair.getLsmFilepath1() + "\n");
            fw.write(mergedLsmPair.getLsmFilepath2() + "\n");
            fw.write(mergedLsmPair.getMergedFilepath() + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read LSM_FILENAME_1\n");
        script.append("read LSM_FILENAME_2\n");
        script.append("read MERGED_FILENAME\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedMergePipelineCommand("$LSM_FILENAME_1", "$LSM_FILENAME_2", "$MERGED_FILENAME", multiscanblendVersion));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 32;
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Bulk merge core dumped");
    	}

        for(MergedLsmPair mergedLsmPair : sampleArea.getMergedLsmPairs()) {
        	File outputFile = new File(mergedLsmPair.getMergedFilepath());
        	if (!outputFile.exists()) {
        		throw new MissingGridResultException(file.getAbsolutePath(), "Missing merge output "+outputFile.getAbsolutePath());
        	}
        }
	}
    
}
