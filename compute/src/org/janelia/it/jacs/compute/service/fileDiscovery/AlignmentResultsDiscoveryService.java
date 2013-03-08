package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;

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
        
        for(Entity resultItem : EntityUtils.getChildrenOfType(alignmentResult, EntityConstants.TYPE_IMAGE_3D)) {
            logger.info("Got result file: "+resultItem.getName());
            
            if (resultItem.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                // TODO: read output metadata file and assign alignment space
                
                String metadataFilename = resultItem.getName().replaceAll("v3draw", "metadata").replaceAll("v3dpbd", "metadata");
                Entity metadataFileEntity = EntityUtils.findChildWithName(alignmentResult, metadataFilename);
                if (metadataFileEntity != null) {
                    logger.info("  Got metadata file: "+metadataFileEntity.getName());
                    
                    
                    File metadataFile = new File(metadataFileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    // TODO: read as properties file

                    
                }
            }            
        }   
    }
}
