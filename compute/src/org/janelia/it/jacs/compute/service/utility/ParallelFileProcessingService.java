package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Do some operation on any number of files in parallel. Subclasses should override getGridServicePrefixName,
 * writeInstanceFile, and writeShellScript.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ParallelFileProcessingService extends SubmitDrmaaJobService {

    private List<File> inputFiles = new ArrayList<File>();
    private List<File> outputFiles = new ArrayList<File>();
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
        FileNode outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	throw new ServiceException("Input parameter OUTPUT_FILE_NODE may not be null");
        }
        
    	int configIndex = 1;
    	while (true) {

            // First determine input file for this index

            String inputFilenameKey="INPUT_FILENAME_"+configIndex;
            String inputPathKey="INPUT_PATH_"+configIndex;
            String inputRegexKey="INPUT_FILENAME_REGEX_"+configIndex;

       	    String inputFilename = (String)processData.getItem(inputFilenameKey);
            String inputPath = (String)processData.getItem(inputPathKey);
            final String inputRegex = (String)processData.getItem(inputRegexKey);

//            logger.info(inputFilenameKey+" = "+inputFilename);
//            logger.info(inputPathKey+" = "+inputPath);
//            logger.info(inputRegexKey+" = "+inputRegex);

            if ( (inputFilename==null && inputPath==null && inputRegex==null) || configIndex>100 )
                break;

            if (inputFilename!=null) {
                File inputFile=new File(outputFileNode.getDirectoryPath(), inputFilename);
                inputFiles.add(inputFile);
            } else if (inputPath!=null) {
                File inputFile=new File(inputPath);
                inputFiles.add(inputFile);
            } else if (inputRegex!=null) {
                // We do the output here also in this case, since for regex they must be coordinated
                String outputPattern = (String)processData.getItem("OUTPUT_FILENAME_PATTERN_"+configIndex);
    			File inputDir = new File(outputFileNode.getDirectoryPath());
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

            // Next, we do outputs
            String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);
            String outputPath = (String)processData.getItem("OUTPUT_PATH_"+configIndex);

            if (outputFilename!=null) {
                File outputFile=new File(outputFileNode.getDirectoryPath(), outputFilename);
                outputFiles.add(outputFile);
            } else if (outputPath!=null) {
                File outputFile=new File(outputPath);
                outputFiles.add(outputFile);
            }
            configIndex++;
    	}
//        logger.info("ParallelFileProcessingService  init()  inputFile count="+inputFiles.size()+ "  outputFile count="+outputFiles.size());
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

    	int i = 0;
    	int configIndex = 1;
    	for(File inputFile : inputFiles) {
    		File outputFile = outputFiles.get(i++);
    		writeInstanceFiles(inputFile, outputFile, configIndex++);
    	}
    	writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    protected void writeInstanceFiles(File inputFile, File outputFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, inputFile, outputFile, configIndex);
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }
    
    /**
     * Write out the parameters passed to a given instance in the job array. The default implementation writes 
     * the input file path and the output file path.
     * @param fw
     * @param inputFile
     * @param outputFile
     * @param configIndex
     * @throws IOException
     */
    protected void writeInstanceFile(FileWriter fw, File inputFile, File outputFile, int configIndex) throws IOException {
        fw.write(inputFile.getAbsolutePath() + "\n");
        fw.write(outputFile.getAbsolutePath() + "\n");
    }

    /**
     * Write the shell script used for all instances in the job array. The default implementation read INPUT_FILENAME
     * and OUTPUT_FILENAME.
     */
    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILENAME\n");
        script.append("read OUTPUT_FILENAME\n");
        writer.write(script.toString());
    }

    /**
     * The default implementation tests for core dumps and ensures that all output files are accounted for.
     */
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException(getGridServicePrefixName()+" core dumped for "+resultFileNode.getDirectoryPath());
    	}

    	for(File outputFile : outputFiles) {
        	if (!outputFile.exists()) {
        		throw new MissingDataException(getGridServicePrefixName()+" missing output file: "+outputFile.getAbsolutePath());
        	}
    	}
	}
}
