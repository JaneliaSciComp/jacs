package org.janelia.it.jacs.compute.service.fly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
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
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.MaskAnnotationResultNode;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.FileUtil;

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
    protected String patternChannel=SystemConfigurationProperties.getString("FlyScreen.AlignedStackPatternChannel");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Boolean refresh;
    protected String maskAnnotationFolderName;
    protected File maskAnnotationResourceDir;
    protected List<String> abbrevationList=new ArrayList<String>();


    public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("MaskGuideService execute() start");

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            logger.info("MaskSampleAnnotationService running under TaskId="+task.getObjectId());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            refresh=processData.getString("REFRESH").trim().toLowerCase().equals("true");
            maskAnnotationFolderName=processData.getString("ROOT_ENTITY_NAME");

            if (maskAnnotationFolderName==null) {
                throw new Exception("ROOT_ENTITY_NAME must be defined in processData to determine maskAnnotationFolder");
            } else {
                maskAnnotationResourceDir=new File(maskAnnotationTopResourceDir, maskAnnotationFolderName);
                logger.info("Using maskAnnotationFolder="+maskAnnotationResourceDir.getAbsolutePath());
            }

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
