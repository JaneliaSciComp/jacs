package org.janelia.it.jacs.compute.service.screen;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.ScreenSampleResultNode;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 12/7/11
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenGroupService implements IService {

    private static final Logger logger = Logger.getLogger(FlyScreenSampleService.class);

    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected Date createDate;
    protected ScreenSampleResultNode resultNode;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;

       public void execute(IProcessData processData) throws ServiceException {
        try {

            logger.info("FlyScreenGroupService execute() start");

            this.processData=processData;
            task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            createDate = new Date();

            List<String> sampleList=(List<String>)processData.getItem("GROUP_LIST");

            logger.info("FlyScreenGroupService execute() group contains sampleList size="+sampleList.size());

            processData.putItem("SAMPLE_ENTITY_ID", sampleList);

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }


}
