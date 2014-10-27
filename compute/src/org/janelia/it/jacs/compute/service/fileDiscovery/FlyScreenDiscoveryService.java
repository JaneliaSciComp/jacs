package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.screen.FlyScreenSampleService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.ScreenPipelineResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenDiscoveryService extends FileDiscoveryService {

    public static final String SCREEN_SAMPLE_TOP_LEVEL_FOLDER_NAME = "FlyLight Screen Data";

    // In this discovery service, we do not want to create an entity tree which looks like the
    // filesystem. Instead, we want to traverse a filesystem tree and create a flat set of
    // entities which we discover at any position in the tree.

    // We start by assuming that only the initial Entity corresponding to the top-level directory
    // has been created and persisted.

    Entity topFolder;
    ScreenPipelineResultNode resultNode;
    Integer sampleGroupSize;
    
    protected static class FlyScreenSample {
        public String StackPath;
        public String QualityCsvPath;

        public FlyScreenSample() {}

        static String getKeyFromStackName(String stackName) {
            String[] cArr = stackName.split("\\.reg\\.local");
            return cArr[0];
        }

        static String getKeyFromQualityCsvName(String csvName) {
            String[] cArr = csvName.split("\\.quality\\.csv");
            return cArr[0];
        }
    }

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        sampleGroupSize=new Integer((String)processData.getString("SAMPLE_GROUP_SIZE").trim());
        super.execute(processData);
        try {
            Task task= ProcessDataHelper.getTask(processData);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            String visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            resultNode = new ScreenPipelineResultNode(task.getOwner(), task, "ScreenSampleResultNode",
                    "ScreenPipelineResultNode for task " + task.getObjectId(), visibility, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);
            logger.info("FlyScreenSampleService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " intended path="+resultNode.getDirectoryPath());
            FileUtil.ensureDirExists(resultNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultNode.getDirectoryPath());
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }


    @Override
    protected void processFolderForData(Entity folder) throws Exception {

        logger.info("FlyScreenDiscoveryService  processFolderForData()  start   folder="+folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

        if (topFolder==null) {
            topFolder=folder;
        } else {
            throw new Exception("Expected only a single call to processFolderForData() - topFolder should be null at this point");
        }

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());

        if (!dir.canRead()) {
        	logger.error("Cannot read from folder " + dir.getAbsolutePath());
        	return;
        }

        // We need to know the pre-existing set of Screen Samples, so we can detect new ones
        Set<String> currentScreenSamples=new HashSet<String>();
        Map<String,Entity> incompleteScreenSamples=new HashMap<String,Entity>();
        for (EntityData ed : topFolder.getEntityData()) {
            Entity dateFolder = ed.getChildEntity();
            if (dateFolder != null && dateFolder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                for (EntityData d : dateFolder.getEntityData()) {
                    Entity child=d.getChildEntity();
                    if (child != null && child.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                        String previousSampleName=child.getName();
                        if (screenSampleIsComplete(child)) {
                            logger.info("Skipping previous complete ScreenSample  name="+previousSampleName);
                            currentScreenSamples.add(previousSampleName);
                        } else {
                            logger.info("Adding incomplete ScreenSample  name="+previousSampleName);
                            incompleteScreenSamples.put(previousSampleName, child);
                        }
                    }
                }
            }
        }
        Map<String, FlyScreenSample> sampleMap=new HashMap<String, FlyScreenSample>();
        processFlyLightScreenDirectory(dir, currentScreenSamples, sampleMap);

        // Next, create the new samples
        List<List<String>> groupList=new ArrayList<List<String>>();
        Long sampleTotal=0L;
        Integer sampleCount=0;
        List<String> sampleIdList=new ArrayList<String>();
        for (String key : sampleMap.keySet()) {
            FlyScreenSample screenSample = sampleMap.get(key);
            Entity screenSampleEntity=incompleteScreenSamples.get(key);
            if (screenSampleEntity==null) {
                File stackPath=new File(screenSample.StackPath);
                File dateFolderFile=stackPath.getParentFile();
                Entity dateFolder=getDateFolderFromSampleKey(key, dateFolderFile.getAbsolutePath());
                screenSampleEntity = new Entity();
                screenSampleEntity.setCreationDate(createDate);
                screenSampleEntity.setUpdatedDate(createDate);
                screenSampleEntity.setOwnerKey(ownerKey);
                screenSampleEntity.setName(key);
                screenSampleEntity.setEntityTypeName(EntityConstants.TYPE_SCREEN_SAMPLE);
                screenSampleEntity = EJBFactory.getLocalEntityBean().saveOrUpdateEntity(screenSampleEntity);
                logger.info("Created new Screen Sample " + key + " id=" + screenSampleEntity.getId());
                helper.addToParent(dateFolder, screenSampleEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
            } else {
                logger.info("Re-using incomplete Screen Sample " + key +" id="+screenSampleEntity.getId());
            }
            String[] alignmentScores = getAlignmentScoresFromQualityFile(screenSample.QualityCsvPath);
            addStackToScreenSample(screenSampleEntity, screenSample, alignmentScores);
            sampleIdList.add(screenSampleEntity.getId().toString());
            sampleCount++;
            if (sampleCount>=sampleGroupSize) {
                groupList.add(sampleIdList);
                sampleTotal+=sampleIdList.size();
                sampleIdList=new ArrayList<String>();
                sampleCount=0;
            }
        }
        if (sampleIdList.size()>0) {
            groupList.add(sampleIdList);
            sampleTotal+=sampleIdList.size();
        }
        logger.info("Adding "+groupList.size()+" groups containing "+sampleTotal+" total samples");
        processData.putItem("GROUP_LIST", groupList);
        sortDateFolders();
    }

    protected boolean screenSampleIsComplete(Entity screenSample) {
        boolean hasStack=false;
        boolean hasMip=false;
        Set<EntityData> edSet=screenSample.getEntityData();
        logger.info("screenSampleIsComplete: edSet has "+edSet.size()+" members to evaluate");
        for (EntityData ed : edSet) {
            Entity child = ed.getChildEntity();
            if (child==null) {
                logger.info("Skipping null ed entry");
            } else {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    String stackPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    File stackFile=new File(stackPath);
                    if (stackFile.exists()) {
                        logger.info("Found existing stack="+stackFile.getAbsolutePath()+" hasStack=true");
                        hasStack=true;
                    } else {
                        logger.info("Could not find existing stack="+stackFile.getAbsolutePath()+" hasStack=false");
                    }
                } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                        child.getName().equals(FlyScreenSampleService.SUPPORTING_FILES_FOLDER_NAME)) {
                    for (EntityData ed2 : child.getEntityData()) {
                        Entity child2=ed2.getChildEntity();
                        if (child2!=null) {
                            if (child2.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_2D) &&
                                child2.getName().toLowerCase().endsWith("mip")) {
                                String mipPath=child2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                File mipFile=new File(mipPath);
                                if (mipFile.exists()) {
                                    logger.info("Found existing mip="+mipFile.getAbsolutePath()+" hasMip=true");
                                    hasMip=true;
                                } else {
                                    logger.info("Could not find existing mip="+mipFile.getAbsolutePath()+" hasMip=false");
                                }
                            } else {
                                logger.info("Ignoring child entity of type="+child.getEntityTypeName());
                            }
                        }
                    }
                }
            }
        }
        if (hasStack && hasMip) {
            return true;
        }
        return false;
    }

    protected void processFlyLightScreenDirectory(File dir, Set<String> currentScreenSamples,
                                                  Map<String, FlyScreenSample> sampleMap) throws Exception {

        // First, find the new sample stack and quality files in the directory
        List<File> fileList=FileUtils.getOrderedFilesInDir(dir);

        for (File file : fileList) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".reg.local.v3dpbd")) {
                    String key=FlyScreenSample.getKeyFromStackName(file.getName());
                    if (!currentScreenSamples.contains(key)) {
                        FlyScreenSample sample=sampleMap.get(key);
                        if (sample==null) {
                            sample=new FlyScreenSample();
                            sampleMap.put(key, sample);
                        }
                        sample.StackPath=file.getAbsolutePath();
                    }
                } else if (file.getName().endsWith(".quality.csv")) {
                    String key=FlyScreenSample.getKeyFromQualityCsvName(file.getName());
                    if (!currentScreenSamples.contains(key)) {
                        FlyScreenSample sample=sampleMap.get(key);
                        if (sample==null) {
                            sample=new FlyScreenSample();
                            sampleMap.put(key, sample);
                        }
                        sample.QualityCsvPath=file.getAbsolutePath();
                    }
                }
            } else {
                processFlyLightScreenDirectory(file, currentScreenSamples, sampleMap);
            }
        }
    }

    String[] getAlignmentScoresFromQualityFile(String filepath) {
        String[] scoreArray=new String[2];
        try {
            BufferedReader reader=new BufferedReader(new FileReader(filepath));
            String descriptionLine=reader.readLine();
            String scoreLine=reader.readLine();
            String[] scoreArr2=scoreLine.split(",");
            scoreArray[0]=scoreArr2[0].trim();
            scoreArray[1]=scoreArr2[1].trim();
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            scoreArray[0]="0.0";
            scoreArray[1]="0.0";
        }
        return scoreArray;
    }

    protected void addStackToScreenSample(Entity screenSampleEntity, FlyScreenSample screenSample,
                                                  String[] alignmentScores) throws Exception {
        boolean alreadyHasQualityScores=false;
        boolean alreadyHasStack=false;
        Entity alignedStack=null;
        Set<Entity> children = screenSampleEntity.getChildren();
        if (children!=null && children.size()>0) {
            for (Entity child : children)  {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    String stackPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    File stackFile=new File(stackPath);
                    logger.info("Found already existing stack="+stackFile.getAbsolutePath());
                    if (stackFile.exists()) {
                        logger.info("Stack already exists on filesystem");
                        alignedStack=child;
                        alreadyHasStack=true;
                        String qiScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
                        if (qiScore!=null && qiScore.trim().length()>0) {
                            alreadyHasQualityScores=true;
                            logger.info("Stack already has quality scores");
                        }
                    }
                } else {
                    logger.info("Skipping child of type="+child.getEntityTypeName());
                }
            }
        }
        if (alignedStack==null) {
            alignedStack = new Entity();
            alignedStack.setOwnerKey(ownerKey);
            alignedStack.setEntityTypeName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
            alignedStack.setCreationDate(createDate);
            alignedStack.setUpdatedDate(createDate);
            alignedStack.setName(screenSampleEntity.getName()+" aligned stack");
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, screenSample.StackPath);
        }
        // The following will capture the new case implicitly
        if (!alreadyHasQualityScores) {
            logger.info("Adding fresh quality scores to stack");
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE, alignmentScores[0]);
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE, alignmentScores[1]);
            alignedStack = EJBFactory.getLocalEntityBean().saveOrUpdateEntity(alignedStack);
            logger.info("Saved stack " + alignedStack.getName() + " as "+alignedStack.getId());
        }
        if (!alreadyHasStack) {
            logger.info("Adding stack to parent entity");
            helper.addToParent(screenSampleEntity, alignedStack, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        if (alreadyHasQualityScores && alreadyHasStack) {
            logger.info("Screen sample id="+screenSampleEntity.getId() + " already has stack and quality values");
        }
    }

    // Example of key: GMR_45B01_AE_01_02-fA01b_C090625_20090625160238328
    protected Entity getDateFolderFromSampleKey(String sampleKey, String folderPath) throws Exception {
        String[] ucList=sampleKey.split("_");
        if (ucList.length<4) {
            return null;
        }
        String dateGuid=ucList[ucList.length-1];
        String monthSection=dateGuid.substring(0,6);
        Set<Entity> children=topFolder.getChildren();
        for (Entity child : children) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                    child.getName().equals(monthSection)) {
                return child;
            }
        }
        // Assume we don't have a folder for this date yet
        Entity dateFolder = new Entity();
        dateFolder.setOwnerKey(ownerKey);
        dateFolder.setEntityTypeName(EntityConstants.TYPE_FOLDER);
        dateFolder.setCreationDate(createDate);
        dateFolder.setUpdatedDate(createDate);
        dateFolder.setName(monthSection);
        dateFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, folderPath);
        EJBFactory.getLocalEntityBean().saveOrUpdateEntity(dateFolder);
        helper.addToParent(topFolder, dateFolder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return dateFolder;
    }

    protected void sortDateFolders() throws Exception {
        Set<EntityData> folderEdSet=topFolder.getEntityData();
        Map<Long, EntityData> folderMapByDateNumber=new HashMap<Long, EntityData>();
        for (EntityData ed : folderEdSet) {
            Entity folder = ed.getChildEntity();
            if (folder!=null) {
                if (folder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                    Long dateNumber=new Long(folder.getName().trim());
                    folderMapByDateNumber.put(dateNumber, ed);
                }
            }
        }
        List<Long> dateNumberList=new ArrayList<Long>();
        for (Long dateNumber : folderMapByDateNumber.keySet()) {
            dateNumberList.add(dateNumber);
        }
        Collections.sort(dateNumberList);
        for (int i=0;i<dateNumberList.size();i++) {
            EntityData ed=folderMapByDateNumber.get(dateNumberList.get(i));
            entityBean.updateChildIndex(ed, i);
        }
    }

}
