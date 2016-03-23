package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Determine paths of archived LSMs and their temporary location in high performance storage.
 * If the LSMs are compressed using bzip2 or gzip, then they will be decompressed. 
 *   
 * Input variables:
 *   SAMPLE_AREA - object containing a list of MergedLsmPair
 *   
 * Output variables:
 *   SAMPLE_AREA - object containing an updated list of MergedLsmPair
 *   LSM_SOURCE_FILE_PATHS - source archive paths
 *   LSM_TARGET_FILE_PATHS - target non-archive paths
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetLsmFilePathsService extends AbstractDomainService {
    
	protected Logger logger = Logger.getLogger(GetLsmFilePathsService.class);
	
	public void execute() throws Exception {

        FileNode resultNode = ProcessDataHelper.getResultFileNode(processData);

        AnatomicalArea sampleArea = (AnatomicalArea) processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
            throw new ServiceException("Input parameter SAMPLE_AREA may not be null");
        }
    	
    	List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();
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
        	
        	newPairs.add(mergedLsmPair.getMovedLsmPair(newPath1, newPath2));
        }
        
        sampleArea.setMergedLsmPairs(newPairs);
        processData.putItem("LSM_SOURCE_FILE_PATHS",sourceFilePaths);
        processData.putItem("LSM_TARGET_FILE_PATHS",targetFilePaths);
    }
	
	private String getTargetLocation(String sourceFile, FileNode targetFileNode) throws Exception {
		if (sourceFile==null) return null;
    	File file = new File(sourceFile);
    	String targetFile = targetFileNode.getFilePath(file.getName());
    	return ArchiveUtils.getDecompressedFilepath(targetFile);
	}
}
