package org.janelia.it.jacs.compute.service.utility;

import java.io.File;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Move a file or a directory tree over from archive.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncToArchiveService implements IService {
	
	private static final int TIME_OUT_SECS = 300; // 5 minutes
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    

    public void execute(IProcessData processData) throws ServiceException {

    	try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            String truePath = (String)processData.getItem("FILE_PATH");
            if (truePath==null) {
            	throw new ServiceException("Input parameter FILE_PATH may not be null");
            }
            
        	logger.info("Moving to archive: "+truePath);
        	
        	String archivePath = truePath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR);
        	File archiveFile = new File(archivePath);
        	
        	StringBuffer script = new StringBuffer();
        	if (archiveFile.exists()) {
        		// Destination already exists
            	throw new ServiceException("Archive already exists at "+archiveFile);
        	}
        	else {
        		// Destination does not exist, move it over
        		archiveFile.getParentFile().mkdirs();
        		script.append("mv "+truePath+" "+archiveFile+"; ");
        	}
        	        	
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);

            if (0!=exitCode) {
            	throw new ServiceException("SyncToArchiveService failed with exitCode "+exitCode);
            }
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running SyncToArchiveService:" + e.getMessage(), e);
        }
    	
    }
}
