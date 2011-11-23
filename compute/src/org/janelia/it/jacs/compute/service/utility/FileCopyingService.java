package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Make copies of any number of files. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileCopyingService implements IService {

    public void execute(IProcessData processData) throws ServiceException {

    	try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            List<File> inputFiles = new ArrayList<File>();
            List<File> outputFiles = new ArrayList<File>();

        	logger.info("Executing FileCopyingService");
        	
            FileNode inputFileNode = (FileNode)processData.getItem("INPUT_FILE_NODE");
            if (inputFileNode==null) {
            	throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
            }

            FileNode outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
            if (outputFileNode==null) {
            	throw new ServiceException("Input parameter OUTPUT_FILE_NODE may not be null");
            }
            
        	int configIndex = 1;
        	while (true) {
        	    String inputFilename = (String)processData.getItem("INPUT_FILENAME_"+configIndex);	
        		if (inputFilename != null) {
            		String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);
                	File inputFile = new File(inputFileNode.getDirectoryPath(), inputFilename);
                	File outputFile = new File(outputFileNode.getDirectoryPath(), outputFilename);
                	logger.info("Input file: "+inputFilename);
                	logger.info("Output file: "+outputFilename);
                	inputFiles.add(inputFile);
                	outputFiles.add(outputFile);
        		}
        		else {
            		final String inputRegex = (String)processData.getItem("INPUT_FILENAME_REGEX_"+configIndex);
            		if (inputRegex == null) break;
            		String outputPattern = (String)processData.getItem("OUTPUT_FILENAME_PATTERN_"+configIndex);	

                	logger.info("Input regex: "+inputRegex);
                	logger.info("Output pattern: "+outputPattern);
                	
        			File inputDir = new File(inputFileNode.getDirectoryPath());
        			String[] filenames = inputDir.list(new FilenameFilter() {
    					@Override
    					public boolean accept(File dir, String name) {
    						return name.matches(inputRegex);
    					}
    				});
        			for(String foundInputFilename : filenames) {
        				String outputFilename = foundInputFilename.replaceAll(inputRegex, outputPattern);
                    	File inputFile = new File(inputDir, foundInputFilename);
                    	File outputFile = new File(inputDir, outputFilename);
    	            	inputFiles.add(inputFile);
                    	outputFiles.add(outputFile);
        			}
        		}
        		configIndex++;
        		if (configIndex>100) break;
        	}

        	StringBuffer script = new StringBuffer();
        	int i = 0;
        	for(File inputFile : inputFiles) {
        		File outputFile = outputFiles.get(i++);
        		script.append("cp "+inputFile.getAbsolutePath()+" "+outputFile.getAbsolutePath()+"\n");
        	}
        	
        	File scriptFile = new File(outputFileNode.getFilePath("metadata"),"copyMetadata.sh");
        	FileUtils.writeStringToFile(scriptFile, script.toString());
        	
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine("sh "+scriptFile.getAbsolutePath(), true, 60);

            if (0!=exitCode) {
            	throw new ServiceException("FileCopyingService failed with exitCode "+exitCode);
            }
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running FileCopyingService:" + e.getMessage(), e);
        }
    	
    }
}
