package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Cleans up the merged tiles to save on disk space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanUpMergedTilesService extends AbstractEntityService {
	
    public void execute() throws Exception {

        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }
        
        if (!(bulkMergeParamObj instanceof List)) {
        	throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
    	
    	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
    	for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
    		File file = new File(mergedLsmPair.getMergedFilepath());
    		FileUtils.forceDelete(file);
    	}
    }
}
