package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Delete any number of files. Does an "rm -rf" on any input file names or file name patterns, so be careful!
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileCleanupService implements IService {

    public void execute(IProcessData processData) throws ServiceException {

    	try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            List<File> targetFiles = new ArrayList<File>();

        	logger.info("Executing FileCleanupService");
        	
            FileNode targetFileNode = (FileNode)processData.getItem("TARGET_FILE_NODE");
            if (targetFileNode==null) {
            	throw new ServiceException("Input parameter TARGET_FILE_NODE may not be null");
            }

            int configIndex = 1;
        	while (true) {
        	    String inputFilename = (String)processData.getItem("TARGET_FILENAME_"+configIndex);	
        		if (inputFilename != null) {
                	File targetFile = new File(targetFileNode.getDirectoryPath(), inputFilename);
                	logger.info("Delete file: "+inputFilename);
                	targetFiles.add(targetFile);
        		}
        		else {
            		final String targetRegex = (String)processData.getItem("TARGET_FILENAME_REGEX_"+configIndex);
            		if (targetRegex == null) break;	

                	logger.info("Delete files matching: "+targetRegex);
                	
        			File inputDir = new File(targetFileNode.getDirectoryPath());
        			String[] filenames = inputDir.list(new FilenameFilter() {
    					@Override
    					public boolean accept(File dir, String name) {
    						return name.matches(targetRegex);
    					}
    				});
        			for(String foundInputFilename : filenames) {
                    	File inputFile = new File(inputDir, foundInputFilename);
                    	targetFiles.add(inputFile);
        			}
        		}
        		configIndex++;
        		if (configIndex>100) break;
        	}
        	
        	StringBuffer script = new StringBuffer();
        	for(File targetFile : targetFiles) {
        		script.append("rm ");
        		if (targetFile.isDirectory()) {
        			script.append("-rf ");
        		}
        		script.append(targetFile.getAbsolutePath());
				script.append("\n");
        	}
        	
        	File scriptFile = new File(targetFileNode.getDirectoryPath(), "cleanup.sh");
        	FileUtils.writeStringToFile(scriptFile, script.toString());
        	
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine("sh "+scriptFile.getAbsolutePath(), true, 60);

            if (0!=exitCode) {
            	throw new ServiceException("FileCleanupService failed with exitCode "+exitCode);
            }
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running FileCleanupService:" + e.getMessage(), e);
        }
    	
    }
}
