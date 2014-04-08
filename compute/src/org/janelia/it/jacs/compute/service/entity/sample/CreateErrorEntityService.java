package org.janelia.it.jacs.compute.service.entity.sample;

import org.hibernate.exception.ExceptionUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;

/**
 * Creates an error entity based on the given exception, and adds it to the root entity.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateErrorEntityService extends AbstractEntityService {

    public static final String ERRORS_DIR_NAME = "Error";
    
    private static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    
    public void execute() throws Exception {
            
    	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
    	if (StringUtils.isEmpty(rootEntityId)) {
    		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
    	}

    	File outputDir = null;
        FileNode resultFileNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
        String username = ownerKey.split(":")[1];

        if (resultFileNode!=null) {
            outputDir = new File(resultFileNode.getDirectoryPath());
        }
        else {
            File userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
            outputDir = new File(userFilestore, ERRORS_DIR_NAME);
            outputDir.mkdirs();
            logger.warn("No RESULT_FILE_NODE is specified, saving error message to general Errors folder: "+outputDir);
            
        }
    	
    	Entity rootEntity = entityBean.getEntityById(rootEntityId);
    	if (rootEntity == null) {
    		throw new IllegalArgumentException("Root entity not found with id="+rootEntityId);
    	}

    	Exception exception = (Exception)processData.getItem(IProcessData.PROCESSING_EXCEPTION);
    	String message = ExceptionUtils.getStackTrace(exception);
    	
    	Entity error = entityBean.createEntity(rootEntity.getOwnerKey(), EntityConstants.TYPE_ERROR, "Error");
    	logger.info("Saved error entity as id="+error.getId());
    	
        File errorFile = new File(outputDir, error.getId().toString()+".txt");
        FileUtils.writeStringToFile(errorFile, message);
        logger.info("Wrote error message to "+errorFile);
        
        entityBean.setOrUpdateValue(error.getId(), EntityConstants.ATTRIBUTE_FILE_PATH, errorFile.getAbsolutePath());
    	
    	entityBean.addEntityToParent(ownerKey, rootEntity.getId(), error.getId(), rootEntity.getMaxOrderIndex()+1, 
    			EntityConstants.ATTRIBUTE_ENTITY);
    }
}
