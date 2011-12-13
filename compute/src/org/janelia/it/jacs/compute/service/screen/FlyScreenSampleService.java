package org.janelia.it.jacs.compute.service.screen;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityFilter;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 12/7/11
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenSampleService implements EntityFilter, IService {

    private static final Logger logger = Logger.getLogger(FlyScreenSampleService.class);

    final public String MODE_UNDEFINED="MODE_UNDEFINED";
    final public String MODE_SETUP="MODE_SETUP";
    final public String MODE_COMPLETE="MODE_COMPLETE";

    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected String mode=MODE_UNDEFINED;
    protected ScreenSampleResultNode resultNode;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected String sampleEntityId;

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
            sampleEntityId=processData.getString("SAMPLE_ENTITY_ID");

            logger.info("FlyScreenSampleService execute()  sampleEntityId="+sampleEntityId+" mode="+mode);

            if (mode==MODE_SETUP) {
                doSetup();
            } else if (mode==MODE_COMPLETE) {
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
    	resultNode = new ScreenSampleResultNode(task.getOwner(), task, "ScreenSampleResultNode",
                "ScreenSampleResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultNode);
        logger.info("FlyScreenSampleService  doSetup()  resultNodeId="+resultNode.getObjectId());
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultNode.getDirectoryPath());
        String creationMessage="Created ScreenSanmpleResultNode path="+resultNode.getDirectoryPath()+" id="+resultNode.getObjectId()+" screenSampleId="+sampleEntityId;
        logger.info(creationMessage);
        task.addMessage(creationMessage);
        EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultNode);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
        Entity screenSampleEntity=EJBFactory.getLocalAnnotationBean().getEntityById(sampleEntityId);
        String stackPath=getStackPath(screenSampleEntity);
        processData.putItem("STACK_PATH", stackPath);
        logger.info("FlyScreenSampleService  doSetup()   stackPath="+stackPath);
    }

    public void doComplete() throws Exception {
        logger.info("FlyScreenSampleService  doComplete() start");
        Entity screenSampleEntity=EJBFactory.getLocalAnnotationBean().getEntityById(sampleEntityId);
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
        if (tifFile!=null && pngFile!=null) {
            logger.info("Deleting tif file="+tifFile.getAbsolutePath());
            tifFile.delete();
        }
        if (pngFile!=null) {
            logger.info("Found png file="+pngFile.getAbsolutePath());
            Entity mipEntity=createMipEntity(pngFile, screenSampleEntity.getName() + " mip");
            addToParent(screenSampleEntity, mipEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
            screenSampleEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, pngFile.getAbsolutePath());
            Entity stackEntity=getStackEntityFromScreenSample(screenSampleEntity);
            stackEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, pngFile.getAbsolutePath());
            EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(screenSampleEntity);
            EJBFactory.getLocalAnnotationBean().saveOrUpdateEntity(stackEntity);
            logger.info("Finished saving entity metadata for png file");
        }
        logger.info("FlyScreenSampleService  doComplete()  done");
    }

    protected String getStackPath(Entity screenSampleEntity) {
        if (screenSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
            List<Entity> stackEntities = screenSampleEntity.getChildrenOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
            for (Entity e : stackEntities) {
                String filepath = e.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (filepath!=null) {
                    File f = new File(filepath);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    } else {
                        logger.error("Could not find expected stack in location="+f.getAbsolutePath());
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

    protected Entity createMipEntity(File pngFile, String name) throws Exception {
        Entity mipEntity = new Entity();
        mipEntity.setUser(user);
        mipEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D));
        mipEntity.setCreationDate(createDate);
        mipEntity.setUpdatedDate(createDate);
        mipEntity.setName(name);
        mipEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pngFile.getAbsolutePath());
        mipEntity = annotationBean.saveOrUpdateEntity(mipEntity);
        return mipEntity;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }


}
