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
    	
        String stitchedFile = (String)processData.getItem("STITCHED_FILENAME");
    	
    	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
    	for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

    		File file = new File(mergedLsmPair.getMergedFilepath());
    		if (file.getAbsolutePath().equals(stitchedFile)) continue; // never delete the "stitched" file
    		
    		File symlink = new File(mergedLsmPair.getMergedFilepath().replace("merge", "group"));
    		if (symlink.exists()) {
	    		logger.info("Cleaning up symlink to merged tile: "+symlink.getAbsolutePath());
	    		FileUtils.forceDelete(symlink);
    		}
    		
    		logger.info("Cleaning up merged tile: "+file.getAbsolutePath());
    		FileUtils.forceDelete(file);
    	}
    }
}
