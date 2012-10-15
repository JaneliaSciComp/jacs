package org.janelia.it.jacs.compute.service.utility;

import java.io.File;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.launcher.archive.ArchiveAccessMDB;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Copy a file or a directory tree over from archive.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Archive.Linux to copy to corresponding location in JacsData.Dir.Linux
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncFromArchiveService implements IService {

	protected Logger logger = Logger.getLogger(ArchiveAccessMDB.class);
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    protected static final String COPY_COMMAND = "cp -a"; 
    protected static final String SYNC_COMMAND = "rsync -aW"; 

    public void execute(IProcessData processData) throws ServiceException {

    	try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            String filePath = (String)processData.getItem("FILE_PATH");
            if (filePath==null) {
            	throw new ServiceException("Input parameter FILE_PATH may not be null");
            }
            
        	logger.info("Will sync with archive: "+filePath);
        	syncDir(filePath);
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Synchronization from archive failed with:" + e.getMessage(), e);
        }
    }
    
    public void syncDir(String filePath) throws Exception {
        
    	logger.info("Synchronizing with archive: "+filePath);
    	
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
    	        	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, 60);

        if (0!=exitCode) {
        	throw new ServiceException("Synchronization from archive failed with exitCode "+exitCode);
        }
    }
}
