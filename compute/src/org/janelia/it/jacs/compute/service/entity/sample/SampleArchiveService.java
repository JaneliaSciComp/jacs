package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.util.FileUtils;
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
    public static final String MODE_COMPLETE = "COMPLETE";
    public static final int GROUP_SIZE = 200;
    public transient static final String PARAM_testRun = "is test run";
	
    protected int numChanges;
    
    private boolean isDebug = false;
    private String mode;
    
    private String sampleEntityId;
    private Set<String> originalPaths = new HashSet<String>();
    
    public void execute() throws Exception {

        super.execute(processData);
        
        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        mode = processData.getString("MODE");
        
    	sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	
        if (mode.equals(MODE_CREATE_ARCHIVE_LIST)) {
            doCreateArchiveList();
        }
        else if (mode.equals(MODE_COMPLETE)) {
            doComplete();
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
        
        List<List<String>> inputGroups = createGroups(originalPaths, GROUP_SIZE);
        processData.putItem("ARCHIVE_FILE_PATHS", inputGroups);
        
		logger.info("Processed "+originalPaths.size()+" entities into "+inputGroups.size()+" groups.");
    }

    private List<List<String>> createGroups(Collection<String> fullList, int groupSize) {
        List<List<String>> groupList = new ArrayList<List<String>>();
        List<String> currentGroup = null;
        for (String s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList<String>();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList<String>();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
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
    
    private void doComplete() throws ComputeException {

    	List<String> originalPaths = (List<String>)processData.getItem("ORIGINAL_FILE_PATHS");
    	if (originalPaths == null) {
    		throw new IllegalArgumentException("ORIGINAL_FILE_PATHS may not be null");
    	}
    	
    	List<String> archivePaths = (List<String>)processData.getItem("ARCHIVE_FILE_PATHS");
    	if (archivePaths == null) {
    		throw new IllegalArgumentException("ARCHIVE_FILE_PATHS may not be null");
    	}
    	
    	for(int i=0; i<originalPaths.size(); i++) {
    		String originalPath = originalPaths.get(i);
    		String archivePath = archivePaths.get(i);
    		File outputFile = new File(archivePath);
    		if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
    			logger.warn("Missing or corrupt archive file: "+outputFile);
    		}
    		else {
    			updateEntities(originalPath, archivePath);	
    		}
    	}

		logger.info("Modified "+numChanges+" entities.");
    }
    
    private void updateEntities(String originalPath, String archivePath) throws ComputeException {

    	try {
    	    entityBean.bulkUpdateEntityDataValue(originalPath, archivePath);
            logger.info("Updated all entities to use archived file: "+archivePath);
    	    
    		File file = new File(originalPath);
    		if (!isDebug) {
				try {
					FileUtils.forceDelete(file);
					logger.info("Deleted old file: "+originalPath);
				}
				catch (Exception e) {
					logger.info("Error deleting symlink "+originalPath+": "+e.getMessage());
				}
    		}
    		
    	}
    	catch (ComputeException e) {
    		logger.error("Unable to update all entities to use new compressed file: "+archivePath);
    	}
	}
}
