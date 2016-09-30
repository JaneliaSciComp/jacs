
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.io.File;

//@MessageDriven(activationConfig = {
//        // crontTrigger starts with seconds.  Below should run at the stroke of 1 AM EST, every day
//        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 1 * * ?")
//})
//@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class DiskUsageProfilerMDB implements Job {
    private static final Logger log = Logger.getLogger(DiskUsageProfilerMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseScheduler")) {
            return;
        }
        log.info("Waking to profile the filestore disk usage.");
        try {
            String filestoreDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
            String reportsDir = SystemConfigurationProperties.getString("Reports.Dir");
            String diskUsageFilename = SystemConfigurationProperties.getString("DiskUsageFilename");
            SystemCall call = new SystemCall(log);
            call.emulateCommandLine("du --max-depth=1 " + filestoreDir + File.separator + "* > " + reportsDir + File.separator + diskUsageFilename, true);
        }
        catch (Exception e) {
            log.error("There was a problem profiling the disk usage.", e);
        }
        log.info("Filestore disk usage profiling complete.");
    }
}
