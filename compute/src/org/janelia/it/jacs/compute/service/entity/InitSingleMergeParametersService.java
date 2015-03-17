package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;

/**
 * Given BULK_MERGE_PARAMETER which contain only a single LSM pair, put that LSM pair's merged filename into
 * the STITCHED_FILENAME variable.
 * 
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 * Output:
 *   STITCHED_FILENAME
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSingleMergeParametersService implements IService {

	public void execute(IProcessData processData) throws ServiceException {

        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }
        
        if (bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        	if (mergedLsmPairs.size() != 1) {
        		throw new ServiceException("BULK_MERGE_PARAMETERS must contain exactly one merged pair");
        	}
        	processData.putItem("STITCHED_FILENAME", mergedLsmPairs.get(0).getMergedFilepath());
        }
        else {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
            
    }
}
