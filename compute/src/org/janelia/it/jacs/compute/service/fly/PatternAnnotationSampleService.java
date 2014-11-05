package org.janelia.it.jacs.compute.service.fly;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.screen.FlyScreenSampleService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.PatternAnnotationResultNode;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/31/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternAnnotationSampleService  implements IService {

    private static final Logger logger = Logger.getLogger(PatternAnnotationSampleService.class);

    final public String PATTERN_ANNOTATION_SUBDIR_NAME="patternAnnotation";
    final public String PATTERN_ANNOTATION_FOLDER_NAME="Pattern Annotation";
    final public String MIPS_SUBFOLDER_NAME="mips";
    final public String SUPPORTING_FILE_SUBFOLDER_NAME="supportingFiles";
    final public String NORMALIZED_SUBFOLDER_NAME="normalized";
    final public String ABBREVIATION_INDEX_FILENAME="compartmentAbbreviationIndex.txt";

    final public String MODE_UNDEFINED="UNDEFINED";
    final public String MODE_SETUP="SETUP";
    final public String MODE_COMPLETE="COMPLETE";

    protected String patternAnnotationResourceDir=SystemConfigurationProperties.getString("FileStore.CentralDir.Archived")+
                SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationResourceDir");
    protected String patternChannel=SystemConfigurationProperties.getString("FlyScreen.AlignedStackPatternChannel");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected String mode=MODE_UNDEFINED;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Double QI_MAXIMUM=0.0;
    protected Double QM_MAXIMUM=0.0;
    protected Boolean refresh;
    protected List<String> abbrevationList=new ArrayList<String>();


    public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("PatternAnnotationSampleService execute() start");

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            logger.info("PatternAnnotationSampleService running under TaskId="+task.getObjectId());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            ownerKey = ProcessDataHelper.getTask(processData).getOwner();
            createDate = new Date();
            mode = processData.getString("MODE");
            refresh=processData.getString("REFRESH").trim().toLowerCase().equals("true");

            if (processData.getString("QI_MAXIMUM")!=null) {
                QI_MAXIMUM=new Double(processData.getString("QI_MAXIMUM").trim());
            }
            if (processData.getString("QM_MAXIMUM")!=null) {
                QM_MAXIMUM=new Double(processData.getString("QM_MAXIMUM").trim());
            }

            if (mode.equals(MODE_SETUP)) {
                doSetup();
            } else if (mode.equals(MODE_COMPLETE)) {
                doComplete();
            } else {
                logger.error("Do not recognize mode type="+mode);
            }

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    /*
            What we want to do here is get everything ready to run the pattern annotation on the grid.
            Because we are (at this point) encouraging the user to go to the filesystem to access stacks
            and such, it is best if we continue to add sample-specific results and data to the same
            location in cases where the new data is directly analytically derived from the existing
            data. For this reason, we will add the pattern annotation as a sub-directory to the
            existing ScreenSampleResultNode.

            So, here are the setup steps:

            1) iterate over the Fly Lines in the group list

            2) for each Fly Line, get the set of Samples

            3) for each Sample, get the most recent ScreenSampleResultNode

            4) create a subdirectory within the SceenSampleResultNode which
                is based on the ID of the task for this pipeline.

            5) Use the directory from #4 as the output for the pattern annotation step.

            6) Get the correct aligned stack path for the sample, and use this as input
                for the pattern annotation.

            NOTE: as part of step 6, we will also get the alignment scores for the
            aligned stack, and if the scores are not over some minimal threshold we
            will not include this sample for processing.

            REFRESH: if refresh is 'true', then we will delete any previous pattern annotation
            for each sample, and replace it with new results. If 'false', then we will skip
            adding new pattern annotation if it already exists.

     */

    public void doSetup() throws Exception {
        logger.info("PatternAnnotationSampleService doSetup() start");

        List<Entity> sampleList=new ArrayList<Entity>();
        List<String> flyLineIdList=(List<String>)processData.getItem("FLY_LINE_GROUP_LIST");
        logger.info("PatternAnnotationSampleService execute() contains "+flyLineIdList.size()+" fly lines, mode="+mode);

        // Create Pattern Annotation Result Node - note that other than the grid-metadata, the result of this service will
        // actually be placed with the ScreenSampleResultNode, so that there is a unified location for the user to browse
        // sample-related data on the filesystem.

        PatternAnnotationResultNode resultNode = new PatternAnnotationResultNode(task.getOwner(), task, "PatternAnnotationResultNode",
                "PatternAnnotationResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);

        // Update sessionName
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        logger.info("PatternAnnotationSampleService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " updated sessionName="+sessionName+
                " intended path="+resultNode.getDirectoryPath());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultNode.getDirectoryPath());
        String creationMessage="Created PatternAnnotationSampleService path="+resultNode.getDirectoryPath()+" id="+resultNode.getObjectId();
        logger.info(creationMessage);

        // We need an index-synchronized set of lists to track those samples with sufficiently good alignments to process here.
        // We hope this is the overwhelming majority of all samples, but will not be all samples.

        List<String> properlyAlignedSampleIdList=new ArrayList<String>();
        List<Entity> properlyAlignedSampleList=new ArrayList<Entity>();
        List<String> patternAnnotationDirList=new ArrayList<String>();
        List<String> alignedStackPathList=new ArrayList<String>();

        // Generate the list of samples from the FlyLine list
        logger.info("Processing " + flyLineIdList.size() + " FlyLine entries");
        for (String flyLineEntityId : flyLineIdList) {
            Entity flyLineEntity=entityBean.getEntityTree(new Long(flyLineEntityId.trim()));
            for (EntityData ed : flyLineEntity.getEntityData()) {
                if (ed.getChildEntity().getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                    sampleList.add(ed.getChildEntity());
                }
            }
        }

        logger.info("Processing " + sampleList.size() + " Screen Samples");
        long sampleDirFailureCount=0;
        for (Entity sample : sampleList) {

            // Refresh beans
            logger.info("Refreshing EJB instances");
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            ownerKey = ProcessDataHelper.getTask(processData).getOwner();

            logger.info("Processing sample name="+sample.getName());

            sample=entityBean.getEntityTree(sample.getId());

            // This is the directory containing the link to the stack, and will have a pattern annotation
            // subdirectory, as well as a separte supportingFiles directory.
            File sampleResultDir=getOrUpdateSampleResultDir(sample);
            if (sampleResultDir==null) {
                logger.info("Could not find sample result directory for sampleId="+sample.getId()+" name="+sample.getName());
                sampleDirFailureCount++;
                continue; // move on to next sample
            }

            // refresh again after sample dir update
            sample=entityBean.getEntityTree(sample.getId());

            // This method ensures that there is a supporting directory for the sample in the sample
            // result node directory, and that the mip has been relocated to this directory.
            sample=updateSampleSupportingDirIfNecessary(sample);

            if (refresh) {
                logger.info("Refresh is true - checking status of patternAnnotationFolder for sampleName="+sample.getName());
                List<Entity> patternAnnotationFolderList=getPatternAnnotationFoldersFromSample(sample);
                if (patternAnnotationFolderList!=null) {
                    for (Entity patternFolderToDelete : patternAnnotationFolderList) {
                        logger.info("Deleting patternFolder id="+patternFolderToDelete.getId()+" dir="+patternFolderToDelete.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        cleanFullOrIncompletePatternAnnotationFolderAndFiles(patternFolderToDelete);
                    }
                } else {
                    logger.info("patternAnnotationFolder is null - nothing to clean");
                }
                // Refresh sample after delete
                logger.info("Refreshing sample after clean operation");
                sample=entityBean.getEntityTree(sample.getId());
            } else {
                logger.info("Refresh is false - using prior data");
            }

            // This ensures that the patternAnnotation directory and its subdirs are correctly setup.
            File patternAnnotationDir=getOrUpdatePatternAnnotationDir(sample);

            File stackFile=null;
            String QmScore=null;
            String QiScore=null;

            // Do another refresh
            sample=entityBean.getEntityTree(sample.getId());

            for (EntityData ed : sample.getEntityData()) {
                Entity child=ed.getChildEntity();
                if (child!=null) {
                    if (child.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                        logger.info("Found Aligned Brain Stack child");
                        stackFile=new File(child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        QmScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE);
                        QiScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
                        logger.info("QmScore="+QmScore+" QiScore="+QiScore);
                    }
                }
            }

            if (QmScore!=null && QiScore!=null) {
                Double qm=new Double(QmScore.trim());
                Double qi=new Double(QiScore.trim());
                logger.info("Using Double-valued Qm="+qm+" Qi="+qi+" QM_MAXIMUM="+QM_MAXIMUM+" QI_MAXIMUM="+QI_MAXIMUM);
                boolean qScoresUndefined=(qm==0.0 && qi==0.0);
                if (!qScoresUndefined && qm<=QM_MAXIMUM && qi<=QI_MAXIMUM) {
                    logger.info("Adding properly-aligned sampleName="+sample.getName());
                    properlyAlignedSampleList.add(sample);
                    properlyAlignedSampleIdList.add(sample.getId().toString());
                    patternAnnotationDirList.add(patternAnnotationDir.getAbsolutePath());
                    alignedStackPathList.add(stackFile.getAbsolutePath());
                    logger.info("This stack has valid Qm and Qi scores="+stackFile.getAbsolutePath());
                } else {
                    logger.info("Skipping stack="+stackFile.getAbsolutePath()+" due to poor or undefined Qm and Qi scores");
                }
            } else {
                logger.error("Could not find expected QmScore and QiScore attributes from aligned brain stack of sample="+sample.getId());
            }

        }

        // Finally, make sure the pattern annotation folder entity and directory exist for each sample. Note that
        // refresh has already occurred if requested, so any existing result which is complete does not need to
        // be processed because the user has presumably chosen not to refresh if it is still there.

        int sampleIndex=0;
        List<String> finalSampleIdList=new ArrayList<String>();
        List<String> finalSampleNameList=new ArrayList<String>();
        List<String> finalAnnotationDirList=new ArrayList<String>();
        List<String> finalMipConversionDirList=new ArrayList<String>();
        List<String> finalAlignedStackList=new ArrayList<String>();

        long alreadyCompleteSampleCount=0;
        for (Entity alignedSample : properlyAlignedSampleList) {
            logger.info("Checking to see if aligned Sample name="+alignedSample.getName()+" is complete");
            if (!patternAnnotationDirIsComplete(alignedSample.getName(), new File(patternAnnotationDirList.get(sampleIndex)), false)) {
                finalSampleIdList.add(alignedSample.getId().toString());
                finalSampleNameList.add(alignedSample.getName());
                finalAnnotationDirList.add(patternAnnotationDirList.get(sampleIndex));
                finalMipConversionDirList.add(patternAnnotationDirList.get(sampleIndex)+File.separator+MIPS_SUBFOLDER_NAME);
                finalAlignedStackList.add(alignedStackPathList.get(sampleIndex));
            } else {
                logger.info("Sample is complete - skipping");
                alreadyCompleteSampleCount++;
            }
            sampleIndex++;
        }

        Map<String, FileNode> mipResultNodeMap=createResultNodeMapForMipConversion(finalMipConversionDirList);

        long sampleCount=finalSampleNameList.size();

        processData.putItem("SAMPLE_ID_LIST", finalSampleIdList);
        processData.putItem("SAMPLE_NAME_LIST", finalSampleNameList);
        processData.putItem("PATTERN_ANNOTATION_PATH", finalAnnotationDirList);
        processData.putItem("MIPS_CONVERSION_PATH", finalMipConversionDirList);
        processData.putItem("ALIGNED_STACK_PATH_LIST", finalAlignedStackList);
        processData.putItem("RESOURCE_DIR_PATH", patternAnnotationResourceDir);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
        processData.putItem("PATTERN_CHANNEL", patternChannel);
        processData.putItem("IMAGE_CONVERSION_RESULT_NODE_MAP", mipResultNodeMap);
        processData.putItem("SAMPLE_COUNT", sampleCount);

        for (String sampleName : finalSampleNameList) {
            logger.info("doSetup() : Adding sampleName to list="+sampleName);
        }

        logger.info("End of doSetup() - including "+finalSampleNameList.size()+" samples - skipped "+alreadyCompleteSampleCount+
                " already-complete samples, and skipped "+sampleDirFailureCount+" samples due to missing sample result directories");
    }

    Map<String, FileNode> createResultNodeMapForMipConversion(List<String> dirPathList) throws Exception {
        Map<String, FileNode> nodeMap=new HashMap<String, FileNode>();
        for (String dirPath : dirPathList) {
            NamedFileNode mipResultNode=new NamedFileNode(task.getOwner(), task, "mipConversion", "mipConversion", visibility, sessionName);
            mipResultNode=(NamedFileNode)computeBean.saveOrUpdateNode(mipResultNode);
            nodeMap.put(dirPath, mipResultNode);
            logger.info("For mip input dir="+dirPath+" sessionName="+sessionName+" created output result dir="+mipResultNode.getDirectoryPath());
        }
        return nodeMap;
    }

    File getOrUpdateSampleResultDir(Entity sample) throws Exception {
        // First, does the sample already have a RESULT_NODE_ID?
        String resultNodeIdString=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID);
        if (resultNodeIdString!=null) {
            return getResultNodeDirFromSample(sample);
        }
        // We have to do some homework to get the resultNodeId. We can't use the path of the sample stack,
        // because this actually lives in the ScreenStaging area, not the ScreenSampleResult. The
        // 'anchor' artifact for the ScreenSampleResult is the mip.
        //
        // Using the mip as the anchor is itself tricky because there is (a) the original location, which
        // is peer-level to the stack link in the result directory, and (b) the post-moved location,
        // in the 'supporting files' folder. We will check (a) first and then (b).

        // First step is to check for the MIP and supporting files folder

        File nodeDir=null;

        for (EntityData ed : sample.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_2D) && child.getName().toLowerCase().contains("mip")) {
                    File mipFile=new File(child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    logger.info("getOrUpdateSampleResultDir() - found mip="+mipFile.getAbsolutePath());
                    nodeDir=mipFile.getParentFile();
                    if (nodeDir.getName().equals(SUPPORTING_FILE_SUBFOLDER_NAME)) {
                        // Then we need to click-up to the next level
                        nodeDir=nodeDir.getParentFile();
                    }
                    logger.info("getOrUpdateSampleResultDir() - based on mip, assuming nodeDir="+nodeDir.getAbsolutePath()+" nodeId="+nodeDir.getName());
                } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(SUPPORTING_FILE_SUBFOLDER_NAME)) {
                    // We can use this folder for the nodeId anchor
                    String supportingFolderPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (supportingFolderPath!=null) {
                        File supportingDir=new File(supportingFolderPath);
                        nodeDir=supportingDir.getParentFile();
                    }
                }
            }
        }
        if (nodeDir!=null) {
            // Verify node
            Long nodeId=new Long(nodeDir.getName().trim());
            ScreenSampleResultNode ssrn=(ScreenSampleResultNode)computeBean.getNodeById(nodeId);
            File checkDir=new File(ssrn.getDirectoryPath());
            if (checkDir.getAbsolutePath().equals(nodeDir.getAbsolutePath())) {
                logger.info("Verified nodeId="+nodeId+" now saving as sample attribute");
                sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID, nodeId.toString());
                entityBean.saveOrUpdateEntity(sample);
            } else {
                throw new Exception("Could not verify result node id="+nodeId);
            }
        }
        return nodeDir;
    }

    File getResultNodeDirFromSample(Entity sample) {
        String resultNodeIdString=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID);
        Long resultNodeId=new Long(resultNodeIdString.trim());
        try {
	        ScreenSampleResultNode resultNode=(ScreenSampleResultNode)computeBean.getNodeById(resultNodeId);
	        return new File(resultNode.getDirectoryPath());
        }
        catch (Exception e) {
        	logger.error("Error",e);
        	return null;
        }
    }

    protected Entity updateSampleSupportingDirIfNecessary(Entity sample) throws Exception {
        // Originally, the screen sample has both the screen raw data stack and mip sharing
        // the same top-level screen sample folder. This is distracting for the user, where
        // the stack and mip get confused. Here we create a supporting folder and place
        // the mip there if it is not already configured this way.

        File screenFolderDir=getResultNodeDirFromSample(sample);
        String screenFolderPath=screenFolderDir.getAbsolutePath();
        File supportingDir=new File(screenFolderPath, FlyScreenSampleService.SUPPORTING_FILES_FOLDER_NAME);

        Entity rawMip=null;
        EntityData rawMipEd=null;
        File rawMipFile=null;
        Entity supportingFolder=null;
        Entity screenStack=null;

        String sampleDefault2DImagePath=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        boolean replaceSampleDefault2DImage=false;
        logger.info("Checking sample supporting dir status for sample id="+sample.getId());
        for (EntityData ed : sample.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_2D)) {
                    String childPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    File childFile=new File(childPath);
                    if (childPath.equals(sampleDefault2DImagePath)) {
                        replaceSampleDefault2DImage=true;
                    }
                    if (childFile.getName().toLowerCase().contains("mip")) {
                        rawMip=child;
                        rawMipEd=ed;
                        rawMipFile=childFile;
                    }
                } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(FlyScreenSampleService.SUPPORTING_FILES_FOLDER_NAME)) {
                    supportingFolder=child;
                } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    screenStack=child;
                }
            }
        }
        if (screenStack==null) {
            throw new Exception("Could not locate aligned screen stack for sample name="+sample.getName() + " id="+sample.getId());
        }
        if (rawMip!=null) {
            logger.info("Updating position of mip and supporting directory for screen sample id="+sample.getId());
            // Then assume we need to move it - first check if a supporting dir already exists
            if (supportingFolder==null) {
                // We need to create it
                if (!supportingDir.exists() && !supportingDir.mkdir()) {
                    throw new Exception("Could not create supporting file directory="+supportingDir.getAbsolutePath());
                }
                logger.info("Creating new supporting folder in location="+supportingDir.getAbsolutePath());
                supportingFolder=addChildFolderToEntity(sample, FlyScreenSampleService.SUPPORTING_FILES_FOLDER_NAME, supportingDir.getAbsolutePath());
                // refresh sample
                sample = entityBean.getEntityById(sample.getId().toString());
            }
            entityBean.deleteEntityData(rawMipEd);
            File newMipFile=new File(supportingDir, rawMipFile.getName());
            logger.info("Moving mip to new location="+newMipFile.getAbsolutePath());
            FileUtil.moveFileUsingSystemCall(rawMipFile, newMipFile);
            addToParent(supportingFolder, rawMip, null, EntityConstants.ATTRIBUTE_ENTITY);
            rawMip.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipFile.getAbsolutePath());
            rawMip.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, newMipFile.getAbsolutePath());
            entityBean.saveOrUpdateEntity(rawMip);
            if (replaceSampleDefault2DImage) {
                logger.info("Resetting default 2D image for screen sample to new image location - refreshing sample");
                sample = entityBean.getEntityById(sample.getId().toString());
                sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipFile.getAbsolutePath());
                sample = entityBean.saveOrUpdateEntity(sample);
                logger.info("Done updating default 2D image for sample="+sample.getName());
                logger.info("Now updating location of mip for aligned screen stack");
                screenStack = entityBean.getEntityById(screenStack.getId().toString());
                screenStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipFile.getAbsolutePath());
                entityBean.saveOrUpdateEntity(screenStack);
                logger.info("Done updating aligned screen stack with mip path="+newMipFile.getAbsolutePath());
            }
        } else {
            logger.info("Could not locate mip in prior location, so assuming does not need update for sample id="+sample.getId());
            logger.info("However, we will check to see if the aligned screen stack needs an updated mip location");
            if (supportingFolder!=null) {
                Entity postMovedAlignedMip=null;
                String newMipPath=null;
                for (EntityData ed : supportingFolder.getEntityData()) {
                    Entity child=ed.getChildEntity();
                    if (child!=null) {
                        if (child.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_2D)) {
                            String childPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                            File childFile=new File(childPath);
                            if (childFile.getName().equals("AlignedStackMIP.png")) {
                                postMovedAlignedMip=child;
                                newMipPath=childFile.getAbsolutePath();
                            }
                        }
                    }
                }
                if (postMovedAlignedMip!=null) {
                    logger.info("Updating mip location for aligned stack");
                    screenStack = entityBean.getEntityById(screenStack.getId().toString());
                    screenStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipPath);
                    entityBean.saveOrUpdateEntity(screenStack);
                    logger.info("Done updating aligned screen stack with mip path="+newMipPath);
                } else {
                    throw new Exception("Could not locate aligned mip in sample name="+sample.getName()+" id="+sample.getId());
                }
            } else {
                throw new Exception("Unexpectedly could not find supporting folder for sampleId="+sample.getId());
            }
        }
        return sample;
    }

    void verifyOrCreateSubFolder(Entity patternAnnotationFolder, String subFolderName) throws Exception {
        Entity subFolder=getSubFolderByName(patternAnnotationFolder, subFolderName);
        String patternDirPath=patternAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        File subFolderDir=new File(patternDirPath, subFolderName);
        if (subFolder==null) {
            subFolder=addChildFolderToEntity(patternAnnotationFolder, subFolderName, subFolderDir.getAbsolutePath());
        } else {
            String actualPath=subFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!actualPath.equals(subFolderDir.getAbsolutePath())) {
                // Need to correct
                subFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, subFolderDir.getAbsolutePath());
                entityBean.saveOrUpdateEntity(subFolder);
            }
        }
        if (!subFolderDir.exists() && !subFolderDir.mkdir()) {
            throw new Exception("Could not create subfolder="+subFolderDir.getAbsolutePath());
        }
    }

    File getOrUpdatePatternAnnotationDir(Entity sample) throws Exception {
        File sampleDir=getOrUpdateSampleResultDir(sample);

        sample=entityBean.getEntityTree(sample.getId());

        // Check to see if the sample already has a patternAnnotation folder
        Entity patternAnnotationFolder=null;
        List<Entity> patternAnnotationFolderList=getPatternAnnotationFoldersFromSample(sample);
        if (patternAnnotationFolderList!=null && patternAnnotationFolderList.size()>1) {
            logger.info("Cleaning extra patternAnnotationFolder");
            patternAnnotationFolder=patternAnnotationFolderList.get(patternAnnotationFolderList.size()-1);
            for (int pa=0;pa<patternAnnotationFolderList.size()-1;pa++) {
                Entity olderPa=patternAnnotationFolderList.get(pa);
                cleanFullOrIncompletePatternAnnotationFolderAndFiles(olderPa);
            }
        } else if (patternAnnotationFolderList!=null && patternAnnotationFolderList.size()==1) {
            patternAnnotationFolder=patternAnnotationFolderList.get(0);
        }
        File patternAnnotationCorrectDir=new File(sampleDir, PATTERN_ANNOTATION_SUBDIR_NAME);
        if (patternAnnotationFolder==null) {
            patternAnnotationFolder=addChildFolderToEntity(sample, PATTERN_ANNOTATION_FOLDER_NAME, patternAnnotationCorrectDir.getAbsolutePath());
        } else {
            String patternAnnotationActualDirPath=patternAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!patternAnnotationActualDirPath.equals(patternAnnotationCorrectDir.getAbsolutePath())) {
                patternAnnotationFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, patternAnnotationCorrectDir.getAbsolutePath());
                entityBean.saveOrUpdateEntity(patternAnnotationFolder);
            }
        }
        // Now check to make sure the physical dir exists
        if (!patternAnnotationCorrectDir.exists() && !patternAnnotationCorrectDir.mkdir()) {
            throw new Exception("Could not verify or create pattern annotation dir="+patternAnnotationCorrectDir.getAbsolutePath());
        }

        // Now move on to the three subdirs: mip, supportingFiles, and normalized
        verifyOrCreateSubFolder(patternAnnotationFolder, MIPS_SUBFOLDER_NAME);
        verifyOrCreateSubFolder(patternAnnotationFolder, SUPPORTING_FILE_SUBFOLDER_NAME);
        verifyOrCreateSubFolder(patternAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);

        return patternAnnotationCorrectDir;
    }


    protected void cleanFullOrIncompletePatternAnnotationFolderAndFiles(Entity patternAnnotationFolder) throws Exception {
        if (!patternAnnotationFolder.getOwnerKey().equals(ownerKey)) {
            throw new Exception("Users do not match for cleanFullOrIncompletePatternAnnotationFolderAndFiles()");
        }
        String patternDirPath=patternAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (patternDirPath==null) {
            logger.info("Could not find directory path for previous pattern folder id="+patternAnnotationFolder.getId());
            return;
        }
        File patternDir=new File(patternDirPath);
        logger.info("Removing prior pattern annotation folder at location="+patternDir.getAbsolutePath());
        List<File> filesToDelete=new ArrayList<File>();

        // We need to iterate through the entities and delete - note we only want to delete 'official'
        // contents and not other links.
        File mipsSubDir=new File(patternDir, MIPS_SUBFOLDER_NAME);
        File suppSubDir=new File(patternDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        File normSubDir=new File(patternDir, NORMALIZED_SUBFOLDER_NAME);
        List<File> dirsToDelete=new ArrayList<File>();
        dirsToDelete.add(mipsSubDir);
        dirsToDelete.add(suppSubDir);
        dirsToDelete.add(normSubDir);
        dirsToDelete.add(patternDir);
        for (File f : dirsToDelete) {
            File [] fileArr=f.listFiles();
            if (fileArr!=null) {
                for (File sf : fileArr) {
                    filesToDelete.add(sf);
                }
            }
        }

        // Now we know the files to delete, so we can delete the folder entity tree
        logger.info("Deleting entity tree for prior pattern annotation folderId="+patternAnnotationFolder.getId());
        entityBean.deleteEntityTreeById(task.getOwner(), patternAnnotationFolder.getId());
        logger.info("Finished deleting entity tree");
        // Now we can delete the files and then directories
        for (File fileToDelete : filesToDelete) {
            if (fileToDelete.exists() && !fileToDelete.isDirectory()) {
                logger.info("Deleting prior pattern annotation file="+fileToDelete.getAbsolutePath());
                fileToDelete.delete();
            }
        }
        for (File fileToDelete : filesToDelete) {
            if (fileToDelete.exists() && fileToDelete.isDirectory()) {
                logger.info("Deleting prior pattern dir="+fileToDelete.getAbsolutePath());
                fileToDelete.delete();
            }
        }
    }

    protected boolean patternAnnotationDirIsComplete(String sampleName, File patternDir, boolean verbose) throws Exception {
        boolean missingAFile=false;
        logger.info("Calling getExpectedPatternAnnotationResultFiles() with sampleName="+sampleName);
        List<File> filenameList=getExpectedPatternAnnotationResultFiles(patternDir, sampleName);
        for (File file : filenameList) {
            if (!file.exists()) {
                if (verbose) {
                    logger.info("Missing expected pattern annotation file="+file.getAbsolutePath());
                }
                missingAFile=true;
            }
        }
        return !missingAFile;
    }

    protected Entity addChildFolderToEntity(Entity parent, String name, String directoryPath) throws Exception {
        Entity folder = new Entity();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setOwnerKey(ownerKey);
        folder.setName(name);
        folder.setEntityTypeName(EntityConstants.TYPE_FOLDER);
        if (directoryPath!=null) {
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, directoryPath);
        }
        folder = entityBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    // This returns a list with the oldest first (ascending by id)
    public List<Entity> getPatternAnnotationFoldersFromSample(Entity screenSample) throws Exception {
        List<Entity> patternAnnotationFolderList=new ArrayList<Entity>();
        for (EntityData ed : screenSample.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(PATTERN_ANNOTATION_FOLDER_NAME)) {
                    patternAnnotationFolderList.add(child);
                }
            }
        }
        Collections.sort(patternAnnotationFolderList, new Comparator<Entity>() {
            @Override
            public int compare(Entity entity, Entity entity1) {
                if (entity.getId()>entity1.getId()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        return patternAnnotationFolderList;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, EntityConstants.ATTRIBUTE_ENTITY);
        logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
                " as child of "+parent.getEntityTypeName()+"#"+parent.getId());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Completion
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void doComplete() throws Exception {
        logger.info("ScreenSampleLineCoordinationService doComplete() start");

        List<String> sampleIdList=(List<String>)processData.getItem("SAMPLE_ID_LIST");
        List<String> sampleNameList=(List<String>)processData.getItem("SAMPLE_NAME_LIST");
        List<String> patternAnnotationPathList=(List<String>)processData.getItem("PATTERN_ANNOTATION_PATH");
        List<String> alignedStackPathList=(List<String>)processData.getItem(("ALIGNED_STACK_PATH_LIST"));
        PatternAnnotationResultNode resultNode=(PatternAnnotationResultNode)processData.getItem(ProcessDataConstants.RESULT_FILE_NODE);

        int index=0;

        for (String patternAnnotationPath : patternAnnotationPathList) {
            String sampleName=sampleNameList.get(index);
            File patternAnnotationDir=new File(patternAnnotationPath);
            //logger.info("Top of doComplete() loop, index="+index+" sampleName="+sampleName+" patternAnnotationDir="+patternAnnotationPath);
            if (!patternAnnotationDir.exists()) {
                throw new Exception("Could not find expected pattern annotation dir="+patternAnnotationDir.getAbsolutePath());
            }
            File mipDir=new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME);
            cleanFilesFromDirectory(".tif", mipDir);

            // We are going to move responsibility for placing the output files in this subdirectory structure on the
            // V3D layer, for performance reasons.

            //moveFilesToSubDirectory("mip", patternAnnotationDir, new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME));
            //moveFilesToSubDirectory("normalized", patternAnnotationDir, new File(patternAnnotationDir, NORMALIZED_SUBFOLDER_NAME));
            //moveFilesToSubDirectory("quant", patternAnnotationDir, new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME));

            //logger.info("Calling patternAnnotationDirIsComplete with sampleName="+sampleName);
            if (!patternAnnotationDirIsComplete(sampleName, patternAnnotationDir, true /* verbose */)) {
                throw new Exception("Pattern annotation in this dir is incomplete="+patternAnnotationPath);
            } else {
                addPatternAnnotationResultEntitiesToSample(sampleIdList.get(index), sampleName, patternAnnotationDir);
            }
            index++;
        }
    }

    protected void moveFilesToSubDirectory(String nameFragment, File fromDir, File toDir) throws Exception {
        if (!fromDir.exists()) {
            throw new Exception("Could not find directory="+fromDir.getAbsolutePath());
        }
        if (!toDir.exists()) {
            throw new Exception("Could not find directory="+toDir.getAbsolutePath());
        }
        List<File> fromList=new ArrayList<File>();
        File[] fromFileArr=fromDir.listFiles();
        for (File f : fromFileArr) {
            if (!f.isDirectory() && f.getName().toLowerCase().contains(nameFragment)) {
                fromList.add(f);
            }
        }
        for (File fromFile : fromList) {
            File toFile=new File(toDir, fromFile.getName());
            logger.info("Moving "+fromFile.getAbsolutePath()+" to "+toFile.getAbsolutePath());
            FileUtil.moveFileUsingSystemCall(fromFile, toFile);
        }
    }

    protected void cleanFilesFromDirectory(String nameFragment, File dir) throws Exception {
        File[] fileArr=dir.listFiles();
        for (File f : fileArr) {
            if (!f.isDirectory() && f.getName().toLowerCase().contains(nameFragment)) {
                logger.info("Deleting file "+f.getAbsolutePath());
                f.delete();
            }
        }
    }

    protected Entity getSubFolderByName(Entity parentEntity, String folderName) {
        for (EntityData ed : parentEntity.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) & child.getName().equals(folderName)) {
                return child;
            }
        }
        return null;
    }

    protected String getFolderNameFromFile(File file) throws Exception {
        String[] tokens=file.getAbsolutePath().split(File.separator);
        return tokens[tokens.length-2];
    }

    protected Entity getEntityFromParentByName(Entity parent, String name) {
        for (EntityData ed : parent.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getName().equals(name)) {
                    return child;
                }
            }
        }
        return null;
    }

    String getPatternAnnotationEntityNameFromFilename(String filename) throws Exception {
        String abbrevation=getAbbreviationFromPatternAnnotationFilename(filename);
        String entityName=filename;
        if (abbrevation!=null) {
            entityName=abbrevation;
            if (filename.toLowerCase().contains("normalized")) {
                entityName=entityName+" normalized";
            }
        } else {
            if (filename.contains("heatmap16ColorMIP")) {
                entityName="Heatmap";
            } else if (filename.contains("heatmap16Color")) {
                entityName="Heatmap";
            } else if (filename.contains("indexCubified")) {
                entityName="Cube Index";
            } else if (filename.contains("inputImageCubified")) {
                entityName="Cube Image";
            } else if (filename.contains("quantifiers")) {
                entityName="Quantifiers";
            }
        }
        return entityName;
    }

    String getAbbreviationFromPatternAnnotationFilename(String filename) throws Exception {
        String[] tokens=getFilenameTokenSet(filename);
        String abbreviation=null;
        if (tokens.length==2) {
            abbreviation=null;
        } else if (tokens.length==3) {
            abbreviation=tokens[1];
        } else if (tokens.length==4 || tokens.length==5) {
            if (tokens[2].contains("normal")) {
                abbreviation=tokens[1];
            } else {
                abbreviation=tokens[1]+"_"+tokens[2];
            }
        } else {
            throw new Exception("Could not properly evaluate filename="+filename+" for abbreviation");
        }
        //logger.info("getAbbreviationFromPatternAnnotationFilename() filename="+filename+" abbreviation="+abbreviation);
        return abbreviation;
    }

    public void addPatternAnnotationResultEntitiesToSample(String sampleId, String sampleName, File patternAnnotationDir) throws Exception {

        Entity screenSample=entityBean.getEntityTree(new Long(sampleId));
        if (screenSample==null) {
            throw new Exception("Could not find screenSample by id="+sampleId);
        }
        List<File> fileList=getExpectedPatternAnnotationResultFiles(patternAnnotationDir, sampleName);

        Entity patternAnnotationFolder=getSubFolderByName(screenSample, PATTERN_ANNOTATION_FOLDER_NAME);
        Entity mipsSubFolder=getSubFolderByName(patternAnnotationFolder, MIPS_SUBFOLDER_NAME);
        Entity normalizedSubFolder=getSubFolderByName(patternAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);
        Entity supportingSubFolder=getSubFolderByName(patternAnnotationFolder, SUPPORTING_FILE_SUBFOLDER_NAME);

        // In this next section, we will iterate through each file, determine its proper entity name, and then
        // decide based on its name what folder it belongs in. Then, we will check to see if this folder already
        // contains an entity with this name. If it does, we will then check that this entity points to the
        // correct filename. If it does not, we will correct the filename.

        // We will take two passes - the first for MIPs and next for stacks
        Map<String, Entity> mipMap=new HashMap<String, Entity>();
        for (File file : fileList) {
            if (file.getName().endsWith(".png") && file.getName().toLowerCase().contains("mip")) {
                String entityName=getPatternAnnotationEntityNameFromFilename(file.getName());
                Entity mipEntity=getEntityFromParentByName(mipsSubFolder, entityName);
                if (mipEntity==null) {
                    mipEntity=createMipEntity(file, entityName);
                    addToParent(mipsSubFolder, mipEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
                //logger.info("Adding to mipMap entityName="+entityName+" based on filename="+file.getName()+" entityId="+mipEntity.getId());
                mipMap.put(entityName, mipEntity);
            }
        }

        // Now we can add mips to the stacks because their entityName should match
        for (File file : fileList) {
            if (file.getName().endsWith(".v3dpbd") && file.getName().toLowerCase().contains("heatmap")) {
                String entityName=getPatternAnnotationEntityNameFromFilename(file.getName());
                Entity stackEntity=null;
                Entity stackFolder=null;
                if (file.getName().toLowerCase().contains("normalized")) {
                    stackFolder=normalizedSubFolder;
                } else {
                    stackFolder=patternAnnotationFolder;
                }
                stackEntity=getEntityFromParentByName(stackFolder, entityName);
                Entity mipEntity=mipMap.get(entityName);
                if (mipEntity==null) {
                    throw new Exception("Could not find MIP Entity to match with stack file="+file.getAbsolutePath());
                }
                if (stackEntity==null) {
                    stackEntity=createStackEntity(file, entityName, mipEntity);
                    addToParent(stackFolder, stackEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }

        // Add supporting data
        File supportingDir=new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        if (supportingDir.exists()) {
            File[] supportingFiles=supportingDir.listFiles();
            if (supportingFiles!=null) {
                for (File f : supportingFiles) {
                    Entity supportingEntity=createSupportingEntity(f, f.getName());
                    addToParent(supportingSubFolder, supportingEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }

        // Finally, we need to add targeted 2D image assignments
        Entity heatmapMip=mipMap.get("Heatmap");
        if (heatmapMip==null) {
            throw new Exception("heatmapMip is unexpectedly null");
        }
        String mipPath=heatmapMip.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        //patternAnnotationFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipPath);

        // We also want to replace the sample 2D image with the heatmap, for those samples where it is available
        screenSample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipPath);
        entityBean.saveOrUpdateEntity(screenSample);
    }

    protected Entity createSupportingEntity(File supportingFile, String name) throws Exception {
        Entity supportingEntity = new Entity();
        supportingEntity.setOwnerKey(ownerKey);
        supportingEntity.setEntityTypeName(EntityConstants.TYPE_TEXT_FILE);
        supportingEntity.setCreationDate(createDate);
        supportingEntity.setUpdatedDate(createDate);
        supportingEntity.setName(name);
        supportingEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, supportingFile.getAbsolutePath());
        supportingEntity = entityBean.saveOrUpdateEntity(supportingEntity);
        return supportingEntity;
    }

    protected Entity createMipEntity(File pngFile, String name) throws Exception {
        Entity mipEntity = new Entity();
        mipEntity.setOwnerKey(ownerKey);
        mipEntity.setEntityTypeName(EntityConstants.TYPE_IMAGE_2D);
        mipEntity.setCreationDate(createDate);
        mipEntity.setUpdatedDate(createDate);
        mipEntity.setName(name);
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity = entityBean.saveOrUpdateEntity(mipEntity);
        return mipEntity;
    }

    protected Entity createStackEntity(File file, String entityName, Entity mipEntity) throws Exception {
        Entity stack = new Entity();
        String mipFilePath=mipEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        stack.setOwnerKey(ownerKey);
        stack.setEntityTypeName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
        stack.setCreationDate(createDate);
        stack.setUpdatedDate(createDate);
        stack.setName(entityName);
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipFilePath);
        stack = entityBean.saveOrUpdateEntity(stack);
        logger.info("Saved stack " + stack.getName() + " as "+stack.getId());
        return stack;
    }

    public List<String> getExpectedPatternAnnotationResultFilenameList(String sampleName) throws Exception {
        List<String> expectedPatternAnnotationResultFilenameList=new ArrayList<String>();
        String[] compartmentSuffixArray=new String[4];
        compartmentSuffixArray[0]="_heatmap16Color.v3dpbd";
        compartmentSuffixArray[1]="_heatmap16ColorMIP.png";
        compartmentSuffixArray[2]="_normalized_heatmap16Color.v3dpbd";
        compartmentSuffixArray[3]="_normalized_heatmap16ColorMIP.png";
        String[] otherSuffixArray=new String[1];
        //otherSuffixArray[0]="_indexCubified.v3dpbd";
        //otherSuffixArray[1]="_inputImageCubified.v3dpbd";
        otherSuffixArray[0]="_quantifiers.txt";
        if (abbrevationList.size()==0) {
            File abbreviationIndexFile=new File(patternAnnotationResourceDir+File.separator+ABBREVIATION_INDEX_FILENAME);
            FileReader fr=new FileReader(abbreviationIndexFile);
            BufferedReader br=new BufferedReader(fr);
            String nextLine=null;
            while ((nextLine=br.readLine())!=null) {
                String[] tokens=nextLine.trim().split(" ");
                if (tokens.length!=2) {
                    throw new Exception("Could not parse line from file="+abbreviationIndexFile.getAbsolutePath()+" line="+nextLine);
                }
                abbrevationList.add(tokens[1]);
            }
            br.close();
        }
        expectedPatternAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[0]);
        expectedPatternAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[1]);
        // Note: there are not normalized files at the global level
        expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[0]);
        //expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[1]);
        //expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[2]);
        for (String abbreviation : abbrevationList) {
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[0]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[1]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[2]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[3]);
        }
        return expectedPatternAnnotationResultFilenameList;
    }

     public List<File> getExpectedPatternAnnotationResultFiles(File patternAnnotationDir, String sampleName) throws Exception {
         //logger.info("Calling getExpectedPatternAnnotationResultFilenameList( with sampleName="+sampleName);
         List<String> filenameList=getExpectedPatternAnnotationResultFilenameList(sampleName);
         File mipSubFolder=new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME);
         File supportingFilesFolder=new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
         File normalizedSubFolder=new File(patternAnnotationDir, NORMALIZED_SUBFOLDER_NAME);
         List<File> expectedFiles=new ArrayList<File>();
         for (String filename : filenameList) {
             //logger.info("getExpectedPatternAnnotationResultFilenameList() filename="+filename);
             String[] tokens=getFilenameTokenSet(filename);
             if (tokens.length==2) {
                 if (tokens[1].equals("indexCubified.v3dpbd")) {
                     File file=new File(supportingFilesFolder, filename);
                     expectedFiles.add(file);
                 } else if (tokens[1].equals("inputImageCubified.v3dpbd")) {
                     File file=new File(supportingFilesFolder, filename);
                     expectedFiles.add(file);
                 } else if (tokens[1].equals("quantifiers.txt")) {
                     File file=new File(supportingFilesFolder, filename);
                     expectedFiles.add(file);
                 } else if (tokens[1].equals("heatmap16ColorMIP.png")) {
                     File file=new File(mipSubFolder, filename);
                     expectedFiles.add(file);
                 } else if (tokens[1].equals("heatmap16Color.v3dpbd")) {
                     File file=new File(patternAnnotationDir, filename);
                     expectedFiles.add(file);
                 }
             } else if (tokens.length==3) {
                 File file=null;
                 if (filename.toLowerCase().contains("mip")) {
                     file=new File(mipSubFolder, filename);
                 } else {
                     file=new File(patternAnnotationDir, filename);
                 }
                 expectedFiles.add(file);
             } else if (tokens.length==4 || tokens.length==5) {
                 File file=null;
                 if (filename.toLowerCase().contains("mip")) {
                     file=new File(mipSubFolder, filename);
                 } else if (filename.toLowerCase().contains("normalized")) {
                     file=new File(normalizedSubFolder, filename);
                 } else {
                     file=new File(patternAnnotationDir, filename);
                 }
                 expectedFiles.add(file);
             } else {
                 throw new Exception("Could not parse filename for expected pattern annotation result file="+filename);
             }
         }
         return expectedFiles;
     }

    // The first array member is the name, the 2nd the remaining component of the filename
    //
    // example: GMR_64G06_AE_01_00-fA01b_C101002_20101003110018750_heatmap16Color.v3dpbd

    public String[] getGmrSampleNameFilename(String filename) {
        String[] result=new String[2];
        Pattern gmrPattern=Pattern.compile("(\\D\\D\\D\\_[\\w&&[^\\_]]+\\_[\\w&&[^\\_]]+\\_[\\w&&[^\\_]]+\\_[\\w&&[^\\_]]+-[\\w&&[^\\_]]+\\_[\\w&&[^\\_]]+\\_\\d+)\\_(\\S.+)");
        Matcher gmrMatcher=gmrPattern.matcher(filename);
        if (gmrMatcher.matches()) {
            result[0]=gmrMatcher.group(1);
            result[1]=gmrMatcher.group(2);
            return result;
        } else {
            return null;
        }
    }

    public String[] getFilenameTokenSet(String filename) {
        String[] tokens=null;
        String [] gmrComponents=getGmrSampleNameFilename(filename);
        if (gmrComponents!=null) {
            String[] remainingComponents=gmrComponents[1].split("_");
            int totalComponents=remainingComponents.length+1;
            tokens=new String[totalComponents];
            for (int i=0;i<totalComponents;i++) {
                if (i==0) {
                    tokens[i]=gmrComponents[0];
                } else {
                    tokens[i]=remainingComponents[i-1];
                }
            }
        } else {
            tokens=filename.split("_");
        }
        //logger.info("getFilenameTokenSet filename="+filename);
        //for (int i=0;i<tokens.length;i++) {
        //    logger.info("token "+i+" ="+tokens[i]);
        //}
        return tokens;
    }

}

