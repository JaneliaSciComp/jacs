package org.janelia.it.jacs.compute.service.fly;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
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

import java.io.File;
import java.io.FileWriter;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/31/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */



/*

    This service creates a reference guide for understanding the labeling and 3D structure of a specific mask set
    as run by the mask annotation pattern service.

    The inputs to this service are:

    1) the 3D RGB file specifying the mask.
    2) the name index file which maps the names to the colors in the RGB mask

    The outputs are:

    1) The creation of the top-level "Mask Guide" folder, if it doesn't already exist
    2) The creation of a sub-folder for this particular mask
    3) A 2D MIP and 3D stack for the whole rgb file
    4) A 2D MIP and 3D stack for each compartment, in which the non-compartment volume is "ghosted"

 */


public class MaskGuideService extends SubmitDrmaaJobService implements IService {

    private static final Logger logger = Logger.getLogger(MaskGuideService.class);

    private static final String MODE = "MODE";
    private static final String MODE_SETUP = "MODE_SETUP";
    private static final String MODE_COMPLETE = "MODE_COMPLETE";

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "maskGuideConfiguration.";

    public static final String PARAM_maskFolderName = "Mask Folder Name";
    public static final String PARAM_refresh = "Refresh";

    public static final String NAME_INDEX_FILENAME = "maskNameIndex.txt";
    public static final String RGB_FILENAME = "maskRGB.v3dpbd";

    public static final String TOP_LEVEL_GUIDE_DIR_NAME = "Pattern Guides";

    protected String maskAnnotationTopResourceDir=SystemConfigurationProperties.getString("FileStore.CentralDir")+
            SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected Boolean refresh;
    protected String maskFolderName;
    protected Entity guideFolder;
    protected File maskAnnotationResourceDir;
    protected File guideDir;
    protected File rgbFile;
    protected File maskNameIndexFile;
    protected IProcessData processData;
    protected String mode;
    FileDiscoveryHelper helper;

    protected void init(IProcessData processData) throws Exception {
        try {
            this.processData=processData;
            configure();

            if (!mode.equals(MODE_SETUP)) {
                throw new Exception("When MaskGuideService is run in grid mode, it must be as mode MODE_SETUP");
            }
            MaskAnnotationResultNode maskGuideResultNode = new MaskAnnotationResultNode(task.getOwner(), task, "MaskAnnotationResultNode",
                    "MaskAnnotationResultNode for task " + task.getObjectId(), visibility, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(maskGuideResultNode);
            resultFileNode = maskGuideResultNode;
            super.init(processData);

            logger.info("MaskGuideService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected void configure() throws Exception {
        logger.info("MaskGuideService execute() start");
        task = ProcessDataHelper.getTask(processData);
        logger.info("MaskGuideService running under TaskId=" + task.getObjectId());
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
        entityBean = EJBFactory.getLocalEntityBean();
        computeBean = EJBFactory.getLocalComputeBean();
        String ownerName = ProcessDataHelper.getTask(processData).getOwner();
        Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
        this.ownerKey = subject.getKey();
        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);

        createDate = new Date();
        refresh = task.getParameter(PARAM_refresh).trim().toLowerCase().equals("true");
        maskFolderName = task.getParameter(PARAM_maskFolderName);

        if (maskFolderName == null) {
            throw new Exception("Mask Folder name must be defined in task");
        } else {
            maskAnnotationResourceDir = new File(maskAnnotationTopResourceDir, maskFolderName);
            logger.info("Using maskAnnotationFolder=" + maskAnnotationResourceDir.getAbsolutePath());
        }

        guideDir = new File(maskAnnotationResourceDir, "guide");
        if (!guideDir.exists()) {
            if (!guideDir.mkdir()) {
                throw new Exception("Could not create guide dir=" + guideDir.getAbsolutePath());
            }
        }

        maskNameIndexFile = new File(maskAnnotationResourceDir, NAME_INDEX_FILENAME);
        if (!maskNameIndexFile.exists()) {
            throw new Exception("Could not locate expected name index file=" + maskNameIndexFile.getAbsolutePath());
        }
        rgbFile = new File(maskAnnotationResourceDir, RGB_FILENAME);
        if (!rgbFile.exists()) {
            throw new Exception("Could not locate expected RGB file=" + rgbFile.getAbsolutePath());
        }

        mode = processData.getString(MODE);
        if (mode==null) {
            throw new Exception("MODE must be non-null");
        }
        if (mode.equals(MODE_SETUP)) {
            logger.info("mode is SETUP");
        } else if (mode.equals(MODE_COMPLETE)) {
            logger.info("mode is COMPLETE");
        } else {
            throw new Exception("Do not recognize mode="+mode);
        }

        Entity topLevelFolder = helper.createOrVerifyRootEntity(TOP_LEVEL_GUIDE_DIR_NAME, true, true);
        Map<String, Entity> subfolderMap = new HashMap<String, Entity>();
        Set<Entity> children = topLevelFolder.getChildren();
        if (children != null && children.size() > 0) {
            for (Entity child : topLevelFolder.getChildren()) {
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                    subfolderMap.put(child.getName(), child);
                }
            }
        }
        guideFolder=subfolderMap.get(maskFolderName);
        if (guideFolder==null) {
            logger.info("Could not find Guide subfolder - creating new one");
            guideFolder=verifyOrCreateSubFolder(topLevelFolder, maskFolderName, guideDir.getAbsolutePath());
        }

    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData=processData;
            configure();
            if (!mode.equals(MODE_COMPLETE)) {
                throw new Exception("When MaskGuideService is run as a regular service, it must be with mode = MODE_COMPLETE");
            }
            completePostProcess();
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }



    @Override
    protected String getGridServicePrefixName() {
        return "maskGuide";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles();
        setJobIncrementStop(1);
        createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath());
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getMaskGuideCommand(maskNameIndexFile.getAbsolutePath(), rgbFile.getAbsolutePath(), guideDir.getAbsolutePath()));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 12;
    }
    
    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void postProcess() throws MissingDataException {
        try {
            if (mode.equals(MODE_SETUP)) {
                setupPostProcess();
            }
        }
        catch (Exception ex) {
            throw new MissingDataException(ex.toString());
        }
    }

    protected void setupPostProcess() {
        Map<String, FileNode> nodeMap=new HashMap<String, FileNode>();
        List<String> convertList=new ArrayList<String>();
        convertList.add(guideDir.getAbsolutePath());
        nodeMap.put(guideDir.getAbsolutePath(), resultFileNode);
        logger.info("Adding to processData MIPS_CONVERSION_PATH of size="+convertList.size());
        processData.putItem("MIPS_CONVERSION_PATH", convertList);
        logger.info("Adding to processData IMAGE_CONVERSION_RESULT_NODE_MAP of size="+nodeMap.size());
        processData.putItem("IMAGE_CONVERSION_RESULT_NODE_MAP", nodeMap);
    }

    protected void completePostProcess() throws Exception {
        logger.info("Creating Mask Guide entities");
        File[] maskFiles = guideDir.listFiles();

        // Delete tifs
        for (File maskFile : maskFiles) {
            if (maskFile.getName().endsWith(".tif")) {
                logger.info("Deleting tif file="+maskFile.getAbsolutePath());
                maskFile.delete();
            }
        }
        // Create mips
        Map<String, Entity> mipMap=new HashMap<String, Entity>();
        for (File maskFile : maskFiles) {
            if (maskFile.getName().endsWith(".png")) {
                String base=getBaseMaskNameFromFile(maskFile);
                Entity mip=createMipEntity(maskFile, base);
                mipMap.put(base, mip);
            }
        }
        // Create stacks
        for (File maskFile : maskFiles) {
            if (maskFile.getName().endsWith(".v3dpbd")) {
                String base=getBaseMaskNameFromFile(maskFile);
                Entity mip=mipMap.get(base);
                if (mip==null) {
                    throw new Exception("Could not find mip for basename="+base);
                }
                Entity stack=createStackEntity(maskFile, base, mip);
                addToParent(guideFolder, stack, 0, EntityConstants.ATTRIBUTE_ENTITY);
            }
        }
        logger.info("Done creating Mask Guide entities");
    }

    protected String getBaseMaskNameFromFile(File f) throws Exception {
        logger.info("getBaseMaskNameFromFile f=" + f.getAbsolutePath());
        String fileName = f.getName();
        logger.info("Getting base name for fileName=" + fileName);
        String[] components = fileName.split("\\.");
        logger.info("components has length=" + components.length);
        String b1 = components[0];
        if (b1.contains("_")) {
            String[] c2 = b1.split("_");
            if (c2.length == 2 && c2[1].toLowerCase().equals("mip")) {
                return c2[0];
            }
            else if (c2.length == 3 && c2[2].toLowerCase().equals("mip")) {
                return c2[0] + "_" + c2[1];
            }
            else if (c2.length == 4 && c2[3].toLowerCase().equals("mip")) {
                return c2[0] + "_" + c2[1] + "_" + c2[2];
            }
            else if (c2.length == 2) {
                return c2[0] + "_" + c2[1];
            }
            else if (c2.length == 3)  {
                return c2[0] + "_" + c2[1] + "_" + c2[2];
            }
            else {
                throw new Exception("Cannot parse filename="+fileName);
            }
        } else {
            return b1;
        }
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

    protected Entity getSubFolderByName(Entity parentEntity, String folderName) {
        for (EntityData ed : parentEntity.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) & child.getName().equals(folderName)) {
                return child;
            }
        }
        return null;
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

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, EntityConstants.ATTRIBUTE_ENTITY);
        logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
                " as child of "+parent.getEntityTypeName()+"#"+parent.getId());
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
        stack.setEntityTypeName(EntityConstants.TYPE_IMAGE_3D);
        stack.setCreationDate(createDate);
        stack.setUpdatedDate(createDate);
        stack.setName(entityName);
        stack = entityBean.saveOrUpdateEntity(stack);
        helper.setDefault2dImage(stack, mipEntity);
        stack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        stack = entityBean.saveOrUpdateEntity(stack);
        logger.info("Saved stack " + stack.getName() + " as "+stack.getId());
        return stack;
    }

}
