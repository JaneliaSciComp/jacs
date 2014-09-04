package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

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

        renameYoshiMacroResults();
    }

	private void renameYoshiMacroResults() throws Exception {

		log.info("Renaming old 20x Normalization results");
		
		for(Entity pipelineRun : entityBean.getEntitiesByNameAndTypeName("user:asoy", "MBEW Pipeline 20x Results", EntityConstants.TYPE_PIPELINE_RUN)) {
			
			Entity sample = entityBean.getAncestorWithType(pipelineRun, EntityConstants.TYPE_SAMPLE);
			if (sample.getName().contains("~")) {
				sample = entityBean.getAncestorWithType(sample, EntityConstants.TYPE_SAMPLE);
			}
			
			String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
			
			if (dataSetIdentifier==null) {
				log.warn("Data set identifier not found on entity "+sample.getId());
				continue;
			}
			
			EntityData ppEd = pipelineRun.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
			
			String oldValue = ppEd.getValue();
			
			if ("YoshiNormalization20x".equals(oldValue)) {
			
				if (dataSetIdentifier.endsWith("mcfo_case_1")) {
					ppEd.setValue("YoshiMacroMCFOCase1");
				}
				else if (dataSetIdentifier.endsWith("polarity_case_1")) {
					ppEd.setValue("YoshiMacroPolarityCase1");
				}
				else if (dataSetIdentifier.endsWith("polarity_case_2")) {
					ppEd.setValue("YoshiMacroPolarityCase2");
				}
				else if (dataSetIdentifier.endsWith("polarity_case_3")) {
					ppEd.setValue("YoshiMacroPolarityCase3");
				}
				else if (dataSetIdentifier.endsWith("polarity_case_4")) {
					ppEd.setValue("YoshiMacroPolarityCase4");
				}
				else {
					log.warn("Unrecognized data set for 20x normalization: "+dataSetIdentifier);
				}
				
				log.info("Changing "+oldValue+" to "+ppEd.getValue());
				entityBean.saveOrUpdateEntityData(ppEd);
			}
			
			// Free memory
			pipelineRun.setEntityData(null);
		}
		
	}
}
