package org.janelia.it.jacs.compute.service.utility;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.launcher.archive.ArchiveAccessHelper;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Copy a file or a directory tree over from archive.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Archive.Linux to copy to corresponding location in JacsData.Dir.Linux
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncFromArchiveService implements IService {
	
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
        	ArchiveAccessHelper.sendArchiveSyncMessage(filePath);
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Synchronization from archive failed with:" + e.getMessage(), e);
        }
    	
    }
}
