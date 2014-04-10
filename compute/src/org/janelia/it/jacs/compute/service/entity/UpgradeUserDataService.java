package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {
    
    private static final Logger log = Logger.getLogger(UpgradeUserDataService.class);
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

        remodelProcessingBlocks();
    }

    private void remodelProcessingBlocks() throws Exception {
        logger.info("  Remodeling processing blocks as sample statuses");
        for(Entity block : entityBean.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_PROCESSING_BLOCK)) {
            Entity sample = entityBean.getAncestorWithType(block, EntityConstants.TYPE_SAMPLE);
            entityBean.setOrUpdateValue(ownerKey, sample.getId(), EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_BLOCKED);
            entityBean.deleteEntityById(ownerKey, block.getId());
        }
    }
}
