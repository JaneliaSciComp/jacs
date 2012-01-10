
package org.janelia.it.jacs.compute.ws;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 20, 2009
 * Time: 11:15:13 AM
 */
public class BaseWSBean {
    protected Logger logger = Logger.getLogger(this.getClass());

    protected String saveAndSubmitJobWithoutValidation(Task newTask, String processName) {
        return saveAndSubmitJob(newTask, processName, false);
    }

    protected String saveAndSubmitJob(Task newTask, String processName) {
        return saveAndSubmitJob(newTask, processName, true);
    }

    protected String saveAndSubmitJob(Task newTask, String processName, boolean validate) {
        StringBuffer sbuf = new StringBuffer("");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        logger.info("saveAndSubmitJob: start");
        try {
            // Be sure to validate the task parameters, if necessary
            if (!validate || validTaskSettings(newTask, sbuf)) {
                logger.info("saveAndSubmitJob: In validation block");
                // Save the job before running
                newTask = computeBean.saveOrUpdateTask(newTask);
                logger.info("saveAndSubmitJob: submitting job");
                // Fire off the process
                computeBean.submitJob(processName, newTask.getObjectId());
                logger.info("saveAndSubmitJob: Adding newTask id=" + newTask.getObjectId());
                sbuf.append("Job Id: ").append(newTask.getObjectId().toString()).append("\n");
            }
            logger.info("saveAndSubmitJob: finished validation block");
        }
        catch (Exception e) {
            e.printStackTrace();
            String error = "There was a problem running the job via the web service.\nContact an administrator.";
            sbuf = new StringBuffer(error);
            logTaskError(computeBean, e, newTask.getObjectId(), error);
        }
        logger.info("saveAndSubmitJob: end");
        return sbuf.toString();
    }

    private boolean validTaskSettings(Task newTask, StringBuffer sbuf) {
        // Add general checks here - be naturally inclusive
        boolean taskIsValid = true;
        try {
            if (SystemConfigurationProperties.getBoolean("Grid.RequiresProjectCode")) {
                boolean projectCodeIsGood;
                String tmpProjectCode = newTask.getParameter(Task.PARAM_project);
                // Check the values
                projectCodeIsGood = (EJBFactory.getRemoteComputeBean().getProjectCodes().contains(tmpProjectCode));

                if (!projectCodeIsGood) {
                    sbuf.append(tmpProjectCode).append(" - is not a valid project code.\n");
                    taskIsValid = false;
                }
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            String error = "There was a problem running the job via the web service.\nContact an administrator.";
            sbuf.append(error);
            taskIsValid = false;
        }

        return taskIsValid;
    }

    protected void logTaskError(ComputeBeanRemote computeBean, Throwable e, long taskId, String error) {
        System.err.println(error);
        e.printStackTrace();
        try {
            computeBean.saveEvent(taskId, Event.ERROR_EVENT, error, new Date());
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    protected void parameterHelper(Task task, String key, String value) {
        if (value != null && !value.trim().equals("")) {
            task.setParameter(key, value);
        }
    }

}
