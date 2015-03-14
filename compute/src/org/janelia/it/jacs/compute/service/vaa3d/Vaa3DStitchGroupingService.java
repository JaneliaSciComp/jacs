package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Group a bunch of merged files into stitchable groups. Put the largest group back into play. 
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 *   REFERENCE_CHANNEL_INDEX (optional; defaults to 4) - the index of the reference channel in each image
 * Output:
 *   BULK_MERGE_PARAMETERS - modified to only include largest group
 *   SAMPLE_AREA - modified to only include largest group
 *   RUN_STITCH - set to false if there is only one tile in the largest group
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DStitchGroupingService extends AbstractEntityGridService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "groupConfiguration.";
    private File groupedFile;
    private int referenceChannelIndex = 4;
    
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

        String referenceChannelIndexStr = (String)processData.getItem("REFERENCE_CHANNEL");
        if (referenceChannelIndexStr!=null) {
        	referenceChannelIndex = Integer.parseInt(referenceChannelIndexStr)+1;	
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
        script.append(Vaa3DHelper.getFormattedGrouperCommand(referenceChannelIndex, mergedFilepath, groupedFilepath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 64;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Grouping core dumped for "+resultFileNode.getDirectoryPath());
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
    		throw new MissingGridResultException(file.getAbsolutePath(), "Grouped output file not found at "+groupedFile.getAbsolutePath(), e);
        }
        catch (IOException e) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Error reading grouped output file at "+groupedFile.getAbsolutePath(), e);
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
    	List<MergedLsmPair> newMergedLsmPairs = new ArrayList<MergedLsmPair>();
        
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj!=null && bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
            	
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
                    logger.info("    LSM1: "+mergedLsmPair.getLsmFilepath1());
                    if (mergedLsmPair.getLsmFilepath2()!=null) {
                    	logger.info("    LSM2: "+mergedLsmPair.getLsmFilepath2());
                    }
                    
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
            	throw new MissingGridResultException(file.getAbsolutePath(), "Error creating merged file symlinks");
            }
            
        	
            // Replace the pairs with only the pairs in the largest group
            processData.putItem("BULK_MERGE_PARAMETERS", newMergedLsmPairs);
        }
        else {
        	throw new MissingDataException("Missing BULK_MERGE_PARAMETERS in process data");
        }
        
        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
            throw new IllegalArgumentException("SAMPLE_AREA may not be null");
        }
        
        logger.debug("Validating sample area tiles");

        outerLoop: for(Iterator<Long> iterator = sampleArea.getTileIds().iterator(); iterator.hasNext(); ) {

        	Long tileEntityId = iterator.next();
        	Entity tileEntity = null;
        	try {
        		tileEntity = entityBean.getEntityAndChildren(tileEntityId);
        	}
        	catch (ComputeException e) {
        		logger.error("Error getting tile "+tileEntityId+". Skipping validation.", e);
        		continue;
        	}
            logger.debug("Validating '"+tileEntity.getName()+"' tile");

            for(Entity lsmStack : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {

                String lsmFilename = lsmStack.getName();
                logger.debug("Looking for "+lsmFilename);
                
                boolean found = false;
                for(MergedLsmPair mergedLsmPair : newMergedLsmPairs) {
                	if (mergedLsmPair.getOriginalFilepath1().endsWith(lsmFilename) || (mergedLsmPair.getOriginalFilepath2()!=null && mergedLsmPair.getOriginalFilepath2().endsWith(lsmFilename))) {
                		found = true;
                	}
                }
                
                if (!found) {
                    logger.info("LSM "+lsmFilename+" is not in the largest group for '"+sampleArea.getName()+"'. Removing tile "+tileEntityId+" from sample area.");
                    iterator.remove();
            		continue outerLoop;
                }
            }
        }
        
        processData.putItem("SAMPLE_AREA", sampleArea);
        

    	if (newMergedLsmPairs.size()==1) {
    		// No stitching to run
    		processData.putItem("RUN_STITCH", Boolean.FALSE);
    	}
    }
    
}
