package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;

/**
 * Given SAMPLE_AREA which contains only a single LSM pair, 
 * put that LSM pair's merged filename the SAMPLE_AREA.
 * 
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   SAMPLE_AREA - the sample area object
 * Output:
 *   SAMPLE_AREA
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSingleMergeParametersService implements IService {

	public void execute(IProcessData processData) throws ServiceException {
	    
        AnatomicalArea sampleArea = (AnatomicalArea) processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
        	throw new ServiceException("Input parameter SAMPLE_AREA may not be null");
        }
        
        List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();
    	if (mergedLsmPairs.size() != 1) {
    		throw new ServiceException("SAMPLE_AREA must contain exactly one merged pair");
    	}
    	String stitchedFilename = mergedLsmPairs.get(0).getMergedFilepath();
    	sampleArea.setStitchedFilename(stitchedFilename);
    }
}
