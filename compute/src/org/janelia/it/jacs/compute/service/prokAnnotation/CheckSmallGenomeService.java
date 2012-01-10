
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.CheckSmallGenomeTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class CheckSmallGenomeService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        String annotationMode = task.getParameter(CheckSmallGenomeTask.PARAM_ANNOTATION_MODE);

        if (null != task.getParameter(CheckSmallGenomeTask.PARAM_STAGE) && !"".equals(task.getParameter(CheckSmallGenomeTask.PARAM_STAGE))) {
            String cmd = "check_small_genome_db.dbi -U " + _databaseUser + " -P " + _databasePassword + " -D " + _targetDatabase + " -A " +
                    task.getParameter(CheckSmallGenomeTask.PARAM_STAGE);
            // If not from NCBI, throw the -T flag
            if (!ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(annotationMode)) {
                cmd += " -T";
            }
            return cmd;
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the stage to check was undefined.");
        }
    }

}