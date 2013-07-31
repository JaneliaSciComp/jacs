package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Copy a file or a directory tree over from archive. Before using this service, consider using ArchiveGridService
 * to do the copies on the grid instead.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Archive.Linux to copy or sync to corresponding location in JacsData.Dir.Linux
 *   FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 * @see org.janelia.it.jacs.compute.service.utility.ArchiveGridService
 */
public class SyncFromArchiveService implements IService {

	protected Logger logger = Logger.getLogger(SyncFromArchiveService.class);

	private static final int TIME_OUT_SECS = 1800; // 30 minutes
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    protected static final String COPY_COMMAND = "cp -a"; 
    protected static final String SYNC_COMMAND = "rsync -aW"; 
    protected static final String MOVE_COMMAND = "mv";
    protected static final String REMOVE_COMMAND = "rm -rf";
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            List<String> truePaths = (List<String>)processData.getItem("FILE_PATHS");
            if (truePaths==null) {
            	String truePath = (String)processData.getItem("FILE_PATH");
            	if (truePath==null) {
            		throw new ServiceException("Both input parameters FILE_PATH and FILE_PATHS may not be null");	
            	}
            	truePaths = new ArrayList<String>();
            	truePaths.add(truePath);
            }
            
            List<String> targetPaths = new ArrayList<String>();
            for(String truePath : truePaths) {
                if (!truePath.contains(JACS_DATA_ARCHIVE_DIR)) {
                    throw new ServiceException("Unrecognized archive path: "+truePath);
                }
                targetPaths.add(truePath.replaceFirst(JACS_DATA_ARCHIVE_DIR, JACS_DATA_DIR));
            }
            
            syncPaths(truePaths, targetPaths);
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Synchronization from archive failed", e);
        }
    }

    /**
     * Alternative execution method which does not run within the launcher framework, or require ProcessData.
     */
    public void execute(String sourceFilepath, String targetFilepath) throws ServiceException {
        List<String> sourcepPaths = new ArrayList<String>();
        sourcepPaths.add(sourceFilepath);
        List<String> targetPaths = new ArrayList<String>();
        targetPaths.add(sourceFilepath);
        execute(sourcepPaths, targetPaths);
    }
    
    /**
     * Alternative execution method which does not run within the launcher framework, or require ProcessData.
     */
    public void execute(Collection<String> sourceFilepaths, Collection<String> targetFilepaths) throws ServiceException {
        try {
            syncPaths(sourceFilepaths, targetFilepaths);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void syncPaths(Collection<String> sourceFilepaths, Collection<String> targetFilepaths) throws Exception {
        LinkedList<String> targets = new LinkedList<String>(targetFilepaths);
        for(String sourceFilepath : sourceFilepaths) {
            String targetFilepath = targets.remove();
            syncPath(sourceFilepath, targetFilepath);
        }
    }
    
    private void syncPath(String sourceFilepath, String targetFilepath) throws Exception {
        logger.info("Synchronizing "+sourceFilepath+" to "+targetFilepath);
    	
        File sourceFile = new File(sourceFilepath);
    	File targetFile = new File(targetFilepath);
    	StringBuffer script = new StringBuffer();
    	
    	if (targetFile.exists() && sourceFile.getName().equals(targetFile.getName())) {
            // Destination already exists, just update it
            script.append(SYNC_COMMAND+" "+sourceFilepath+" "+targetFile.getParent());
    	}
    	else {
    	    // Otherwise, we just need to move it into place
    	    if (targetFile.exists() && targetFile.isDirectory()) {
    	        // Remove the target directory first
    	        script.append(REMOVE_COMMAND+" "+targetFile+"; ");
    	    }
            File tempFile = new File(targetFile.getParent(),"tmp-"+System.nanoTime());
            script.append(COPY_COMMAND+" "+sourceFilepath+" "+tempFile.getAbsolutePath()+"; ");
            script.append(MOVE_COMMAND+" "+tempFile.getAbsolutePath()+" "+targetFile);
    	}
    	
    	logger.info("Running synchronization script: "+script);
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);
        
        if (0!=exitCode) {
        	throw new ServiceException("Synchronization from archive failed with exitCode "+exitCode);
        }
    }
}
