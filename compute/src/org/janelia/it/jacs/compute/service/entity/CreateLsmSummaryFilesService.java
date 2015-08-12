package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Takes the bulk merge parameters and creates summary files for each one. 
 * The parameters should be included in the ProcessData:
 *   BULK_MERGE_PARAMETERS 
 *   RESULT_FILE_NODE
 *   OUTPUT_FILE_NODE
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateLsmSummaryFilesService extends AbstractEntityGridService {

	private static final int START_DISPLAY_PORT = 1990;
    private static final String CONFIG_PREFIX = "summaryConfiguration.";
    
    protected File outputDir;
    protected Logger logger;
    private List<File> inputFiles = new ArrayList<File>();
    private List<String> chanSpecs = new ArrayList<String>();
    
    protected void init() throws Exception {
    	
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());

    	FileNode outputNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
    	if (outputNode == null) {
    		throw new IllegalArgumentException("OUTPUT_FILE_NODE may not be null");
    	}
    	outputDir = new File(outputNode.getDirectoryPath());
    	
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
            throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (!(bulkMergeParamObj instanceof List)) {
            throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
        
        List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;

        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            inputFiles.add(new File(mergedLsmPair.getLsmFilepath1()));
            chanSpecs.add(getChanSpec(mergedLsmPair.getLsmEntityId1()));
            
            if (mergedLsmPair.getLsmFilepath2() != null) {
                inputFiles.add(new File(mergedLsmPair.getLsmFilepath2()));
                chanSpecs.add(getChanSpec(mergedLsmPair.getLsmEntityId2()));
            }
        }
    }

    private String getChanSpec(Long lsmEntityId) {
    	try {
			Entity lsm = entityBean.getEntityById(lsmEntityId);
			if (lsm!=null) {
                String chanSpec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                if (chanSpec==null) {
            		logger.error("Null channel specification for LSM "+lsmEntityId);
                }
                return chanSpec;
			}
    	}
    	catch (Exception e) {
    		logger.error("Error getting channel specification for LSM "+lsmEntityId,e);
    	}
		return null;
	}

	@Override
    protected String getGridServicePrefixName() {
        return "summary";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

    	int configIndex = 1;
    	for(File inputFile : inputFiles) {
    		writeInstanceFiles(inputFile, configIndex++);
    	}
    	createShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }
    
    private void writeInstanceFiles(File inputFile, int configIndex) throws Exception {
    	int randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        
        int i = configIndex-1;
        String chanSpec = chanSpecs.get(i);
    	String signalChannels = ChanSpecUtils.getSignalChannelIndexes(chanSpec);
    	String referenceChannel = ChanSpecUtils.getReferenceChannelIndexes(chanSpec);
        
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(outputDir.getAbsolutePath() + "\n");
            fw.write(inputFile.getAbsolutePath() + "\n");
            fw.write(signalChannels + "\n");
            fw.write(referenceChannel + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read INPUT_FILE\n");
        script.append("read SIGNAL_CHAN\n");
        script.append("read REF_CHAN\n");;
        script.append("read DISPLAY_PORT\n");
        script.append("cd "+outputDir.getAbsolutePath()).append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(NeuronSeparatorHelper.getSummaryCreatorCommands());
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 3;
    }
    
    @Override
    public void postProcess() throws MissingDataException {
        for(File inputFile : inputFiles) {
        	String prefix = inputFile.getName().replaceFirst(".lsm", "");
	    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, prefix);
	    	if (outputFiles.length < 1) {
	    		File outputFile = new File(outputDir, prefix);
	    		throw new MissingGridResultException(outputDir.getAbsolutePath(), "Missing summary output files: "+outputFile.getAbsolutePath()+"*");
	    	}
        }
    }
}
