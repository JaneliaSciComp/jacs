package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.List;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
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
public class InitSingleMergeParametersService extends AbstractDomainService {

	public void execute() throws ServiceException {

        AnatomicalArea sampleArea = (AnatomicalArea) data.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
        	throw new ServiceException("Input parameter SAMPLE_AREA may not be null");
        }
        
        List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();
    	if (mergedLsmPairs.size() != 1) {
    		throw new ServiceException("SAMPLE_AREA must contain exactly one merged pair, but has "+mergedLsmPairs.size());
    	}
    	String stitchedFilename = mergedLsmPairs.get(0).getMergedFilepath();
    	sampleArea.setStitchedFilepath(stitchedFilename);
        data.putItem("SAMPLE_AREA", sampleArea);
    }
}
