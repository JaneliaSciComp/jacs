package org.janelia.it.jacs.compute.service.dispatch;

import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * This service creates an entry for a task that needs to be dispatched to a different computation node and
 * notifies the computation node to start dispatching that job.
 */
public class StartComputationService implements IService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        System.out.println("!!!!!! Start computation");
        // TODO
    }

}
