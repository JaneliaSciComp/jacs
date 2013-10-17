package org.janelia.it.jacs.compute.service.annotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.screen.FlyScreenSampleService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.CompartmentAnnotation3DTask;
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
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 9/10/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class CompartmentAnnotation3DService extends AbstractEntityService {

    public static final String FLY_WHOLE_20X_PATTERN_CONFIGURATION = "fly_whole_20x";

    final public String PATTERN_ANNOTATION_SUBDIR_NAME="patternAnnotation";
    final public String PATTERN_ANNOTATION_FOLDER_NAME="Pattern Annotation";
    final public String MIPS_SUBFOLDER_NAME="mips";
    final public String SUPPORTING_FILE_SUBFOLDER_NAME="supportingFiles";
    final public String NORMALIZED_SUBFOLDER_NAME="normalized";
    final public String ABBREVIATION_INDEX_FILENAME="compartmentAbbreviationIndex.txt";


    private static final Logger logger = Logger.getLogger(CompartmentAnnotation3DService.class);
    CompartmentAnnotation3DTask task;
    List<Long> sampleIdList;
    FileDiscoveryHelper helper;
    String visibility;
    String sessionName;
    PatternAnnotationResultNode resultNode;
    String mode;
    Map<String, String> alignmentSpaceToPatternAnnotationConfigurationMap;
    String configurationName;
    protected List<String> abbrevationList=new ArrayList<String>();
    String patternAnnotationResourceDirPath;
    protected Date createDate;


    protected String compartmentAnnotationResourceDir=SystemConfigurationProperties.getString("CompartmentAnnotation.ResourceDir");


    public void execute() throws Exception {
        try {
            logger.info("CompartmentAnnotation3DService execute() start");
            task = (CompartmentAnnotation3DTask)ProcessDataHelper.getTask(processData);
            helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            mode = processData.getString("MODE");

            setupConfigurationMap();

            configurationName = task.getParameter(CompartmentAnnotation3DTask.PARAM_configurationName);
            patternAnnotationResourceDirPath = compartmentAnnotationResourceDir + "/" + configurationName;

            createDate=new Date();

            File patternAnnotationResourceDir=new File(patternAnnotationResourceDirPath);
            if (!patternAnnotationResourceDir.exists()) {
                throw new Exception("Could not find resource dir="+patternAnnotationResourceDir.getAbsolutePath());
            }

            if (mode.equals("SETUP")) {
                doSetup();
            } else if (mode.equals("COMPLETE")) {
                doComplete();
            } else {
                throw new Exception("Do not recognize mode type="+mode);
            }
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    protected void setupConfigurationMap() {
        alignmentSpaceToPatternAnnotationConfigurationMap=new HashMap<String,String>();
        alignmentSpaceToPatternAnnotationConfigurationMap.put(FLY_WHOLE_20X_PATTERN_CONFIGURATION, "Unified 20x Alignment Space");
    }

    protected void doSetup() throws Exception {


        // Create node
        resultNode = new PatternAnnotationResultNode(task.getOwner(), task, "PatternAnnotationResultNode",
                "PatternAnnotationResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);


        // Validate input file
        File inputFile = new File(task.getParameter(CompartmentAnnotation3DTask.PARAM_sampleIdListPath));
        if (inputFile.exists()) {
            logger.info("Found input file=" + inputFile.getAbsolutePath());
        } else {
            throw new Exception("Could not find input file at location=" + inputFile.getAbsolutePath());
        }

        // Read input file
        sampleIdList=new ArrayList<Long>();
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            if (inputLine.trim().length() > 0) {
                Long sampleId=0L;
                try {
                    BigInteger sampleIdB = new BigInteger(inputLine.trim());
                    sampleId=new Long(sampleIdB.toString());
                    Entity sample = entityBean.getEntityById(sampleId);
                    if (sample==null) {
                        logger.info("sample is null");
                    }
                    if (sample!=null && sample.getId()>0) {
                        sampleIdList.add(sampleId);
                        logger.info("Adding sampleId=" + sampleId);
                    } else {
                        logger.info("Could not find sample with id="+sampleId);
                    }
                }
                catch (Exception ex) {
                    logger.info("Could not find sample for Id=" + sampleId + ", skipping");
                };
            }
        }
        reader.close();
        if (!(sampleIdList.size() > 0)) {
            logger.info("No samples to process");
            return;
        }

        List<String> sampleIdStringList = new ArrayList<String>();
        List<String> sampleNameList = new ArrayList<String>();
        List<String> patternAnnotationPathList = new ArrayList<String>();
        List<String> alignedStackPathList = new ArrayList<String>();
        List<String> mipsDirList = new ArrayList<String>();

        String resourceDir = compartmentAnnotationResourceDir + "/" + task.getParameter(CompartmentAnnotation3DTask.PARAM_configurationName);
        String patternChannel = task.getParameter(CompartmentAnnotation3DTask.PARAM_patternChannel);
        String resultNodePath = resultNode.getDirectoryPath();

        for (Long sampleId : sampleIdList) {
            String[] nameAndStackPath = getNameAndAlignedStackPathFromSampleId(sampleId);
            String sampleName=nameAndStackPath[0];
            String stackPath=nameAndStackPath[1];
            logger.info("For sampleId="+sampleId+", name="+sampleName+", stackPath="+stackPath);
            String[] pathComponents = stackPath.split("/");
            String lastPathComponent = pathComponents[pathComponents.length - 1];
            String[] fileComponents = lastPathComponent.split("//.");
            if (fileComponents==null || fileComponents.length==0) {
                throw new Exception("Could not parse fileComponents from lastPathComponent="+lastPathComponent);
            }
            String patternAnnotationPath = resultNodePath + "/" + sampleName;
            sampleIdStringList.add(""+sampleId);
            sampleNameList.add(sampleName);
            patternAnnotationPathList.add(patternAnnotationPath);
            mipsDirList.add(patternAnnotationPath + "/" + MIPS_SUBFOLDER_NAME);
            alignedStackPathList.add(stackPath);
        }

        Map<String, FileNode> mipResultNodeMap=createResultNodeMapForMipConversion(mipsDirList);

        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
        processData.putItem("SAMPLE_ID_LIST", sampleIdStringList);
        processData.putItem("SAMPLE_NAME_LIST", sampleNameList);
        processData.putItem("PATTERN_ANNOTATION_PATH", patternAnnotationPathList);
        processData.putItem("ALIGNED_STACK_PATH_LIST", alignedStackPathList);
        processData.putItem("RESOURCE_DIR_PATH", resourceDir);
        processData.putItem("PATTERN_CHANNEL", patternChannel);
        processData.putItem("MIPS_CONVERSION_PATH", mipsDirList);
        processData.putItem("IMAGE_CONVERSION_RESULT_NODE_MAP", mipResultNodeMap);

        logger.info("CompartmentAnnotation3DService execute() end");
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

    String[] getNameAndAlignedStackPathFromSampleId(Long sampleId) throws Exception {
        String[] result = new String[2];
        String alignmentSpaceName=alignmentSpaceToPatternAnnotationConfigurationMap.get(configurationName);
        if (alignmentSpaceName==null) {
            throw new Exception("Could not find alignmentSpaceName for configuration=" + configurationName);
        }
        logger.info("Getting entity tree for sample " + sampleId);
        Set<Entity> alignedStacksWithMatchingAlignmentSpace=new HashSet<Entity>();
        Entity sample=entityBean.getEntityTree(sampleId);
        result[0]=sample.getName();
        Set<Entity> pipelineRuns=findMatchingChildEntityType(sample, EntityConstants.TYPE_PIPELINE_RUN);
        for (Entity run : pipelineRuns) {
            Set<Entity> alignmentResults=findMatchingChildEntityType(run, EntityConstants.TYPE_ALIGNMENT_RESULT);
            for (Entity alignmentResult : alignmentResults) {
                Set<Entity> supportingFolders=findMatchingChildEntityType(alignmentResult, EntityConstants.TYPE_SUPPORTING_DATA);
                for (Entity supportingFolder : supportingFolders) {
                    Set<Entity> alignedStacks=findMatchingChildEntityType(supportingFolder, EntityConstants.TYPE_IMAGE_3D);
                    for (Entity alignedStack : alignedStacks) {
                        if (alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE).equals(alignmentSpaceName)) {
                            alignedStacksWithMatchingAlignmentSpace.add(alignedStack);
                        }
                    }
                }
            }
        }
        if (alignedStacksWithMatchingAlignmentSpace.size()==0) {
            throw new Exception("Could not find any matching aligned stacks");
        } else if (alignedStacksWithMatchingAlignmentSpace.size()>1) {
            throw new Exception("More than one matching aligned stack - ambiguous situation");
        } else {
            Entity alignedStack=alignedStacksWithMatchingAlignmentSpace.iterator().next();
            String stackPath=alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            logger.info("Found stack path="+stackPath);
            result[1] = stackPath;
        }
        return result;
    }

    Set<Entity> findMatchingChildEntityType(Entity parent, String matchString) {
        Set<Entity> returnSet=new HashSet<Entity>();
        Set<Entity> children=parent.getChildren();
        for (Entity child : children) {
            if (child.getEntityType().getName().equals(matchString)) {
                logger.info("Found entity matching type="+matchString);
                returnSet.add(child);
            }
        }
        return returnSet;
    }

    public void doComplete() throws Exception {
        logger.info("CompartmentAnnotation3DService doComplete() start");

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
            File mipDir=new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME);
            cleanFilesFromDirectory(".tif", mipDir);

            if (!patternAnnotationDirIsComplete(sampleName, patternAnnotationDir, true /* verbose */)) {
                throw new Exception("Pattern annotation in this dir is incomplete="+patternAnnotationPath);
            } else {
                addPatternAnnotationResultEntitiesToSample(sampleIdList.get(index), sampleName, patternAnnotationDir);
            }
            index++;
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

    public List<File> getExpectedPatternAnnotationResultFiles(File patternAnnotationDir, String sampleName) throws Exception {
        List<String> filenameList=getExpectedPatternAnnotationResultFilenameList(sampleName);
        File mipSubFolder=new File(patternAnnotationDir, MIPS_SUBFOLDER_NAME);
        File supportingFilesFolder=new File(patternAnnotationDir, SUPPORTING_FILE_SUBFOLDER_NAME);
        File normalizedSubFolder=new File(patternAnnotationDir, NORMALIZED_SUBFOLDER_NAME);
        List<File> expectedFiles=new ArrayList<File>();
        for (String filename : filenameList) {
            if (filename.contains("MIP.")) {
                expectedFiles.add(new File(mipSubFolder, filename));
            } else if (filename.toLowerCase().contains("_normalized_")) {
                expectedFiles.add(new File(normalizedSubFolder, filename));
            } else if (filename.contains("indexCubified.v3dpbd") ||
                       filename.contains("inputImageCubified.v3dpbd") ||
                       filename.contains("quantifiers.txt")) {
                expectedFiles.add(new File(supportingFilesFolder, filename));
            } else {
                expectedFiles.add(new File(patternAnnotationDir, filename));
            }
        }
        return expectedFiles;
    }

    public List<String> getExpectedPatternAnnotationResultFilenameList(String sampleName) throws Exception {
        List<String> expectedPatternAnnotationResultFilenameList=new ArrayList<String>();
        String[] compartmentSuffixArray=new String[4];
        compartmentSuffixArray[0]="_heatmap16Color.v3dpbd";
        compartmentSuffixArray[1]="_heatmap16ColorMIP.png";
        compartmentSuffixArray[2]="_normalized_heatmap16Color.v3dpbd";
        compartmentSuffixArray[3]="_normalized_heatmap16ColorMIP.png";
        String[] otherSuffixArray=new String[1];
        otherSuffixArray[0]="_quantifiers.txt";
        if (abbrevationList.size()==0) {
            File abbreviationIndexFile=new File(patternAnnotationResourceDirPath+File.separator+ABBREVIATION_INDEX_FILENAME);
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
        expectedPatternAnnotationResultFilenameList.add(sampleName+otherSuffixArray[0]);
        for (String abbreviation : abbrevationList) {
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[0]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[1]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[2]);
            expectedPatternAnnotationResultFilenameList.add(sampleName+"_"+abbreviation+compartmentSuffixArray[3]);
        }
        return expectedPatternAnnotationResultFilenameList;
    }

    public void addPatternAnnotationResultEntitiesToSample(String sampleId, String sampleName, File patternAnnotationDir) throws Exception {

        Entity screenSample=entityBean.getEntityTree(new Long(sampleId));
        if (screenSample==null) {
            throw new Exception("Could not find screenSample by id="+sampleId);
        } else {
            logger.info("Adding Pattern Annotation Results to sample Id="+sampleId);
        }

        List<File> fileList=getExpectedPatternAnnotationResultFiles(patternAnnotationDir, sampleName);

        Entity patternAnnotationFolder=getSubFolderByName(screenSample, PATTERN_ANNOTATION_FOLDER_NAME);

        if (patternAnnotationFolder == null) {
            patternAnnotationFolder = addChildFolderToEntity(screenSample, PATTERN_ANNOTATION_FOLDER_NAME, patternAnnotationDir.getAbsolutePath());
        }

        if (patternAnnotationFolder==null) {
            throw new Exception("patternAnnotationFolder is null");
        }

        verifyOrCreateSubFolder(patternAnnotationFolder, MIPS_SUBFOLDER_NAME);
        verifyOrCreateSubFolder(patternAnnotationFolder, SUPPORTING_FILE_SUBFOLDER_NAME);
        verifyOrCreateSubFolder(patternAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);

        Entity mipsSubFolder=getSubFolderByName(patternAnnotationFolder, MIPS_SUBFOLDER_NAME);
        Entity normalizedSubFolder=getSubFolderByName(patternAnnotationFolder, NORMALIZED_SUBFOLDER_NAME);
        Entity supportingSubFolder=getSubFolderByName(patternAnnotationFolder, SUPPORTING_FILE_SUBFOLDER_NAME);

        if (mipsSubFolder==null) {
            throw new Exception("mipsSubFolder is null");
        }

        if (normalizedSubFolder==null) {
            throw new Exception("normalizedSubFolder is null");
        }

        if (supportingSubFolder==null) {
            throw new Exception("supportingSubFolder is null");
        }

        // In this next section, we will iterate through each file, determine its proper entity name, and then
        // decide based on its name what folder it belongs in. Then, we will check to see if this folder already
        // contains an entity with this name. If it does, we will then check that this entity points to the
        // correct filename. If it does not, we will correct the filename.

        // We will take two passes - the first for MIPs and next for stacks
        Map<String, Entity> mipMap=new HashMap<String, Entity>();
        for (File file : fileList) {
            logger.info("Checking file: " + file.getName() + " for mip");
            if (file.getName().endsWith(".png") && file.getName().toLowerCase().contains("mip")) {
                String entityName=getPatternAnnotationEntityNameFromFilename(file.getName());
                logger.info("For mip, using entityName=" + entityName);
                Entity mipEntity=getEntityFromParentByName(mipsSubFolder, entityName);
                if (mipEntity==null) {
                    logger.info("Creating mip entity and adding to parent mipsSubFolder id="+mipsSubFolder.getId());
                    mipEntity=createMipEntity(file, entityName);
                    addToParent(mipsSubFolder, mipEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
                mipMap.put(entityName, mipEntity);
            } else {
                logger.info("Not a mip");
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
        screenSample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, mipPath);
        entityBean.saveOrUpdateEntity(screenSample);
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
        String[] ucomponents=filename.split("_");
        int ulen=ucomponents.length;
        String abbreviation="";
        int startPosition=ulen-2;
        if (filename.contains("_normalized_")) {
            startPosition--;
        }
        int i=startPosition;
        while(i>(startPosition-2)) {
            if ( (ucomponents[i].equals("L") || ucomponents[i].equals("R")) &&
                    probablyCompartmentAbbreviation(ucomponents[i-1])) {
                abbreviation=ucomponents[i-1]+"_"+ucomponents[i];
                return abbreviation;
            } else if (probablyCompartmentAbbreviation(ucomponents[i])) {
                return ucomponents[i];
            }
            i--;
        }
        return null;
    }

    protected boolean probablyCompartmentAbbreviation(String s) {
        if (s.toUpperCase().equals(s) && s.length()<5) {
            return true;
        }
        return false;
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

    protected Entity createMipEntity(File pngFile, String name) throws Exception {
        Entity mipEntity = new Entity();
        mipEntity.setOwnerKey(ownerKey);
        mipEntity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D));
        mipEntity.setCreationDate(createDate);
        mipEntity.setUpdatedDate(createDate);
        mipEntity.setName(name);
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, pngFile.getAbsolutePath());
        mipEntity = entityBean.saveOrUpdateEntity(mipEntity);
        return mipEntity;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
                " as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }

    protected Entity createStackEntity(File file, String entityName, Entity mipEntity) throws Exception {
        Entity stack = new Entity();
        String mipFilePath=mipEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        stack.setOwnerKey(ownerKey);
        stack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK));
        stack.setCreationDate(createDate);
        stack.setUpdatedDate(createDate);
        stack.setName(entityName);
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, mipFilePath);
        stack = entityBean.saveOrUpdateEntity(stack);
        logger.info("Saved stack " + stack.getName() + " as "+stack.getId());
        return stack;
    }

    protected Entity createSupportingEntity(File supportingFile, String name) throws Exception {
        Entity supportingEntity = new Entity();
        supportingEntity.setOwnerKey(ownerKey);
        supportingEntity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_TEXT_FILE));
        supportingEntity.setCreationDate(createDate);
        supportingEntity.setUpdatedDate(createDate);
        supportingEntity.setName(name);
        supportingEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, supportingFile.getAbsolutePath());
        supportingEntity = entityBean.saveOrUpdateEntity(supportingEntity);
        return supportingEntity;
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

    protected Entity addChildFolderToEntity(Entity parent, String name, String directoryPath) throws Exception {
        Entity folder = new Entity();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setOwnerKey(ownerKey);
        folder.setName(name);
        folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        if (directoryPath!=null) {
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, directoryPath);
        }
        folder = entityBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }


}
