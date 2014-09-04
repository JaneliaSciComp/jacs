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

        renameYoshi20xMacroResults();
    }

	private void renameYoshi20xMacroResults() throws Exception {

		for(Entity pipelineRun : entityBean.getEntitiesByNameAndTypeName("user:asoy", "MBEW Pipeline 20x Results", EntityConstants.TYPE_PIPELINE_RUN)) {
			
			Entity sample = entityBean.getAncestorWithType(pipelineRun, EntityConstants.TYPE_SAMPLE);
			if (sample.getName().contains("~")) {
				sample = entityBean.getAncestorWithType(sample, EntityConstants.TYPE_SAMPLE);
			}
			
			String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
			
			
			
			
			
		}
		
	}

}
