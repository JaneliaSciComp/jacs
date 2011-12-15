package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.ScreenPipelineResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenDiscoveryService extends FileDiscoveryService {

    // In this discovery service, we do not want to create an entity tree which looks like the
    // filesystem. Instead, we want to traverse a filesystem tree and create a flat set of
    // entities which we discover at any position in the tree.

    // We start by assuming that only the initial Entity corresponding to the top-level directory
    // has been created and persisted.

    Entity topFolder;
    ScreenPipelineResultNode resultNode;

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
        Set<String> currentSceenSamples=new HashSet<String>();
        Map<String,Entity> incompleteScreenSamples=new HashMap<String,Entity>();
        for (EntityData ed : topFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                String previousSampleName=child.getName();
                if (screenSampleIsComplete(child)) {
                    logger.info("Skipping previous complete ScreenSample  name="+previousSampleName);
                    currentSceenSamples.add(previousSampleName);
                } else {
                    logger.info("Adding incomplete ScreenSample  name="+previousSampleName);
                    incompleteScreenSamples.put(previousSampleName, child);
                }
            }
        }
        Map<String, FlyScreenSample> sampleMap=new HashMap<String, FlyScreenSample>();
        processFlyLightScreenDirectory(dir, currentSceenSamples, sampleMap);

        // Next, create the new samples
        List<String> sampleIdList=new ArrayList<String>();
        EntityType screenSampleType= EJBFactory.getLocalAnnotationBean().getEntityTypeByName(EntityConstants.TYPE_SCREEN_SAMPLE);
        if (screenSampleType==null) {
            String errorMsg="EntityType screenSampleType  returned  NULL";
            logger.error(errorMsg);
            throw new Exception(errorMsg);
        }
        for (String key : sampleMap.keySet()) {
            FlyScreenSample screenSample = sampleMap.get(key);
            Entity screenSampleEntity=incompleteScreenSamples.get(key);
            if (screenSampleEntity==null) {
                screenSampleEntity = new Entity();
                screenSampleEntity.setCreationDate(createDate);
                screenSampleEntity.setUpdatedDate(createDate);
                screenSampleEntity.setUser(user);
                screenSampleEntity.setName(key);
                screenSampleEntity.setEntityType(screenSampleType);
                screenSampleEntity = EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(screenSampleEntity);
                logger.info("Created new Screen Sample " + key + " id=" + screenSampleEntity.getId());
                addToParent(topFolder, screenSampleEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
            }
            String[] alignmentScores = getAlignmentScoresFromQualityFile(screenSample.QualityCsvPath);
            addStackToScreenSample(screenSampleEntity, screenSample, alignmentScores);
            sampleIdList.add(screenSampleEntity.getId().toString());
        }
        logger.info("Adding "+sampleIdList.size()+" sample IDs to SAMPLE_ENTITY_ID");
        processData.putItem("SAMPLE_ENTITY_ID", sampleIdList);
    }

    protected boolean screenSampleIsComplete(Entity screenSample) {
        boolean hasStack=false;
        boolean hasMip=false;
        for (EntityData ed2 : screenSample.getEntityData()) {
            Entity child2 = ed2.getChildEntity();
            if (child2==null) {
                continue;
            }
            if (child2.getEntityType().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                String stackPath=child2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                File stackFile=new File(stackPath);
                if (stackFile.exists())
                    hasStack=true;
            } else if (child2.getEntityType().equals(EntityConstants.TYPE_IMAGE_2D) &&
                    child2.getName().toLowerCase().endsWith("mip")) {
                String mipPath=child2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                File mipFile=new File(mipPath);
                if (mipFile.exists())
                    hasMip=true;
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
        List<File> fileList=getOrderedFilesInDir(dir);

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
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    String stackPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    File stackFile=new File(stackPath);
                    logger.info("Found already existing stack="+stackFile.getAbsolutePath());
                    if (stackFile.exists()) {
                        logger.info("Stack already exists on filesystem");
                        alignedStack=child;
                        alreadyHasStack=true;
                        String qiScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
                        if (qiScore!=null && qiScore.trim().length()>0) {
                            alreadyHasQualityScores=true;
                            logger.info("Stack already has quality scores");
                        }
                    }
                } else {
                    logger.info("Skipping child of type="+child.getEntityType().getName());
                }
            }
        }
        if (alignedStack==null) {
            alignedStack = new Entity();
            alignedStack.setUser(user);
            alignedStack.setEntityType(EJBFactory.getLocalAnnotationBean().getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK));
            alignedStack.setCreationDate(createDate);
            alignedStack.setUpdatedDate(createDate);
            alignedStack.setName(screenSampleEntity.getName()+" aligned stack");
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, screenSample.StackPath);
        }
        // The following will capture the new case implicitly
        if (!alreadyHasQualityScores) {
            logger.info("Adding fresh quality scores to stack");
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, alignmentScores[0]);
            alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE, alignmentScores[1]);
            alignedStack = EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(alignedStack);
            logger.info("Saved stack " + alignedStack.getName() + " as "+alignedStack.getId());
        }
        if (!alreadyHasStack) {
            logger.info("Adding stack to parent entity");
            addToParent(screenSampleEntity, alignedStack, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        if (alreadyHasQualityScores && alreadyHasStack) {
            logger.info("Screen sample id="+screenSampleEntity.getId() + " already has stack and quality values");
        }
    }


}
