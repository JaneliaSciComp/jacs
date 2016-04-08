package org.janelia.it.jacs.compute.service.domain;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractDomainService {
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

    }
    
}
