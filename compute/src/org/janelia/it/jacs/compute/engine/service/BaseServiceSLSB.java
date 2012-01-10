
package org.janelia.it.jacs.compute.engine.service;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;

/**
 * This is the base class for all Service SLSBs.  It wraps a pojo service instance and can be used to demarcate
 * the transaction boundary of the service e.g. CreateBlastResultFileNodeSLSB needs to run in a separate transaction
 * context.  It extends this class without having to worry about preparation work needed for execution of the pojo
 * service.
 *
 * @author Tareq Nabeel
 */
public abstract class BaseServiceSLSB implements IService {
    public abstract IService getService(IProcessData processData);

    public void execute(IProcessData processData) throws ServiceException {
        try {
            ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            IService service = getService(processData);
            service.execute(processData);
        }
        catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
