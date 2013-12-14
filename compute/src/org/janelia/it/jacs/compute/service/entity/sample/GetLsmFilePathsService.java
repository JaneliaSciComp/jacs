package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Determine paths of archived LSMs and their temporary location in high performance storage.
 * If the LSMs are compressed using bzip2 or gzip, then they will be decompressed. 
 *   
 * Input variables:
 *   BULK_MERGE_PARAMETERS - LSM paths
 *   
 * Output variables:
 *   BULK_MERGE_PARAMETERS - updated LSM paths
 *   LSM_SOURCE_FILE_PATHS - source archive paths
 *   LSM_TARGET_FILE_PATHS - target non-archive paths
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetLsmFilePathsService implements IService {

    private static final String EXTENSION_BZIP2 = ".bz2";
    private static final String EXTENSION_GZIP = ".gz";
    
	protected Logger logger = Logger.getLogger(GetLsmFilePathsService.class);
	
	public void execute(IProcessData processData) throws ServiceException {
        try {

            Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
            if (bulkMergeParamObj==null) {
            	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
            }

            if (!(bulkMergeParamObj instanceof List)) {
            	throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
            }
        	
        	FileNode resultNode = ProcessDataHelper.getResultFileNode(processData);
        	
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        	List<String> sourceFilePaths = new ArrayList<String>();
        	List<String> targetFilePaths = new ArrayList<String>();
        	
        	List<MergedLsmPair> newPairs = new ArrayList<MergedLsmPair>();
            for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
                
                String newPath1 = null;
                if (mergedLsmPair.getLsmFilepath1()!=null) {
                    newPath1 = getTargetLocation(mergedLsmPair.getLsmFilepath1(), resultNode);
                	sourceFilePaths.add(mergedLsmPair.getLsmFilepath1());
                	targetFilePaths.add(newPath1);
                }
            	
                String newPath2 = null;
            	if (mergedLsmPair.getLsmFilepath2()!=null) {
            	    newPath2 = getTargetLocation(mergedLsmPair.getLsmFilepath2(), resultNode);
            	    sourceFilePaths.add(mergedLsmPair.getLsmFilepath2());
            	    targetFilePaths.add(newPath2);
            	}
            	
            	MergedLsmPair newPair = new MergedLsmPair(newPath1, newPath2, mergedLsmPair.getMergedFilepath(), mergedLsmPair.getTag());
            	newPairs.add(newPair);
            }
            
            processData.putItem("LSM_SOURCE_FILE_PATHS",sourceFilePaths);
            processData.putItem("LSM_TARGET_FILE_PATHS",targetFilePaths);
            processData.putItem("BULK_MERGE_PARAMETERS",newPairs);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
	
	private String getTargetLocation(String sourceFile, FileNode targetFileNode) throws Exception {
		if (sourceFile==null) return null;
    	File file = new File(sourceFile);
    	String targetFile = targetFileNode.getFilePath(file.getName());
    	if (targetFile.endsWith(EXTENSION_BZIP2)) {
    	    return targetFile.substring(0, targetFile.length()-EXTENSION_BZIP2.length());
    	}
        if (targetFile.endsWith(EXTENSION_GZIP)) {
            return targetFile.substring(0, targetFile.length()-EXTENSION_GZIP.length());
        }
    	return targetFile;
	}
}
