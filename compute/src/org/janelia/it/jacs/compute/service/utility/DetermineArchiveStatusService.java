package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Determine if the given files need to be de-archived.
 *   
 * Input variables:
 *   FILE_PATH/FILE_PATHS - alternative variable for providing a list of FILE_PATH
 *   TARGET_FILE_NODE - target file node if de-archival has to be run
 *   
 * Output variables:  
 *   RUN_DEARCHIVAL - dearchive the source file paths into the target paths
 *   SOURCE_FILE_PATHS - FILE_PATHS, or an array containing FILE_PATH
 *   TARGET_FILE_PATHS - the place where the FILE_PATHS can be accessed
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DetermineArchiveStatusService extends AbstractEntityService {

	protected Logger logger = Logger.getLogger(DetermineArchiveStatusService.class);
	
	protected static final String ARCHIVE_PREFIX = "/archive";
    
    public void execute() throws Exception {

        List<String> filePaths = (List<String>)processData.getItem("FILE_PATHS");
        if (filePaths==null) {
        	String truePath = (String)processData.getItem("FILE_PATH");
        	if (truePath==null) {
        		throw new ServiceException("Both input parameters FILE_PATH and FILE_PATHS may not be null");	
        	}
        	filePaths = new ArrayList<String>();
        	filePaths.add(truePath);
        }

        FileNode targetFileNode = (FileNode)processData.getItem("TARGET_FILE_NODE");
        List<String> targetFilePaths = new ArrayList<String>();

        boolean runDearchival = false;
        for(String filePath : filePaths) {
            String targetFilePath = filePath;
            if (filePath.startsWith(ARCHIVE_PREFIX)) {
                File file = new File(filePath);
                targetFilePath = new File(targetFileNode.getDirectoryPath(), file.getName()).getAbsolutePath();
                runDearchival = true;
            }
            targetFilePaths.add(targetFilePath);
        }
        
        logger.info("Putting "+runDearchival+" into RUN_DEARCHIVAL");
        processData.putItem("RUN_DEARCHIVAL", runDearchival);

        logger.info("Putting "+filePaths.size()+" paths into SOURCE_FILE_PATHS");
        processData.putItem("SOURCE_FILE_PATHS", filePaths);

        logger.info("Putting "+targetFilePaths.size()+" paths into TARGET_FILE_PATHS");
        processData.putItem("TARGET_FILE_PATHS", targetFilePaths);
    }
}
