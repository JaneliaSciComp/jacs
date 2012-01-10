
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.GipTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class GipService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(GipTask.PARAM_CONFIG_LOCATION) && !"".equals(task.getParameter(GipTask.PARAM_CONFIG_LOCATION))) {
            return "GIP.pl " + task.getParameter(GipTask.PARAM_CONFIG_LOCATION);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the config file location was undefined.");
        }
    }

}