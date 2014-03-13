package org.janelia.it.jacs.compute.service.entity;

import java.io.File;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.entity.sample.CreateErrorEntityService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {
    
    private static final Logger log = Logger.getLogger(UpgradeUserDataService.class);
    
    private static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    private String username;
    private File userFilestore;
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

        this.username = ownerKey.split(":")[1];
        this.userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);

        moveErrorsToFilesystem();
    }

    private void moveErrorsToFilesystem() throws Exception {
        
        File errorsDir = new File(userFilestore, CreateErrorEntityService.ERRORS_DIR_NAME);
        errorsDir.mkdirs();
        
        logger.info("  Moving error messages to "+errorsDir);
        
        for(Entity error : entityBean.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_ERROR)) {
            
            EntityData ed = error.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_MESSAGE);
            
            if (ed==null || StringUtils.isEmpty(ed.getValue())) {
                if (error.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)==null) {
                    log.warn("Error has no message and no file path: "+error.getId());    
                }
                continue;
            }
            
            File errorFile = new File(errorsDir, error.getId().toString()+".txt");
            FileUtils.writeStringToFile(errorFile, ed.getValue());
            
            error.getEntityData().remove(ed);
            entityBean.deleteEntityData(ed);
            entityBean.setOrUpdateValue(error.getId(), EntityConstants.ATTRIBUTE_FILE_PATH, errorFile.getAbsolutePath());
        }
    }
}
