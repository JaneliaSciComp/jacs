package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.util.FileUtils;
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
public class SyncToArchiveService extends AbstractEntityService {

	protected Logger logger = Logger.getLogger(SyncToArchiveService.class);
	
	private static final int TIME_OUT_SECS = 300; // 5 minutes
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");

    protected static final String SYNC_COMMAND = "rsync -aW"; 
    protected static final String REMOVE_COMMAND = "rm -rf"; 
    
    public void execute() throws Exception {

        List<String> truePaths = (List<String>)processData.getItem("FILE_PATHS");
        if (truePaths==null) {
        	String truePath = (String)processData.getItem("FILE_PATH");
        	if (truePath==null) {
        		throw new ServiceException("Both input parameters FILE_PATH and FILE_PATHS may not be null");	
        	}
        	truePaths = new ArrayList<String>();
        	truePaths.add(truePath);
        }
        
        logger.info("Synchronizing "+truePaths.size()+" paths to archive");
        
        for(String truePath : truePaths) {
        	try {
        		syncDir(truePath);
        	}
        	catch (Exception e) {
        		logger.error("Sync failed for dir "+truePath, e);
        	}
        }
    }

    private void syncDir(String filePath) throws Exception {

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

        logger.info("Synchronizing to archive: "+truePath);
    	syncPath(truePath, archivePath);
    	
        try {
            updateEntities(truePath, archivePath);
        }
        catch (Exception e) {
            logger.error("Error updating entities. Original file will NOT be deleted.",e);
            return;
        }
        
        // Looks like everything went well, so we can remove the original file
        
        File file = new File(truePath);
        try {
            FileUtils.forceDelete(file);
            logger.info("Deleted old path: "+truePath);
        }
        catch (Exception e) {
            logger.info("Error deleting old path "+truePath+": "+e.getMessage());
        }
    }
    
    private void syncPath(String sourcePath, String targetPath) throws Exception {
        new File(targetPath).getParentFile().mkdirs();
        StringBuffer script = new StringBuffer();
        script.append(SYNC_COMMAND+" "+sourcePath+"/ "+targetPath+"/; ");
        logger.info("Running: "+script);
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);
        if (0!=exitCode) {
            throw new ServiceException("Synchronization to archive failed with exitCode "+exitCode);
        }
    }
    
    private void updateEntities(String originalPath, String archivePath) throws ComputeException {
        computeBean.moveFileNodesToArchive(originalPath);
        logger.info("Updated all file nodes to use archived file: "+archivePath);
        entityBean.bulkUpdateEntityDataPrefix(originalPath, archivePath);
        logger.info("Updated all entities to use archived file: "+archivePath);
        
    }
}
