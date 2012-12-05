package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Archive certain files in a certain directory and replace them with symbolic links to their archived versions. Parameters:
 *   INPUT_PATHS - directory file
 *   FILE_PATTERN - wildcard pattern for choosing files to archive and link
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArchivingGridService extends SubmitDrmaaJobService {

    protected static final String ARCHIVE_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
    		SystemConfigurationProperties.getString("MoveToArchive.ScriptPath");
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private List<String> inputPaths;
    private String filePattern;
    
    @Override
    protected String getGridServicePrefixName() {
        return "archive";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        inputPaths = (List<String>)processData.getItem("INPUT_PATHS");
        if (inputPaths==null) {
        	throw new ServiceException("Input parameter INPUT_PATHS may not be empty");
        }

        filePattern = (String)processData.getItem("FILE_PATTERN");
        if (filePattern==null) {
        	throw new ServiceException("Input parameter FILE_PATTERN may not be empty");
        }
        
        logger.info("Starting FastLoadArtifactPipelineGridService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath());
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
    	int configIndex = 1;
    	for(String inputPath : inputPaths) {
    		writeInstanceFiles(inputPath, configIndex++);
    	}
    	writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(String inputFile, int configIndex) throws Exception {
    	File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, inputFile, configIndex);
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    protected void writeInstanceFile(FileWriter fw, String inputDir, int configIndex) throws IOException {
        fw.write(inputDir + "\n");
        fw.write(filePattern + "\n");
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_DIR\n");
        script.append("read FILE_PATTERN\n");
        script.append("cd $INPUT_DIR\n");
        script.append(ARCHIVE_CMD + " $FILE_PATTERN\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 3;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
	}
}