package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for alignment results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentResultsDiscoveryService extends SupportingFilesDiscoveryService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_ALIGNMENT_RESULT);
        super.execute(processData);
    }

    @Override
    protected void processFolderForData(Entity alignmentResult) throws Exception {

        if (!alignmentResult.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
            throw new IllegalStateException("Expected Alignment Result as input");
        }

        super.processFolderForData(alignmentResult);
        
        Map<String,Entity> resultItemMap = new HashMap<String,Entity>();
        for(Entity resultItem : alignmentResult.getChildren()) {
            resultItemMap.put(resultItem.getName(), resultItem);
        }
        
        String consensusAlignmentSpace = null;
        Entity supportingFiles = EntityUtils.getSupportingData(alignmentResult);
        
        for(Entity textFile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_TEXT_FILE)) {
            logger.info("Got text file: "+textFile.getName());
            
            if (textFile.getName().endsWith(".properties")) {
                
                logger.info("Got properties file: "+textFile.getName());
                File propertiesFile = new File(textFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                Properties properties = new Properties();
                properties.load(new FileReader(propertiesFile));
                
                String filename = properties.getProperty("alignment.stack.filename");
                Entity resultItem = resultItemMap.get(filename);
                
                if (resultItem==null) {
                    logger.warn("Could not find result item with filename: "+filename);
                    continue;
                }
                
                String alignmentSpace = properties.getProperty("alignment.space.name");
                String opticalRes = properties.getProperty("alignment.resolution.voxels");
                String pixelRes = properties.getProperty("alignment.image.size");
                String boundingBox = properties.getProperty("alignment.bounding.box");
                String objective = properties.getProperty("alignment.objective");
                
                helper.setAlignmentSpace(resultItem, alignmentSpace);
                helper.setOpticalResolution(resultItem, opticalRes);
                helper.setPixelResolution(resultItem, pixelRes);
                helper.setBoundingBox(resultItem, boundingBox);
                helper.setObjective(resultItem, objective);
                
                if (consensusAlignmentSpace==null) {
                    consensusAlignmentSpace = alignmentSpace;
                }
                else if (!consensusAlignmentSpace.equals(alignmentSpace)) {
                    logger.warn("No consensus for alignment space: "+consensusAlignmentSpace+"!="+alignmentSpace);
                }
            }
        }   
        
        if (consensusAlignmentSpace!=null) {
            helper.setAlignmentSpace(alignmentResult, consensusAlignmentSpace);
        }
    }
}
