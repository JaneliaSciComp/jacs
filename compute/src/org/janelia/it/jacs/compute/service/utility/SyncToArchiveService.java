package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Move a file or a directory tree over to the archive. Before using this service, consider relying on
 * SyncSampleToArchive.process to move all files instead.
 *   
 * Input variables:
 *   FILE_PATH - path in JacsData.Dir.Linux to move or sync to corresponding location in JacsData.Dir.Archive.Linux
 *   FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   
 * This service can also be called directly, with the execute(filepath) method. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncToArchiveService extends AbstractDomainService {

	protected Logger logger = Logger.getLogger(SyncToArchiveService.class);
	
	private static final int TIME_OUT_SECS = 3600; // 1 hour
	
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
        syncPaths(truePaths);
    }

    /**
     * Alternative execution method which does not run within the launcher framework, or require ProcessData.
     */
    public void execute(String filepath) throws ServiceException {
        try {
            this.logger = Logger.getLogger(this.getClass());
            this.contextLogger = new ContextLogger(logger);
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.domainDao = DomainDAOManager.getInstance().getDao();
            List<String> paths = new ArrayList<String>();
            paths.add(filepath);
            syncPaths(paths);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    /**
     * Alternative execution method which does not run within the launcher framework, or require ProcessData.
     */
    public void execute(Collection<String> filepaths) throws ServiceException {
        try {
            this.logger = Logger.getLogger(this.getClass());
            this.contextLogger = new ContextLogger(logger);
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.domainDao = DomainDAOManager.getInstance().getDao();
            syncPaths(filepaths);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void syncPaths(Collection<String> filepaths) {

        logger.info("Synchronizing "+filepaths.size()+" paths to archive");

        long start = System.currentTimeMillis();
        for(String truePath : filepaths) {
            try {
                syncDir(truePath);
            }
            catch (Exception e) {
                logger.error("Sync failed for dir "+truePath, e);
            }
        }

        long end = System.currentTimeMillis();
        logger.info("Total synchronization of "+filepaths.size()+" paths took "+(end-start)+"ms");
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
        long start = System.currentTimeMillis();
    	syncPath(truePath, archivePath);
    	long end = System.currentTimeMillis();
    	logger.info("Synchronization of "+truePath+" took "+(end-start)+"ms");
    	
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
        // The forward slash at the end of both paths is critical, as it tells rsync to mirror the directory structure.
        // Without the slash, it may create an additional level at the target. 
        script.append(SYNC_COMMAND+" "+sourcePath+"/ "+targetPath+"/; ");
        logger.info("Running: "+script);
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, TIME_OUT_SECS);
        if (0!=exitCode) {
            throw new ServiceException("Synchronization to archive failed with exitCode "+exitCode);
        }
    }
    
    private void updateEntities(String originalPath, String archivePath) throws ComputeException {
        int updatedNodes = computeBean.moveFileNodesToArchive(originalPath);
        logger.info("Updated "+updatedNodes+" file nodes to use archived file: "+archivePath);
        int updatedDatas = domainDao.bulkUpdatePathPrefix(originalPath, archivePath);
        logger.info("Updated "+updatedDatas+" entity data values to use archived file: "+archivePath);
    }
}
