package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOStitchedFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 */
public class MCFOStitchedFileDiscoveryService implements IService {

    private static final String TOP_LEVEL_FOLDER_NAME_PARAM = "TOP_LEVEL_FOLDER_NAME";
    private static final String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";
    private static final String LINKING_FOLDER = "LINKING_FOLDER";

    private String linkingDir;
    private boolean refresh = true;
    private boolean runNeuronSeperator = true;
    
    private Logger logger;
    private MCFOStitchedFileDiscoveryTask task;
    private AnnotationBeanLocal annotationBean;
    private ComputeBeanLocal computeBean;
    private User user;
    private Date createDate;
    
    private List<String> directoryPathList = new ArrayList<String>();
    
    public void execute(IProcessData processData) {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MCFOStitchedFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(task.getOwner());
            createDate = new Date();
            refresh = "true".equals(task.getParameter(MCFOStitchedFileDiscoveryTask.PARAM_refresh));
            
            String topLevelFolderName = null;
            
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                } 
                else if (paramName.equals(TOP_LEVEL_FOLDER_NAME_PARAM)) {
                    topLevelFolderName = (String)entry.getValue();
                }
                else if (paramName.equals(LINKING_FOLDER)) {
                	linkingDir = (String)entry.getValue();
                	linkingDir = linkingDir.replaceAll("%USER%", user.getUserLogin());
                }
            }

            if (topLevelFolderName == null) {
            	throw new Exception("No TOP_LEVEL_FOLDER_NAME parameter provided");
            }
            
            String taskInputDirectoryList = task.getParameter(MCFOStitchedFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList != null) {
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
            }

            if (directoryPathList.isEmpty()) {
            	throw new Exception("No input directories provided");
            }
            
            for (String directoryPath : directoryPathList) {
                logger.info("MCFOStitchedFileDiscoveryService including directory = "+directoryPath);
            }
            
            Entity topLevelFolder = createOrVerifyRootEntity(topLevelFolderName);
            processStitchedDirectories(topLevelFolder);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
    	
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);

    	Entity topLevelFolder = null;
        if (topLevelFolders!=null) {
        	// Only accept the current user's top level folder
	        for(Entity entity : topLevelFolders) {
	        	if (entity.getUser().getUserLogin().equals(user.getUserLogin())) {
	                // This is the folder we want, now load the entire folder hierarchy
	                topLevelFolder = annotationBean.getFolderTree(entity.getId());
	                logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
	        		break;
	        	}
	        }
        }
         
        if (topLevelFolder == null) {
            logger.info("Creating new topLevelFolder with name="+topLevelFolderName);
            topLevelFolder = new Entity();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            topLevelFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Saved top level folder as "+topLevelFolder.getId());
        }
        
        logger.info("Using topLevelFolder with id="+topLevelFolder.getId());
        return topLevelFolder;
    }

    protected void processStitchedDirectories(Entity topLevelFolder) throws Exception {
        for (String directoryPath : directoryPathList) {
            logger.info("Processing dir="+directoryPath);
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } 
            else {
                Entity folder = verifyOrCreateChildFolderFromDir(topLevelFolder, dir);
                logger.info("Searching this dir for stitched folders="+dir.getAbsolutePath());
                for (File stitchedDir : dir.listFiles()) {
                    if (stitchedDir.isDirectory()) {
                        Entity stitchedFolder = verifyOrCreateChildFolderFromDir(folder, stitchedDir);
                        logger.info("Processing stitched folder="+stitchedDir.getAbsolutePath());
                        processStitchedFolder(stitchedFolder);
                    }
                }
            }
        }
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {

        logger.info("Looking for folder entity with path "+dir.getAbsolutePath()+" in parent folder "+parentFolder.getId());
        Entity folder = null;
        
        for (EntityData ed : parentFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
        	
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                String folderPath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (folderPath == null) {
                    logger.warn("Unexpectedly could not find attribute '"+EntityConstants.ATTRIBUTE_FILE_PATH+"' for entity id="+child.getId());
                }
                else if (folderPath.equals(dir.getAbsolutePath())) {
                    if (folder != null) {
                    	logger.warn("Unexpectedly found multiple child folders with path=" + dir.getAbsolutePath()+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                    	folder = ed.getChildEntity();	
                    }
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setUser(user);
            folder.setName(dir.getName());
            folder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder = annotationBean.saveOrUpdateEntity(folder);
            logger.info("Saved folder as "+folder.getId());
            addToParent(parentFolder, folder);
        }
        else {
            logger.info("Found folder with id="+folder.getId());
        }
        
        return folder;
    }

    protected void processStitchedFolder(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder=" + dir.getAbsolutePath() + " id=" + folder.getId());

        if (!dir.canRead()) {
            logger.info("Cannot read from folder " + dir.getAbsolutePath());
            return;
        }

        File stitchedDir = null;

        for (File file : dir.listFiles()) {
            if (file.isDirectory() && file.getName().equals("stitched")) {
                stitchedDir = file;
            }
        }

        if (stitchedDir != null) {
            Entity sample = findExistingSample(folder);
            String linkName = dir.getName();
            File symbolicLink = new File(linkingDir, linkName);

            try {

                if (sample!=null && !refresh) {
                    logger.info("  Existing sample exists (id=" + sample.getId() + "), skipping.");
                    return;
                }

                Entity stitchedStack=null;
                if (sample == null) {
                    String sampleName = getSampleNameFromDir(stitchedDir);
                    File stackFile = getStackFileFromStitchedDir(stitchedDir);
                    if (stackFile==null) {
                        logger.info("Could not locate stack file - skipping folder="+dir.getAbsolutePath());
                        return;
                    }
                    stitchedStack = createStitchedStackFromFile(stackFile);
                    sample = createSample(stitchedStack, sampleName, symbolicLink);
                    addToParent(folder, sample, 0);
                } else {
                    stitchedStack = getStitchedStackFromSample(sample);
                }

                launchColorSeparationPipeline(sample, stitchedStack, symbolicLink);


            } catch (ComputeException e) {
                logger.warn("Could not delete existing sample for regeneration, id=" + sample.getId(), e);
            }

        } else {
            logger.info("Could not locate stitched subdirectory...skipping this folder=" + dir.getAbsolutePath());
        }

    }

    private Entity getStitchedStackFromSample(Entity sample) throws Exception {
        for (EntityData ed : sample.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child == null) continue;
            if (!EntityConstants.TYPE_STITCHED_V3D_RAW.equals(child.getEntityType().getName())) continue;
            return child;
        }
        return null;
    }

    private String getSampleNameFromDir(File stitchedDir) throws Exception {
        String sampleName=null;
        for (File file : stitchedDir.listFiles()) {
            if (file.getName().endsWith("-stitched.v3draw")) {
                int endPosition=file.getName().lastIndexOf("-stitched.v3draw");
                if (endPosition>0) {
                    sampleName=file.getName().substring(0,endPosition);
                }
            }
        }
        return sampleName;
    }

    private File getStackFileFromStitchedDir(File stitchedDir) throws Exception {
        for (File file : stitchedDir.listFiles()) {
            if (file.getName().endsWith("-stitched.v3draw")) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * Find and return the child Sample entity
     */
    private Entity findExistingSample(Entity folder) {

    	for(EntityData ed : folder.getEntityData()) {
			Entity sampleFolder = ed.getChildEntity();
    		if (sampleFolder == null) continue;
    		if (!EntityConstants.TYPE_SAMPLE.equals(sampleFolder.getEntityType().getName())) continue;
    	    return sampleFolder;
    	}

        // Could not find sample child entity
    	return null;
    }

    protected Entity createSample(Entity stack, String name, File link) throws Exception {
		
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample = annotationBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());

        addToParent(sample, stack, 0);
        
        return sample;
    }

      private Entity createStitchedStackFromFile(File file) throws Exception {
        Entity stitchedStack = new Entity();
        stitchedStack.setUser(user);
        stitchedStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_STITCHED_V3D_RAW));
        stitchedStack.setCreationDate(createDate);
        stitchedStack.setUpdatedDate(createDate);
        stitchedStack.setName(file.getName());
        stitchedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stitchedStack = annotationBean.saveOrUpdateEntity(stitchedStack);
        logger.info("Saved stitched stack as "+stitchedStack.getId());
        return stitchedStack;
    }

    private void launchColorSeparationPipeline(Entity sample, Entity stitchedStack, File symbolicLink) throws Exception {
        if (runNeuronSeperator) {

        	NeuronSeparatorPipelineTask neuTask = new NeuronSeparatorPipelineTask(new HashSet<Node>(), 
            		user.getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>());
            String stackId = ""+stitchedStack.getId();
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputStitchedStackId, stackId);
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_symbolLinkName, symbolicLink.getAbsolutePath());
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId, sample.getId().toString());
            computeBean.saveOrUpdateTask(neuTask);

	        neuTask.setJobName("Remote Neuron Separator for MCFOStitchedFileDiscovery, task id="+neuTask.getObjectId());
	        computeBean.submitJob("NeuronSeparationPipelineRemote", neuTask.getObjectId());
        }
        
    }

    private void addToParent(Entity parent, Entity entity) throws Exception {
    	addToParent(parent, entity, null);
    }
    
    private void addToParent(Entity parent, Entity entity, Integer index) throws Exception {
        EntityData ed = parent.addChildEntity(entity);
        ed.setOrderIndex(index);
        annotationBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+" as child of "+parent.getEntityType().getName()+"#"+entity.getId());
    }
}
