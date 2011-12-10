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
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.User;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 12/7/11
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenSampleService implements EntityFilter, IService {

    private static final Logger logger = Logger.getLogger(FlyScreenSampleService.class);

    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;

       public void execute(IProcessData processData) throws ServiceException {
        try {

            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public boolean includeEntity(IProcessData processData, Entity entity) {

        // Do not include non-samples
        if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
            return false;
        }

        // If we're not doing a refresh, do not include any sample that already has a result
        for(Entity child : entity.getChildren()) {
            if (child.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {

                // Now we evaluate the components of the sample to determine if something is missing

                logger.info("Skipping completed sample "+entity.getName()+" (id="+entity.getId()+")");
                return false;
            }
        }

        // Include everything by default
        return true;
    }


}
