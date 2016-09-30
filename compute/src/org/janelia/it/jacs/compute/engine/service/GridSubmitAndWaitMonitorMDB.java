
package org.janelia.it.jacs.compute.engine.service;

import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.compute.service.common.grid.submit.GridProcessResult;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.IOUtils;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.inject.Singleton;
import javax.jms.ObjectMessage;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

//@MessageDriven(activationConfig = {
//        // crontTrigger starts with seconds.  Below should run every 1 minutes
//        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0/1 * * * ?")
//})
//@ResourceAdapter("quartz-ra.rar")

/**
 * Created by IntelliJ IDEA.
 * NOTE: This timer service used to clean up every 5 minutes but that was causing Process pipe proliferation problems, so
 * I reduced it to 1 minute and forces Process.destroy() in the GridSubmitHelperMap class.
 *
 * User: lkagan
 * Date: Sep 4, 2009
 * Time: 4:29:34 PM
 *
 */
@Singleton
@Startup
public class GridSubmitAndWaitMonitorMDB implements Job {
    private static final Logger logger = Logger.getLogger(GridSubmitAndWaitMonitorMDB.class);

    @Schedule(second="0", minute="*/1", hour="*")
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Set<String> keySet = GridSubmitHelperMap.getInstance().getDataMapKeys();
        try {
            // iterate over all monitored processes
            for (String submissionKey : keySet) {
                Map<String, Object> submissionData = GridSubmitHelperMap.getInstance().getFromDataMap(submissionKey);
                if (submissionData==null) {
                    logger.warn("Missing submission data for "+submissionKey+". Removing zombie process from map.");
                    GridSubmitHelperMap.getInstance().removeFromDataMap(submissionKey);
                    continue;
                }
                Process proc = (Process) submissionData.get(GridSubmitHelperMap.PROCESS_OBJECT);
                if (proc != null) {
                    // check if it's exited or not
                    try {
                        int exitVal = proc.exitValue();
                        if (exitVal != 0) {
                            // try to get error from error stream
                            InputStream shellErr = proc.getErrorStream(); // this is actual process output!
                            String errorText = IOUtils.readInputStream(shellErr);
                            logger.error("Detected child process exited with error " + exitVal + ". Will attempt to report error.");
                            // error in the proccess - it might not have posted a message - have to do it here
                            GridProcessResult gpr = new GridProcessResult(-1L, false);
                            gpr.setGridSubmissionKey(submissionKey);
                            gpr.setError(errorText);
                            if (!postFinishedMessage(gpr)) {
                                // remove it from the monitoring set - it a ZOMBIE!
                            	logger.trace("Removing zombie process: "+submissionKey);
                                GridSubmitHelperMap.getInstance().removeFromDataMap(submissionKey);
                            }
                        }
                        else {
                        	// This state (exited but still in the map) might be okay for a while, but if a process gets stuck here for a while, 
                        	// it means it has gotten stuck in the queue system. Last time that was caused by not having enough worker threads in the basic pool.
                        }
                    }
                    catch (IllegalThreadStateException e) {
                        // this exception is thrown if the process has not yet completed
                        // perfectly good case for us
                    }
                }
                else {
                    logger.info("Null process entered into monitored set. Removing now");
                    GridSubmitHelperMap.getInstance().removeFromDataMap(submissionKey);
                }
            }
        }
        catch (Exception e) {
            logger.error("Error in child process monitor", e);
        }
        if (null != keySet && keySet.size() > 0) {
            logger.info("Child process monitor scanned " + keySet.size() + " processes");
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("Process(es): ");
            for (String s : keySet) {
                sbuf.append(s).append(", ");
            }
            logger.info(sbuf.toString());
        }
    }

    private boolean postFinishedMessage(GridProcessResult gpr) {
        try {
            String queueName = SystemConfigurationProperties.getString("ComputeServer.GridSubmitter.ReturnQueue");
            AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
            messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
            ObjectMessage msg = messageInterface.createObjectMessage();
            msg.setObject(gpr);
            messageInterface.sendMessageWithinTransaction(msg);
            messageInterface.commit();
            messageInterface.endMessageSession();
        }
        catch (Exception e) {
            logger.error("ZOMBIE PROCESS: Unable to post return message.", e);
            return false;
        }
        return true;
    }
}
