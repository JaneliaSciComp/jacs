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
 * Copy a file or a directory tree over from archive.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Archive.Linux to copy or sync to corresponding location in JacsData.Dir.Linux
 *   FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncFromArchiveService implements IService {

	protected Logger logger = Logger.getLogger(SyncFromArchiveService.class);

	private static final int TIME_OUT_SECS = 300; // 5 minutes
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    protected static final String COPY_COMMAND = "cp -a"; 
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
            	syncDir(truePath);
            }
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Synchronization from archive failed", e);
        }
    }
    
    private void syncDir(String filePath) throws Exception {
        
    	logger.info("Synchronizing from archive: "+filePath);
    	
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
    	StringBuffer script = new StringBuffer();
    	
    	if (file.exists()) {
    		// Destination already exists, just update it
    		script.append(SYNC_COMMAND+" "+archivePath+" "+file.getParent());
    	}
    	else {
    		// Destination does not exist, sync to a temp directory and then move it into place
    		File tempFile = new File(file.getParent(),"tmp-"+System.nanoTime());
        	script.append(COPY_COMMAND+" "+archivePath+" "+tempFile.getAbsolutePath()+"; ");
        	script.append("mv "+tempFile.getAbsolutePath()+" "+truePath);
    	}
    	
    	logger.info("Running: "+script);
    	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);

        if (0!=exitCode) {
        	throw new ServiceException("Synchronization from archive failed with exitCode "+exitCode);
        }
    }
}
