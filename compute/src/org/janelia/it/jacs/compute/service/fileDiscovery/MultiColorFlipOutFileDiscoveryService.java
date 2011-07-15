package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 */
public class MultiColorFlipOutFileDiscoveryService implements IService {

    private static final String TOP_LEVEL_FOLDER_NAME_PARAM = "TOP_LEVEL_FOLDER_NAME";
    private static final String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";

    private boolean runNeuronSeperator = true;
    
    private Logger logger;
    private MultiColorFlipOutFileDiscoveryTask task;
    private AnnotationBeanRemote annotationBean;
    private ComputeBeanRemote computeBean;
    private User user;
    private Date createDate;
    
    private List<String> directoryPathList = new ArrayList<String>();

    private class LsmPair {
        public LsmPair() {}
        public Entity lsmEntity1;
        public Entity lsmEntity2;
    }
    
    public void execute(IProcessData processData) {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MultiColorFlipOutFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            computeBean = EJBFactory.getRemoteComputeBean();
            user = computeBean.getUserByName(task.getOwner());
            createDate = new Date();
            
            String topLevelFolderName = null;
            
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                } else if (paramName.equals(TOP_LEVEL_FOLDER_NAME_PARAM)) {
                    topLevelFolderName=(String)entry.getValue();
                }
            }
            
            if (topLevelFolderName == null) {
            	throw new Exception("No TOP_LEVEL_FOLDER_NAME parameter provided");
            }
            
            String taskInputDirectoryList = task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_inputDirectoryList);
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
                logger.info("MultiColorFlipOutFileDiscoveryService including directory = "+directoryPath);
            }
            
            Entity topLevelFolder = createOrVerifyRootEntity(topLevelFolderName);
            processDirectoriesForLsm(topLevelFolder);
            processFolderForSingleNeuronStackSets(topLevelFolder);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
    	
    	Entity topLevelFolder = null;
        // We expect there to be a single folder in the system with this name
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);
        
        if (topLevelFolders!=null && topLevelFolders.size()>1) {
            for (Entity e : topLevelFolders) {
                logger.info("Found topLevelFolder entityId="+e.getId());
            }
            throw new Exception("Unexpectedly found " + topLevelFolders.size()+" folders for MultiColorFlipOutFileDiscoveryService with name="+topLevelFolderName);
        }
        
        if (topLevelFolders!=null && topLevelFolders.size()==1) {
            topLevelFolder = topLevelFolders.iterator().next();
            // This is the folder we want, now load the entire folder hierarchy
            topLevelFolder = annotationBean.getFolderTree(topLevelFolder.getId());
            logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
        } 
        else if (topLevelFolders==null || topLevelFolders.size()==0) {
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

    protected void processDirectoriesForLsm(Entity topLevelFolder) throws Exception {
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
                processFolderForLsm(folder);
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

    protected void processFolderForLsm(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file);
                processFolderForLsm(subfolder);
            } 
            else {
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                    verifyOrCreateLsmStack(folder, file);
                }
            }
        }
    }

    protected void verifyOrCreateLsmStack(Entity parentFolder, File file) throws Exception {
    	
        Entity lsmStack = null;
        logger.info("Considering LSM file " + file.getName());
        List<Entity> possibleLsmFiles = annotationBean.getEntitiesWithFilePath(file.getAbsolutePath());
        List<Entity> lsmStacks = new ArrayList<Entity>();
        for (Entity entity : possibleLsmFiles) {
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                lsmStacks.add(entity);
            }
        }
        
        if (lsmStacks.size() == 0) {
            lsmStack = createLsmStackFromFile(file);
            addToParent(parentFolder, lsmStack);
         } 
        else if (lsmStacks.size() == 1) {
            lsmStack = lsmStacks.get(0);
            
            // Make sure the folder already contains it
            boolean hasLsmStack=false;
            for (EntityData ed : parentFolder.getEntityData()) {
                if (ed.getChildEntity()!=null && ed.getChildEntity().getId().equals(lsmStack.getId())) {
                    hasLsmStack = true;
                    break;
                }
            }
            
            if (!hasLsmStack) {
                logger.info("LSM already exists");
                // We can't do this check anymore because the LSMs are moved out of the folder and into the Sample later on
            	// TODO: check to make sure the LSM stack is a descendant of the folder. Or maybe just remove this check completely.
            	//logger.info("Although the LSM stack already exists, it does not seem to be part of the folder so we are adding it");
                //addToParent(parentFolder, lsmStack);
            } 
            else {
                logger.info("The folder already contains the LSM stack so we do not need to add it again");
            }
            
        } 
        else {
            logger.warn("Unexpectedly found " + lsmStacks.size() + " lsm stacks for file="+file.getAbsolutePath());
        }
    }

    protected Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }

    protected void processFolderForSingleNeuronStackSets(Entity folder) throws Exception {

        // In this method, we will start in the top-level folder, and recursively search each subfolder.
        // Within a given folder, we will find the set of lsm files (if any).
        // For each lsm file, we will try to find a filename-based unique match, indicating they may
        // be pairs of a single imaging run.
        // If a unique match is not found, then the files are not included in a set.
        // Each matched pair of files will be considered a candidate set. A search will be done to
        // see if the members are part of an already-existing set.
        // If the pair is not already part of a set, then:
        //   (1) a new SingleNeuronStackSet will be created
        //   (2) the v3d cmd tool will be used to generate a combined signal tif file
        //   (3) the signal tif file will be used as input to the SingleNeuronSeparatorPipeline for processing

        // Check that this entity is a folder
        if (!folder.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
            throw new Exception("Expected folder entity type but received type="+folder.getEntityType().getName());
        }

        // Scan through children:
        //   * For child folders, recursively call this function
        //   * For lsm files, add to list for analysis
        List<Entity> lsmStackList=new ArrayList<Entity>();
        for (EntityData ed : folder.getEntityData()) {
            Entity child = 	ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    lsmStackList.add(child);
                } 
                else if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                    processFolderForSingleNeuronStackSets(child);
                }
            }
        }

        // At this point we have a collection of lsm files in this folder - we will analyze for pairs
        logger.info("Checking for LSM pairs in folder "+folder.getName());
        Set<LsmPair> lsmPairs = findLsmPairs(lsmStackList);
        logger.info("    Found " + lsmPairs.size() + " pairs");

        // We will consider each pair, and if the lsm members of the pair do not have a parent which
        // is an LSM Stack Pair entity, the pair will be submitted for processing with
        // this pipeline.
        for (LsmPair lsmPair : lsmPairs) {
//            Set<Entity> lsm1Parents = annotationBean.getParentEntities(lsmPair.lsmEntity1.getId());
//            Set<Entity> lsm2Parents = annotationBean.getParentEntities(lsmPair.lsmEntity2.getId());
//            Long lsm1ResultId = null;
//            Long lsm2ResultId = null;
//            for (Entity e : lsm1Parents) {
//                if (e.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK_PAIR)) {
//                    lsm1ResultId = e.getId();
//                    break;
//                }
//            }
//            for (Entity e : lsm2Parents) {
//                if (e.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK_PAIR)) {
//                    lsm2ResultId = e.getId();
//                    break;
//                }
//            }
//            if (lsm1ResultId != null && (lsm1ResultId==lsm2ResultId)) {
//                // We already have a result
//                logger.info("Found prior result, skipping LSM pair " +lsmPair.lsmEntity1.getId()+","+lsmPair.lsmEntity2.getId() + " prior result id="+lsm1ResultId);
//            } else {
//                launchColorSeparationPipeline(lsmPair);
//            }
        	
        	// Remove the two LSM Stacks from the original folder, and put them under their own paired folder

    		logger.info("Moving paired LSM stacks to their own Sample entity");
    		
        	Set<EntityData> datas = annotationBean.getParentEntityDatas(lsmPair.lsmEntity1.getId());
        	for(EntityData data : datas) {
        		computeBean.genericDelete(data);
        		logger.info("Deleted old parent link "+data.getId()+" for LSM stack "+lsmPair.lsmEntity1.getId());
        	}
        	
        	datas = annotationBean.getParentEntityDatas(lsmPair.lsmEntity2.getId());
        	for(EntityData data : datas) {
        		computeBean.genericDelete(data);
        		logger.info("Deleted old parent link "+data.getId()+" for LSM stack "+lsmPair.lsmEntity2.getId());
        	}
        	
        	Entity sample = createSample(lsmPair);
            addToParent(folder, sample);
        	
        	launchColorSeparationPipeline(sample, lsmPair);
        }
    }

    protected Entity createSample(LsmPair lsmPair) throws Exception {

        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName("Sample");
        sample = annotationBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        
    	Entity lsmStackPair = new Entity();
        lsmStackPair.setUser(user);
        lsmStackPair.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
        lsmStackPair.setCreationDate(createDate);
        lsmStackPair.setUpdatedDate(createDate);
        lsmStackPair.setName("Scans");
        lsmStackPair = annotationBean.saveOrUpdateEntity(lsmStackPair);
        logger.info("Saved LSM stack pair as "+lsmStackPair.getId());
        addToParent(sample, lsmStackPair);
        addToParent(lsmStackPair, lsmPair.lsmEntity1);
        addToParent(lsmStackPair, lsmPair.lsmEntity2);
        
        return sample;
    }

    private Set<LsmPair> findLsmPairs(List<Entity> lsmStackList) throws Exception {
    	
        Set<LsmPair> pairSet = new HashSet<LsmPair>();
        Pattern lsmPattern = Pattern.compile("(.+)\\_L(\\d+)(.*\\.lsm)");
        Set<Entity> alreadyPaired = new HashSet<Entity>();
        
        for (Entity lsm1 : lsmStackList) {
        	
            if (alreadyPaired.contains(lsm1)) continue;
            	
            String lsm1Filename = lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (lsm1Filename==null || lsm1Filename.length()==0) {
                throw new Exception("LSM id="+lsm1.getId()+" unexpectedly does not have an ATTRIBUTE_FILE_PATH");
            }
            
            Matcher lsm1Matcher = lsmPattern.matcher(lsm1Filename);
            if (lsm1Matcher.matches() && lsm1Matcher.groupCount()==3) {
                String lsm1Prefix = lsm1Matcher.group(1);
                String lsm1Index = lsm1Matcher.group(2);
                String lsm1Suffix = lsm1Matcher.group(3);
                //logger.info("findLsmPairs: filename="+lsm1Filename+" prefix="+lsm1Prefix+" index="+lsm1Index+" suffix="+lsm1Suffix);
                
                Set<Entity> possibleMatches = new HashSet<Entity>();
                for (Entity lsm2 : lsmStackList) {
                	
                	if (alreadyPaired.contains(lsm2)) continue;
                    
                    String lsm2Filename = lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (lsm2Filename==null || lsm2Filename.length()==0) {
                        throw new Exception("lsm id="+lsm2.getId()+" unexpectedly does not have an ATTRIBUTE_FILE_PATH");
                    }
                    
                    if (!lsm1Filename.equals(lsm2Filename)) {
                        // Obviously we do not want to pair something to itself
                        Matcher lsm2Matcher = lsmPattern.matcher(lsm2Filename);
                        if (lsm2Matcher.matches() && lsm2Matcher.groupCount()==3) {
                            String lsm2Prefix=lsm2Matcher.group(1);
                            String lsm2Index=lsm2Matcher.group(2);
                            String lsm2Suffix=lsm2Matcher.group(3);
                            
                            Integer lsm1IndexInt=null;
                            Integer lsm2IndexInt=null;

                            boolean indexMatch=false;
                            try {
                                lsm1IndexInt = new Integer(lsm1Index.trim());
                                lsm2IndexInt = new Integer(lsm2Index.trim());
                            } 
                            catch (Exception ex) {
                                indexMatch = true;
                            }
                            
                            if ( !indexMatch && 
                            		((lsm1IndexInt%2==0 && lsm2IndexInt==lsm1IndexInt-1) || 
                            				(lsm2IndexInt%2==0 && lsm1IndexInt==lsm2IndexInt-1))) {
                                indexMatch=true;
                            }
                            
                            if (indexMatch && lsm1Prefix.equals(lsm2Prefix) && lsm1Suffix.equals(lsm2Suffix)) {
                                //logger.info("Possible match = TRUE for " + lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) + " , " +
                                //lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                                possibleMatches.add(lsm2);
                            } 
                            else {
                                //logger.info("Possible match = FALSE for " + lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) + " , " +
                                //lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                            }
                        }
                    }
                }
                
                if (possibleMatches.size() == 1) {
                    // We have a unique match
                    Entity lsm2 = possibleMatches.iterator().next();
                    alreadyPaired.add(lsm1);
                    alreadyPaired.add(lsm2);
                    LsmPair pair = new LsmPair();
                    pair.lsmEntity1 = lsm1;
                    pair.lsmEntity2 = lsm2;
                    pairSet.add(pair);
                    logger.info("Adding lsm pair: " + lsm1.getId() + ", " + lsm2.getId());
                }

            }
        }
        return pairSet;
    }

    protected void launchColorSeparationPipeline(Entity sample, LsmPair lsmPair) throws Exception {
        if (runNeuronSeperator) {
            //String lsm1FilePath=lsmPair.lsmEntity1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            //String lsm2FilePath=lsmPair.lsmEntity2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

            NeuronSeparatorPipelineTask neuTask = new NeuronSeparatorPipelineTask(
                    new HashSet<Node>(), user.getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>());

            // Rather than use the file parameter, we will use the entity parameter since we want to add these
            // as children to the result entity
            String lsmEntityIds = lsmPair.lsmEntity1.getId()+" , "+lsmPair.lsmEntity2.getId();
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList, lsmEntityIds);
            String sampleEntityId = sample.getId().toString();
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId, sampleEntityId);
            neuTask.setJobName("Neuron Separator for MultiColorFlipOutFileDiscovery");
            EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
            EJBFactory.getRemoteComputeBean().submitJob("NeuronSeparationPipeline", neuTask.getObjectId());
            
//            runNeuronSeperator = false;
        }
    }
    
    private void addToParent(Entity parent, Entity entity) throws Exception {
        EntityData ed = parent.addChildEntity(entity);
        computeBean.genericSave(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+" as child of "+parent.getEntityType().getName()+"#"+entity.getId());
    }
}
