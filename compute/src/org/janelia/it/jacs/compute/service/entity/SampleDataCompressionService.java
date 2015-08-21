package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
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
    public static final String RECORD_MODE_FLIP = "FLIP";
	
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
    private Set<Pattern> exclusions = new HashSet<>();
    
    private final Set<String> inputFiles = new HashSet<>();
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

        if (!RECORD_MODE_ADD.equals(recordMode) && !RECORD_MODE_UPDATE.equals(recordMode) && !RECORD_MODE_FLIP.equals(recordMode)) {
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
            for(String filePattern : excludeFiles.split("\\s*,\\s*")) {
            	Pattern p = Pattern.compile(filePattern.replaceAll("\\*", "(.*?)"));
                exclusions.add(p);
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
                String bpid = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
                if ((filepath!=null && filepath.endsWith(inputType)) || (bpid!=null && bpid.endsWith(inputType))) {
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
        
        if (!imageEntity.getOwnerKey().equals(ownerKey)) {
            contextLogger.warn("Entity is not owned by "+ownerKey);
        	return;
        }

        String filepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String bpid = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
        
        if (filepath==null) {
        	if (bpid==null) {
                contextLogger.warn("Entity has null filepath and BPID: "+imageEntity.getId());
                return;	
        	}
        	filepath = EntityConstants.SCALITY_PATH_PREFIX+bpid;
        }
        
        populateChildren(imageEntity);
        
        if (outputType.equals("h5j")) {

            // The H5J already exists as the prime image?
        	if (filepath.endsWith("h5j")) {
        		
                contextLogger.info("Entity already has correct output type: "+imageEntity.getId());
                
                if (RECORD_MODE_ADD.equals(recordMode)) {
                	contextLogger.warn("Unflipping H5J/PBD entities is not supported: "+imageEntity.getId());
                	return;
                }
                else if (RECORD_MODE_UPDATE.equals(recordMode)) {
                	// The H5J is in the correct place. If we previously ran this compression in "FLIP" mode, then there may be a PBD that is no longer required. 
                    Entity existingPbd = imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE);
                    if (existingPbd!=null) {
	                    String pbdFilepath = existingPbd.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	                    deleteIfNecessary(pbdFilepath);
                    }
                	return;
                }
                else if (RECORD_MODE_FLIP.equals(recordMode)) {
                	// The H5J is already in the correct place. We hope there is a Scality-based PBD file underneath.
                    Entity existingPbd = imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE);
                    if (existingPbd==null) {
                    	contextLogger.warn("Flipped H5J is missing a lossless image child: "+imageEntity.getId());
                    }
                	return;
                }
        	}

            // The H5J already exists as the secondary image?
            EntityData existingH5jEd = imageEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE);
            if (existingH5jEd!=null) {
	            Entity existingH5j = existingH5jEd.getChildEntity();
	            if (existingH5j!=null) {

		            // Add in case we flip them, we don't want to see it again. 
		            visited.add(existingH5j.getId());
		            
	                contextLogger.debug("Slightly lossy entity already has correct output type: "+existingH5j.getId());
	                
	                // The H5J entity already exists
	                String h5jFilepath = existingH5j.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	                if (RECORD_MODE_ADD.equals(recordMode)) {
	                    if (h5jFilepath!=null & h5jFilepath.endsWith(".h5j")) {
	                        // It's already in the correct place
	                        contextLogger.info("Slightly lossy image already has correct output type: "+existingH5j.getId());
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
	                else if (RECORD_MODE_FLIP.equals(recordMode)) {
	
	                	String h5jName = existingH5j.getName();
	                	String h5jScalityBpid = existingH5j.getValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
	                	String h5jFormat = existingH5j.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
	                	if (StringUtils.isEmpty(h5jFormat)) {
	                		h5jFormat = "h5j";
	                	}
	                	
	                	String pbdName = imageEntity.getName();
	                	String pbdFilepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	                	String pbdScalityBpid = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
	                	String pbdFormat = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
	                	if (StringUtils.isEmpty(pbdFormat)) {
	                		pbdFormat = "v3dpbd";
	                	}
	                	
	                	// Flip them
	                	
	                    int numUpdated = entityBean.bulkUpdateEntityDataValue(pbdFilepath, h5jFilepath);
	                    contextLogger.info("Updated "+numUpdated+" entity data values to use: "+h5jFilepath);
	                	
	                	imageEntity.setName(h5jName);
	                	updateEntityData(imageEntity, EntityConstants.ATTRIBUTE_FILE_PATH, h5jFilepath);
	                	updateEntityData(imageEntity, EntityConstants.ATTRIBUTE_SCALITY_BPID, h5jScalityBpid);
	                	updateEntityData(imageEntity, EntityConstants.ATTRIBUTE_IMAGE_FORMAT, h5jFormat);
	                	
	                	existingH5j.setName(pbdName);
	                	updateEntityData(existingH5j, EntityConstants.ATTRIBUTE_FILE_PATH, pbdFilepath);
	                	updateEntityData(existingH5j, EntityConstants.ATTRIBUTE_SCALITY_BPID, pbdScalityBpid);
	                	updateEntityData(existingH5j, EntityConstants.ATTRIBUTE_IMAGE_FORMAT, pbdFormat);
	                	
	                    if (!isDebug) {
	                    	entityBean.saveOrUpdateEntity(imageEntity);
	                    }
	        			contextLogger.info("Updated entity: "+imageEntity.getName()+" (id="+imageEntity.getId()+")");
	        			
	                    if (!isDebug) {
	                    	entityBean.saveOrUpdateEntity(existingH5j);
	                    }
	        			contextLogger.info("Updated entity: "+existingH5j.getName()+" (id="+existingH5j.getId()+")");
	
	                    if (!isDebug) {
		        			existingH5jEd.setEntityAttrName(EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE);
		        			existingH5jEd.setValue(pbdFilepath);
		        			entityBean.saveOrUpdateEntityData(existingH5jEd);
	                    }
	        			contextLogger.info("Updated entity data from slightly lossy to lossless image: "+existingH5jEd.getId());
	        			
	                    return;
	                }
	            }
	        }
        }

        if (filepath.startsWith(EntityConstants.SCALITY_PATH_PREFIX)) {
            contextLogger.warn("Cannot process entity that is in Scality: "+imageEntity.getId());
        	return;
        }

        File file = new File(filepath);

        if (!filepath.startsWith(centralDir)) {
            contextLogger.warn("Entity has path outside of filestore: "+imageEntity.getId());
            return;
        }
        
        if (isExcluded(file.getName())) {
            contextLogger.debug("Excluding file: "+imageEntity.getId());
            return;
        }
        
        if (!file.exists()) {
            contextLogger.warn("Entity file does not exist: "+imageEntity.getId());
            return;
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

	private boolean isExcluded(String filename) {		
		for(Pattern p : exclusions) {
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				return true;
			}
		}
		return false;
    }
	
    private void updateEntityData(Entity entity, String attName, String value) throws ComputeException {
		contextLogger.debug("updateEntityData "+attName+"="+value+" for "+entity.getId());
    	if (!StringUtils.isEmpty(value)) {
    		entity.setValueByAttributeName(attName, value);
    	}
    	else {
    		EntityData ed = entity.getEntityDataByAttributeName(attName);
    		if (ed!=null) {
    			entity.getEntityData().remove(ed);
    			contextLogger.debug("Removing entity data "+ed.getEntityAttrName()+" for "+entity.getId());
        		if (!isDebug) {
        			entityBean.deleteEntityData(ed);
        		}
    		}
    	}
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
			// Check to make sure we generated the file
    		File outputFile = new File(outputPath);
            if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
                contextLogger.warn("Missing or corrupt output file: "+outputFile);
                return;
            }
            
            if (RECORD_MODE_UPDATE.equals(recordMode) || RECORD_MODE_FLIP.equals(recordMode)) {
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
                    
                    numChanges++;
                }
                else if (RECORD_MODE_FLIP.equals(recordMode)) {
                    
                    if (addEntity==null) {
                        if (!isDebug) {
	                    	// Create "new" PBD entity, so we don't need to update all the default 3d image links
	                        addEntity = entityBean.createEntity(entity.getOwnerKey(), EntityConstants.TYPE_IMAGE_3D, entity.getName());
                        	updateEntityData(addEntity, EntityConstants.ATTRIBUTE_FILE_PATH, inputPath);
                        	updateEntityData(addEntity, EntityConstants.ATTRIBUTE_IMAGE_FORMAT, entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT));
                        	entityBean.saveOrUpdateEntity(addEntity);
                        }
                        contextLogger.info("Created secondary entity: "+addEntity.getName()+" (id="+addEntity.getId()+")");
                    }
                    if (!isDebug) {
                        entityBean.addEntityToParent(entity, addEntity, entity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE, inputPath);
                    }

                    // Now convert the original PBD entity into an H5J entity
        			entity.setName(entity.getName().replaceAll(inputExtension, outputExtension));
                	updateEntityData(entity, EntityConstants.ATTRIBUTE_IMAGE_FORMAT, outputExtension);
                	
            		if (!isDebug) {
    	            	entityBean.saveOrUpdateEntity(entity);
            		}
            		
        			contextLogger.info("Updated entity: "+entity.getName()+" (id="+entity.getId()+")");
                    
                    numChanges++;
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
        
        if (!filepath.startsWith(centralDir)) {
            contextLogger.warn("Path outside of filestore: "+filepath);
            return;
        }
        
        if (!isDebug) {
            if (filepath.startsWith(EntityConstants.SCALITY_PATH_PREFIX)) {
            	String bpid = filepath.replaceFirst(EntityConstants.SCALITY_PATH_PREFIX,"");
            	ScalityDAO scality = new ScalityDAO();
                try {
                	scality.delete(bpid);
                    contextLogger.info("Deleted old Scality object: "+bpid);
                }
                catch (Exception e) {
                	logger.info("Error deleting Scality object "+bpid+"",e);
                }
            }
            else {
                File file = new File(filepath);
                try {
                    FileUtils.forceDelete(file);
                    contextLogger.info("Deleted old file: "+filepath);
                }
                catch (Exception e) {
                    logger.info("Error deleting file "+filepath,e);
                }
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
