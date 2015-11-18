package org.janelia.it.jacs.compute.service.fly;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.MaskAnnotationResultNode;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
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


//        1) determine the subset which are appropriate for processing given the quality of their alignment
//        2) create the ResultNodes and various subdirectories for managing the results
//        3) organize the samples into collections to be processed by the Vaa3D image processing utilities
//

public class MaskSampleAnnotationService  implements IService {

    private static final Logger logger = Logger.getLogger(MaskSampleAnnotationService.class);

    private final boolean DEBUG = false;

    final public String MASK_ANNOTATION_SUBDIR_NAME="maskAnnotation";
    final static public String MASK_ANNOTATION_FOLDER_NAME="Mask Annotation";
    final public String MIPS_SUBFOLDER_NAME="mips";
    final public String SUPPORTING_FILE_SUBFOLDER_NAME="supportingFiles";
    final public String NORMALIZED_SUBFOLDER_NAME="normalized";
    final public String MASK_NAME_INDEX_FILENAME="maskNameIndex.txt";
    final public String COMPLETE_MARKER_FILENAME="FilesCompleteMarker";


    final public String MODE_UNDEFINED="UNDEFINED";
    final public String MODE_SETUP="SETUP";
    final public String MODE_COMPLETE="COMPLETE";

    protected String maskAnnotationTopResourceDir=SystemConfigurationProperties.getString("FileStore.CentralDir")+
            SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
    protected String patternChannel=SystemConfigurationProperties.getString("FlyScreen.AlignedStackPatternChannel");

    //protected EntityBeanLocal entityBean;
    //protected ComputeBeanLocal computeBean;

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;


    protected String ownerKey;
    protected Date createDate;
    protected String mode=MODE_UNDEFINED;
    protected Task task;
    protected EntityHelper entityHelper;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Double QI_MAXIMUM=0.0;
    protected Double QM_MAXIMUM=0.0;
    protected Boolean refresh;
    protected String maskAnnotationFolderName;
    protected File maskAnnotationResourceDir;
    protected List<String> abbrevationList=new ArrayList<String>();

    protected Long subfolderCreationCount=0L;
    protected Long mipsFolderEntityCount=0L;


    public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("MaskSampleAnnotationService execute() start");

            refreshEntityBeans();

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            logger.info("MaskSampleAnnotationService running under TaskId="+task.getObjectId());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;

            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            createDate = new Date();
            mode = processData.getString("MODE");
            refresh=processData.getString("REFRESH").trim().toLowerCase().equals("true");
            maskAnnotationFolderName=processData.getString("ROOT_ENTITY_NAME");

            if (maskAnnotationFolderName==null) {
                throw new Exception("ROOT_ENTITY_NAME must be defined in processData to determine maskAnnotationFolder");
            } else {
                maskAnnotationResourceDir=new File(maskAnnotationTopResourceDir, maskAnnotationFolderName);
                logger.info("Using maskAnnotationFolder="+maskAnnotationResourceDir.getAbsolutePath());
            }

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

    protected void refreshEntityBeans() throws Exception {
        entityBean = EJBFactory.getLocalEntityBean();
        computeBean = EJBFactory.getLocalComputeBean();
        entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger); // can't be in constructor or will timeout
    }


    /*
            What we want to do here is get everything ready to run the mask annotation on the grid.
            Because we are (at this point) encouraging the user to go to the filesystem to access stacks
            and such, it is best if we continue to add sample-specific results and data to the same
            location in cases where the new data is directly analytically derived from the existing
            data. For this reason, we will add the mask annotation as a sub-directory to the
            existing ScreenSampleResultNode.

            Note that we will always add mask annotation to the <sample>/<mask annotation>/<mask folder>
            location, where <mask folder> is a name which varies depending on the mask set to be applied.
            The current plan is for different  mask sets to be configured with resources in different
            subfolders under a mask resources parent directory.

            So, here are the setup steps:

            1) iterate over the Fly Lines in the group list

            2) for each Fly Line, get the set of Samples

            3) for each Sample, get the most recent ScreenSampleResultNode

            4) create a subdirectory within the SceenSampleResultNode which
                corresponds to the appropriate <mask annotation>/<mask set> nomenclature

            5) Use the directory from #4 as the output for the mask annotation step.

            6) Get the correct aligned stack path for the sample, and use this as input
                for the mask annotation.

            NOTE: as part of step 6, we will also get the alignment scores for the
            aligned stack, and if the scores are not over some minimal threshold we
            will not include this sample for processing.

            REFRESH: if refresh is 'true', then we will delete any previous pattern annotation
            for each sample, and replace it with new results. If 'false', then we will skip
            adding new pattern annotation if it already exists.

     */

    public void doSetup() throws Exception {
        logger.info("MaskSampleAnnotationService doSetup() start");

        List<Entity> sampleList=new ArrayList<Entity>();
        List<String> flyLineIdList=(List<String>)processData.getItem("FLY_LINE_GROUP_LIST");
        logger.info("MaskSampleAnnotationService execute() contains "+flyLineIdList.size()+" fly lines, mode="+mode);

        // Create Mask Annotation Result Node - note that other than the grid-metadata, the result of this service will
        // actually be placed with the ScreenSampleResultNode, so that there is a unified location for the user to browse
        // sample-related data on the filesystem.

        MaskAnnotationResultNode resultNode = new MaskAnnotationResultNode(task.getOwner(), task, "MaskAnnotationResultNode",
                "MaskAnnotationResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);

        // Update sessionName
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        logger.info("MaskAnnotationSampleService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " updated sessionName="+sessionName+
                " intended path="+resultNode.getDirectoryPath());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultNode.getDirectoryPath());
        String creationMessage="Created MaskAnnotationSampleService path="+resultNode.getDirectoryPath()+" id="+resultNode.getObjectId();
        logger.info(creationMessage);

        // We need an index-synchronized set of lists to track those samples with sufficiently good alignments to process here.
        // We hope this is the overwhelming majority of all samples, but will not be all samples.

        List<String> properlyAlignedSampleIdList=new ArrayList<String>();
        List<Entity> properlyAlignedSampleList=new ArrayList<Entity>();
        List<String> maskAnnotationDirList=new ArrayList<String>();
        List<String> alignedStackPathList=new ArrayList<String>();

        // Generate the list of samples from the FlyLine list
        logger.info("Processing " + flyLineIdList.size() + " FlyLine entries");
        for (String flyLineEntityId : flyLineIdList) {
            //Entity flyLineEntity=entityBean.getEntityTree(new Long(flyLineEntityId.trim()));
            Entity flyLineEntity=entityBean.getEntityById(flyLineEntityId.trim());
            Set<Entity> flyLineChildren=entityBean.getChildEntities(new Long(flyLineEntityId));
            for (Entity flyLineChild : flyLineChildren) {
                if (flyLineChild.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                    sampleList.add(flyLineChild);
                }
            }
        }

        logger.info("Processing " + sampleList.size() + " Screen Samples");
        long sampleDirFailureCount=0;
        Map<Entity, Long> entityNewSubfolderCountMap=new HashMap<Entity, Long>();
        Map<Entity, Long> entityNewMipEntityCountMap=new HashMap<Entity, Long>();
        for (Entity sample : sampleList) {

            // Refresh beans
//            logger.info("Refreshing EJB instances");
//            refreshEntityBeans();

            // user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());

            logger.info("Processing sample name="+sample.getName());

            if (!refresh) {
                Entity maskTopLevelFolder=getSubFolderByNameWithoutSession(sample, MASK_ANNOTATION_FOLDER_NAME);
                if (maskTopLevelFolder!=null) {
                    String maskDirPath=maskTopLevelFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (maskDirPath!=null) {
                        File maskFolderDir=new File(maskDirPath, maskAnnotationFolderName);
                        File doneFile=new File(maskFolderDir.getAbsolutePath(), COMPLETE_MARKER_FILENAME);
                        if (doneFile.exists()) {
                            logger.info("Found done marker file - skipping sample "+sample.getName());
                            continue;
                        } else {
                            logger.info("done marker file "+doneFile.getAbsolutePath()+" does not exist - processing sample "+sample.getName());
                        }
                    } else {
                        logger.info("maskDirPath is null - cannot done-check sample "+sample.getName());
                    }
                } else {
                    logger.info("maskTopLevelFolder is null - cannot done-check sample "+sample.getName());
                }
            }


            // This is the directory containing the link to the stack, and will have a mask annotation
            // subdirectory, as well as a separte supportingFiles directory.
            File sampleResultDir=getOrUpdateSampleResultDir(sample);
            if (sampleResultDir==null) {
                logger.info("Could not find sample result directory for sampleId="+sample.getId()+" name="+sample.getName());
                sampleDirFailureCount++;
                continue; // move on to next sample
            }

            // refresh again after sample dir update
            sample=entityBean.getEntityTree(sample.getId());

            // If refresh, then delete previous mask contents
            if (refresh) {
                if (DEBUG) logger.info("Refresh is true - checking status of mask annotation folder="+maskAnnotationFolderName+" for sample="+sample.getName());
                Entity maskAnnotationFolder=getMaskAnnotationFolderFromSampleByName(sample, sampleResultDir, maskAnnotationFolderName, false);
                if (maskAnnotationFolder!=null) {
                        if (DEBUG) logger.info("Deleting mask annotation folder id="+maskAnnotationFolder.getId()+" dir="+maskAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        cleanFullOrIncompleteMaskAnnotationFolderAndFiles(maskAnnotationFolder);
                } else {
                    if (DEBUG) logger.info("mask annotation folder is null - nothing to clean");
                }
                // Refresh sample after delete
                logger.info("Refreshing sample after clean operation");
                sample=entityBean.getEntityTree(sample.getId());
            } else {
                if (DEBUG) logger.info("Refresh is false - using prior data");
            }

            // This ensures that the mask annotation directory and its subdirs are correctly setup.
            Long startingSubfolderCount=subfolderCreationCount;
            Long startingMipsEntityCount=mipsFolderEntityCount;
            Entity maskAnnotationFolder=getMaskAnnotationFolderFromSampleByName(sample, sampleResultDir, maskAnnotationFolderName, true);
            maskAnnotationFolder=configureMaskSubdirectories(maskAnnotationFolder);
            Long newMipsEntityCount=mipsFolderEntityCount-startingMipsEntityCount;

            // This is to detect and track the case where the results are precomputed but the entity tree is not yet created
            Long newSubfoldersForThisSample=subfolderCreationCount-startingSubfolderCount;
            File maskAnnotationDir=new File(maskAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

            File stackFile=null;
            String QmScore=null;
            String QiScore=null;

            // Do another refresh
            sample=entityBean.getEntityTree(sample.getId());
            entityNewSubfolderCountMap.put(sample, newSubfoldersForThisSample);
            entityNewMipEntityCountMap.put(sample, newMipsEntityCount);

            for (EntityData ed : sample.getEntityData()) {
                Entity child=ed.getChildEntity();
                if (child!=null) {
                    if (child.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                        if (DEBUG) logger.info("Found Aligned Brain Stack child");
                        stackFile=new File(child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        QmScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE);
                        QiScore=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
                        if (DEBUG) logger.info("QmScore="+QmScore+" QiScore="+QiScore);
                    }
                }
            }

            if (QmScore!=null && QiScore!=null) {
                Double qm=new Double(QmScore.trim());
                Double qi=new Double(QiScore.trim());
                if (DEBUG) logger.info("Using Double-valued Qm="+qm+" Qi="+qi+" QM_MAXIMUM="+QM_MAXIMUM+" QI_MAXIMUM="+QI_MAXIMUM);
                boolean qScoresUndefined=(qm==0.0 && qi==0.0);
                //if (!qScoresUndefined && qm<=QM_MAXIMUM && qi<=QI_MAXIMUM) {
                if (!qScoresUndefined && qi<=QI_MAXIMUM) {
                    if (DEBUG) logger.info("Adding properly-aligned sampleName="+sample.getName());
                    properlyAlignedSampleList.add(sample);
                    properlyAlignedSampleIdList.add(sample.getId().toString());
                    maskAnnotationDirList.add(maskAnnotationDir.getAbsolutePath());
                    alignedStackPathList.add(stackFile.getAbsolutePath());
                    if (DEBUG) logger.info("This stack has valid Qm and Qi scores="+stackFile.getAbsolutePath());
                } else {
                    if (DEBUG) logger.info("Skipping stack="+stackFile.getAbsolutePath()+" due to poor or undefined Qm and Qi scores");
                }
            } else {
                logger.error("Could not find expected QmScore and QiScore attributes from aligned brain stack of sample="+sample.getId());
            }

        }

        // Finally, make sure the mask annotation folder entity and directory exist for each sample. Note that
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
            File maskAnnotationDir=new File(maskAnnotationDirList.get(sampleIndex));
            if (!maskAnnotationDirIsComplete(alignedSample.getName(), maskAnnotationDir, false)) {
                logger.info("Warning: mask annotation dir is incomplete: "+maskAnnotationDir.getAbsolutePath());
//                finalSampleIdList.add(alignedSample.getId().toString());
//                finalSampleNameList.add(alignedSample.getName());
//                finalAnnotationDirList.add(maskAnnotationDirList.get(sampleIndex));
//                finalMipConversionDirList.add(maskAnnotationDirList.get(sampleIndex)+File.separator+MIPS_SUBFOLDER_NAME);
//                finalAlignedStackList.add(alignedStackPathList.get(sampleIndex));
            } else {
                Long newSubfoldersForThisSample=entityNewSubfolderCountMap.get(alignedSample);
                Long newMipEntityCount=entityNewMipEntityCountMap.get(alignedSample);
                if (newSubfoldersForThisSample>0 || newMipEntityCount==0) {
                    logger.info("Mask annotation dir content is complete but the entity tree needs to be created");
                    addMaskAnnotationResultEntitiesToSample(alignedSample.getId().toString(), alignedSample.getName(), maskAnnotationDir);
                    logger.info("Done creating entity tree");
                } else {
                    logger.info("Sample is complete - skipping");
                }
                alreadyCompleteSampleCount++;
            }
            sampleIndex++;
        }

        Map<String, FileNode> mipResultNodeMap=createResultNodeMapForMipConversion(finalMipConversionDirList);

        long sampleCount=finalSampleNameList.size();

// Commented-out to test the entity-creation-only mode

//        processData.putItem("SAMPLE_ID_LIST", finalSampleIdList);
//        processData.putItem("SAMPLE_NAME_LIST", finalSampleNameList);
//        processData.putItem("MASK_ANNOTATION_PATH", finalAnnotationDirList);
//        processData.putItem("MIPS_CONVERSION_PATH", finalMipConversionDirList);
//        processData.putItem("ALIGNED_STACK_PATH_LIST", finalAlignedStackList);
//        processData.putItem("RESOURCE_DIR_PATH", maskAnnotationResourceDir.getAbsolutePath());
//        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
//        processData.putItem("PATTERN_CHANNEL", patternChannel);
//        processData.putItem("IMAGE_CONVERSION_RESULT_NODE_MAP", mipResultNodeMap);
        processData.putItem("SAMPLE_COUNT", sampleCount);

        for (String sampleName : finalSampleNameList) {
            if (DEBUG) logger.info("doSetup() : Adding sampleName to list="+sampleName);
        }

        logger.info("End of doSetup() - including "+finalSampleNameList.size()+" samples - skipped "+alreadyCompleteSampleCount+
                " already-complete samples, and skipped "+sampleDirFailureCount+" samples due to missing sample result directories");
    }

    Entity configureMaskSubdirectories(Entity maskFolder) throws Exception {
        Entity mipsSubFolder=verifyOrCreateSubFolder(maskFolder, MIPS_SUBFOLDER_NAME, MIPS_SUBFOLDER_NAME);
        mipsFolderEntityCount+=mipsSubFolder.getChildren().size();
        verifyOrCreateSubFolder(maskFolder, SUPPORTING_FILE_SUBFOLDER_NAME, SUPPORTING_FILE_SUBFOLDER_NAME);
        verifyOrCreateSubFolder(maskFolder, NORMALIZED_SUBFOLDER_NAME, NORMALIZED_SUBFOLDER_NAME);
        return maskFolder;
    }

    Entity getMaskAnnotationFolderFromSampleByName(Entity sample, File sampleDir, String maskFolderName, boolean create) throws Exception {
        Entity maskSubFolder=null;
        if (!create) {
            Entity maskTopLevelFolder=getSubFolderByNameWithoutSession(sample, MASK_ANNOTATION_FOLDER_NAME);
            if (maskTopLevelFolder!=null) {
                maskSubFolder=getSubFolderByNameWithoutSession(maskTopLevelFolder, maskFolderName);
            }
        } else {
            File maskTopLevelDir=new File(sampleDir, MASK_ANNOTATION_SUBDIR_NAME);
            Entity maskFolder=verifyOrCreateSubFolder(sample, MASK_ANNOTATION_FOLDER_NAME, maskTopLevelDir.getAbsolutePath());
            maskSubFolder=verifyOrCreateSubFolder(maskFolder, maskFolderName, maskFolderName);
        }
        return maskSubFolder;
    }

    Map<String, FileNode> createResultNodeMapForMipConversion(List<String> dirPathList) throws Exception {
        Map<String, FileNode> nodeMap=new HashMap<String, FileNode>();
        for (String dirPath : dirPathList) {
            NamedFileNode mipResultNode=new NamedFileNode(task.getOwner(), task, "mipConversion", "mipConversion", visibility, sessionName);
            mipResultNode=(NamedFileNode)computeBean.saveOrUpdateNode(mipResultNode);
            nodeMap.put(dirPath, mipResultNode);
            if (DEBUG) logger.info("For mip input dir="+dirPath+" sessionName="+sessionName+" created output result dir="+mipResultNode.getDirectoryPath());
        }
        return nodeMap;
    }

    File getOrUpdateSampleResultDir(Entity sample) throws Exception {
        // First, does the sample already have a RESULT_NODE_ID?
        String resultNodeIdString=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID);
        if (resultNodeIdString!=null) {
            return getResultNodeDirFromSample(sample);
        } else {
            throw new Exception("Could not find ATTRIBUTE_RESULT_NODE_ID for sample id="+sample.getId());
        }
    }

    File getResultNodeDirFromSample(Entity sample) throws Exception {
        String resultNodeIdString=sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_RESULT_NODE_ID);
        Long resultNodeId=new Long(resultNodeIdString.trim());
        ScreenSampleResultNode resultNode=(ScreenSampleResultNode)computeBean.getNodeById(resultNodeId);
        File dir=new File(resultNode.getDirectoryPath());
        return dir;
    }

    Entity verifyOrCreateSubFolder(Entity parent, String subFolderName, String subFolderPathName) throws Exception {
        Entity subFolder=getSubFolderByName(parent, subFolderName);
        String parentDirPath=parent.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        File subFolderDir;
        if (parentDirPath==null) {
            subFolderDir=new File(subFolderPathName);
            File testParent=subFolderDir.getParentFile();
            if (!testParent.isDirectory()) {
                throw new Exception("Could not find implied parent dir="+testParent.getAbsolutePath());
            }
        } else {
            subFolderDir=new File(parentDirPath, subFolderPathName);
        }
        if (subFolder==null) {
            subFolder=addChildFolderToEntity(parent, subFolderName, subFolderDir.getAbsolutePath());
            subfolderCreationCount++;
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
        return subFolder;
    }

    protected void cleanFullOrIncompleteMaskAnnotationFolderAndFiles(Entity maskAnnotationFolder) throws Exception {
        if (!maskAnnotationFolder.getOwnerKey().equals(ownerKey)) {
            throw new Exception("Users do not match for cleanFullOrIncompleteMaskAnnotationFolderAndFiles()");
        }
        String maskDirPath=maskAnnotationFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (maskDirPath==null) {
            if (DEBUG) logger.info("Could not find directory path for previous mask folder id="+maskAnnotationFolder.getId());
            return;
        }
        File maskDir=new File(maskDirPath);
        if (DEBUG) logger.info("Removing prior mask annotation folder at location="+maskDir.getAbsolutePath());
        List<File> filesToDelete=new ArrayList<File>();

        // We need to iterate through the entities and delete - note we only want to delete 'official'
        // contents and not other links.
        File mipsSubDir=new File(maskDir, MIPS_SUBFOLDER_NAME);
        File suppSubDir=new File(maskDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        File normSubDir=new File(maskDir, NORMALIZED_SUBFOLDER_NAME);
        List<File> dirsToDelete=new ArrayList<File>();
        dirsToDelete.add(mipsSubDir);
        dirsToDelete.add(suppSubDir);
        dirsToDelete.add(normSubDir);
        dirsToDelete.add(maskDir);
        for (File f : dirsToDelete) {
            File [] fileArr=f.listFiles();
            if (fileArr!=null) {
                for (File sf : fileArr) {
                    filesToDelete.add(sf);
                }
            }
        }

        // Now we know the files to delete, so we can delete the folder entity tree
        if (DEBUG) logger.info("Deleting entity tree for prior mask annotation folderId="+maskAnnotationFolder.getId());
        entityBean.deleteEntityTreeById(ownerKey, maskAnnotationFolder.getId());
        if (DEBUG) logger.info("Finished deleting entity tree");
        // Now we can delete the files and then directories
        for (File fileToDelete : filesToDelete) {
            if (fileToDelete.exists() && !fileToDelete.isDirectory()) {
                if (DEBUG) logger.info("Deleting prior mask annotation file="+fileToDelete.getAbsolutePath());
                fileToDelete.delete();
            }
        }
        for (File fileToDelete : filesToDelete) {
            if (fileToDelete.exists() && fileToDelete.isDirectory()) {
                if (DEBUG) logger.info("Deleting prior mask dir="+fileToDelete.getAbsolutePath());
                fileToDelete.delete();
            }
        }
    }

    protected boolean maskAnnotationDirIsComplete(String sampleName, File maskDir, boolean verbose) throws Exception {
        boolean missingAFile = false;
        if (DEBUG) logger.info("Calling maskAnnotationDirIsComplete() with sampleName=" + sampleName);
        File completeMarkerFile = new File(maskDir, COMPLETE_MARKER_FILENAME);
        if (completeMarkerFile.exists()) {
            if (DEBUG) logger.info("Found complete marker file for sample=" + sampleName + " - assuming complete");
            missingAFile = false;
        } else {
            List<File> filenameList = getExpectedMaskAnnotationResultFiles(maskDir, sampleName);
            for (File file : filenameList) {
                if (!file.exists()) {
                    if (verbose) {
                        if (DEBUG) logger.info("Missing expected mask annotation file=" + file.getAbsolutePath());
                    }
                    missingAFile = true;
                }
            }
            if (!missingAFile) {
                completeMarkerFile.createNewFile();
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
        if (DEBUG) logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, attrName);
        if (DEBUG) logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
                " as child of "+parent.getEntityTypeName()+"#"+parent.getId());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Completion
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void doComplete() throws Exception {
        logger.info("ScreenSampleLineCoordinationService doComplete() start");

        List<String> sampleIdList=(List<String>)processData.getItem("SAMPLE_ID_LIST");
        List<String> sampleNameList=(List<String>)processData.getItem("SAMPLE_NAME_LIST");
        List<String> maskAnnotationPathList=(List<String>)processData.getItem("MASK_ANNOTATION_PATH");
        List<String> alignedStackPathList=(List<String>)processData.getItem(("ALIGNED_STACK_PATH_LIST"));
        MaskAnnotationResultNode resultNode=(MaskAnnotationResultNode)processData.getItem(ProcessDataConstants.RESULT_FILE_NODE);

        int index=0;

        for (String maskAnnotationPath : maskAnnotationPathList) {
            String sampleName=sampleNameList.get(index);
            File maskAnnotationDir=new File(maskAnnotationPath);
            //logger.info("Top of doComplete() loop, index="+index+" sampleName="+sampleName+" patternAnnotationDir="+patternAnnotationPath);
            if (!maskAnnotationDir.exists()) {
                throw new Exception("Could not find expected pattern annotation dir="+maskAnnotationDir.getAbsolutePath());
            }
            File mipDir=new File(maskAnnotationDir, MIPS_SUBFOLDER_NAME);
            cleanFilesFromDirectory(".tif", mipDir);

            // We are going to move responsibility for placing the output files in this subdirectory structure on the
            // V3D layer, for performance reasons.

            //moveFilesToSubDirectory("mip", patternAnnotationDir, new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME));
            //moveFilesToSubDirectory("normalized", patternAnnotationDir, new File(patternAnnotationDir, NORMALIZED_SUBFOLDER_NAME));
            //moveFilesToSubDirectory("quant", patternAnnotationDir, new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME));

            //logger.info("Calling patternAnnotationDirIsComplete with sampleName="+sampleName);
            if (!maskAnnotationDirIsComplete(sampleName, maskAnnotationDir, true /* verbose */)) {
                throw new Exception("Pattern annotation in this dir is incomplete="+maskAnnotationPath);
            } else {
                addMaskAnnotationResultEntitiesToSample(sampleIdList.get(index), sampleName, maskAnnotationDir);
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
            if (DEBUG) logger.info("Moving "+fromFile.getAbsolutePath()+" to "+toFile.getAbsolutePath());
            FileUtil.moveFileUsingSystemCall(fromFile, toFile);
        }
    }

    protected void cleanFilesFromDirectory(String nameFragment, File dir) throws Exception {
        File[] fileArr=dir.listFiles();
        for (File f : fileArr) {
            if (!f.isDirectory() && f.getName().toLowerCase().contains(nameFragment)) {
                if (DEBUG) logger.info("Deleting file "+f.getAbsolutePath());
                f.delete();
            }
        }
    }

    protected Entity getSubFolderByNameWithoutSession(Entity parentEntity, String folderName) throws ComputeException {
        Set<Entity> children = entityBean.getChildEntities(parentEntity.getId());
        for (Entity child : children) {
            if (child != null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) & child.getName().equals(folderName)) {
                return child;
            }
        }
        return null;
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

    String getMaskAnnotationEntityNameFromFilename(String filename) throws Exception {
        String abbrevation=getAbbreviationFromMaskAnnotationFilename(filename);
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
            } else if (filename.contains("compositeMask")) {
                entityName="Mask";
            } else if (filename.contains("compositeMaskMIP")) {
                entityName="Mask";
            }
        }
        return entityName;
    }

    String getAbbreviationFromMaskAnnotationFilename(String filename) throws Exception {
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
        return abbreviation;
    }

    public void addMaskAnnotationResultEntitiesToSample(String sampleId, String sampleName, File maskAnnotationDir) throws Exception {

        Entity screenSample=entityBean.getEntityTree(new Long(sampleId));
        if (screenSample==null) {
            throw new Exception("Could not find screenSample by id="+sampleId);
        }
        List<File> fileList=getExpectedMaskAnnotationResultFiles(maskAnnotationDir, sampleName);

        Entity maskAnnotationTopFolder=getSubFolderByName(screenSample, MASK_ANNOTATION_FOLDER_NAME);
        Entity maskAnnotationFolder=getSubFolderByName(maskAnnotationTopFolder, maskAnnotationFolderName);
        Entity mipsSubFolder=getSubFolderByName(maskAnnotationFolder, MIPS_SUBFOLDER_NAME);
        Entity normalizedSubFolder=getSubFolderByName(maskAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);
        Entity supportingSubFolder=getSubFolderByName(maskAnnotationFolder, SUPPORTING_FILE_SUBFOLDER_NAME);

        // In this next section, we will iterate through each file, determine its proper entity name, and then
        // decide based on its name what folder it belongs in. Then, we will check to see if this folder already
        // contains an entity with this name. If it does, we will then check that this entity points to the
        // correct filename. If it does not, we will correct the filename.

        // We will take two passes - the first for MIPs and next for stacks
        Map<String, Entity> mipMap=new HashMap<String, Entity>();
        for (File file : fileList) {
            if (file.getName().endsWith(".png") && file.getName().toLowerCase().contains("mip")) {
                String entityName=getMaskAnnotationEntityNameFromFilename(file.getName());
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
            if (file.getName().endsWith(".v3dpbd") &&
                    (file.getName().toLowerCase().contains("heatmap") ||
                    (file.getName().toLowerCase().contains("composite")))) {
                String entityName=getMaskAnnotationEntityNameFromFilename(file.getName());
                Entity stackEntity=null;
                Entity stackFolder=null;
                if (file.getName().toLowerCase().contains("normalized")) {
                    stackFolder=normalizedSubFolder;
                } else {
                    stackFolder=maskAnnotationFolder;
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
        File supportingDir=new File(maskAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        if (supportingDir.exists()) {
            File[] supportingFiles=supportingDir.listFiles();
            if (supportingFiles!=null) {
                for (File f : supportingFiles) {
                    Entity supportingEntity=createSupportingEntity(f, f.getName());
                    addToParent(supportingSubFolder, supportingEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }

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
        stack = entityBean.saveOrUpdateEntity(stack);
        entityHelper.setDefault2dImage(stack, mipEntity);
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stack = entityBean.saveOrUpdateEntity(stack);
        if (DEBUG) logger.info("Saved stack " + stack.getName() + " as "+stack.getId());
        return stack;
    }

    public List<String> getExpectedMaskAnnotationResultFilenameList(String sampleName) throws Exception {
        List<String> expectedMaskAnnotationResultFilenameList=new ArrayList<String>();
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
            File abbreviationIndexFile=new File(maskAnnotationResourceDir+File.separator+MASK_NAME_INDEX_FILENAME);
            FileReader fr=new FileReader(abbreviationIndexFile);
            BufferedReader br=new BufferedReader(fr);
            String nextLine=null;
            while ((nextLine=br.readLine())!=null) {
                List<String> nameLineList= MaskAnnotationDataManager.parseMaskNameIndexLine(nextLine);
                if (nameLineList.size()<3) {
                    throw new Exception("Could not parse line from file="+abbreviationIndexFile.getAbsolutePath()+" line="+nextLine);
                }
                if (DEBUG) logger.info("Adding "+nameLineList.get(2)+" to abbreviation list");
                abbrevationList.add(nameLineList.get(2));
            }
            br.close();
        }
        expectedMaskAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[0]);
        expectedMaskAnnotationResultFilenameList.add(sampleName+compartmentSuffixArray[1]);
        // Note: there are not normalized files at the global level
        expectedMaskAnnotationResultFilenameList.add(sampleName+otherSuffixArray[0]);
        //expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[1]);
        //expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[2]);
        for (String abbreviation : abbrevationList) {
            expectedMaskAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[0]);
            expectedMaskAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[1]);
            expectedMaskAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[2]);
            expectedMaskAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[3]);
        }
        return expectedMaskAnnotationResultFilenameList;
    }

    public List<File> getExpectedMaskAnnotationResultFiles(File maskAnnotationDir, String sampleName) throws Exception {
        List<String> filenameList=getExpectedMaskAnnotationResultFilenameList(sampleName);
        File mipSubFolder=new File(maskAnnotationDir, MIPS_SUBFOLDER_NAME);
        File supportingFilesFolder=new File(maskAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        File normalizedSubFolder=new File(maskAnnotationDir, NORMALIZED_SUBFOLDER_NAME);
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
                    File file=new File(maskAnnotationDir, filename);
                    expectedFiles.add(file);
                } else if (tokens[1].equals("compositeMask.v3dpbd")) {
                    File file=new File(maskAnnotationDir, filename);
                    expectedFiles.add(file);
                } else if (tokens[1].equals("compositeMaskMIP.png")) {
                    File file=new File(mipSubFolder, filename);
                    expectedFiles.add(file);
                }
            } else if (tokens.length==3) {
                File file=null;
                if (filename.toLowerCase().contains("mip")) {
                    file=new File(mipSubFolder, filename);
                } else {
                    file=new File(maskAnnotationDir, filename);
                }
                expectedFiles.add(file);
            } else if (tokens.length==4 || tokens.length==5) {
                File file=null;
                if (filename.toLowerCase().contains("mip")) {
                    file=new File(mipSubFolder, filename);
                } else if (filename.toLowerCase().contains("normalized")) {
                    file=new File(normalizedSubFolder, filename);
                } else {
                    file=new File(maskAnnotationDir, filename);
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

