package org.janelia.it.jacs.compute.service.dispatch;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
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
public class DispatchComputationService implements IService {

    private static final String DISPATCH_REQUEST_QUEUE = "queue/dispatchRequest";

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            DispatcherDAO dispatcherDao = new DispatcherDAO();
            Task dispatchedTask = ProcessDataHelper.getTask(processData);
            DispatcherJob dispatcherJob = createJob(dispatchedTask);
            String defaultProcessingHost = SystemConfigurationProperties.getString("computeserver.processing.host");
            dispatcherJob.setDispatchHost(dispatcherDao.getProcessingHost(dispatcherJob, defaultProcessingHost));
            dispatcherDao.save(dispatcherJob);
            notifyComputationNode(dispatcherJob);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private DispatcherJob createJob(Task t) {
        DispatcherJob job = new DispatcherJob();
        job.setDispatchedTaskId(t.getObjectId());
        job.setDispatchedTaskOwner(t.getOwner());
        return job;
    }

    private void notifyComputationNode(DispatcherJob job) throws Exception {
        AsyncMessageInterface messageInterface = new AsyncMessageInterface(
                "AsyncMessageInterface.LocalConnectionFactory",
                "AsyncMessageInterface.RemoteConnectionFactory",
                "AsyncMessageInterface.ProviderURLPattern",
                "AsyncMessageInterface.DeadLetterQueue");
        String providerUrlTemplate = SystemConfigurationProperties.getString(messageInterface.getProviderUrlProperty());
        messageInterface.setProviderUrl(SystemConfigurationProperties.getString("AsyncMessageInterface.ProviderURL"));
        String providerUrl = providerUrlTemplate.replace("<host>", job.getDispatchHost());
        messageInterface.setProviderUrl(providerUrl);
        try {
            JmsUtil.sendMessageToQueue(messageInterface, messageInterface.getRemoteConnectionType(),
                    ImmutableMap.<String, Object>of(
                            IProcessData.PROCESS_ID, job.getDispatchedTaskId(),
                            IProcessData.JOB_ID, job.getDispatchId()
                            ),
                    DISPATCH_REQUEST_QUEUE);
        } finally {
            messageInterface.endMessageSession();
        }
    }

}
