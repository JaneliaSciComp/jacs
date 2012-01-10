
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.model.tasks.prokAnnotation.SgcSetupTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:42:41 PM
 */
public class SgcSetupService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        boolean setAnnotationFlag = Boolean.valueOf(task.getParameter(SgcSetupTask.PARAM_TOGGLE_ANNOTATION));
        return "sgc_set_up_all.dbi -u " + _databaseUser + " -p " + _databasePassword + " -D " + _targetDatabase + " -E" +
                (setAnnotationFlag ? " -A" : "");
    }

}