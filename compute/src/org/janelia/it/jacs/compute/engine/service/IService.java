
package org.janelia.it.jacs.compute.engine.service;

import org.janelia.it.jacs.compute.engine.data.IProcessData;

/**
 * This is the super interface for all services
 *
 * @author Tareq Nabeel
 */
public interface IService {

    /**
     * This method executes the service
     *
     * @param processData the running state of the process
     * @throws ServiceException any exceptions encountered during execution of the service
     *                          should be wrapped with this execution
     */
    public void execute(IProcessData processData) throws ServiceException;
}
