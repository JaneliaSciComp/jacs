
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ShortOrfTrimTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class ShortOrfTrimService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(ShortOrfTrimTask.PARAM_REMOVE_BLACKLISTED_ORFS) && !"".equals(task.getParameter(ShortOrfTrimTask.PARAM_REMOVE_BLACKLISTED_ORFS))) {
            return "short_orf_trim.pl -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                    ((Boolean.valueOf(task.getParameter(ShortOrfTrimTask.PARAM_REMOVE_BLACKLISTED_ORFS))) ? " -m" : "");
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the deletion of blacklisted ORFs was undefined.");
        }
    }

}