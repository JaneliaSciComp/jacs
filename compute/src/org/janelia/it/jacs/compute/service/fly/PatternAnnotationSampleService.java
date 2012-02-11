package org.janelia.it.jacs.compute.service.fly;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
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
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
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

    protected String patternAnnotationResourceDir=SystemConfigurationProperties.getString("FlyScreen.CompartmentAnnotationResourceDir");
    protected String patternChannel=SystemConfigurationProperties.getString("FlyScreen.AlignedStackPatternChannel");

    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected String mode=MODE_UNDEFINED;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Double QI_MAXIMUM=0.0;
    protected Double QM_MAXIMUM=0.0;
    protected Boolean refresh;
    protected List<String> expectedPatternAnnotationResultFilenameList=new ArrayList<String>();


    public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("PatternAnnotationSampleService execute() start");

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
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
        logger.info("PatternAnnotationSampleService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " intended path="+resultNode.getDirectoryPath());
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
            Entity flyLineEntity=annotationBean.getEntityTree(new Long(flyLineEntityId.trim()));
            for (EntityData ed : flyLineEntity.getEntityData()) {
                if (ed.getChildEntity().getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                    sampleList.add(ed.getChildEntity());
                }
            }
        }

        logger.info("Processing " + sampleList.size() + " Screen Samples");
        long sampleDirFailureCount=0;
        for (Entity sample : sampleList) {

            logger.info("Processing sample name="+sample.getName());

            // This is the directory containing the link to the stack, and will have a pattern annotation
            // subdirectory, as well as a separte supportingFiles directory.
            File sampleResultDir=getOrUpdateSampleResultDir(sample);
            if (sampleResultDir==null) {
                logger.info("Could not find sample result directory for sampleId="+sample.getId()+" name="+sample.getName());
                sampleDirFailureCount++;
                continue; // move on to next sample
            }

            // This method ensures that there is a supporting directory for the sample in the sample
            // result node directory, and that the mip has been relocated to this directory.
            updateSampleSupportingDirIfNecessary(sample);

            if (refresh) {
                Entity patternAnnotationFolder=getPatternAnnotationFolderAndCleanExtra(sample);
                if (patternAnnotationFolder!=null) {
                    cleanFullOrIncompletePatternAnnotationFolderAndFiles(patternAnnotationFolder);
                }
                // Refresh sample after delete
                logger.info("Refreshing sample after clean operation");
                sample=annotationBean.getEntityTree(sample.getId());
            }

            // This ensures that the patternAnnotation directory and its subdirs are correctly setup.
            File patternAnnotationDir=getOrUpdatePatternAnnotationDir(sample);

            File stackFile=null;
            String QmScore=null;
            String QiScore=null;

            for (EntityData ed : sample.getEntityData()) {
                Entity child=ed.getChildEntity();
                if (child!=null) {
                    if (child.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                        logger.info("Found Aligned Brain Stack child");
                        stackFile=new File(child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        QmScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE);
                        QiScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
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
        List<String> finalAlignedStackList=new ArrayList<String>();

        long alreadyCompleteSampleCount=0;
        for (Entity alignedSample : properlyAlignedSampleList) {
            logger.info("Checking to see if aligned Sample name="+alignedSample.getName()+" is complete");
            if (!patternAnnotationDirIsComplete(alignedSample.getName(), new File(patternAnnotationDirList.get(sampleIndex)), false)) {
                finalSampleIdList.add(alignedSample.getId().toString());
                finalSampleNameList.add(alignedSample.getName());
                finalAnnotationDirList.add(patternAnnotationDirList.get(sampleIndex));
                finalAlignedStackList.add(alignedStackPathList.get(sampleIndex));
                sampleIndex++;
            } else {
                logger.info("Sample is complete - skipping");
                alreadyCompleteSampleCount++;
            }
        }

        processData.putItem("SAMPLE_ID_LIST", finalSampleIdList);
        processData.putItem("SAMPLE_NAME_LIST", finalSampleNameList);
        processData.putItem("PATTERN_ANNOTATION_PATH", finalAnnotationDirList);
        processData.putItem("ALIGNED_STACK_PATH_LIST", finalAlignedStackList);
        processData.putItem("RESOURCE_DIR_PATH", patternAnnotationResourceDir);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
        processData.putItem("PATTERN_CHANNEL", patternChannel);

        for (String sampleName : finalSampleNameList) {
            logger.info("doSetup() : Adding sampleName to list="+sampleName);
        }

        logger.info("End of doSetup() - including "+finalSampleNameList.size()+" samples - skipped "+alreadyCompleteSampleCount+
                " already-complete samples, and skipped "+sampleDirFailureCount+" samples due to missing sample result directories");
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
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_2D) && child.getName().toLowerCase().contains("mip")) {
                    File mipFile=new File(child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    logger.info("getOrUpdateSampleResultDir() - found mip="+mipFile.getAbsolutePath());
                    nodeDir=mipFile.getParentFile();
                    if (nodeDir.getName().equals(SUPPORTING_FILE_SUBFOLDER_NAME)) {
                        // Then we need to click-up to the next level
                        nodeDir=nodeDir.getParentFile();
                    }
                    logger.info("getOrUpdateSampleResultDir() - based on mip, assuming nodeDir="+nodeDir.getAbsolutePath()+" nodeId="+nodeDir.getName());
                } else if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(SUPPORTING_FILE_SUBFOLDER_NAME)) {
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
            } else {
                throw new Exception("Could not verify result node id="+nodeId);
            }
        }
        return nodeDir;
    }

    File getResultNodeDirFromSample(Entity sample) {
        String resultNodeIdString=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID);
        Long resultNodeId=new Long(resultNodeIdString.trim());
        ScreenSampleResultNode resultNode=(ScreenSampleResultNode)computeBean.getNodeById(resultNodeId);
        File dir=new File(resultNode.getDirectoryPath());
        return dir;
    }

    protected void updateSampleSupportingDirIfNecessary(Entity sample) throws Exception {
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

        String sampleDefault2DImagePath=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        boolean replaceSampleDefault2DImage=false;
        logger.info("Checking sample supporting dir status for sample id="+sample.getId());
        for (EntityData ed : sample.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_2D)) {
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
                } else if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(FlyScreenSampleService.SUPPORTING_FILES_FOLDER_NAME)) {
                    supportingFolder=child;
                }
            }
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
            }
            annotationBean.deleteEntityData(rawMipEd);
            File newMipFile=new File(supportingDir, rawMipFile.getName());
            logger.info("Moving mip to new location="+newMipFile.getAbsolutePath());
            FileUtil.moveFileUsingSystemCall(rawMipFile, newMipFile);
            addToParent(supportingFolder, rawMip, null, EntityConstants.ATTRIBUTE_ENTITY);
            rawMip.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipFile.getAbsolutePath());
            rawMip.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, newMipFile.getAbsolutePath());
            annotationBean.saveOrUpdateEntity(rawMip);
            if (replaceSampleDefault2DImage) {
                logger.info("Resetting default 2D image for screen sample to new image location");
                sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, newMipFile.getAbsolutePath());
                annotationBean.saveOrUpdateEntity(sample);
            }
        } else {
            logger.info("Could not locate mip in prior location, so assuming does not need update for sample id="+sample.getId());
        }
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
                annotationBean.saveOrUpdateEntity(subFolder);
            }
        }
        if (!subFolderDir.exists() && !subFolderDir.mkdir()) {
            throw new Exception("Could not create subfolder="+subFolderDir.getAbsolutePath());
        }
    }

    File getOrUpdatePatternAnnotationDir(Entity sample) throws Exception {
        File sampleDir=getOrUpdateSampleResultDir(sample);

        // Check to see if the sample already has a patternAnnotation folder
        Entity patternAnnotationFolder=getPatternAnnotationFolderAndCleanExtra(sample);
        File patternAnnotationCorrectDir=new File(sampleDir, PATTERN_ANNOTATION_SUBDIR_NAME);
        if (patternAnnotationFolder==null) {
            patternAnnotationFolder=addChildFolderToEntity(sample, PATTERN_ANNOTATION_FOLDER_NAME, patternAnnotationCorrectDir.getAbsolutePath());
        } else {
            String patternAnnotationActualDirPath=patternAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!patternAnnotationActualDirPath.equals(patternAnnotationCorrectDir.getAbsolutePath())) {
                patternAnnotationFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, patternAnnotationCorrectDir.getAbsolutePath());
                annotationBean.saveOrUpdateEntity(patternAnnotationFolder);
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
        String patternDirPath=patternAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (patternDirPath==null) {
            logger.info("Could not find directory path for previous pattern folder id="+patternAnnotationFolder.getId());
            return;
        }
        File patternDir=new File(patternDirPath);
        logger.info("Removing prior pattern annotation folder at location="+patternDir.getAbsolutePath());
        String patternAnnotationDirPath=patternDir.getAbsolutePath();
        List<File> filesToDelete=new ArrayList<File>();

        // We need to iterate through the entities and delete - note we only want to delete 'official'
        // contents and not other links.
        for (EntityData ed : patternAnnotationFolder.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getUser().getUserLogin().equals(task.getOwner())) {
                    if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                        String folderPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                        File childFolderFile=new File(folderPath);
                        File childFolderParent=childFolderFile.getParentFile();
                        if (childFolderParent.getAbsolutePath().equals(patternAnnotationDirPath)) {
                            filesToDelete.add(childFolderFile);
                            File[] childFolderFiles=childFolderFile.listFiles();
                            if (childFolderFiles!=null && childFolderFiles.length>0) {
                                for (File f : childFolderFiles) {
                                    filesToDelete.add(f);
                                }
                            }
                        }
                    }
                    String filePath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (filePath!=null) {
                        File childFile=new File(filePath);
                        File parentDir=childFile.getParentFile();
                        if (parentDir.getAbsoluteFile().equals(patternAnnotationDirPath)) {
                            // We can be confident that the file needs to be deleted
                            logger.info("Deleting prior annotation file="+childFile.getAbsolutePath());
                            filesToDelete.add(childFile);
                        }
                    }
                } else {
                    logger.error("Cannot delete entity belonging to user="+child.getUser().getUserLogin()+" as user="+task.getOwner());
                }
            }
        }

        // Now we know the files to delete, so we can delete the folder entity tree
        logger.info("Deleting entity tree for prior pattern annotation folderId="+patternAnnotationFolder.getId());
        annotationBean.deleteSmallEntityTree(task.getOwner(), patternAnnotationFolder.getId());
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
        folder.setUser(user);
        folder.setName(name);
        folder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        if (directoryPath!=null) {
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, directoryPath);
        }
        folder = annotationBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    public Entity getPatternAnnotationFolderAndCleanExtra(Entity screenSample) throws Exception {
        List<Entity> patternAnnotationFolderList=new ArrayList<Entity>();
        for (EntityData ed : screenSample.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(PATTERN_ANNOTATION_FOLDER_NAME)) {
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
        for (int i=0;i<patternAnnotationFolderList.size();i++) {
            if (i<patternAnnotationFolderList.size()-1) {
                logger.info("Removing extra pattern folder id="+patternAnnotationFolderList.get(i).getId());
                annotationBean.deleteSmallEntityTree(user.getUserLogin(), patternAnnotationFolderList.get(i).getId());
            }
        }
        return null;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
                " as child of "+parent.getEntityType().getName()+"#"+parent.getId());
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
            if (!patternAnnotationDir.exists()) {
                throw new Exception("Could not find expected pattern annotation dir="+patternAnnotationDir.getAbsolutePath());
            }
            if (!patternAnnotationDirIsComplete(sampleName, patternAnnotationDir, true /* verbose */)) {
                throw new Exception("Pattern annotation in this dir is incomplete="+patternAnnotationPath);
            } else {
                addPatternAnnotationResultEntitiesToSample(sampleIdList.get(index), sampleName, patternAnnotationDir);
            }
            index++;
        }
    }

    protected Entity getSubFolderByName(Entity parentEntity, String folderName) {
        for (EntityData ed : parentEntity.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) & child.getName().equals(folderName)) {
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
                entityName="Heatmap MIP";
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
        String[] tokens=filename.split("_");
        if (tokens.length==2) {
            return null;
        } else if (tokens.length==3) {
            return tokens[1];
        } else if (tokens.length==4 || tokens.length==5) {
            return tokens[1]+"_"+tokens[2];
        } else {
            throw new Exception("Could not properly evaluate filename="+filename+" for abbreviation");
        }
    }

    public void addPatternAnnotationResultEntitiesToSample(String sampleId, String sampleName, File patternAnnotationDir) throws Exception {

        Entity screenSample=annotationBean.getEntityTree(new Long(sampleId));
        if (screenSample==null) {
            throw new Exception("Could not find screenSample by id="+sampleId);
        }
        List<File> fileList=getExpectedPatternAnnotationResultFiles(patternAnnotationDir, sampleName);

        Entity patternAnnotationFolder=getSubFolderByName(screenSample, PATTERN_ANNOTATION_FOLDER_NAME);
        Entity mipsSubFolder=getSubFolderByName(patternAnnotationFolder, MIPS_SUBFOLDER_NAME);
        Entity normalizedSubFolder=getSubFolderByName(patternAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);

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

        // Finally, we need to add targeted 2D image assignments
        String mipPath=mipMap.get("Heatmap MIP").getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        patternAnnotationFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipPath);

        // We also want to replace the sample 2D image with the heatmap, for those samples where it is available
        screenSample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipPath);
    }

    protected Entity createMipEntity(File pngFile, String name) throws Exception {
        Entity mipEntity = new Entity();
        mipEntity.setUser(user);
        mipEntity.setEntityType(EJBFactory.getLocalAnnotationBean().getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D));
        mipEntity.setCreationDate(createDate);
        mipEntity.setUpdatedDate(createDate);
        mipEntity.setName(name);
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity = EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(mipEntity);
        return mipEntity;
    }

    protected Entity createStackEntity(File file, String entityName, Entity mipEntity) throws Exception {
        Entity stack = new Entity();
        String mipFilePath=mipEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        stack.setUser(user);
        stack.setEntityType(EJBFactory.getLocalAnnotationBean().getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK));
        stack.setCreationDate(createDate);
        stack.setUpdatedDate(createDate);
        stack.setName(entityName);
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, mipFilePath);
        stack = EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(stack);
        logger.info("Saved stack " + stack.getName() + " as "+stack.getId());
        return stack;
    }

    public List<String> getExpectedPatternAnnotationResultFilenameList(String sampleName) throws Exception {
         if (expectedPatternAnnotationResultFilenameList.size()>0) {
             return expectedPatternAnnotationResultFilenameList;
         } else {
             String[] compartmentSuffixArray=new String[4];
             compartmentSuffixArray[0]="_heatmap16Color.v3dpbd";
             compartmentSuffixArray[1]="_heatmap16ColorMIP.png";
             compartmentSuffixArray[2]="_normalized_heatmap16Color.v3dpbd";
             compartmentSuffixArray[3]="_normalized_heatmap16ColorMIP.png";
             String[] otherSuffixArray=new String[3];
             otherSuffixArray[0]="_indexCubified.v3dpbd";
             otherSuffixArray[1]="_inputImageCubified.v3dpbd";
             otherSuffixArray[2]="_quantifiers.txt";
             File abbreviationIndexFile=new File(patternAnnotationResourceDir+File.separator+ABBREVIATION_INDEX_FILENAME);
             FileReader fr=new FileReader(abbreviationIndexFile);
             BufferedReader br=new BufferedReader(fr);
             String nextLine=null;
             List<String> abbrevationList=new ArrayList<String>();
             while ((nextLine=br.readLine())!=null) {
                 String[] tokens=nextLine.trim().split(" ");
                 if (tokens.length!=2) {
                     throw new Exception("Could not parse line from file="+abbreviationIndexFile.getAbsolutePath()+" line="+nextLine);
                 }
                 abbrevationList.add(tokens[1]);
             }
             expectedPatternAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[0]);
             expectedPatternAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[1]);
             // Note: there are not normalized files at the global level
             expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[0]);
             expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[1]);
             expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[2]);
             for (String abbreviation : abbrevationList) {
                 expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[0]);
                 expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[1]);
                 expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[2]);
                 expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[3]);
             }
             return expectedPatternAnnotationResultFilenameList;
         }
     }

     public List<File> getExpectedPatternAnnotationResultFiles(File patternAnnotationDir, String sampleName) throws Exception {
         List<String> filenameList=getExpectedPatternAnnotationResultFilenameList(sampleName);
         File mipSubFolder=new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME);
         File supportingFilesFolder=new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
         File normalizedSubFolder=new File(patternAnnotationDir, NORMALIZED_SUBFOLDER_NAME);
         List<File> expectedFiles=new ArrayList<File>();
         for (String filename : filenameList) {
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
                 File file=new File(patternAnnotationDir, filename);
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
        Pattern gmrPattern=Pattern.compile("(\\D\\D\\D\\_\\w+\\_\\w+\\_\\w+\\_\\w+-\\w+\\_\\w+\\_\\w+)(\\_.+)");
        Matcher gmrMatcher=gmrPattern.matcher(filename);
        if (gmrMatcher.matches()) {
            result[0]=gmrMatcher.group(1);
            result[1]=gmrMatcher.group(2);
            return result;
        } else {
            return null;
        }
    }

}

