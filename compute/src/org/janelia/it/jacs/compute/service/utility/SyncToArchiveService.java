package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Move a file or a directory tree over to the archive.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Linux to move or sync to corresponding location in JacsData.Dir.Archive.Linux
 *   FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncToArchiveService implements IService {

	protected Logger logger = Logger.getLogger(SyncToArchiveService.class);
	
	private static final int TIME_OUT_SECS = 300; // 5 minutes
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");

    protected static final String MOVE_COMMAND = "mv"; 
    protected static final String SYNC_COMMAND = "rsync -aW"; 

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
            
            for(String truePath : truePaths) {
            	try {
            		syncDir(truePath);
            	}
            	catch (Exception e) {
            		logger.error("Sync failed for dir "+truePath, e);
            	}
            }
            
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Synchronization to archive failed", e);
        }
    }

    private void syncDir(String filePath) throws Exception {

    	logger.info("Synchronizing to archive: "+filePath);

    	String archivePath = null;
    	String truePath = null;
    	if (filePath.contains(JACS_DATA_DIR)) {
    		truePath = filePath;
    		archivePath = filePath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR);
    	}
    	else if (filePath.contains(JACS_DATA_ARCHIVE_DIR)) {
    		archivePath = filePath;
    		truePath = filePath.replaceFirst(JACS_DATA_ARCHIVE_DIR, JACS_DATA_DIR);
    	}
    	else {
    		throw new ServiceException("Unrecognized path: "+filePath);
    	}

    	File file = new File(truePath);
    	File archiveFile = new File(archivePath);
    	StringBuffer script = new StringBuffer();
    	
    	if (archiveFile.exists()) {
    		// Destination already exists, just update it
    		script.append(SYNC_COMMAND+" "+file.getParent()+" "+archivePath);
    	}
    	else {
    		// Destination does not exist, move it over
    		archiveFile.getParentFile().mkdirs();
    		script.append(MOVE_COMMAND+" "+truePath+" "+archiveFile.getAbsolutePath()+"; ");
    	}
    	        	
    	logger.info("Running: "+script);
    	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);

        if (0!=exitCode) {
        	throw new ServiceException("Synchronization to archive failed with exitCode "+exitCode);
        }
    }
}
