package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Group a bunch of merged files into stitchable groups. Put the largest group back into play. 
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DStitchGroupingService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "groupConfiguration.";
    private File groupedFile;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "group";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        FileNode inputFileNode = (FileNode)processData.getItem("INPUT_FILE_NODE");
        if (inputFileNode==null) {
        	throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
        }
        
        groupedFile = new File(resultFileNode.getDirectoryPath(),"igroups.txt");
        
        writeInstanceFiles();
        setJobIncrementStop(1);
        
    	createShellScript(writer, inputFileNode.getDirectoryPath(), groupedFile.getAbsolutePath());
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }

    /**
     * Write the shell script that runs the grouper on the merged files.
     * @param writer
     * @param mergedFilepath
     * @param groupedFilepath
     * @throws Exception
     */
    private void createShellScript(FileWriter writer, String mergedFilepath, String groupedFilepath) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedGrouperCommand(mergedFilepath, groupedFilepath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 8 slots on 96 GB node. Greed.
    	jt.setNativeSpecification("-pe batch 8 -l mem96=true");
    	return jt;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
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
    		throw new MissingDataException("Grouping core dumped for "+resultFileNode.getDirectoryPath());
    	}
    	
        List<List<String>> groups = new ArrayList<List<String>>();
        List<String> currGroup = new ArrayList<String>();
        	
        try {
            BufferedReader reader = new BufferedReader(new FileReader(groupedFile));
            String line;
            while((line = reader.readLine()) != null) {
            	line = line.trim();
            	if (line.startsWith("# tiled image group")) {
            		if (!currGroup.isEmpty()) {
            			groups.add(currGroup);
            			currGroup = new ArrayList<String>();
            		}
            	}
            	else if ("".equals(line)) {
            		// skip blank line
            	}
            	else {
            		currGroup.add(line);
            	}
            }
            if (!currGroup.isEmpty()) groups.add(currGroup);
          
        }
        catch (FileNotFoundException e) {
    		throw new MissingDataException("Grouped output file not found at "+groupedFile.getAbsolutePath(), e);
        }
        catch (IOException e) {
    		throw new MissingDataException("Error reading grouped output file at "+groupedFile.getAbsolutePath(), e);
        }
    	
        int maxSizeIndex = 0;
        int maxSize = 0;
        
        for(int i=0; i<groups.size(); i++) {
        	int s = groups.get(i).size();
        	if (s>maxSize) {
        		maxSize = s;
        		maxSizeIndex = i;
        	}
        }
        
        logger.info("Grouper found "+groups.size()+" groups: "+groupedFile.getAbsolutePath());
        logger.info("Largest group is: "+maxSizeIndex);
        
        List<String> maxGroup = groups.get(maxSizeIndex);
        
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj!=null && bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        	List<MergedLsmPair> newMergedLsmPairs = new ArrayList<MergedLsmPair>();
            	
        	for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
        		for(String filename : maxGroup) {
        			if (mergedLsmPair.getMergedFilepath().equals(filename)) {
        				// We want this pair, since its part of the max group
        				newMergedLsmPairs.add(mergedLsmPair);
        			}
        		}
        	}

            try {
	        	for(MergedLsmPair mergedLsmPair : newMergedLsmPairs) {
	        		File mergedFile = new File(mergedLsmPair.getMergedFilepath());
	        		File mergedFileLink = new File(resultFileNode.getDirectoryPath(), mergedFile.getName());
	                logger.info("Largest group contains: "+mergedFile);
                    String cmd = "ln -s "+mergedFile.getAbsolutePath()+" "+mergedFileLink.getAbsolutePath();
            		String[] args = cmd.split("\\s+");
                    StringBuffer stdout = new StringBuffer();
                    StringBuffer stderr = new StringBuffer();
                    SystemCall call = new SystemCall(stdout, stderr);
                	int exitCode = call.emulateCommandLine(args, null, null, 3600);	
                	if (exitCode!=0) throw new Exception("Could not create symlink to merged file");
	        	}
            }
            catch (Exception e) {
            	throw new MissingDataException("Error creating merged file symlinks");
            }
            
        	
            // Replace the pairs with only the pairs in the largest group
            processData.putItem("BULK_MERGE_PARAMETERS", newMergedLsmPairs);
        }
        else {
        	throw new MissingDataException("Missing BULK_MERGE_PARAMETERS in process data");
        }
    }
    
}
