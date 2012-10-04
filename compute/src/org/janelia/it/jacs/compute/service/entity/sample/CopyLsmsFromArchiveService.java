package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Copy archived LSMs over to a temporary directory in high performance storage.
 *   
 * Input variables:
 *   BULK_MERGE_PARAMETERS - LSM paths
 *   
 * Output variables:
 *   BULK_MERGE_PARAMETERS - updated LSM paths
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CopyLsmsFromArchiveService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "copyConfiguration.";
    protected static final String COPY_COMMAND = "cp "; 
    
    private Map<String,String> fileMap = new HashMap<String,String>();
    
    @Override
    protected String getGridServicePrefixName() {
        return "copy";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;

            int configIndex = 1;
            for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            	writeInstanceFiles(mergedLsmPair.getLsmFilepath1(), configIndex++);
            	if (mergedLsmPair.getLsmFilepath2()!=null) {
            		writeInstanceFiles(mergedLsmPair.getLsmFilepath2(), configIndex++);
            	}
            }
            
        	createShellScript(writer);
            setJobIncrementStop(configIndex-1);
        }
        else {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be an ArrayList<MergedLsmPair>");
        }
    }

    private void writeInstanceFiles(String filepath, int configIndex) throws Exception {
    	
    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(filepath);
    	String tempFile = parentNode.getFilePath(file.getName());
    	fileMap.put(filepath, tempFile);
    	
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(filepath + "\n");
            fw.write(tempFile + "\n");
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
        script.append("read INPUT_FILENAME\n");
        script.append("read OUTPUT_FILENAME\n");
        script.append(COPY_COMMAND+" $INPUT_FILENAME $OUTPUT_FILENAME\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {    	
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// May need to access /archive, so we need limit 50.
    	// Reserve 1 slot on a node, we're not worried about memory.
    	jt.setNativeSpecification("-pe batch 1 -l limit50=1 ");
    	return jt;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	List<MergedLsmPair> tmpLsmPairs = new ArrayList<MergedLsmPair>();
    	
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
    	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
        	String tempLsm1 = fileMap.get(mergedLsmPair.getLsmFilepath1());
        	String tempLsm2 = fileMap.get(mergedLsmPair.getLsmFilepath2());
        	
        	File outputFile1 = new File(tempLsm1);
        	if (!outputFile1.exists()) {
        		throw new MissingDataException("Missing output "+outputFile1.getAbsolutePath());
        	}
        	if (tempLsm2!=null) {
	        	File outputFile2 = new File(tempLsm2);
	        	if (!outputFile2.exists()) {
	        		throw new MissingDataException("Missing output "+outputFile2.getAbsolutePath());
	        	}
        	}
        	
        	tmpLsmPairs.add(new MergedLsmPair(tempLsm1, tempLsm2, mergedLsmPair.getMergedFilepath()));
        }
        
        processData.putItem("BULK_MERGE_PARAMETERS", tmpLsmPairs);
	}
}
