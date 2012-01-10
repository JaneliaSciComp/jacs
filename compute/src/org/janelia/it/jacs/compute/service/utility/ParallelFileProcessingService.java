package org.janelia.it.jacs.compute.service.utility;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Do some operation on any number of files in parallel. Subclasses should override getGridServicePrefixName,
 * writeInstanceFile, and writeShellScript.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ParallelFileProcessingService extends SubmitDrmaaJobService {

	private static final boolean DEBUG = false;
    private static final int GLOBAL_CASE = -1;

    // These 2 vars are the core which must be populated for the service to run.
    // Additionally, a result node or id must be supplied for SubmitDrmaaJobService, which may
    // be the first ID in a result node list.
    private List<File> inputFiles = new ArrayList<File>();
    private List<File> outputFiles = new ArrayList<File>();

    private List<FileNode> outputFileNodes;

    private List<String> inputNameList = new ArrayList<String>();
    private List<String> outputNameList = new ArrayList<String>();
    private List<String> inputRegexList = new ArrayList<String>();
    private List<String> outputPatternList = new ArrayList<String>();
    private List<String> inputPathList = new ArrayList<String>();
    private List<String> outputPathList = new ArrayList<String>();
    
    protected void init(IProcessData processData) throws Exception {

        // First we make sure a single result file node is established for the drmaa run.
        // Note that the result node is handled independently of the output nodes.
        Long resultFileNodeId=0L;
        resultFileNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
        if (resultFileNode==null) {
            resultFileNodeId = (Long)processData.getLong("RESULT_FILE_NODE_ID");
        }
        if (resultFileNode==null && resultFileNodeId==null) {
            // We assume a list and simply use the first node from the list for drmaa
            List<String> resultNodeIdList = (List<String>)processData.getItem("RESULT_FILE_NODE_ID_LIST");
            if (resultNodeIdList!=null && resultNodeIdList.size()>0) {
                processData.putItem("RESULT_FILE_NODE_ID", new Long(resultNodeIdList.get(0).trim()));
            } else {
                List<FileNode> resultFileNodes = (List<FileNode>)processData.getItem("RESULT_FILE_NODE_LIST");
                if (resultFileNodes!=null && resultFileNodes.size()>0) {
                    processData.putItem("RESULT_FILE_NODE", resultFileNodes.get(0));
                }
            }
        }
        // Now it should be safe to call init() on SubmitDrmaaJobService
    	super.init(processData);

        // Next we establish the output list. There are multiple modes in which this service can be run:
        //
        //  1. The output file node is specified, and an indexed list of input and output filenames are specified,
        //      all of which are assumed to be in the 'output file node'. In this case, the service is applied
        //      to each pair in the indexed list, but all inputs and outputs are within the output filenode directory.
        //
        //  2. Similar to #1, except that regular expression is used to construct the list of inputs and outputs,
        //      by using REGEX and PATTERN tags.
        //
        //  3. Similar to #1, except that PATH is used rather than filename to specify input files. In this
        //      case, it is not assumed that the input is in the 'output file node', but in a totally independent
        //      location.
        //
        //  4. Similar to #2, except that a list both input paths and output file nodes is supplied. A single
        //      filename is expected to be given for output, then this output name is used for all outputs (presumably
        //      in each of the different output file node directories).


        // Next, setup output file node(s)
        FileNode outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        outputFileNodes = (List<FileNode>)processData.getItem("OUTPUT_FILE_NODE_LIST");
        if (outputFileNode==null && outputFileNodes==null) {
        	throw new ServiceException("Input parameter OUTPUT_FILE_NODE and OUTPUT_FILE_NODE_LIST may not both be null");
        }
        if (outputFileNode!=null && outputFileNodes!=null) {
            throw new ServiceException("Both OUTPUT_FILE_NODE and OUTPUT_FILE_NODE_LIST cannot be specified - one or the other");
        }
        if (outputFileNodes==null) {
            outputFileNodes=new ArrayList<FileNode>();
            outputFileNodes.add(outputFileNode);
        }

        final String inputNameGlobal=(String)processData.getItem("INPUT_FILENAME");
        final String inputRegexGlobal=(String)processData.getItem("INPUT_FILENAME_REGEX");
        final String outputNameGlobal=(String)processData.getItem("OUTPUT_FILENAME");
        final String outputPatternGlobal=(String)processData.getItem("OUTPUT_FILENAME_PATTERN");
        final List<String> inputPathListGlobal=(List<String>)processData.getItem("INPUT_PATH_LIST");
        final List<String> outputPathListGlobal=(List<String>)processData.getItem("OUTPUT_PATH_LIST");

        // Next, configure input/output arguments
        int argumentIndex=1;
        while (true) {

            String inputFilenameKey="INPUT_FILENAME_"+argumentIndex;
            String inputPathKey="INPUT_PATH_"+argumentIndex;
            String inputRegexKey="INPUT_FILENAME_REGEX_"+argumentIndex;
            String outputFilenameKey="OUTPUT_FILENAME_"+argumentIndex;
            String outputPathKey="OUTPUT_PATH_"+argumentIndex;
            String outputPatternKey="OUTPUT_FILENAME_PATTERN_"+argumentIndex;

            String inputFilename=(String)processData.getItem(inputFilenameKey);
            String inputPath=(String)processData.getItem(inputPathKey);
            String inputRegex=(String)processData.getItem(inputRegexKey);
            String outputFilename=(String)processData.getItem(outputFilenameKey);
            String outputPath=(String)processData.getItem(outputPathKey);
            String outputPattern=(String)processData.getItem(outputPatternKey);

            int hits=0;

            if (inputFilename!=null) { inputNameList.add(inputFilename); hits++; }
            if (inputPath!=null) { inputPathList.add(inputPath); hits++; }
            if (inputRegex!=null) { inputRegexList.add(inputRegex); hits++; }
            if (outputFilename!=null) { outputNameList.add(outputFilename); hits++; }
            if (outputPath!=null) { outputPathList.add(outputPath); hits++; }
            if (outputPattern!=null) { outputPatternList.add(outputPattern); hits++; }

            if (hits==0 || argumentIndex>=100) break;

            argumentIndex++;
        }

        boolean inputPathListAlreadySpecified=false;
        boolean outputPathListAlreadySpecified=false;
        if (inputPathListGlobal!=null && inputPathListGlobal.size()==outputFileNodes.size()) {
            for (String filepath : inputPathListGlobal) {
                inputFiles.add(new File(filepath));
            }
            inputPathListAlreadySpecified=true;
        }
        if (outputPathListGlobal!=null && outputPathListGlobal.size()==outputFileNodes.size()) {
            for (String filepath : outputPathListGlobal) {
                outputFiles.add(new File(filepath));
            }
            outputPathListAlreadySpecified=true;
        }
        for (FileNode outputNode: outputFileNodes) {
        	if (DEBUG) logger.info("Process output file node: "+outputNode.getDirectoryPath());
            // -1 is a mechanism for us to handle the global case
            for (int argIndex=GLOBAL_CASE; argIndex<argumentIndex-1; argIndex++) {
                File inputFile=null;

            	if (DEBUG) logger.info("  argIndex: "+argIndex);
            	if (DEBUG) logger.info("    Inputs...");
                // First do input, then output
                if (!inputPathListAlreadySpecified) {
                	
                	if (argIndex==GLOBAL_CASE) {
                        if (inputNameGlobal!=null) {
                        	if (DEBUG) logger.info("      Global Case 1: inputNameGlobal:"+inputNameGlobal);
                            inputFile=new File(outputNode.getDirectoryPath(), inputNameGlobal);
                        } 
                        else if (inputRegexGlobal!=null && outputPatternGlobal!=null) {
                        	if (DEBUG) logger.info("      Global Case 2: inputRegexGlobal:"+inputRegexGlobal+", outputPatternGlobal:"+outputPatternGlobal);
                            File inputDir=new File(outputNode.getDirectoryPath());
                            String[] inputFilenames=getFilesMatching(inputDir, inputRegexGlobal);
                            for (String inputFilename : inputFilenames) {
                                String outputFilename = inputFilename.replaceAll(inputRegexGlobal, outputPatternGlobal);
                                inputFiles.add(new File(inputDir, inputFilename));
                                outputFiles.add(new File(inputDir, outputFilename));
                            }
                        } 
                	}
                	else {
                        if (!inputNameList.isEmpty()) {
                        	if (DEBUG) logger.info("      Case 1: inputNameList:"+inputNameList.size());
                            inputFile=new File(outputNode.getDirectoryPath(), inputNameList.get(argIndex));
                        } 
                        else if (!inputPathList.isEmpty()) {
                        	if (DEBUG) logger.info("      Case 2: inputPathList:"+inputPathList.size());
                            inputFile=new File(inputPathList.get(argIndex));
                        }
                        else if (!inputRegexList.isEmpty() && !outputPatternList.isEmpty()) {
                        	if (DEBUG) logger.info("      Case 3: inputRegexList:"+inputRegexList.size());
                            File inputDir=new File(outputNode.getDirectoryPath());
                            String regex = inputRegexList.get(argIndex);
                            String pattern = outputPatternList.get(argIndex);
                            String[] inputFilenames = getFilesMatching(inputDir, regex);
                            for (String inputFilename : inputFilenames) {
                                String outputFilename = inputFilename.replaceAll(regex, pattern);
                                inputFiles.add(new File(inputDir, inputFilename));
                                outputFiles.add(new File(inputDir, outputFilename));
                            }
                        }
                	}
                	
                    if (inputFile!=null) {
                    	inputFiles.add(inputFile);
                    }
                }

                // Now output
            	if (DEBUG) logger.info("    Outputs...");
                if (!outputPathListAlreadySpecified) {
                    File outputFile=null;
                	
                	if (argIndex==GLOBAL_CASE) {
                        if (outputNameGlobal!=null) {
                        	if (DEBUG) logger.info("      Global Case 1: outputNameGlobal:"+outputNameGlobal);
                            outputFile=new File(outputNode.getDirectoryPath(), outputNameGlobal);
                        } 
                	}
                	else {
                        if (!outputNameList.isEmpty()) {
                        	if (DEBUG) logger.info("      Case 1: outputNameList:"+outputNameList.size());
                            outputFile=new File(outputNode.getDirectoryPath(), outputNameList.get(argIndex));
                        } 
                        else if (!outputPathList.isEmpty()) {
                        	if (DEBUG) logger.info("      Case 2: outputPathList:"+outputPathList.size());
                            outputFile=new File(outputPathList.get(argIndex));
                        }
                	}
                    
                    if (outputFile!=null) {
                    	outputFiles.add(outputFile);
                    }
            		
                }
                
            }
        }
        
        if (inputFiles.size()!=outputFiles.size()) {
            throw new Exception("Input and Output file counts must match");
        }
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

    private String[] getFilesMatching(File dir, final String regexPattern) {
    	return dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.matches(regexPattern);
            }
        });
    }
}
