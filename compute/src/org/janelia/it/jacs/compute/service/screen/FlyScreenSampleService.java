package org.janelia.it.jacs.compute.service.screen;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityFilter;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import javax.mail.Folder;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 12/7/11
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenSampleService implements EntityFilter, IService {

    private static final Logger logger = Logger.getLogger(FlyScreenSampleService.class);

    final public String MODE_UNDEFINED="UNDEFINED";
    final public String MODE_SETUP="SETUP";
    final public String MODE_COMPLETE="COMPLETE";

    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected String mode=MODE_UNDEFINED;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected List<String> sampleEntityIdList;

    protected static final String RELATIVE_SAMPLE_TO_JACS_PATH = SystemConfigurationProperties.getString("FlyScreen.RelativePathFromScreenSampleToJacsRoot");
    public static final String SUPPORTING_FILES_FOLDER_NAME="supporting files";


       public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("FlyScreenSampleService execute() start");

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            mode = processData.getString("MODE");
            sampleEntityIdList=(List<String>)processData.getItem("GROUP_LIST");

            logger.info("FlyScreenSampleService execute() contains "+sampleEntityIdList.size()+" samples, mode="+mode);

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

    public boolean includeEntity(IProcessData processData, Entity entity) {
        if (getStackPath(entity)!=null) {
            return true;
        }
        return false;
    }

    public void doSetup() throws Exception {
        logger.info("FlyScreenSampleService  doSetup() start");

        List<ScreenSampleResultNode> resultNodeList=new ArrayList<ScreenSampleResultNode>();
        List<String> resultNodeIdList=new ArrayList<String>();
        List<String> stackPathList=new ArrayList<String>();

        // Error Check that ProcessData state is being handled correctly
        for (String sampleEntityId : sampleEntityIdList) {

            // Proceed if no error
            ScreenSampleResultNode resultNode = new ScreenSampleResultNode(task.getOwner(), task, "ScreenSampleResultNode",
                    "ScreenSampleResultNode for task " + task.getObjectId(), visibility, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);
            logger.info("FlyScreenSampleService  doSetup()  resultNodeId="+resultNode.getObjectId()+ " intended path="+resultNode.getDirectoryPath());
            FileUtil.ensureDirExists(resultNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultNode.getDirectoryPath());
            String creationMessage="Created ScreenSanmpleResultNode path="+resultNode.getDirectoryPath()+" id="+resultNode.getObjectId()+" screenSampleId="+sampleEntityId;
            logger.info(creationMessage);
            resultNodeList.add(resultNode);
            resultNodeIdList.add(resultNode.getObjectId().toString());
            Entity screenSampleEntity=EJBFactory.getLocalAnnotationBean().getEntityTree(new Long(sampleEntityId.trim()));
            String stackPath=getStackPath(screenSampleEntity);
            stackPathList.add(stackPath);
            logger.info("FlyScreenSampleService  doSetup()   stackPath="+stackPath);

        }
        processData.putItem("SAMPLE_FILE_NODE_LIST", resultNodeList);
        processData.putItem("SAMPLE_FILE_NODE_ID_LIST", resultNodeIdList);
        processData.putItem("STACK_PATH_LIST", stackPathList);
    }

    public void doComplete() throws Exception {
        logger.info("FlyScreenSampleService  doComplete() start");

        List<ScreenSampleResultNode> resultNodeList=(List<ScreenSampleResultNode>)processData.getItem("SAMPLE_FILE_NODE_LIST");

        int index=0;
        for (ScreenSampleResultNode resultNode : resultNodeList) {
            Entity screenSampleEntity=EJBFactory.getLocalAnnotationBean().getEntityTree(new Long(sampleEntityIdList.get(index).trim()));
            File resultDir=new File(resultNode.getDirectoryPath());
            logger.info("FlyScreenSampleService  doComplete()  using resultDir="+resultDir.getAbsolutePath());
            File[] resultFiles=resultDir.listFiles();
            File pngFile=null;
            File tifFile=null;
            for (File f : resultFiles) {
                logger.info("Checking file="+f.getAbsolutePath());
                if (f.getName().toLowerCase().endsWith(".tif")) {
                    tifFile=f;
                } else if (f.getName().toLowerCase().endsWith(".png")) {
                    pngFile=f;
                }
            }

            // Clean up tif file
            if (tifFile!=null && pngFile!=null) {
                logger.info("Deleting tif file="+tifFile.getAbsolutePath());
                tifFile.delete();
            }

            // Clean up map channel intermediary
            File alignedStackNoCompartments=new File(resultDir, "AlignedStackNoCompartments.v3dpbd");
            if (!alignedStackNoCompartments.exists()) {
                throw new Exception("Could not locate intermediate file="+alignedStackNoCompartments.getAbsolutePath());
            }
            alignedStackNoCompartments.delete();


            if (pngFile!=null) {
                logger.info("Found png file="+pngFile.getAbsolutePath());

                // Create supporting files folder
                File supportingFileDir=new File(resultDir, SUPPORTING_FILES_FOLDER_NAME);
                if (!supportingFileDir.mkdir()) {
                    throw new Exception("Could not create new directory="+supportingFileDir.getAbsolutePath());
                }
                Entity supportingFilesFolder=addChildFolderToEntity(screenSampleEntity, SUPPORTING_FILES_FOLDER_NAME,
                        supportingFileDir.getAbsolutePath());
                File supportingPngFile=new File(supportingFileDir, pngFile.getName());
                FileUtil.moveFileUsingSystemCall(pngFile, supportingPngFile);
                pngFile=supportingPngFile;

                // Create MIP
                Entity mipEntity=createMipEntity(pngFile, screenSampleEntity.getName() + " mip");
                addToParent(screenSampleEntity, mipEntity, null, EntityConstants.ATTRIBUTE_ENTITY);

                // Add default image to screen sample
                screenSampleEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, pngFile.getAbsolutePath());
                EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(screenSampleEntity);

                // Add default image to stack
                Entity stackEntity=getStackEntityFromScreenSample(screenSampleEntity);
                stackEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, pngFile.getAbsolutePath());
                EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(stackEntity);

                // Add default image to mip
                mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, pngFile.getAbsolutePath());
                EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(mipEntity);

                // Add link to stack file
                SystemCall sc=new SystemCall(logger);
                File stackFile=new File(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                String stackPath=stackFile.getAbsolutePath();
                String[] splist=stackPath.split("jacsData");
                String relativePath=RELATIVE_SAMPLE_TO_JACS_PATH+splist[1];
                sc.emulateCommandLine("ln -s "+relativePath+" "+resultDir.getAbsolutePath()+"/"+stackFile.getName(), true);

                logger.info("Finished saving entity metadata for png file");
            }
            index++;
        }
        logger.info("FlyScreenSampleService  doComplete()  done");
    }

    protected String getStackPath(Entity screenSampleEntity) {
        logger.info("getStackPath() checking screenSampleEntity id="+screenSampleEntity.getId());
        Set<Entity> children = screenSampleEntity.getChildren();
        logger.info("Found " + children.size() + " children for screenSampleEntity id="+screenSampleEntity.getId());
        if (children!=null && children.size()>0) {
            for (Entity child : children)  {
                Long childId=child.getId();
                logger.info("childId="+childId);
                //child=EJBFactory.getLocalAnnotationBean().getEntityById(childId.toString());
                String message = "For screenSampleEntity id="+screenSampleEntity.getId() + " considering child name="+child.getName()+" id="+child.getId();
                logger.info(message);
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    String stackPath=child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    File stackFile=new File(stackPath);
                    if (stackFile.exists()) {
                        return stackFile.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

     protected Entity getStackEntityFromScreenSample(Entity screenSampleEntity) {
        if (screenSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
            List<Entity> stackEntities = screenSampleEntity.getChildrenOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
            if (stackEntities.size()>0) {
                return stackEntities.get(0);
            }
        }
        return null;
    }

    protected Entity getMipEntityFromScreenSample(Entity screenSampleEntity) {
        if (screenSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
            List<Entity> mipEntities = screenSampleEntity.getChildrenOfType(EntityConstants.TYPE_IMAGE_2D);
            for (Entity mip : mipEntities) {
                if (mip.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).endsWith(".png")) {
                    return mip;
                }
            }
        }
        return null;
    }


    protected Entity createMipEntity(File pngFile, String name) throws Exception {
        Entity mipEntity = new Entity();
        mipEntity.setUser(user);
        mipEntity.setEntityType(EJBFactory.getLocalAnnotationBean().getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D));
        mipEntity.setCreationDate(createDate);
        mipEntity.setUpdatedDate(createDate);
        mipEntity.setName(name);
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity = EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(mipEntity);
        return mipEntity;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
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
        logger.info("Saved folder " + name+" as " + folder.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }



}
