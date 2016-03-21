package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

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
public class CopyLsmsFromArchiveService extends AbstractDomainService {

	public void execute() throws Exception {

        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (!(bulkMergeParamObj instanceof List)) {
        	throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
    	
    	FileNode resultNode = ProcessDataHelper.getResultFileNode(processData);
    	
    	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;

    	List<MergedLsmPair> newPairs = new ArrayList<MergedLsmPair>();
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
        	String newPath1 = copyFile(mergedLsmPair.getLsmFilepath1(), resultNode);
        	String newPath2 = copyFile(mergedLsmPair.getLsmFilepath2(), resultNode);
        	newPairs.add(mergedLsmPair.getMovedLsmPair(newPath1, newPath2));
        }
        
        processData.putItem("BULK_MERGE_PARAMETERS",newPairs);
    }
	
	private String copyFile(String sourceFile, FileNode targetFileNode) throws Exception {
		if (sourceFile==null) return null;
    	File file = new File(sourceFile);
    	String targetFile = targetFileNode.getFilePath(file.getName());
    	logger.info("Copying from archive: "+sourceFile);
    	FileUtil.copyFileUsingSystemCall(sourceFile, targetFile);
    	return targetFile;
	}
}
