package org.janelia.it.jacs.compute.service.dispatch;

import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.JobControlBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;

/**
 * This service creates an entry for a task that needs to be dispatched to a different computation node and
 * notifies the computation node to start dispatching that job.
 */
public class StartComputationService implements IService {

    private static final Logger LOG = LoggerFactory.getLogger(StartComputationService.class);

    @Override
    public synchronized void execute(IProcessData processData) throws ServiceException {
        String currentProcessingId = SystemConfigurationProperties.getString("computeserver.processing.identifier");
        int maxRetries = SystemConfigurationProperties.getInt("computeserver.processing.maxRetries");
        DispatcherDAO dispatcherDao = new DispatcherDAO();
        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
        JobControlBeanLocal jobBean = EJBFactory.getLocalJobControlBean();
        for (Iterator<DispatcherJob> jobIterator = dispatcherDao.getPendingJobsIterator(currentProcessingId, maxRetries);
             jobIterator.hasNext();) {
            DispatcherJob job = jobIterator.next();
            LOG.info("Submit job {}", job.getDispatchId());
            try {
                Long taskId = job.getDispatchedTaskId();
                Task task = computeBean.getTaskById(taskId);
                computeBean.submitJob(task.getJobName(), taskId);
                job.setDispatchStatus(DispatcherJob.Status.SUBMITTED);
            } catch (Exception e) {
                job.setDispatchStatus(DispatcherJob.Status.FAILED);
                LOG.info("Job {} submission failed", job.getDispatchId(), e);
            } finally {
                job.incRetries();
                job.setDispatchedDate(new Date());
                jobBean.updateDispatcherJob(job);
            }
        }
    }

}
