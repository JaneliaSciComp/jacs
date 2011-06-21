package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.apache.xerces.impl.dv.dtd.ENTITYDatatypeValidator;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiColorFlipOutFileDiscoveryService implements IService {

    private MultiColorFlipOutFileDiscoveryTask task;
    private String sessionName;
    private static String TOP_LEVEL_FOLDER_NAME_PARAM = "TOP_LEVEL_FOLDER_NAME";
    private static String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";
    private org.apache.log4j.Logger logger;
    List<String> directoryPathList = new ArrayList<String>();
    AnnotationBeanRemote annotationBean;
    ComputeBeanRemote computeBean;
    String topLevelFolderName;
    Entity topLevelFolder;
    User user;
    boolean neuronSeparatorTestFlag=false;

    public class LsmPair {
        public LsmPair() {}
        public Entity lsmEntity1;
        public Entity lsmEntity2;
    }

    public void execute(IProcessData processData) {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MultiColorFlipOutFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            computeBean = EJBFactory.getRemoteComputeBean();
            user = computeBean.getUserByName(task.getOwner());
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                } else if (paramName.equals(TOP_LEVEL_FOLDER_NAME_PARAM)) {
                    topLevelFolderName=(String)entry.getValue();
                }
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
            for (String directoryPath : directoryPathList) {
                logger.info(" MultiColorFlipOutFileDiscoveryService including directory = "+directoryPath);
            }
            createOrVerifyRootEntity();
            processDirectoriesForLsm();
            annotationBean.saveOrUpdateEntity(topLevelFolder);
            processFolderForSingleNeuronStackSets(topLevelFolder);
            annotationBean.saveOrUpdateEntity(topLevelFolder);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    protected void createOrVerifyRootEntity() throws Exception {
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
            logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
        } else if (topLevelFolders==null || topLevelFolders.size()==0) {
            logger.info("Creating new topLevelFolder with name="+topLevelFolderName);
            topLevelFolder=new Entity();
            Date createDate = new Date();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            EntityType folderType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            topLevelFolder.setEntityType(folderType);
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            logger.info("Calling saveOrUpdateEntity for topLevelFolder Entity");
            topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Done calling saveOrUpdateEntity for topLevelFolder Entity");
        }
        logger.info("Using topLevelFolder id="+topLevelFolder.getId());
    }

    protected void processDirectoriesForLsm() throws Exception {
        for (String directoryPath : directoryPathList) {
            logger.info("Processing dir="+directoryPath);
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } else {
                Entity folder = verifyOrCreateChildFolderFromDir(topLevelFolder, dir);
                processFolderForLsm(folder);
                annotationBean.saveOrUpdateEntity(folder);
            }
        }
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {
        logger.info("verifyOrCreateChildFolderFromDir() called with parentFolder id="+parentFolder.getId());
        logger.info("Looking for Entity matching Folder path="+dir.getAbsolutePath());
        Entity folder=null;
        Set<EntityData> data=parentFolder.getEntityData();
        for (EntityData ed : data) {
            long childId=0;
            String entityTypeName="<not specified>";
            if (ed.getChildEntity()!=null) {
                childId=ed.getChildEntity().getId();
                entityTypeName=ed.getChildEntity().getEntityType().getName();
            }
            logger.info("Checking EntityData entry with value="+ed.getValue()+" childEntityId="+childId+" entityType="+entityTypeName);
            if (ed.getChildEntity()!=null &&
                    ed.getChildEntity().getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                String folderPath = ed.getChildEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (folderPath==null) {
                    throw new Exception("Unexpectedly could not find ATTRIBUTE_FILE_PATH for entity id="+ed.getChildEntity().getId());
                }
                if (folderPath.equals(dir.getAbsolutePath())) {
                    if (folder!=null) {
                        throw new Exception("Unexpectedly found multiple child folders with path=" + dir.getAbsolutePath()+" for parent folder id="+parentFolder.getId());
                    }
                    folder = ed.getChildEntity();
                }
            } else {
                logger.info("EntityData does not qualify as Folder");
            }
        }
        if (folder!=null) {
            logger.info("Found folder="+folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            return folder;
        } else {
            logger.info("Could not find matching folder, so creating new one");
            // We need to create a new folder
            folder = new Entity();
            Date createDate = new Date();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setUser(user);
            folder.setName(dir.getAbsolutePath());
            EntityType folderType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            folder.setEntityType(folderType);
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder=annotationBean.saveOrUpdateEntity(folder);
            logger.info("After saving new folder, id="+folder.getId());
            parentFolder.addChildEntity(folder);
            logger.info("Before updating parentFolder id="+parentFolder.getId());
            //annotationBean.saveOrUpdateEntity(parentFolder);
            logger.info("After updating parentFolder id="+parentFolder.getId());
            logger.info("Created new folder with path="+folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        }
        return folder;
    }

    protected void processFolderForLsm(Entity folder) throws Exception {
        File dir=new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        File[] dirContents = dir.listFiles();
        for (File file : dirContents) {
            if (file.isDirectory()) {
                Entity subfolder=verifyOrCreateChildFolderFromDir(folder, file);
                processFolderForLsm(subfolder);
                annotationBean.saveOrUpdateEntity(subfolder);
            } else {
                logger.info("Found file = " + file.getAbsolutePath());
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                    verifyOrCreateLsmStack(folder, file);
                }
            }
        }
    }

    protected void verifyOrCreateLsmStack(Entity folder, File file) throws Exception {
        Entity lsmStack=null;
        logger.info("Considering LSM file = " + file.getAbsolutePath());
        List<Entity> possibleLsmFiles = annotationBean.getEntitiesWithFilePath(file.getAbsolutePath());
        List<Entity> lsmStacks = new ArrayList<Entity>();
        for (Entity entity : possibleLsmFiles) {
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                lsmStacks.add(entity);
            }
        }
        if (lsmStacks.size()==0) {
            lsmStack = createLsmStackFromFile(file);
            folder.addChildEntity(lsmStack);
         } else if (lsmStacks.size()==1) {
            logger.info("Found lsm stack = " + lsmStacks.get(0).getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            lsmStack = lsmStacks.get(0);
            // Make sure the folder already contains it
            boolean hasLsmStack=false;
            for (EntityData ed : folder.getEntityData()) {
                if (ed.getChildEntity()!=null && ed.getChildEntity().getId().equals(lsmStack.getId())) {
                    hasLsmStack=true;
                    break;
                }
            }
            if (!hasLsmStack) {
                logger.info("Although the lsm stack already exists, it does not seem to be part of the folder so we are adding it");
                folder.addChildEntity(lsmStack);
            } else {
                logger.info("The folder already contains the lsm stack so we do not need to add it again");
            }
        } else {
            throw new Exception("Unexpectedly found " + lsmStacks.size() + " lsm stacks for file="+file.getAbsolutePath());
        }
    }

    protected Entity createLsmStackFromFile(File file) throws Exception {
        EntityType lsmEntityType = annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK);
        if (lsmEntityType==null) {
            throw new Exception("Could not find EntityType = " + EntityConstants.TYPE_LSM_STACK);
        }
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(lsmEntityType);
        Date createDate = new Date();
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        logger.info("Created lsm stack = " +lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        return lsmStack;
    }

    protected void launchColorSeparationPipeline(LsmPair lsmPair) throws Exception {

        Entity lsmStack = lsmPair.lsmEntity1; // placeholder
        if (!neuronSeparatorTestFlag) {
            String lsmFilePath=lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            NeuronSeparatorTask neuTask = new NeuronSeparatorTask(
                    new HashSet<Node>(), user.getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>());
            neuTask.setJobName("Neuron Separator for MultiColorFlipOutFileDiscovery input="+lsmFilePath);
            neuTask.setParameter(NeuronSeparatorTask.PARAM_inputFilePath, lsmFilePath);
            neuTask = (NeuronSeparatorTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
            EJBFactory.getRemoteComputeBean().submitJob("NeuronSeparation", neuTask.getObjectId());
            Thread.sleep(2000);
            NeuronSeparatorResultNode resultNode = (NeuronSeparatorResultNode)EJBFactory.getRemoteComputeBean().getResultNodeByTaskId(neuTask.getObjectId());
            logger.info("MultiColorFlipOutFileDiscoveryService assuming NeuronSeparator output node path="+resultNode.getDirectoryPath());
            neuronSeparatorTestFlag=true;
        }
    }

    protected void processFolderForSingleNeuronStackSets(Entity folder) throws Exception {
        //
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
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    lsmStackList.add(child);
                } else if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)){
                    processFolderForSingleNeuronStackSets(child);
                }
            }
        }

        // At this point we have a collection of lsm files in this folder - we will analyze for pairs
        Set<LsmPair> lsmPairs = findLsmPairs(lsmStackList);

        // We will consider each pair, and if the lsm members of the pair do not have a parent which
        // is a SingleNeuronSeparatorPipeline entity, the pair will be submitted for processing with
        // this pipeline.
        for (LsmPair lsmPair : lsmPairs) {
            Set<Entity> lsm1Parents=annotationBean.getParentEntities(lsmPair.lsmEntity1.getId());
            Set<Entity> lsm2Parents=annotationBean.getParentEntities(lsmPair.lsmEntity2.getId());
            long lsm1ResultId=0;
            long lsm2ResultId=0;
            for (Entity e : lsm1Parents) {
                if (e.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                    lsm1ResultId=e.getId();
                    break;
                }
            }
            for (Entity e : lsm2Parents) {
                if (e.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                    lsm2ResultId=e.getId();
                    break;
                }
            }
            if (lsm1ResultId!=0 && (lsm1ResultId==lsm2ResultId)) {
                // We already have a result
                logger.info("Found prior result, skipping lsm pair { " +lsmPair.lsmEntity1.getId()+" ,"+lsmPair.lsmEntity2.getId() + "} " +
                    " prior result id="+lsm1ResultId);
            } else {
                launchColorSeparationPipeline(lsmPair);
            }
        }
    }

    private Set<LsmPair> findLsmPairs(List<Entity> lsmStackList) throws Exception {
        Set<LsmPair> pairSet=new HashSet<LsmPair>();
        Pattern lsmPattern=Pattern.compile("(\\S+)\\_L(\\d+)(.*\\.lsm)");
        Set<Entity> alreadyPaired=new HashSet<Entity>();
        for (Entity lsm1 : lsmStackList) {
            if (!alreadyPaired.contains(lsm1)) {
                String lsm1Filename=lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (lsm1Filename==null || lsm1Filename.length()==0) {
                    throw new Exception("lsm id="+lsm1.getId()+" unexpectedly does not have an ATTRIBUTE_FILE_PATH");
                }
                Matcher lsm1Matcher=lsmPattern.matcher(lsm1Filename);
                if (lsm1Matcher.matches() && lsm1Matcher.groupCount()==3) {
                    String lsm1Prefix=lsm1Matcher.group(1);
                    String lsm1Index=lsm1Matcher.group(2);
                    String lsm1Suffix=lsm1Matcher.group(3);
                    Set<Entity> possibleMatches=new HashSet<Entity>();
                    for (Entity lsm2 : lsmStackList) {
                        if (!alreadyPaired.contains(lsm2)) {
                            String lsm2Filename=lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                            if (lsm2Filename==null || lsm2Filename.length()==0) {
                                throw new Exception("lsm id="+lsm2.getId()+" unexpectedly does not have an ATTRIBUTE_FILE_PATH");
                            }
                            if (!lsm1Filename.equals(lsm2Filename)) {
                                // Obviously we do not want to pair something to itself
                                Matcher lsm2Matcher=lsmPattern.matcher(lsm2Filename);
                                if (lsm2Matcher.matches() && lsm2Matcher.groupCount()==3) {
                                    String lsm2Prefix=lsm2Matcher.group(1);
                                    String lsm2Index=lsm2Matcher.group(2);
                                    String lsm2Suffix=lsm2Matcher.group(3);
                                    if (lsm1Prefix.equals(lsm2Prefix) && lsm1Suffix.equals(lsm2Suffix)) {
                                        possibleMatches.add(lsm2);
                                    }
                                }
                            }
                        }
                        if (possibleMatches.size()==1) {
                            // We have a unique match
                            alreadyPaired.add(lsm1);
                            alreadyPaired.add(lsm2);
                            LsmPair pair=new LsmPair();
                            pair.lsmEntity1=lsm1;
                            pair.lsmEntity2=lsm2;
                            pairSet.add(pair);
                        }
                    }
                }
            }
        }
        return pairSet;
    }

}

