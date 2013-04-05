package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Move large Sample files to archive to save space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleArchiveService extends AbstractEntityService {

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
	
    public static final String MODE_CREATE_ARCHIVE_LIST = "CREATE_ARCHIVE_LIST";
    public transient static final String PARAM_testRun = "is test run";
	
    protected int numChanges;
    
    private boolean isDebug = false;
    private String mode;
    
    private String sampleEntityId;
    private Set<String> originalPaths = new HashSet<String>();
    
    public void execute() throws Exception {
        
        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        mode = processData.getString("MODE");
        
    	sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	
        if (mode.equals(MODE_CREATE_ARCHIVE_LIST)) {
            doCreateArchiveList();
        }
        else {
            logger.error("Do not recognize mode type="+mode);
        }
    }
    
    private void doCreateArchiveList() throws Exception {

        logger.info("Finding files to archive...");
        
        if (isDebug) {
        	logger.info("This is a test run. Nothing will actually happen.");
        }
        else {
        	logger.info("This is the real thing. Files will get copied to archive, and the originals will be deleted!");
        }
    	
        processImageEntities();
        
        processData.putItem("ORIGINAL_FILE_PATHS", originalPaths);
        
		logger.info("Processed "+originalPaths.size()+" paths");
    }
    
	public void processImageEntities() throws Exception {

        if (sampleEntityId!=null) {
            addSampleFiles(new Long(sampleEntityId));
        }
        else {
        	logger.info("Finding sample files belonging to "+ownerKey);
    		for(Entity sampleEntity : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE)) {
    			addSampleFiles(sampleEntity.getId());
    		}
        }
    	
		logger.info("The processing was a success.");
    }
	
	private void addSampleFiles(Long sampleEntityId) throws Exception {
	    logger.info("Finding sample image files under id="+sampleEntityId);
        
        Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample not found with id="+sampleEntityId);
        }
        
        entityLoader.populateChildren(sampleEntity);
        for(Entity pipelineRun : sampleEntity.getOrderedChildren()) {
            entityLoader.populateChildren(pipelineRun);
            if (pipelineRun.getEntityType().getName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
                for(Entity result : pipelineRun.getChildren()) {
                    entityLoader.populateChildren(result);
                    Entity supportingFiles = EntityUtils.getSupportingData(result);
                    entityLoader.populateChildren(supportingFiles);
                    for(Entity fileEntity : supportingFiles.getOrderedChildren()) {
                        if (fileEntity.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                            if (fileEntity.getName().endsWith("v3dpbd")) {
                                addFile(fileEntity);
                            }
                        }
                    }
                }
            }
        }
	}
    
    private void addFile(Entity imageEntity) throws ComputeException {

    	if (!imageEntity.getOwnerKey().equals(ownerKey)) return;
    	
    	String filepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

        if (filepath==null) {
            logger.warn("File path for "+imageEntity.getId()+" is null");
            return;
        }

        if (filepath.startsWith("/archive")) {
            logger.warn("Ignoring file which is already archived: "+imageEntity.getId());
            return;
        }
        
    	if (!filepath.contains(centralDir)) {
            logger.warn("Ignoring file with which does not contain the FileStore.CentralDir: "+imageEntity.getId());
            return;
        }
    	
    	File file = new File(filepath);
    	
    	if (!file.exists()) {
    		logger.warn("File path for "+imageEntity.getId()+" does not exist: "+filepath);
    		return;
    	}
    	
    	logger.info("Will compress file: "+imageEntity.getName()+" (id="+imageEntity.getId()+")");
    	
    	originalPaths.add(filepath);
    }
    
}
