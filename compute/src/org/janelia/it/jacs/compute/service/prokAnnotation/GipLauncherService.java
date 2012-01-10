
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.GipLauncherTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class GipLauncherService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(GipLauncherTask.PARAM_ASSEMBLY_ID) && !"".equals(task.getParameter(GipLauncherTask.PARAM_ASSEMBLY_ID)) &&
                null != task.getParameter(GipLauncherTask.PARAM_GENE_TYPE) && !"".equals(task.getParameter(GipLauncherTask.PARAM_GENE_TYPE))) {
            return "gip_launcher.pl -U " + _databaseUser + " -P " + _databasePassword + " -D " + _targetDatabase +
                    " -A ISCURRENT" + " -G locus" +
                    " -g " + getDefaultProjectCode();
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the assembly id and/or gene type was undefined.");
        }
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}