
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.OverlapAnalysisTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class OverlapAnalysisService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(OverlapAnalysisTask.PARAM_IS_NT_DATA) && !"".equals(task.getParameter(OverlapAnalysisTask.PARAM_IS_NT_DATA))) {
            boolean isNtData = Boolean.valueOf(task.getParameter(OverlapAnalysisTask.PARAM_IS_NT_DATA));
            return "OverlapAnalysis.dbi -U " + _databaseUser + " -P " + _databasePassword + " -D " + _targetDatabase + ((isNtData) ? " -N" : "");
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the data type was undefined.");
        }
    }

}