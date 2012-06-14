package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;


/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/31/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */


public class MaskGuideService  implements IService {

    private static final Logger logger = Logger.getLogger(MaskGuideService.class);

    public static final String PARAM_maskFolderName = "Mask Folder Name";
    public static final String PARAM_refresh = "Refresh";

    protected String maskAnnotationTopResourceDir=SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Boolean refresh;
    protected String maskFolderName;
    protected File maskAnnotationResourceDir;


    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger.info("MaskGuideService execute() start");
            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            logger.info("MaskGuideService running under TaskId="+task.getObjectId());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            refresh=task.getParameter(PARAM_refresh).trim().toLowerCase().equals("true");
            maskFolderName=task.getParameter(PARAM_maskFolderName);

            if (maskFolderName==null) {
                throw new Exception("Mask Folder name must be defined in task");
            } else {
                maskAnnotationResourceDir=new File(maskAnnotationTopResourceDir, maskFolderName);
                logger.info("Using maskAnnotationFolder="+maskAnnotationResourceDir.getAbsolutePath());
            }
            logger.info("MaskGuideService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
