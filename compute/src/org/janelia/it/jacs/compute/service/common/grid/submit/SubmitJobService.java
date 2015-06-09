
package org.janelia.it.jacs.compute.service.common.grid.submit;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Sep 1, 2009
 * Time: 4:14:33 PM
 */
public interface SubmitJobService {
    
    public void submitJobAndWait(IProcessData processData) throws SubmitJobException;

    public Process submitAsynchJob(IProcessData processData, String submissionKey) throws SubmitJobException;

    public void cleanup();
    
    public void postProcess() throws MissingDataException;

    public void handleErrors() throws Exception;
}
