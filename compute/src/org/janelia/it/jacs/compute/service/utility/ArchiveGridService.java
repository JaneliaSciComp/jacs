package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Copy a file or a directory tree from archive to the file share, or vice versa.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Archive.Linux or JacsData.Dir.Linux
 *   FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArchiveGridService extends SubmitDrmaaJobService {

	protected Logger logger = Logger.getLogger(ArchiveGridService.class);

	private static final int TIMEOUT_SECONDS = 1800; // 30 minutes

	public static final String PARAM_sourceFilePaths = "source file paths";
	public static final String PARAM_targetFilePaths = "target file paths";
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");

    protected static final String ARCHIVE_SYNC_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ArchiveSync.ScriptPath");

    protected static final String SCALITY_SYNC_CMD = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ArchiveSyncSproxyd.Timing.ScriptPath");
    
    protected static final String REMOVE_COMMAND = "rm -rf"; 
    
    @Override
    protected String getGridServicePrefixName() {
        return "archive";
    }
    
    private List<String> sourcePaths;
    private List<String> targetPaths;
    private boolean deleteSourceFiles = false;
    
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);

        try {
            
            Object sourceFilePathObject = processData.getItem("SOURCE_FILE_PATHS");
            if (sourceFilePathObject!=null) {
                if (sourceFilePathObject instanceof List) {
                    this.sourcePaths = (List<String>)sourceFilePathObject;
                }
                else if (sourceFilePathObject instanceof String) {
                    this.sourcePaths = Task.listOfStringsFromCsvString((String)sourceFilePathObject);
                }
                else {
                    throw new ServiceException("SOURCE_FILE_PATHS is an unsupported type: "+sourceFilePathObject.getClass().getName());
                }
            }
            else {
                throw new ServiceException("SOURCE_FILE_PATHS cannot be null.");
            }
            
            if (sourcePaths.isEmpty()) {
                logger.info("SOURCE_FILE_PATHS is empty, nothing to do.");
                return;
            }
            
            Object targetFilePathObject = processData.getItem("TARGET_FILE_PATHS");
            if (targetFilePathObject!=null) {
                if (targetFilePathObject instanceof List) {
                    this.targetPaths = (List<String>)targetFilePathObject;
                }
                else if (targetFilePathObject instanceof String) {
                    this.targetPaths = Task.listOfStringsFromCsvString((String)targetFilePathObject);
                }
                else {
                    throw new ServiceException("TARGET_FILE_PATHS is an unsupported type: "+targetFilePathObject.getClass().getName());
                }
            }
            else {
                // In cases where the target paths were not specified, they can be derived
                this.targetPaths = new ArrayList<String>();
                for(String truePath : sourcePaths) {
                    if (truePath.contains(JACS_DATA_ARCHIVE_DIR)) {
                        targetPaths.add(truePath.replaceFirst(JACS_DATA_ARCHIVE_DIR, JACS_DATA_DIR));
                    }
                    else if (truePath.contains(JACS_DATA_DIR)) {
                        targetPaths.add(truePath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR));
                    }
                    else {
                        throw new ServiceException("Source paths must be either in JacsData.Dir.Linux or JacsData.Dir.Archive.Linux. " +
                        		"The given path is located elsewhere: "+truePath);
                    }
                }
            }
            
            processData.putItem("INPUT_FILE_PATHS", sourcePaths);
            processData.putItem("OUTPUT_FILE_PATHS", targetPaths);
            
            String deleteSourceFilesStr = (String)processData.getItem("DELETE_SOURCE_FILES");
            if (deleteSourceFilesStr!=null && deleteSourceFilesStr.equalsIgnoreCase("true")) {
                deleteSourceFiles = true;
            }
        }
        catch (Exception e) {
            if (e instanceof ServiceException) {
                throw (ServiceException)e;
            }
            throw new ServiceException("Archive copy failed", e);
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;
        LinkedList<String> targets = new LinkedList<String>(targetPaths);
        for(String sourceFilepath : sourcePaths) {
            String targetFilepath = targets.remove();
            if (sourceFilepath!=null && targetFilepath!=null) {
                logger.info("Will copy "+sourceFilepath+" to "+targetFilepath);
                writeInstanceFiles(sourceFilepath, targetFilepath, configIndex++);
            }
            else {
                logger.warn("Null source or target filepath. Source:"+sourceFilepath+", Target:"+targetFilepath);
            }
        }
        writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(String sourceFilepath, String targetFilepath, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, sourceFilepath, targetFilepath);
        }
        catch (IOException e) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    protected void writeInstanceFile(FileWriter fw, String sourceFilepath, String targetFilepath) throws IOException {

    	if (sourceFilepath.startsWith(EntityConstants.SCALITY_PATH_PREFIX)) {
    		String bpid = sourceFilepath.replaceFirst(EntityConstants.SCALITY_PATH_PREFIX, "");
    		String scalityUrl = ScalityDAO.getClusterUrlFromBPID(bpid);
            fw.write(scalityUrl + "\n");
    	}
    	else {
            fw.write(sourceFilepath + "\n");
    	}
    	
        fw.write(targetFilepath + "\n");
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read SOURCE_FILE\n");
        script.append("read TARGET_FILE\n");
        script.append("if [ \"$SOURCE_FILE\" == \"$TARGET_FILE\" ]; then\n");
        script.append("  echo \"Source and target are identical\"\n");
        script.append("else\n");
        // Scality PUT is not supported here. Use the SyncSampleToScalityService instead
        script.append("  if [[ $SOURCE_FILE == http* ]]; then\n");
        script.append("    timing=`"+SCALITY_SYNC_CMD + " GET \"$SOURCE_FILE\" \"$TARGET_FILE\"`\n");
        script.append("    echo \"$timing\"\n");
        script.append("  else\n");
        script.append("    "+ARCHIVE_SYNC_CMD + " \"$SOURCE_FILE\" \"$TARGET_FILE\"\n");
        if (deleteSourceFiles) {
            script.append("    "+REMOVE_COMMAND + " \"$SOURCE_FILE\"\n");    
        }
        script.append("  fi\n");
        script.append("fi\n");
        writer.write(script.toString());
    }
    
    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredSlots() {
        return 1;
    }

    /**
     * Make sure the files were copied to the target location.
     */
    @Override
	public void postProcess() throws MissingDataException {
    	FileNode node = ProcessDataHelper.getResultFileNode(processData);
    	for(String filepath : targetPaths) {
    		File file = new File(filepath);
        	if (!file.exists()) {
        		throw new MissingGridResultException(node.getDirectoryPath(), "Target file not found at "+file.getAbsolutePath());
        	}
    	}
    }
}
