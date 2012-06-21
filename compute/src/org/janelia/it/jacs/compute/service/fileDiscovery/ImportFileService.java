package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
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
import org.janelia.it.jacs.model.user_data.entity.FileTreeLoaderResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/24/12
 * Time: 11:46 AM
 */
public class ImportFileService extends FileDiscoveryService{

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


    final public String SUPPORTING_FILES_FOLDER_NAME="supportingFiles";

    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected String topLevelFolderName;
    protected String rootDirectoryPath;
    protected Entity topLevelFolder;
    protected Entity supportingFilesFolder;
    protected File rootDirectory;
    protected FileTreeLoaderResultNode resultNode;


    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            task= ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;

            logger.info("Creating top-level folder");
            topLevelFolderName=processData.getString("ROOT_ENTITY_NAME");
            topLevelFolder=createOrVerifyRootEntity(topLevelFolderName);

            logger.info("Validating root directorypath");
            rootDirectoryPath=processData.getString("INPUT_DIR_LIST");
            rootDirectory=new File(rootDirectoryPath);
            if (!rootDirectory.exists()) {
                throw new Exception("Could not find rootDirectory="+rootDirectory.getAbsolutePath());
            }

            resultNode = new FileTreeLoaderResultNode(task.getOwner(), task, "FileTreeLoaderResultNode",
                    "FileTreeLoaderResultNode for task " + task.getObjectId(), visibility, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);
            logger.info("FileTreeLoaderService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " intended path="+resultNode.getDirectoryPath());
            FileUtil.ensureDirExists(resultNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultNode.getDirectoryPath());
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());


            addDirectoryAndContentsToFolder(topLevelFolder, rootDirectory, 0 /*index*/);


        } catch (Exception e) {
            logger.error("Error in FileTreeLoader execute() : " + e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }


    protected void addDirectoryAndContentsToFolder(Entity folder, File dir, Integer index) throws Exception {
        Entity dirEntity=verifyOrCreateChildFolderFromDir(folder, dir, index);
        List<File> orderedFiles=getOrderedFilesInDir(dir);
        for (int i=0;i<orderedFiles.size();i++) {
            File f=orderedFiles.get(i);
            if (passesExclusionFilter(f)) {
                if (f.isDirectory()) {
                    addDirectoryAndContentsToFolder(dirEntity, f, i);
                } else {
                    verifyOrCreateFileEntityForFolder(dirEntity, f, i);
                }
            }
        }
    }

    protected boolean passesExclusionFilter(File f) {
        if (f.getName().startsWith(".")) {
            return false;
        } else {
            return true;
        }
    }

    protected void verifyOrCreateFileEntityForFolder(Entity folder, File f, Integer index) throws Exception {
        // First check to see if an entity for this file path already exists
        Entity fileEntity=null;
        boolean alreadyExists=false;
        for (Entity child : folder.getChildren()) {
            String filePath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (filePath!=null && filePath.equals(f.getAbsolutePath())) {
                // Entity already exists
                alreadyExists=true;
                fileEntity=child;
            }
        }

        if (!alreadyExists) {
            // Assume the entity needs to be created
            EntityType entityType=getEntityTypeForFile(f);
            fileEntity=createEntityForFile(f, entityType);
            addToParent(folder, fileEntity, index, EntityConstants.ATTRIBUTE_ENTITY);
        }



    }


    protected Entity createEntityForFile(File f, EntityType entityType) throws Exception {
        Entity fileEntity=new Entity();
        fileEntity.setCreationDate(createDate);
        fileEntity.setUpdatedDate(createDate);
        fileEntity.setUser(user);
        fileEntity.setName(f.getName());
        fileEntity.setEntityType(entityType);
        fileEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, f.getAbsolutePath());
        fileEntity = entityBean.saveOrUpdateEntity(fileEntity);
        logger.info("Saved file "+f.getAbsolutePath()+" as entity="+fileEntity.getId());
        return fileEntity;
    }




}

