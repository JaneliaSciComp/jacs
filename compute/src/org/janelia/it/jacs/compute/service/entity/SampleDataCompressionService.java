package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Compress a set of existing files to a new set of formats.  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDataCompressionService extends AbstractEntityService {

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
	private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
	
	public static final String RECORD_MODE_UPDATE = "UPDATE";
    public static final String RECORD_MODE_ADD = "ADD";
	
    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final String MODE_CREATE_OUTPUT_LIST = "CREATE_OUTPUT_LIST";
    public static final String MODE_COMPLETE = "COMPLETE";
    public static final int GROUP_SIZE = 200;
    public transient static final String PARAM_testRun = "is test run";
	
    private boolean isDebug = false;

    private String mode;
    private String recordMode;
    private String rootEntityId;
    private String inputType;
    private String outputType;
    
    private boolean deleteSourceFiles = true;
    private Set<String> excludeFileSet = new HashSet<String>();
    
    private final Set<String> inputFiles = new HashSet<String>();
    private final Set<Long> visited = new HashSet<Long>();
    private Map<String,Set<Long>> entityMap;
    protected int numChanges;

    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        mode = data.getRequiredItemAsString("MODE");
        recordMode = data.getRequiredItemAsString("RECORD_MODE");
    	rootEntityId = data.getItemAsString("ROOT_ENTITY_ID");
    	inputType = data.getRequiredItemAsString("INPUT_TYPE");
        outputType = data.getRequiredItemAsString("OUTPUT_TYPE");

        if (!RECORD_MODE_ADD.equals(recordMode) && !RECORD_MODE_UPDATE.equals(recordMode)) {
            throw new IllegalStateException("Illegal RECORD_MODE: "+recordMode);
        }
        
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
            throw new IllegalStateException("Do not recognize mode '"+mode+"'");
        }
    }
    
    private void doCreateInputList() throws ComputeException {

        this.entityMap = new HashMap<String,Set<Long>>();
               
        String excludeFiles = data.getItemAsString("EXCLUDE_FILES");
        if (!StringUtils.isEmpty(excludeFiles)) {
            for(String excludeFile : excludeFiles.split("\\s*,\\s*")) {
                excludeFileSet.add(excludeFile);
            }
        }

        this.deleteSourceFiles = !"false".equals(data.getRequiredItem("DELETE_INPUTS"));

        if (isDebug) {
            contextLogger.info("This is a test run. Nothing will actually happen.");
        }
        else {
            if (deleteSourceFiles) {
                contextLogger.info("This is the real thing. Files will get compressed, and then the originals will be deleted!");    
            }
            else {
                contextLogger.info("This is the real thing. Files will get compressed, and added to the existing entities.");
            }
        }
        
        if (rootEntityId!=null) {
            contextLogger.info("Finding files to compress under root "+rootEntityId+" with type "+inputType);
            
            Entity entity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(rootEntityId));
            if (entity == null) {
                throw new IllegalArgumentException("Entity not found: "+rootEntityId);
            }
            
            if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                throw new IllegalArgumentException("Entity is not a sample: "+rootEntityId);
            }
            
            Map<Long,Entity> entities = EntityUtils.getEntityMap(EntityUtils.getDescendantsOfType(entity,EntityConstants.TYPE_IMAGE_3D));
            for(Entity image : entities.values()) {
                String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (filepath!=null && filepath.endsWith(inputType)) {
                    addEntityToInputList(image);
                }
            }
        }
        else {
            contextLogger.info("Finding files belonging to "+ownerKey+" with type "+inputType);
            
            for(Entity image : entityBean.getUserEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_FILE_PATH, "%"+inputType)) {
                if (!image.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                    contextLogger.warn("Entity is not an Image 3D: "+image.getId());
                    continue;
                }
                addEntityToInputList(image);
            }
        }
        
        List<List<String>> inputGroups = createGroups(inputFiles, GROUP_SIZE);
        processData.putItem("INPUT_PATH_LIST", inputGroups);
        processData.putItem("ENTITY_MAP", entityMap);
        
        if (inputFiles.isEmpty()) {
            contextLogger.info("Nothing to be done.");
        }
        else {
            contextLogger.info("Processed "+inputFiles.size()+" entities into "+inputGroups.size()+" groups.");
        }
    }
    
    private void addEntityToInputList(Entity imageEntity) throws ComputeException {

        if (visited.contains(imageEntity.getId())) return;
        visited.add(imageEntity.getId());
        
        if (!imageEntity.getOwnerKey().equals(ownerKey)) return;

        String filepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        
        if (filepath==null) {
            contextLogger.warn("Entity has null filepath: "+imageEntity.getId());
            return;
        }

        if (!filepath.contains(centralDir)) {
            contextLogger.warn("Entity has path outside of filestore: "+imageEntity.getId());
            return;
        }

        File file = new File(filepath);
        
        if (excludeFileSet.contains(file.getName())) {
            contextLogger.info("Excluding file: "+imageEntity.getId());
            return;
        }
        
        if (!file.exists()) {
            contextLogger.warn("Entity file does not exist: "+imageEntity.getId());
            return;
        }
        
        populateChildren(imageEntity);
        
        if (outputType.equals("h5j")) {
            Entity existingH5j = imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE);
            if (existingH5j!=null) {
                // The H5J entity already exists
                String h5jFilepath = existingH5j.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (RECORD_MODE_ADD.equals(recordMode)) {
                    if (h5jFilepath!=null & h5jFilepath.endsWith(".h5j")) {
                        // It's already in the correct place
                        return;
                    }
                }
                else if (RECORD_MODE_UPDATE.equals(recordMode)) {

                    if (isDebug) {
                        contextLogger.info("Would update existing Image with existing H5J (id="+existingH5j.getId()+")");
                        return;
                    }
                    
                    // We just need to move it into the right place
                    int numUpdated = entityBean.bulkUpdateEntityDataValue(filepath, h5jFilepath);
                    contextLogger.info("Updated "+numUpdated+" entity data values to use compressed file: "+h5jFilepath);
                    
                    
                    // Update the image to use H5J format
                    imageEntity.setName(existingH5j.getName());
                    imageEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, h5jFilepath);
                    imageEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, "h5j");
                    entityBean.saveOrUpdateEntity(imageEntity);
                    
                    // Move all references to the H5J
                    for(EntityData parentEd : entityBean.getParentEntityDatas(existingH5j.getId())) {
                        if (!parentEd.getParentEntity().getId().equals(imageEntity.getId())) {
                            parentEd.setChildEntity(imageEntity);
                            contextLogger.info("Carrying forward reference to H5J#"+existingH5j.getId()+" from "+parentEd.getParentEntity().getName());
                            entityBean.saveOrUpdateEntityData(parentEd);
                        }
                    }
                    
                    // Delete the old H5J entity
                    entityBean.deleteEntityTreeById(existingH5j.getOwnerKey(), existingH5j.getId(), true);
                    contextLogger.info("Replaced Entity#"+imageEntity.getId()+" with "+imageEntity.getName());
                    
                    deleteIfNecessary(filepath);
                    return;
                }
                
            }
        }
        
        contextLogger.info("Will compress file: "+imageEntity.getName()+" (id="+imageEntity.getId()+")");
    	
    	inputFiles.add(filepath);
    	
    	Set<Long> eset = entityMap.get(filepath);
    	if (eset == null) {
    		eset = new HashSet<>();
    		entityMap.put(filepath, eset);
    	}
    	eset.add(imageEntity.getId());
    }

    private List<List<String>> createGroups(Collection<String> fullList, int groupSize) {
        List<List<String>> groupList = new ArrayList<>();
        List<String> currentGroup = null;
        for (String s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList<>();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }
    
    private void doCreateOutputList() throws ComputeException {

        List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");

        List<String> outputPaths = new ArrayList<String>();
        for(String filepath : inputPaths) {
            String extension = getExtension(filepath);
            outputPaths.add(filepath.replaceAll(extension, outputType));
        }
        processData.putItem("OUTPUT_PATH_LIST", outputPaths);
    }
    
    private void doComplete() throws ComputeException {

        this.deleteSourceFiles = !"false".equals(data.getRequiredItem("DELETE_INPUTS"));
    	this.entityMap = (Map<String,Set<Long>>)data.getRequiredItem("ENTITY_MAP");
    	List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");
        List<String> outputPaths = (List<String>)data.getRequiredItem("OUTPUT_PATH_LIST");
    	
    	for(int i=0; i<inputPaths.size(); i++) {
    		String inputPath = inputPaths.get(i);
            String outputPath = outputPaths.get(i);
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
                contextLogger.warn("Missing or corrupt output file: "+outputFile);
            }
            else {
                updateEntities(inputPath, outputPath);  
            }
    	}

    	contextLogger.info("Modified "+numChanges+" entities.");
    }
    
    private void updateEntities(String inputPath, String outputPath) throws ComputeException {
        
        String inputExtension = getExtension(inputPath);
        String outputExtension = getExtension(outputPath);
        
    	try {
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
                contextLogger.warn("Missing or corrupt output file: "+outputFile);
                return;
            }
            
            if (RECORD_MODE_UPDATE.equals(recordMode)) {
                if (!isDebug) {
            	    // Update all entities which referred to the old path
            	    int numUpdated = entityBean.bulkUpdateEntityDataValue(inputPath, outputPath);
            	    contextLogger.info("Updated "+numUpdated+" entity data values to use new compressed file: "+outputPath);
                }
            }
            
            Set<Long> inputEntites = entityMap.get(inputPath);
            if (inputEntites == null) {
                contextLogger.warn("No entities found with this path: "+inputPath);
                return;
            }
            
            Entity addEntity = null;
            
    	    // Update the input stacks

        	for(Entity entity : entityBean.getEntitiesById(new ArrayList<Long>(inputEntites))) {

                if (RECORD_MODE_UPDATE.equals(recordMode)) {
                    // Update the format, if the entity has one
            		String format = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
            		if (!StringUtils.isEmpty(format)) {
            			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, outputExtension);
            		}
            		
            		// Update the name
            		if (entity.getName().endsWith(inputExtension)) {
            			entity.setName(entity.getName().replaceAll(inputExtension, outputExtension));
            		}
            		
            		if (!isDebug) {
    	            	Entity savedEntity = entityBean.saveOrUpdateEntity(entity);
    	            	contextLogger.info("Updated entity: "+savedEntity.getName()+" (id="+savedEntity.getId()+")");
            		}
            		else {
            			contextLogger.info("Updated entity: "+entity.getName()+" (id="+entity.getId()+")");
            		}
                    
                    numChanges++;
                }
                else if (RECORD_MODE_ADD.equals(recordMode)) {
                    if (addEntity==null) {
                        if (!isDebug) {
                            String secName = entity.getName().replaceAll(inputExtension, outputExtension);
                            addEntity = entityBean.createEntity(entity.getOwnerKey(), EntityConstants.TYPE_IMAGE_3D, secName);
                            addEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, outputPath);
                            addEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, outputExtension);
                            entityBean.saveOrUpdateEntity(addEntity);
                        }
                        contextLogger.info("Created secondary entity: "+addEntity.getName()+" (id="+addEntity.getId()+")");
                    }
                    if (!isDebug) {
                        entityBean.addEntityToParent(entity, addEntity, entity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE, outputPath);
                    }
                }
        	}
    	
        	deleteIfNecessary(inputPath);
    	}
    	catch (ComputeException e) {
    		contextLogger.error("Unable to update all entities to use new compressed file: "+outputPath);
    	}
	}
    
    private void deleteIfNecessary(String filepath) {
        if (!deleteSourceFiles) return;
        File file = new File(filepath);
        if (!isDebug) {
            try {
                // TODO: what if the file is stored in Scality?
                FileUtils.forceDelete(file);
                contextLogger.info("Deleted old file: "+filepath);
            }
            catch (Exception e) {
                contextLogger.info("Error deleting symlink "+filepath+": "+e.getMessage());
            }
        }
    }

    private String getExtension(String filepath) {
        int dot = filepath.indexOf('.');
        if (dot>0) {
            return filepath.substring(dot+1);
        }
        return "";
    }
}
