package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;

import java.util.ArrayList;
import java.util.List;

/**
 * Given BULK_MERGE_PARAMETER which contain only a single LSM pair, put that LSM pair's merged filename into
 * the STITCHED_FILENAME variable.
 * 
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 * OUTPUT:
 *   STITCHED_FILENAME
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitMultiMergeParametersService implements IService {

	public void execute(IProcessData processData) throws ServiceException {
        try {

            Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
            if (bulkMergeParamObj==null) {
            	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
            }
            
            if (bulkMergeParamObj instanceof List) {
            	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
                List<String> tileFilePaths = new ArrayList<String>();
                for (MergedLsmPair lsmPair : mergedLsmPairs) {
                    tileFilePaths.add(lsmPair.getMergedFilepath());
                }
                processData.putItem("TILE_FILENAMES", tileFilePaths);
            }

            // The sample needs at least one file to use as the default
            List<String> stackFilenames = (List<String>) processData.getItem("STACK_FILENAMES");
            if (null!=stackFilenames && stackFilenames.size()>=1) {
                processData.putItem("STACK_FILENAME", stackFilenames.get(0));
            }
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
