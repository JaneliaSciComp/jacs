package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Compress existing v3draw files to v3dpbd to save space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFODataCompressService implements IService {

    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final String MODE_CREATE_OUTPUT_LIST = "CREATE_OUTPUT_LIST";
    public static final String MODE_COMPLETE = "COMPLETE";
    public static final int GROUP_SIZE = 200;
    public transient static final String PARAM_testRun = "is test run";
	
    protected Logger logger;
    protected Task task;
    protected String username;
    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    
    protected int numChanges;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    private String mode = MODE_UNDEFINED;
    protected IProcessData processData;
    
    private Set<String> inputFiles = new HashSet<String>();
    private Map<String,Set<Entity>> entities = new HashMap<String,Set<Entity>>();
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            username = task.getOwner();
            isDebug = Boolean.parseBoolean(task.getParameter(PARAM_testRun));
            mode = processData.getString("MODE");
            this.processData = processData;

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
			logger.info("Encountered an exception. Before dying, we...");
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running MCFODataCompressService", e);
        }
    }
    
    private void doCreateInputList() throws ComputeException {

        logger.info("Finding V3DRAW files to compress...");
        
        if (isDebug) {
        	logger.info("This is a test run. Nothing will actually happen.");
        }
        else {
        	logger.info("This is the real thing. Files will get compressed, and then the originals will be deleted!");
        }
        
        processCommonRootFolders();
        
        List<List<String>> inputGroups = createGroups(inputFiles, GROUP_SIZE);
        processData.putItem("INPUT_PATH_LIST", inputGroups);
        processData.putItem("ENTITY_MAP", entities);
        
		logger.info("Processed "+inputFiles.size()+" entities into "+inputGroups.size()+" groups.");
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
        if (currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }
    
    private void doCreateOutputList() throws ComputeException {

    	List<String> inputPaths = (List<String> )processData.getItem("INPUT_PATH_LIST");
    	
    	List<String> outputPaths = new ArrayList<String>();
    	for(String s : inputPaths) {
    		outputPaths.add(s.replaceAll("v3draw", "v3dpbd"));
    	}
        processData.putItem("OUTPUT_PATH_LIST", outputPaths);
    }
    
	public void processCommonRootFolders() throws ComputeException {
		
        List<Entity> entities=annotationBean.getCommonRootEntitiesByTypeName(username, EntityConstants.TYPE_FOLDER);
        for(Entity topEntity : entities) {
            logger.info("Found top-level entity name="+topEntity.getName());
            Entity tree = annotationBean.getEntityTree(topEntity.getId());
            processEntityTree(tree);
        }
		logger.info("The processing was a success.");
    }
    
    public void processEntityTree(Entity entity) throws ComputeException {
    	
    	if (visited.contains(entity.getId())) return;
    	visited.add(entity.getId());

    	String entityTypeName = entity.getEntityType().getName();
    	
    	if (entityTypeName.equals(EntityConstants.TYPE_IMAGE_3D)) {
    		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		if (filepath.endsWith(".v3draw")) {
    			addV3dRawFile(entity);
    		}
    		else if (entity.getName().endsWith(".v3draw")) {
        		if (!isDebug) {
	    			// How did the name get out of sync? Let's fix it. 
	    			if (filepath.endsWith(".v3dpbd")) {
	    				entity.setName(entity.getName().replaceAll("v3draw", "v3dpbd"));
	    	        	annotationBean.saveOrUpdateEntity(entity);
	    	        	logger.info("Fixed entity name: "+entity.getName()+" (id="+entity.getId()+")");	
	    			}
        		}
    		}
    	}
    	
		for(Entity child : entity.getChildren()) {
			processEntityTree(child);
		}
    }
    
    private void addV3dRawFile(Entity imageEntity) throws ComputeException {

    	if (!imageEntity.getUser().getUserLogin().equals(username)) return;
    	
    	String filepath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	File file = new File(filepath);
    	
    	if (!file.exists()) {
    		logger.warn("File path for "+imageEntity.getId()+" does not exist: "+filepath);
    		return;
    	}
    	
    	logger.info("Will compress file: "+imageEntity.getName());
    	
    	inputFiles.add(filepath);
    	
    	Set<Entity> eset = entities.get(filepath);
    	if (eset == null) {
    		eset = new HashSet<Entity>();
    		entities.put(filepath, eset);
    	}
    	eset.add(imageEntity);
    }
    
    private void doComplete() throws ComputeException {

    	this.entities = (Map<String,Set<Entity>>)processData.getItem("ENTITY_MAP");
    	List<String> inputPaths = (List<String> )processData.getItem("INPUT_PATH_LIST");
    	List<String> outputPaths = (List<String> )processData.getItem("OUTPUT_PATH_LIST");
    	
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

    	Set<Entity> inputEntites = entities.get(inputPath);
    	if (inputEntites == null) {
    		logger.warn("No entities found with this path: "+inputPath);
    		return;
    	}

    	try {
    		if (!isDebug) {
	        	for(Entity entity : inputEntites) {
	        		entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, outputPath);
	        		String format = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT);
	        		if (format != null && !"".equals(format)) {
	        			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, "v3dpbd");
	        		}
	        		if (entity.getName().endsWith(".v3draw")) {
	        			entity.getName().replaceAll("v3draw", "v3dpbd");
	        		}
	            	annotationBean.saveOrUpdateEntity(entity);
	            	numChanges++;
	            	logger.info("Updated entity: "+entity.getName()+" (id="+entity.getId()+")");
	        	}
    		}
        	
        	logger.info("Updated all entities to use new compressed file: "+outputPath);
        	
        	if (!isDebug) {
        		File file = new File(inputPath);
        		FileUtils.forceDelete(file);
        		logger.info("Deleted old file: "+inputPath);
        		
        		if (file.getName().startsWith("merged-")) {
        			// Delete the symlink as well, if there is one
        			String filenodeDir = file.getParentFile().getParent();
        			File symlink = new File(filenodeDir, "group/"+file.getName());
        			if (symlink.exists()) {
        				symlink.delete();
        				logger.info("Deleted symlink: "+inputPath);
        			}
        		}
        		
        	}
    	}
    	catch (ComputeException e) {
    		logger.error("Unable to update all entities to use new compressed file: "+outputPath);
    	}
    	catch (IOException e) {
    		logger.error("Unable to delete file: "+inputPath);
    	}
	}
}
