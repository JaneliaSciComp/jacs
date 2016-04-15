package org.janelia.it.jacs.compute.service.domain;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.util.DomainHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.FileTreeLoaderResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 2/17/12
 * Time: 9:41 AM
 */
public class FileTreeLoaderService implements IService {

    // This service will operate as a general loader for user data.
    //
    // It will construct a folder/entity tree corresponding to the users' file tree.
    //
    // The path attributes of the entities will be set such that the 'Reveal in Finder' action brings the
    // user to the location of the data on the filesystem.
    //
    // The existence of user data in 'entity space' permits the user to annotate and also use the
    // data for other computational purposes in the system, but the most basic value-add for the Workstation
    // is for viewing large amounts of data easily.
    //
    // To support the utility of the entity-space, each file will be given a type attribute according to
    // its file extension.
    //
    // To support viewing capability, artifacts will be generated on files which have 2D and 3D images.
    // These artifacts will be 2D MIPs and 3D pbd files.
    //
    /*

        The mechanics of the pipeline will work as follows.

        1. There will be an initial recursive pass through the filesystem directory tree.

        2. Each file and directory will result in the creation of an entity (folder for directories and
            file-specific for files). We will also create a 2nd-level folder called "supportingFiles" and use this
            as the parent for all image artifacts.

        3. File types which are image stacks will be registered for PBD creation and attributes by creating a list
             of an object which contains { entityId, sourceImagePath, artifactImagePath }. In the initial pass,
             the 'artifactImagePath' will not be populated until the result nodes are created, permitting the
             artifactImagePath to be known.

        4. File types which are not image stacks, but are themselves 2D images, we will create PNG artifacts for.
            We will therefore create a list of MIP creation targets using the same object { entityId, sourceImagePath,
            artifactImagePath }. We will also postpone creating the 'artifactImagePath' until node creation.

        5. After PBD creation, the PBD files themselves will need MIP artifact creation. To support this, the MIP
            mode will add to its list a set of MIPs for the just-processed PBD files. These MIPs must be connected
            back to the original pre-PBD stacks as their default 2D images.

        6. In the completion step, the MIP and PBD artifacts have been created, and we now have the lists of objects
            with { entityId, sourceImagePath, artifactImagePath }. These things must be done:

            1. We need to create new Entities for the artifacts, and use the 2nd-level 'supportingFiles' folder as the parent.
            2. We need to populate the 'DEFAULT_2D_IMAGE' fields of the source files for the MIPs
            3. We need to populate the 'V3D_PROXY_PATH' fields for the stacks with the PBD paths

        Data-Management:

            Because the inner loop is where the final paths for the various artifacts is created, we have a problem
            in that this state information needs to be re-centralized - whereas we only have a copy of the
            processData hash provided by the compute engine.

            We will work around this by maintaining a static hash of the PBD and MIP groups state, in which
            the key is <taskId><index>.

    */
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    final public String MODE_SETUP="SETUP";
    final public String MODE_COMPLETE="COMPLETE";
    final public String MODE_PBDLIST="PBDLIST";
    final public String MODE_MIPLIST="MIPLIST";
    final public String MODE_MIPSYNC="MIPSYNC";

    final public String PBD_SIZE_THRESHOLD="PBD_SIZE_THRESHOLD";
    final public String PBD_EXTENSIONS="PBD_EXTENSIONS";
    final public String MIP_EXTENSIONS="MIP_EXTENSIONS";

    protected Logger logger;
    protected DomainDAO domainDAO;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected DomainHelper domainHelper;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;

    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected String topLevelFolderName;
    protected Long topLevelFolderId;
    protected String rootDirectoryPath;
    protected boolean filesUploaded;
    protected TreeNode topLevelFolder;
    protected TreeNode parentFolder;
//    protected Entity supportingFilesFolder;
    protected File rootDirectory;
    protected FileTreeLoaderResultNode resultNode;
    protected String mode;
    protected int groupSize;
    protected Map<Long, List<ArtifactInfo>> pbdGroupMap;
    protected Map<Long, List<ArtifactInfo>> mipGroupMap;
    protected List<String> pbdExtensions;
    protected List<String> mipExtensions;
    protected Long pbdThreshold;
    protected List<String> nodeFilePaths;

    protected static class ArtifactInfo {
        public Long sourceDomainId;
        public String sourcePath;
        public String artifactPath;
    }

    protected static Map<Long, Map<Long, List<ArtifactInfo>>> pbdGroupByTaskMap;
    protected static Map<Long, Map<Long, List<ArtifactInfo>>> mipGroupByTaskMap;

    static {
        pbdGroupByTaskMap=new HashMap<Long, Map<Long, List<ArtifactInfo>>>();
        mipGroupByTaskMap=new HashMap<Long, Map<Long, List<ArtifactInfo>>>();
    }


   @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.domainDAO = DomainDAOManager.getInstance().getDao();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.processData=processData;
            this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task= ProcessDataHelper.getTask(processData);
            this.sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            this.visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = domainDAO.getSubjectByName(ownerName);
            this.ownerKey = subject.getKey();
            
            this.domainHelper = new DomainHelper(computeBean, ownerKey, logger);
            
            getNodeDirs();
            
            /*helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
            helper.addFileExclusion("DrmaaSubmitter.log");
            helper.addFileExclusion("*.oos");
            helper.addFileExclusion("sge_*");
            helper.addFileExclusion("temp");
            helper.addFileExclusion("core.*");
*/
            topLevelFolderName=processData.getString("TOP_LEVEL_FOLDER_NAME");
            String topLevelFolderIdStr = processData.getString("PARENT_FOLDER_ID");
            if (topLevelFolderIdStr==null) {
                throw new ServiceException(
                        "failed to determine the folder where the imported files will be created");
            } else {
                try {
                    topLevelFolderId = Long.parseLong(topLevelFolderIdStr);
                } catch (NumberFormatException e) {
                    throw new ServiceException(
                            "failed to parse PARENT_FOLDER_ID '" + topLevelFolderIdStr + "'", e);
                }
                parentFolder = (TreeNode)domainDAO.getDomainObject(ownerKey,
                        Reference.createFor(TreeNode.class, topLevelFolderId));
                for (Reference parentChild: parentFolder.getChildren()) {
                    DomainObject parentChildObj = domainDAO.getDomainObject(ownerKey, parentChild);
                    if (parentChildObj.getName().equals(topLevelFolderName) && parentChildObj instanceof TreeNode) {
                        topLevelFolder = (TreeNode)parentChildObj;
                        logger.info("Loading top-level folder with id " + topLevelFolder.getId());
                        break;
                    }
                }
                if (topLevelFolder == null) {
                    logger.info("Creating top-level folder");
                    TreeNode node = new TreeNode();
                    node.setName(topLevelFolderName);
                    topLevelFolder = domainDAO.save(ownerKey, node);
                    List<Reference> refList = new ArrayList<>();
                    refList.add(Reference.createFor(TreeNode.class, topLevelFolder.getId()));
                    domainDAO.addChildren(ownerKey,parentFolder,refList);
                }
            }

            logger.info("Validating root directorypath");
            rootDirectoryPath=processData.getString("FILE_TREE_ROOT_DIRECTORY");
            rootDirectory=new File(rootDirectoryPath);
            if (!rootDirectory.exists()) {
                throw new Exception("Could not find rootDirectory="+rootDirectory.getAbsolutePath());
            }

            filesUploaded = Boolean.parseBoolean(
                    String.valueOf(processData.getItem("FILES_UPLOADED")));

            logger.info("Getting GROUP_SIZE");
            groupSize = new Integer(processData.getString("GROUP_SIZE").trim());

            logger.info("Getting MODE");
            mode = processData.getString("MODE");

            logger.info("Establishing GroupMaps");
            pbdGroupMap=getPbdGroupMap(task, false /*remove*/);
            mipGroupMap=getMipGroupMap(task, false /*remove*/);

            if (mode.equals(MODE_SETUP)) {
                doSetup();
            } else if (mode.equals(MODE_PBDLIST)) {
                doPbdList();
            } else if (mode.equals(MODE_MIPSYNC)) {
                doMipSync();
            } else if (mode.equals(MODE_MIPLIST)) {
                doMipList();
            } else if (mode.equals(MODE_COMPLETE)) {
                doComplete();
            } else {
                throw new Exception("Do not recognize mode="+mode);
            }

        } catch (ServiceException se) {
            logger.error("Error in FileTreeLoader execute() : " + se.getMessage(), se);
            throw se;
        } catch (Exception e) {
            logger.error("Error in FileTreeLoader execute() : " + e.getMessage(), e);
            throw new ServiceException(e);
        }
    }
   
    protected static synchronized Map<Long, List<ArtifactInfo>> getPbdGroupMap(Task task, boolean remove) {
        Long taskId=task.getObjectId();
        if (taskId==null) {
            return null;
        }
        if (remove) {
            pbdGroupByTaskMap.remove(taskId);
            return null;
        }
        Map<Long, List<ArtifactInfo>> pbdGroupMap=pbdGroupByTaskMap.get(taskId);
        if (pbdGroupMap==null) {
            pbdGroupMap=new HashMap<Long, List<ArtifactInfo>>();
            pbdGroupByTaskMap.put(taskId, pbdGroupMap);
        }
        return pbdGroupMap;
    }

    protected static synchronized Map<Long, List<ArtifactInfo>> getMipGroupMap(Task task, boolean remove) {
        Long taskId=task.getObjectId();
        if (taskId==null) {
            return null;
        }
        if (remove) {
            mipGroupByTaskMap.remove(taskId);
            return null;
        }
        Map<Long, List<ArtifactInfo>> mipGroupMap=mipGroupByTaskMap.get(taskId);
        if (mipGroupMap==null) {
            mipGroupMap=new HashMap<Long, List<ArtifactInfo>>();
            mipGroupByTaskMap.put(taskId, mipGroupMap);
        }
        return mipGroupMap;
    }
    
    private void getNodeDirs() {
        this.nodeFilePaths = (List<String>)processData.getItem("NODE_FILE_PATHS");
        if (nodeFilePaths==null) {
            nodeFilePaths = new ArrayList<String>();
            processData.putItem("NODE_FILE_PATHS", nodeFilePaths);    
        }
    }
    
    private void addNodeDir(String nodeFilePath) {
        nodeFilePaths.add(0, nodeFilePath);
    }

    protected void doSetup() throws Exception {
        logger.info("Starting doSetup()");

        // Initialize vars for this section
        String pbdExtensionString=processData.getString(PBD_EXTENSIONS);
        if (pbdExtensionString!=null) {
            String[] extensions=pbdExtensionString.split("\\s+");
            pbdExtensions=Arrays.asList(extensions);
        }
        String mipExtensionString=processData.getString(MIP_EXTENSIONS);
        if (mipExtensionString!=null) {
            String[] extensions=mipExtensionString.split("\\s+");
            mipExtensions=Arrays.asList(extensions);
        }
        String pbdThresholdString=processData.getString(PBD_SIZE_THRESHOLD);
        if (pbdThresholdString!=null) {
            pbdThreshold=new Long(pbdThresholdString.trim());
        }

        // First, set up result node
        resultNode = new FileTreeLoaderResultNode(task.getOwner(), task, "FileTreeLoaderResultNode",
                "FileTreeLoaderResultNode for task " + task.getObjectId(), visibility, sessionName);

        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);
        addNodeDir(resultNode.getDirectoryPath());
        
        logger.info("FileTreeLoaderService  doSetup()  resultNodeId=" + resultNode.getObjectId() + " intended path=" + resultNode.getDirectoryPath());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultNode.getDirectoryPath());
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_DIR, resultNode.getDirectoryPath());

        // Next, we will recursively traverse the file tree, and as we do so, we will create entities for
        // directories and files. We will also create lists of files for PBD and MIP generation.
        // In the 'COMPLETION' phase we will create entities for the MIPs and PBDs, and set the attributes
        // of the source entities to match these results.

        if (filesUploaded) {
            moveUploadedFilesIntoFileStore();
        } else {
            addDirectoryAndContentsToFolder(topLevelFolder, rootDirectory, 0 /*index*/);
        }

        processData.putItem("GROUP_SIZE", Long.toString(groupSize));
        List<Long> pbdKeyList=new ArrayList<Long>(pbdGroupMap.keySet());
        Collections.sort(pbdKeyList);
        logger.info("pbdKeyList has "+pbdKeyList.size()+" entries");
        processData.putItem("PBD_INDEX", pbdKeyList);
        List<Long> mipKeyList=new ArrayList<Long>(mipGroupMap.keySet());
        Collections.sort(mipKeyList);
        logger.info("mipKeyList has "+mipKeyList.size()+" entries");
        processData.putItem("MIP_INDEX", mipKeyList);

        logger.info("Finishing doSetup()");
    }

    protected void addDirectoryAndContentsToFolder(TreeNode folder, File dir, Integer index) throws Exception {
        logger.info("addDirectoryAndContentsToFolder: entry, folder=" +
                    folder + ", dir="+ dir.getAbsolutePath());
        TreeNode dirFolder=verifyOrCreateChildFolderFromDir(folder, dir, index);
        List<File> orderedFiles=FileUtils.getOrderedFilesInDir(dir);
        for (int i=0;i<orderedFiles.size();i++) {
            File f=orderedFiles.get(i);
            if (passesExclusionFilter(f)) {
                if (f.isDirectory()) {
                    addDirectoryAndContentsToFolder(dirFolder, f, i);
                } else {
                    verifyOrCreateFileEntityForFolder(dirFolder, f, i);
                    dirFolder = (TreeNode)domainDAO.getDomainObject(ownerKey,
                            Reference.createFor(TreeNode.class, dirFolder.getId()));
                }
            }
        }
    }

    protected TreeNode verifyOrCreateChildFolderFromDir(TreeNode parentFolder, File dir, Integer index) throws Exception {
        logger.info("Looking for folder with path "+dir.getAbsolutePath()+" in parent folder "+parentFolder.getId());
        if (parentFolder.getChildren()!=null) {
            for (Reference child: parentFolder.getChildren()) {
                DomainObject childObj = domainDAO.getDomainObject(ownerKey, child);
                if (childObj.getName()!=null && childObj.getName().equals(dir.getName())) {
                    return (TreeNode)childObj;
                }
            }
        }
        TreeNode folder = domainHelper.createChildFolder(parentFolder,ownerKey,dir.getName(), index);
        logger.info("Saved folder as "+folder.getId());
        return folder;
    }

    protected boolean passesExclusionFilter(File f) {
        return !f.getName().startsWith(".");
    }

    protected void verifyOrCreateFileEntityForFolder(TreeNode folder, File f, Integer index) throws Exception {
        // if no object exists in this treenode, create an objectset
        ObjectSet objectSet = null;
        logger.info("ASDASDFAFDASFASDFASDFA" + folder.getChildren());
        if (folder.getChildren()!=null) {
            for (Reference item: folder.getChildren()) {
                if (item.getTargetClassName().equals(DomainHelper.OBJECTSET_CLASSNAME)) {
                    objectSet = (ObjectSet) domainDAO.getDomainObject(ownerKey, item);
                    break;
                }
            }
        }
        if (objectSet==null) {
            objectSet = domainHelper.createChildObjectSet(folder,ownerKey,"Image Set");
        }

        // create an image object, if it doesn't already exist
        // add the image object
        Image imageFile = new Image();
        imageFile.setName(f.getName());
        boolean alreadyExists=false;

        if (objectSet.getMembers()!=null) {
            for (Long imageId: objectSet.getMembers()) {
                Reference imageRef = Reference.createFor(Image.class, imageId);

                Image imageObj = (Image)domainDAO.getDomainObject(ownerKey,imageRef);
                if (imageObj.getFilepath().equals(f.getAbsolutePath())) {
                    alreadyExists=true;
                    imageFile = imageObj;
                }
            }
        }

        if (!alreadyExists) {
            imageFile.setFilepath(f.getAbsolutePath());
            imageFile = domainDAO.save(ownerKey, imageFile);
            Reference imageRef = Reference.createFor(Image.class, imageFile.getId());
            List<Reference> imageRefList = new ArrayList<>();
            imageRefList.add(imageRef);
            domainDAO.addMembers(ownerKey, objectSet, imageRefList);
        }

        // Handle artifacts
        boolean willHaveMipArtifactBecauseHasPbdArtifact=false;
        if (imageFile.getFiles()==null) {
            imageFile.setFiles(new HashMap<FileType, String>());
        }
        if (shouldHaveArtifact(f, pbdExtensions) && shouldTifHavePbdArtifact(f)) {
            willHaveMipArtifactBecauseHasPbdArtifact=true;
            if (!imageFile.getFiles().containsKey(FileType.LosslessStack)) {
                // We need to create one
                addToArtifactList(imageFile, null /*altSourcePath*/, pbdGroupMap);
            }
        }

        if (!willHaveMipArtifactBecauseHasPbdArtifact && shouldHaveArtifact(f, mipExtensions)) {
            if (!imageFile.getFiles().containsKey(FileType.SignalMip)) {
                // We need to create one
                addToArtifactList(imageFile, null /*altSourcePath*/, mipGroupMap);
            }
        }

    }

    protected boolean shouldTifHavePbdArtifact(File f) {
        return !f.getName().toLowerCase().endsWith(".tif") || f.length() >= pbdThreshold;
    }

    protected void addToArtifactList(Image pbdSource, String altSourcePath, Map<Long, List<ArtifactInfo>> groupMap) {
        long currentIndex=groupMap.size()-1;
        List<ArtifactInfo> artifactList;
        if (currentIndex<0) {
            currentIndex=0L;
            artifactList=new ArrayList<ArtifactInfo>();
            groupMap.put(currentIndex, artifactList);
        } else {
            artifactList=groupMap.get(currentIndex);
        }
        if (artifactList.size()>=groupSize) {
            // Then we need to start a new list
            artifactList=new ArrayList<ArtifactInfo>();
            currentIndex++;
            groupMap.put(currentIndex, artifactList);
        }
        ArtifactInfo newInfo=new ArtifactInfo();
        newInfo.sourceDomainId =pbdSource.getId();
        if (altSourcePath!=null) {
            newInfo.sourcePath=altSourcePath;
        } else {
            newInfo.sourcePath=pbdSource.getFilepath();
        }
        // leave artifactPath null for now
        artifactList.add(newInfo);
    }

    protected boolean shouldHaveArtifact(File file, Collection<String> collection) {
        String[] exComponents=file.getName().split("\\.");
        if (exComponents.length<2) {
            return false;
        }
        String extension=exComponents[exComponents.length-1];
        return collection.contains(extension.toUpperCase());
    }

    protected void doPbdList() throws Exception {
        logger.info("Starting doPbdList()");

        Long groupIndex=processData.getLong("PBD_INDEX");
        logger.info("PBD_INDEX="+groupIndex);
        List<ArtifactInfo> artifactList=pbdGroupMap.get(groupIndex);

        FileTreeLoaderResultNode resultNode=new FileTreeLoaderResultNode(ownerKey, task, "FileTreeLoaderResultNode",
                "FileTreeLoaderResultNode for "+rootDirectoryPath+" pbd group index="+groupIndex, visibility, sessionName);
        resultNode=(FileTreeLoaderResultNode)computeBean.saveOrUpdateNode(resultNode);
        addNodeDir(resultNode.getDirectoryPath());

        logger.info("Created resultNode id=" + resultNode.getObjectId() + " PBD groupIndex=" +
                    groupIndex + " listSize=" + artifactList.size());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        List<String> inputPathList=new ArrayList<String>();
        List<String> outputPathList=new ArrayList<String>();
        for (ArtifactInfo ai : artifactList) {
            inputPathList.add(ai.sourcePath);
            String outputPath=resultNode.getDirectoryPath()+File.separator+"pbdArtifact_"+ai.sourceDomainId +".v3dpbd";
            ai.artifactPath=outputPath;
            outputPathList.add(outputPath);
            // Add mip entry
            Image source = (Image)domainDAO.getDomainObject(ownerKey, Reference.createFor(Image.class, ai.sourceDomainId));
            addToArtifactList(source, outputPath, mipGroupMap);
        }
        logger.info("doPbdList() putting vars in processData, inputListSize="+inputPathList.size()+" outputListSize="+outputPathList.size());
        processData.putItem("PBD_RESULT_NODE", resultNode);
        processData.putItem("PBD_INPUT_LIST", inputPathList);
        processData.putItem("PBD_OUTPUT_LIST", outputPathList);

        logger.info("Finished doPbdList()");
    }

    protected void doMipSync() throws Exception {
        logger.info("doMipSync() start");
        List<Long> mipKeyList=new ArrayList<Long>(mipGroupMap.keySet());
        Collections.sort(mipKeyList);
        logger.info("mipKeyList has "+mipKeyList.size()+" entries");
        processData.putItem("MIP_INDEX", mipKeyList);
        logger.info("doMipSync() end");
    }

    protected void doMipList() throws Exception {
        logger.info("Starting doMipList()");

        Long groupIndex=processData.getLong("MIP_INDEX");
        logger.info("MIP_INDEX="+groupIndex);
        List<ArtifactInfo> artifactList=mipGroupMap.get(groupIndex);
        
        FileTreeLoaderResultNode resultNode=new FileTreeLoaderResultNode(ownerKey, task, "FileTreeLoaderResultNode",
                "FileTreeLoaderResultNode for "+rootDirectoryPath+" mip group index="+groupIndex, visibility, sessionName);
        resultNode=(FileTreeLoaderResultNode)computeBean.saveOrUpdateNode(resultNode);
        addNodeDir(resultNode.getDirectoryPath());
        
        logger.info("Created resultNode id="+resultNode.getObjectId()+" MIP groupIndex="+groupIndex+" listSize="+artifactList.size());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        List<String> inputPathList=new ArrayList<String>();
        List<String> outputPathList=new ArrayList<String>();
        for (ArtifactInfo ai : artifactList) {
            inputPathList.add(ai.sourcePath);
            String outputPath=resultNode.getDirectoryPath()+File.separator+"mipArtifact_"+ai.sourceDomainId +".tif";
            ai.artifactPath=resultNode.getDirectoryPath()+File.separator+"mipArtifact_"+ai.sourceDomainId +".png";
            outputPathList.add(outputPath);
        }
        logger.info("doMipList() putting vars in processData, inputListSize="+inputPathList.size()+" outputListSize="+outputPathList.size());
        processData.putItem("MIP_RESULT_NODE", resultNode);
        processData.putItem("MIP_INPUT_LIST", inputPathList);
        processData.putItem("MIP_OUTPUT_LIST", outputPathList);

        logger.info("Finished doMipList()");
    }

    /*
        doComplete()

        Executes the following steps:

        1. Iterates through each group of PBD results
        2. Creates a new DomainObject for the PBD result
       // 3. Adds this object as a ATTRIBUTE_PERFORMANCE_PROXY_IMAGE attribute for the original source Entity
        4. Builds a map of <sourceDomainId> => <PBD result DomainId>
        5. Then, iterates through each group of the MIP results
        6. Creates a new Entity for the MIP result
        7. Adds the MIP entity as the DEFAULT_2D_IMAGE for the matching PBD, if there is one
        8. Adds the MIP entity as the DEFAULT_2D_IMAGE for the original source Entity

     */
    protected void doComplete() throws Exception {
        logger.info("Starting doComplete()");

        // Handle PBDs
        List<Long> pbdGroupKeyList=new ArrayList<Long>(pbdGroupMap.keySet());
        Collections.sort(pbdGroupKeyList);
        logger.info("doComplete() pbdGroupKeyList has "+pbdGroupKeyList.size()+" entries");
        for (Long pbdGroupKey : pbdGroupKeyList) {
            List<ArtifactInfo> artifactList=pbdGroupMap.get(pbdGroupKey);
            logger.info("doComplete() artifactList has "+artifactList.size()+" entries");
            for (ArtifactInfo ai : artifactList) {
                String pbdResultPath=ai.artifactPath;
                if (pbdResultPath==null) {
                    logger.error("pbdResultPath for sourceDomainId="+ai.sourceDomainId +" is null");
                } else {
                    File pbdResultFile=new File(pbdResultPath);
                    logger.info("doComplete() using pbdResultFile="+pbdResultFile.getAbsolutePath());
                    if (!pbdResultFile.exists()) {
                        logger.error("Could not find expected pbd result file="+pbdResultFile.getAbsolutePath()+" for sourceDomainId="+ai.sourceDomainId);
                    } else {
                        // First, get the source Image DomainObject and attach pbd results
                        logger.info("doComplete() creating pbdResult for source " + ai.sourceDomainId);
                        Image sourceImage = (Image)domainDAO.getDomainObject(ownerKey, Reference.createFor(Image.class, ai.sourceDomainId));
                        if (sourceImage!=null) {
                            if (sourceImage.getFiles()==null) {
                                sourceImage.setFiles(new HashMap<FileType, String>());
                            }
                            sourceImage.getFiles().put(FileType.LosslessStack, pbdResultPath);
                        }
                        domainDAO.save(ownerKey, sourceImage);
                    }
                }
            }
        }
        logger.info("doComplete() finished PBD section, beginning MIP section");

        // Handle MIPs
        List<Long> mipGroupKeyList=new ArrayList<Long>(mipGroupMap.keySet());
        Collections.sort(mipGroupKeyList);
        for (Long mipGroupKey : mipGroupKeyList) {
            List<ArtifactInfo> artifactList=mipGroupMap.get(mipGroupKey);
            for (ArtifactInfo ai : artifactList) {
                String mipResultPath=ai.artifactPath;
                if (mipResultPath==null) {
                    logger.error("mipResultPath for sourceDomainId="+ai.sourceDomainId +" is null");
                } else {
                    File mipResultFile=new File(mipResultPath);
                    if (!mipResultFile.exists()) {
                        logger.error("Could not find expected mip result file="+mipResultFile.getAbsolutePath()+" for sourceDomainId="+ai.sourceDomainId);
                    } else {
                        // Create the mip filepath
                        Image sourceImage = (Image)domainDAO.getDomainObject(ownerKey, Reference.createFor(DomainHelper.IMAGE_CLASSNAME,
                                ai.sourceDomainId));
                        if (sourceImage!=null) {
                            if (sourceImage.getFiles()==null) {
                                sourceImage.setFiles(new HashMap<FileType, String>());
                            }
                            sourceImage.getFiles().put(FileType.SignalMip, mipResultPath);
                        }
                        domainDAO.save(ownerKey, sourceImage);
                        logger.info("Done with entityData save");

                        // Delete extra tif file
                        String pngName=mipResultFile.getName();
                        if (pngName.endsWith(".png")) {
                            String tifName=pngName.substring(0, pngName.length()-4)+".tif";
                            File tifFile=new File(mipResultFile.getParentFile(), tifName);
                            if (! tifFile.delete()) {
                                logger.warn("failed to delete " + tifFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }

        clearResultMaps();

        logger.info("Finished doComplete()");
    }

    protected void clearResultMaps() {
        Long taskId=task.getObjectId();
        if (taskId!=null) {
            getPbdGroupMap(task, true /*remove*/);
            getMipGroupMap(task, true /*remove*/);
        }
    }

    // move files to scality
    private void moveUploadedFilesIntoFileStore() throws Exception {

        final File uploadDirectory = rootDirectory;
        final File fileStoreDirectory = new File(resultNode.getDirectoryPath());

        if (uploadDirectory.isDirectory()) {

            if (! uploadDirectory.getAbsolutePath().contains("/upload/")) {
                throw new ServiceException(
                        "'/upload/' is expected somewhere in the upload path to prevent " +
                        "accidental movement of inappropriate files, aborting task since" +
                        "upload path was specified as " + uploadDirectory.getAbsolutePath());
            }

            File[] uploadFiles = uploadDirectory.listFiles();

            if ((uploadFiles == null) || (uploadFiles.length == 0)) {
                throw new ServiceException("rootDirectoryPath for upload " +
                                           uploadDirectory.getAbsolutePath() +
                                           " is empty");
            }

            List<File> movedFiles = new ArrayList<File>(uploadFiles.length);
            List<File> movedDirectories = new ArrayList<File>(uploadFiles.length);

            logger.info("moveUploadedFilesIntoFileStore: moving " + uploadFiles.length +
                        " uploaded file(s) to " + fileStoreDirectory.getAbsolutePath());

            File fileStoreFile;
            for (File uploadFile : uploadFiles) {
                fileStoreFile = new File(fileStoreDirectory, uploadFile.getName());

                FileUtil.moveFileUsingSystemCall(uploadFile, fileStoreFile);

                if (fileStoreFile.isDirectory()) {
                    movedDirectories.add(fileStoreFile);
                } else {
                    movedFiles.add(fileStoreFile);
                }
            }

            if (! uploadDirectory.delete()) {
                logger.warn("failed to remove parent upload directory " +
                            uploadDirectory.getAbsolutePath());
            }

            for (File movedDir : movedDirectories) {
                addDirectoryAndContentsToFolder(topLevelFolder, movedDir, 0 /*index*/);
            }

            for (File movedFile : movedFiles) {
                verifyOrCreateFileEntityForFolder(topLevelFolder, movedFile, 1 /*index*/);
                topLevelFolder = (TreeNode)domainDAO.getDomainObject(ownerKey,
                        Reference.createFor(TreeNode.class, topLevelFolder.getId()));
            }

        } else {
            throw new ServiceException("rootDirectoryPath for upload " +
                                       uploadDirectory.getAbsolutePath() +
                                       " is not a directory");
        }

        rootDirectory = fileStoreDirectory;
        processData.putItem("FILE_TREE_ROOT_DIRECTORY", fileStoreDirectory.getAbsolutePath());
    }

}
