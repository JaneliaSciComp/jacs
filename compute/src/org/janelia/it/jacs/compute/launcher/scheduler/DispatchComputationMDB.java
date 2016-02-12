package org.janelia.it.jacs.compute.launcher.scheduler;

import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;

@MessageDriven(
    activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 1 AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 */2 * * * ?")
    }
)
@ResourceAdapter("quartz-ra.rar")
public class DispatchComputationMDB implements Job {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DispatchComputationMDB.class);

    @Resource
    private MessageDrivenContext mdctx;
    @Resource(mappedName = DispatchSettingsMBean.DISPATCHER_SETTINGS_JNDI_NAME)
    private DispatchSettingsMBean dispatchSettings;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        int prefetchSize = dispatchSettings.getPrefetchSize();
        String processingNodeId = dispatchSettings.getCurrentProcessingId();
        if (prefetchSize <= 0 || StringUtils.isBlank(processingNodeId)) {
            LOG.debug("Job dispatcher is disabled.");
            // dispatcher is disabled
            return;
        }
        LOG.debug("Look for queued jobs.");
        int maxRetries = dispatchSettings.getMaxRetries();
        ComputeBeanLocal computeBean = (ComputeBeanLocal) mdctx.lookup(EJBFactory.LOCAL_COMPUTE_JNDI_NAME);
        JobControlBeanLocal jobBean = (JobControlBeanLocal) mdctx.lookup(EJBFactory.LOCAL_JOB_CONTROL_JNDI_NAME);
        for (DispatcherJob job : jobBean.nextPendingJobs(processingNodeId, maxRetries, prefetchSize)) {
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
        LOG.debug("Completed dispatching currently queued jobs.");
    }

    private void updateJob(JobControlBeanLocal jobBean, DispatcherJob job) {
        jobBean.updateDispatcherJob(job);
    }

}
