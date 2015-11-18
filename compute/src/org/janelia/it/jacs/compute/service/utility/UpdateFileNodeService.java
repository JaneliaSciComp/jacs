package org.janelia.it.jacs.compute.service.utility;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;

/**
 * Update file nodes and entities with new file paths.
 *   
 * Input variables:
 *   INPUT_FILE_PATHS - original paths
 *   OUTPUT_FILE_PATHS - new updated paths
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpdateFileNodeService extends AbstractEntityService {

	protected Logger logger = Logger.getLogger(UpdateFileNodeService.class);
	
    public void execute() throws Exception {

        List<String> inputPaths = (List<String>)processData.getItem("INPUT_FILE_PATHS");
        if (inputPaths==null) {
    		throw new ServiceException("INPUT_FILE_PATHS may not be null");
        }

        List<String> outputPaths = (List<String>)processData.getItem("OUTPUT_FILE_PATHS");
        if (outputPaths==null) {
            throw new ServiceException("OUTPUT_FILE_PATHS may not be null");
        }

        LinkedList<String> outputs = new LinkedList<String>(outputPaths);
        for(String inputPath : inputPaths) {
            String outputPath = outputs.remove();
            updateEntities(inputPath, outputPath);
        }
    }
    
    private void updateEntities(String originalPath, String newPath) throws ComputeException {
        int updatedNodes = computeBean.moveFileNodesToArchive(originalPath);
        logger.info("Updated "+updatedNodes+" file nodes to use new path: "+newPath);
        int updatedDatas = entityBean.bulkUpdateEntityDataPrefix(originalPath, newPath);
        logger.info("Updated "+updatedDatas+" entity data values to use new path: "+newPath);
    }
}
