
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.JobControlBeanLocal;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.Date;
import java.util.Iterator;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 1 AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 1 * * ?")
})
@ResourceAdapter("quartz-ra.rar")
public class DispatchComputationMDB implements Job {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DispatchComputationMDB.class);

    private String currentProcessingId = SystemConfigurationProperties.getString("computeserver.dispatch.identifier");
    private int maxRetries = SystemConfigurationProperties.getInt("computeserver.dispatch.maxRetries");
    private int prefetchSize = SystemConfigurationProperties.getInt("computeserver.dispatch.prefetchSize");

    public String getCurrentProcessingId() {
        return currentProcessingId;
    }

    public void setCurrentProcessingId(String currentProcessingId) {
        this.currentProcessingId = currentProcessingId;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getPrefetchSize() {
        return prefetchSize;
    }

    public void setPrefetchSize(int prefetchSize) {
        this.prefetchSize = prefetchSize;
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (prefetchSize <= 0) {
            return;
        }
        LOG.info("Waking to dispatch queued jobs.");
        DispatcherDAO dispatcherDao = new DispatcherDAO();
        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
        JobControlBeanLocal jobBean = EJBFactory.getLocalJobControlBean();
        for (Iterator<DispatcherJob> jobIterator = dispatcherDao.getPendingJobsIterator(currentProcessingId, maxRetries, prefetchSize);
             jobIterator.hasNext();) {
            DispatcherJob job = jobIterator.next();
            LOG.info("Submit job {}", job.getDispatchId());
            try {
                Long taskId = job.getDispatchedTaskId();
                computeBean.submitJob(job.getProcessDefnName(), taskId);
                job.setDispatchHost(currentProcessingId);
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
        LOG.info("Completed dispatching currently queued jobs.");
    }
}
