
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.LoadFastaContigsTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class LoadFastaContigsService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(LoadFastaContigsTask.PARAM_CONTIG_FILE_PATH) && !"".equals(task.getParameter(LoadFastaContigsTask.PARAM_CONTIG_FILE_PATH))) {
            return "load_fasta_contigs_to_sgd.pl -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                    " -I -f " + task.getParameter(LoadFastaContigsTask.PARAM_CONTIG_FILE_PATH);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the contig file location was undefined.");
        }
    }

}