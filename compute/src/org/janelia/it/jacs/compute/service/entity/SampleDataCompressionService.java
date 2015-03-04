package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Compress existing v3draw files to v3dpbd to save space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDataCompressionService implements IService {

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
	private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
	
    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final String MODE_CREATE_OUTPUT_LIST = "CREATE_OUTPUT_LIST";
    public static final String MODE_COMPLETE = "COMPLETE";
    public static final int GROUP_SIZE = 200;
    public transient static final String PARAM_testRun = "is test run";
	
    protected Logger logger;
    protected Task task;
    protected String ownerKey;
    protected AnnotationBeanLocal annotationBean;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    
    protected int numChanges;
    
    private boolean isDebug = false;
    private String mode = MODE_UNDEFINED;
    protected IProcessData processData;
    
    private String rootEntityId;
    private Set<String> inputFiles = new HashSet<String>();
    private Map<String,Set<Long>> entityMap = new HashMap<String,Set<Long>>();
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            
            String testRun = task.getParameter(PARAM_testRun);
            if (testRun!=null) {
            	isDebug = Boolean.parseBoolean(testRun);	
            }
            
            mode = processData.getString("MODE");
            this.processData = processData;

        	rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
        	
            if (mode.equals(MODE_CREATE_INPUT_LIST)) {
                doCreateInputList();
            }
            else if (mode.equals(MODE_CREATE_OUTPUT_LIST)) {
                doCreateOutputList();
            }
            else if (mode.equals(MODE_COMPLETE)) {
                doComplete();
            } 
            else {
                logger.error("Do not recognize mode type="+mode);
            }
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running SampleDataCompressionService", e);
        }
    }
    
    private void doCreateInputList() throws ComputeException {

        processV3dRawEntities();
        
        List<List<String>> inputGroups = createGroups(inputFiles, GROUP_SIZE);
        processData.putItem("INPUT_PATH_LIST", inputGroups);
        processData.putItem("ENTITY_MAP", entityMap);
        
        if (inputFiles.isEmpty()) {
            logger.info("Nothing to be done.");
        }
        else {
            logger.info("Processed "+inputFiles.size()+" entities into "+inputGroups.size()+" groups.");
        }
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
    
    private void doCreateOutputList() throws ComputeException {

    	List<String> inputPaths = (List<String>)processData.getItem("INPUT_PATH_LIST");
    	if (inputPaths == null) {
    		throw new IllegalArgumentException("INPUT_PATH_LIST may not be null");
    	}
    	
    	List<String> outputPaths = new ArrayList<String>();
    	for(String s : inputPaths) {
    		outputPaths.add(s.replaceAll("v3draw", "v3dpbd"));
    	}
        processData.putItem("OUTPUT_PATH_LIST", outputPaths);
    }
    
	public void processV3dRawEntities() throws ComputeException {

        if (isDebug) {
            logger.info("This is a test run. Nothing will actually happen.");
        }
        else {
            logger.info("This is the real thing. Files will get compressed, and then the originals will be deleted!");
        }
        
        if (rootEntityId!=null) {
            logger.info("Finding V3DRAW files to compress under root "+rootEntityId);
        	
        	Entity entity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(rootEntityId));
        	if (entity == null) {
        		throw new IllegalArgumentException("Entity not found with id="+rootEntityId);
        	}
        	
        	for(Entity image : EntityUtils.getDescendantsOfType(entity,EntityConstants.TYPE_IMAGE_3D)) {
        		String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		if (filepath!=null && filepath.endsWith("v3draw")) {
        			addV3dRawFile(image);
        		}
        	}
        }
        else {
        	logger.info("Finding V3DRAW files belonging to "+ownerKey);
        	
    		for(Entity entity : entityBean.getUserEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_FILE_PATH, "%v3draw")) {
    			if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
    				logger.warn("Ignoring entity with v3draw filepath that is not an Image 3D: "+entity.getId());
    				continue;
    			}
    			if (!entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).contains(centralDir)) {
    				logger.warn("Ignoring entity with which does not contain the FileStore.CentralDir: "+entity.getId());
    				continue;
    			}
    			addV3dRawFile(entity);
    		}
        }
    }
    
    private void addV3dRawFile(Entity imageEntity) throws ComputeException {

    	if (!imageEntity.getOwnerKey().equals(ownerKey)) return;
    	
    	String filepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	
    	if (filepath==null) {
    		logger.warn("File path for "+imageEntity.getId()+" is null");
    		return;
    	}
    	
    	File file = new File(filepath);
    	
    	if (!file.exists()) {
    		logger.warn("File path for "+imageEntity.getId()+" does not exist: "+filepath);
    		return;
    	}
    	
    	logger.info("Will compress file: "+imageEntity.getName()+" (id="+imageEntity.getId()+")");
    	
    	inputFiles.add(filepath);
    	
    	Set<Long> eset = entityMap.get(filepath);
    	if (eset == null) {
    		eset = new HashSet<Long>();
    		entityMap.put(filepath, eset);
    	}
    	eset.add(imageEntity.getId());
    }
    
    private void doComplete() throws ComputeException {

    	this.entityMap = (Map<String,Set<Long>>)processData.getItem("ENTITY_MAP");
    	List<String> inputPaths = (List<String>)processData.getItem("INPUT_PATH_LIST");
    	if (inputPaths == null) {
    		throw new IllegalArgumentException("INPUT_PATH_LIST may not be null");
    	}
    	
    	List<String> outputPaths = (List<String>)processData.getItem("OUTPUT_PATH_LIST");
    	if (outputPaths == null) {
    		throw new IllegalArgumentException("OUTPUT_PATH_LIST may not be null");
    	}
    	
    	for(int i=0; i<inputPaths.size(); i++) {
    		String inputPath = inputPaths.get(i);
    		String outputPath = outputPaths.get(i);
    		File outputFile = new File(outputPath);
    		if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
    			logger.warn("Missing or corrupt output file: "+outputFile);
    		}
    		else {
    			updateEntities(inputPath, outputPath);	
    		}
    	}

		logger.info("Modified "+numChanges+" entities.");
    }
    
    private void updateEntities(String inputPath, String outputPath) throws ComputeException {

    	try {
    	    // Update all entities which referred to the old path
    	    int numUpdated = entityBean.bulkUpdateEntityDataValue(inputPath, outputPath);
            logger.info("Updated "+numUpdated+" entities to use new compressed file: "+outputPath);

            Set<Long> inputEntites = entityMap.get(inputPath);
            if (inputEntites == null) {
                logger.warn("No entities found with this path: "+inputPath);
                return;
            }
            
    	    // Update the input stacks
        	for(Entity entity : entityBean.getEntitiesById(new ArrayList<Long>(inputEntites))) {
        	    
        	    // Update the path. This was already fixed by the bulk update above, but we need to fix it again, 
        	    // because we're using an entity with the old state.
                entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, outputPath);
                
                // Update the format
        		String format = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
        		if (format != null && !"".equals(format)) {
        			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, "v3dpbd");
        		}
        		
        		// Update the name
        		if (entity.getName().endsWith(".v3draw")) {
        			entity.setName(entity.getName().replaceAll("v3draw", "v3dpbd"));
        		}
        		
        		if (!isDebug) {
	            	Entity savedEntity = entityBean.saveOrUpdateEntity(entity);
	            	logger.info("Updated entity: "+savedEntity.getName()+" (id="+savedEntity.getId()+")");
        		}
        		else {
        			logger.info("Updated entity: "+entity.getName()+" (id="+entity.getId()+")");
        		}
            	numChanges++;
        	}	
    	
    		File file = new File(inputPath);
    		if (!isDebug) {
				try {
					FileUtils.forceDelete(file);
					logger.info("Deleted old file: "+inputPath);
				}
				catch (Exception e) {
					logger.info("Error deleting symlink "+inputPath+": "+e.getMessage());
				}
    		}
    		
    	}
    	catch (ComputeException e) {
    		logger.error("Unable to update all entities to use new compressed file: "+outputPath);
    	}
	}
}
