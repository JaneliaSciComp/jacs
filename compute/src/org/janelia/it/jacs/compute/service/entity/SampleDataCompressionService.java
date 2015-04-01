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
import org.janelia.it.jacs.shared.utils.EntityUtils;

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

    private String mode = MODE_UNDEFINED;
    private String recordMode = RECORD_MODE_UPDATE;
    private boolean deleteSourceFiles = true;
    
    private String rootEntityId;
    private Set<String> inputFiles = new HashSet<String>();
    private Map<String,Set<Long>> entityMap = new HashMap<String,Set<Long>>();

    protected int numChanges;

    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        mode = data.getRequiredItemAsString("MODE");
    	rootEntityId = data.getItemAsString("ROOT_ENTITY_ID");
    	
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

        String inputType = data.getRequiredItemAsString("INPUT_TYPE");
        
        processEntities(inputType);
        
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


        String outputType = data.getRequiredItemAsString("OUTPUT_TYPE");
    	List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");

        List<String> outputPaths = new ArrayList<String>();
    	for(String filepath : inputPaths) {
    	    String extension = getExtension(filepath);
            outputPaths.add(filepath.replaceAll(extension, outputType));
    	}
        processData.putItem("OUTPUT_PATH_LIST", outputPaths);
    }
    
	public void processEntities(String extension) throws ComputeException {

        if (isDebug) {
            logger.info("This is a test run. Nothing will actually happen.");
        }
        else {
            logger.info("This is the real thing. Files will get compressed, and then the originals will be deleted!");
        }
        
        if (rootEntityId!=null) {
            logger.info("Finding files to compress under root "+rootEntityId+" with type "+extension);
        	
        	Entity entity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(rootEntityId));
        	if (entity == null) {
        		throw new IllegalArgumentException("Entity not found with id="+rootEntityId);
        	}
        	
        	for(Entity image : EntityUtils.getDescendantsOfType(entity,EntityConstants.TYPE_IMAGE_3D)) {
        		String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		if (filepath!=null && filepath.endsWith(extension)) {
        			addEntityToInputList(image);
        		}
        	}
        }
        else {
        	logger.info("Finding files belonging to "+ownerKey+" with type "+extension);
        	
    	    for(Entity entity : entityBean.getUserEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_FILE_PATH, "%"+extension)) {
    			if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
    				logger.warn("Ignoring entity with filepath that is not an Image 3D: "+entity.getId());
    				continue;
    			}
    			if (!entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).contains(centralDir)) {
    				logger.warn("Ignoring entity with which does not contain the FileStore.CentralDir: "+entity.getId());
    				continue;
    			}
    			addEntityToInputList(entity);
    		}
        }
    }
    
    private void addEntityToInputList(Entity imageEntity) throws ComputeException {

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
    		eset = new HashSet<>();
    		entityMap.put(filepath, eset);
    	}
    	eset.add(imageEntity.getId());
    }
    
    private void doComplete() throws ComputeException {

        this.recordMode = data.getRequiredItemAsString("RECORD_MODE");
        this.deleteSourceFiles = !"false".equals(data.getRequiredItem("DELETE_INPUTS"));
    	this.entityMap = (Map<String,Set<Long>>)data.getRequiredItem("ENTITY_MAP");
    	List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");
        List<String> outputPaths = (List<String>)data.getRequiredItem("OUTPUT_PATH_LIST");
    	
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
        
        String inputExtension = getExtension(inputPath);
        String outputExtension = getExtension(outputPath);
        
    	try {
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
                logger.warn("Missing or corrupt output file: "+outputFile);
                return;
            }
            
            if (RECORD_MODE_UPDATE.equals(recordMode)) {
                if (!isDebug) {
            	    // Update all entities which referred to the old path
            	    int numUpdated = entityBean.bulkUpdateEntityDataValue(inputPath, outputPath);
                    logger.info("Updated "+numUpdated+" entities to use new compressed file: "+outputPath);
                }
            }
            
            Set<Long> inputEntites = entityMap.get(inputPath);
            if (inputEntites == null) {
                logger.warn("No entities found with this path: "+inputPath);
                return;
            }
            
            Entity secEntity = null;
            
    	    // Update the input stacks

        	for(Entity entity : entityBean.getEntitiesById(new ArrayList<Long>(inputEntites))) {

                if (RECORD_MODE_UPDATE.equals(recordMode)) {
                    // Update the format
            		String format = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
            		if (format != null && !"".equals(format)) {
            			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, outputExtension);
            		}
            		
            		// Update the name
            		if (entity.getName().endsWith(inputExtension)) {
            			entity.setName(entity.getName().replaceAll(inputExtension, outputExtension));
            		}
            		
            		if (!isDebug) {
    	            	Entity savedEntity = entityBean.saveOrUpdateEntity(entity);
    	            	logger.info("Updated entity: "+savedEntity.getName()+" (id="+savedEntity.getId()+")");
            		}
            		else {
            			logger.info("Updated entity: "+entity.getName()+" (id="+entity.getId()+")");
            		}
                }
                else if (RECORD_MODE_ADD.equals(recordMode)) {
                    if (secEntity==null) {
                        if (!isDebug) {
                            String secName = entity.getName().replaceAll(inputExtension, outputExtension);
                            secEntity = entityBean.createEntity(entity.getOwnerKey(), EntityConstants.TYPE_IMAGE_3D, secName);
    
                            // Update the format
                            String format = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
                            if (format != null && !"".equals(format)) {
                                entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, outputExtension);
                            }
                            entityBean.saveOrUpdateEntity(entity);
                        }
                        logger.info("Created secondary entity: "+secEntity.getName()+" (id="+secEntity.getId()+")");
                    }
                    if (!isDebug) {
                        entityBean.addEntityToParent(entity, secEntity, entity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_LOSSY_IMAGE);
                    }
                }
                else {
                    throw new IllegalStateException("Illegal RECORD_MODE: "+recordMode);
                }
                
            	numChanges++;
        	}
    	
        	if (deleteSourceFiles) {
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
    		
    	}
    	catch (ComputeException e) {
    		logger.error("Unable to update all entities to use new compressed file: "+outputPath);
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
