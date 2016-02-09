package org.janelia.it.jacs.compute.launcher.scheduler;

import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import java.util.Date;
import java.util.Iterator;

@MessageDriven(activationConfig = {
    // crontTrigger starts with seconds.  Below should run at the stroke of 1 AM EST, every day
    @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 */2 * * * ?")
})
@ResourceAdapter("quartz-ra.rar")
public class DispatchComputationMDB implements Job {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DispatchComputationMDB.class);

    private String currentProcessingId = SystemConfigurationProperties.getString("computeserver.dispatch.identifier");
    private int maxRetries = SystemConfigurationProperties.getInt("computeserver.dispatch.maxRetries");
    private int prefetchSize = SystemConfigurationProperties.getInt("computeserver.dispatch.prefetchSize");

    @Resource
    private MessageDrivenContext mdctx;

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
        ComputeBeanLocal computeBean = (ComputeBeanLocal) mdctx.lookup(EJBFactory.LOCAL_COMPUTE_JNDI_NAME);
        JobControlBeanLocal jobBean = (JobControlBeanLocal) mdctx.lookup(EJBFactory.LOCAL_JOB_CONTROL_JNDI_NAME);
        for (DispatcherJob job : jobBean.nextPendingJobs(currentProcessingId, maxRetries, prefetchSize)) {
            LOG.info("Submit job {}", job.getDispatchId());
            try {
                computeBean.submitJob(job.getProcessDefnName(), job.getDispatchedTaskId());
                job.setDispatchStatus(DispatcherJob.Status.SUBMITTED);
                updateJob(jobBean, job);
            } catch (Exception e) {
                job.setDispatchStatus(DispatcherJob.Status.FAILED);
                updateJob(jobBean, job);
                LOG.info("Job {} submission failed", job.getDispatchId(), e);
            }
        }
        LOG.info("Completed dispatching currently queued jobs.");
    }

    private void updateJob(JobControlBeanLocal jobBean, DispatcherJob job) {
        jobBean.updateDispatcherJob(job);
    }
}
